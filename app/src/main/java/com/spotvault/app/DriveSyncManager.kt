package com.spotvault.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Free, automated cloud backup using Drive's hidden `appDataFolder` — a per-user storage space
 * invisible in the user's normal Drive UI, counted against their own quota, and readable only by
 * this app. This deliberately reuses [VaultBackupManager]'s existing JSON+photos zip (the same
 * format the manual "Export Vault Backup" button produces) rather than zipping raw SQLite files:
 * that avoids any risk of backing up a `.db` file mid-write, and restores go back through Room's
 * own DAOs instead of overwriting database files directly.
 */
/** Drive access needs an interactive consent screen to refresh — cannot be resolved silently in
 * the background. See [DriveSyncManager.silentAccessToken]. */
class DriveReauthRequiredException : Exception("Drive access needs re-authorization; can't do that from the background")

object DriveSyncManager {
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val BACKUP_FILE_NAME = "droppinvault_backup.zip"

    // uploadBackup's own cleanup pass lists every remote backup file and deletes everything but
    // the id it just created — with no coordination, two calls overlapping (the weekly
    // DriveAutoBackupWorker firing while the user taps Settings' "Back Up Now") can each delete
    // the *other* call's freshly-uploaded file in the same pass, leaving zero backups even though
    // both calls report success. This only serializes calls within this process — it can't help
    // if two different devices signed into the same account happen to back up at once — but that
    // is a far rarer overlap than the same app racing its own worker.
    private val backupMutex = Mutex()
    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    /** Google recommends resumable above ~5MB; below that multipart is one RTT and fine. */
    private const val RESUMABLE_UPLOAD_THRESHOLD_BYTES = 5L * 1024 * 1024
    /** Must be a multiple of 256 KiB per Drive resumable-upload rules. */
    private const val RESUMABLE_CHUNK_BYTES = 8 * 1024 * 1024
    private const val RESUMABLE_CHUNK_RETRIES = 3

    // Web-application-type OAuth client ID from Google Cloud Console. Not a secret — Google's own
    // Credential Manager / Authorization Client docs have this embedded directly in app code,
    // the same way a package name is; the actual access token never touches this ID.
    private const val WEB_CLIENT_ID = "259595149223-evb819a81fjtvibcvqvo983466jumhpq.apps.googleusercontent.com"

    // Defaults (10s connect/read/write) are too tight for a multi-MB, photo-heavy backup on
    // spotty Wi-Fi or a weak cellular signal — a single stall over 10s would hard-fail the whole
    // upload/download with no retry. Read/write get the longer allowance since those are the
    // legs that actually carry the zip; connect stays shorter since a slow connect usually means
    // the request should just fail fast instead.
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    // Drive answers incomplete chunk uploads with HTTP 308 Resume Incomplete. OkHttp treats 308
    // as a permanent redirect by default and would mis-handle the session URI — keep redirects off
    // for the PUT loop only.
    private val resumableHttpClient by lazy {
        httpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    sealed interface AuthOutcome {
        data class Authorized(val accessToken: String) : AuthOutcome
        data class NeedsConsent(val intentSender: IntentSender) : AuthOutcome
    }

    /** The two ways a user can resolve finding a Drive backup that doesn't obviously belong to
     * an empty device — see [ConnectOutcome.ConflictFound]. */
    enum class ConflictChoice { RESTORE_FROM_DRIVE, OVERWRITE_DRIVE_BACKUP }

    /** Result of [connectAndSync] once sign-in and scope authorization have both succeeded. */
    sealed interface ConnectOutcome {
        /** Either nothing to restore (fresh account) or a same-device reconnect that already
         * resolved automatically — [restoredCount] is null unless an existing backup was restored. */
        data class Connected(val email: String, val restoredCount: Int?) : ConnectOutcome

        /** A backup already exists in this Drive account AND this device already has local spots
         * of its own — restoring would discard the device's data, uploading would discard the
         * Drive backup, so which one wins has to be the user's call, not a silent default. The
         * caller must resolve this via [resolveConflict] with the same [accessToken] before
         * "drive_connected" is considered true — nothing has been written to Drive or to prefs
         * yet at this point. */
        data class ConflictFound(val email: String, val accessToken: String) : ConnectOutcome
    }

    /** Step 1 of connecting: Credential Manager's account picker. Only used to confirm which
     * Google account the user wants to use and show their email in the UI — the Drive scope
     * itself is granted separately in [authorizeDriveAccess], since Credential Manager handles
     * identity, not incremental scope grants. */
    suspend fun signIn(activity: Activity): Result<String> = runCatching {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = try {
            CredentialManager.create(activity).getCredential(activity, request)
        } catch (e: GetCredentialException) {
            throw IllegalStateException("Google sign-in was cancelled or failed: ${e.message}", e)
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        googleCredential.id
    }

    /** Step 2: request (or silently refresh) an access token scoped strictly to
     * drive.appdata. If Play Services already has this scope granted for the signed-in account —
     * true for every call after the first, including from [DriveAutoBackupWorker], which has no
     * UI to show a consent screen from — this returns [AuthOutcome.Authorized] directly. Only the
     * very first grant (or one the user has revoked) needs [AuthOutcome.NeedsConsent] resolved
     * via an activity-result launcher. */
    suspend fun authorizeDriveAccess(activity: Activity): AuthOutcome = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
            .build()
        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result: AuthorizationResult ->
                if (!cont.isActive) return@addOnSuccessListener
                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent == null) {
                        cont.resumeWithException(IllegalStateException("Drive authorization needs consent but no resolution was provided"))
                    } else {
                        cont.resume(AuthOutcome.NeedsConsent(pendingIntent.intentSender))
                    }
                } else {
                    val token = result.accessToken
                    if (token == null) {
                        cont.resumeWithException(IllegalStateException("Drive authorization succeeded without an access token"))
                    } else {
                        cont.resume(AuthOutcome.Authorized(token))
                    }
                }
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
    }

    /** Same idea, but for a background [Context] with no Activity — used by
     * [DriveAutoBackupWorker]. Only ever succeeds silently (no UI); if Play Services can't
     * refresh without showing a consent screen, this fails and the worker just skips that run. */
    suspend fun silentAccessToken(context: Context): Result<String> = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
            .build()
        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { result: AuthorizationResult ->
                if (!cont.isActive) return@addOnSuccessListener
                val token = result.accessToken
                if (result.hasResolution() || token == null) {
                    // Distinct type (not a plain IllegalStateException) so DriveAutoBackupWorker can
                    // tell "this account's Drive consent needs an interactive re-grant" — persistent
                    // until the user reconnects — apart from an ordinary transient failure (network
                    // blip, Play Services hiccup) that's worth silently retrying next week instead.
                    cont.resume(Result.failure(DriveReauthRequiredException()))
                } else {
                    cont.resume(Result.success(token))
                }
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }

    /** Finishes the consent step started by [AuthOutcome.NeedsConsent] once the launcher
     * delivers a result. */
    fun accessTokenFromConsentResult(activity: Activity, data: Intent?): Result<String> = runCatching {
        val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        result.accessToken ?: error("Drive consent completed without an access token")
    }

    /** True if a backup zip is already sitting in this account's appDataFolder — used right
     * after connecting during onboarding to decide whether to offer a restore. */
    suspend fun hasRemoteBackup(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { findBackupFileId(accessToken) != null }
    }

    suspend fun uploadBackup(
        context: Context,
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        prefs: SharedPreferences,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        backupMutex.withLock { runCatching {
            val tempFile = File(context.cacheDir, "drive_upload_${System.currentTimeMillis()}.zip")
            try {
                VaultBackupManager.exportBackup(context, dao, vehicleDao, spotPhotoDao, tagDao, prefs, Uri.fromFile(tempFile))
                    .getOrThrow()
                // Upload the new backup *before* touching the old one — deleting first and
                // uploading second would leave a window where a dropped connection or interrupted
                // upload wipes the account's only backup and replaces it with nothing. Each Drive
                // upload creates a new file rather than overwriting by name, so old copies (the
                // previous backup, plus any orphaned duplicates left by earlier interruptions) are
                // only removed once the new one is confirmed to exist.
                val newFileId = uploadFile(accessToken, tempFile)
                // Best-effort cleanup: the new backup already exists at this point, which is the
                // part that actually matters, so a failure removing an old/duplicate copy (a
                // network hiccup, a stale id that's already gone) shouldn't turn a successful
                // backup into a reported failure — that just means one extra file sits in the
                // appDataFolder until the next successful upload cleans it up instead.
                runCatching {
                    findAllBackupFileIds(accessToken)
                        .filter { it != newFileId }
                        .forEach { staleId -> deleteFile(accessToken, staleId) }
                }
                // Written here, once, so every caller (onboarding's first connect, Settings'
                // manual "Back Up Now", and the weekly DriveAutoBackupWorker) gets a correct
                // "Last backup" timestamp — previously only the Worker persisted this, so a
                // manual upload looked successful in the moment but silently reverted to "No
                // backup uploaded yet" the next time Settings was reopened.
                val fingerprint = VaultBackupManager.computeFingerprint(dao, vehicleDao, spotPhotoDao, tagDao, prefs)
                prefs.edit()
                    .putLong("drive_last_backup_success", System.currentTimeMillis())
                    .putString("drive_last_backup_fingerprint", fingerprint)
                    .apply()
            } finally {
                tempFile.delete()
            }
        } }
    }

    suspend fun downloadAndRestore(
        context: Context,
        db: AppDatabase,
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        prefs: SharedPreferences,
        accessToken: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        backupMutex.withLock { runCatching {
            val fileId = findBackupFileId(accessToken) ?: error("No backup found in Google Drive")
            val tempFile = File(context.cacheDir, "drive_restore_${System.currentTimeMillis()}.zip")
            try {
                downloadFile(accessToken, fileId, tempFile)
                // First-run restore only: an empty vault is being hydrated from the user's own
                // prior backup, not merged with anything — same "replace" semantics the manual
                // Import Backup flow uses when the user explicitly chooses to replace.
                VaultBackupManager.importBackup(context, db, dao, vehicleDao, spotPhotoDao, tagDao, prefs, Uri.fromFile(tempFile), replaceExisting = true)
                    .getOrThrow()
            } finally {
                tempFile.delete()
            }
        } }
    }

    private fun findBackupFileId(accessToken: String): String? = findAllBackupFileIds(accessToken).firstOrNull()

    /** All files named [BACKUP_FILE_NAME] in the appDataFolder — normally just one, but a prior
     * interrupted upload (before the upload-then-delete reordering in [uploadBackup]) could have
     * left duplicates behind, silently eating into the user's Drive quota forever. Returning every
     * match lets [uploadBackup] clean all of them up, not just the first one found. */
    private fun findAllBackupFileIds(accessToken: String): List<String> {
        // Paginate — after years of interrupted uploads, stale droppinvault_backup.zip copies can
        // span more than one Drive list page; without pageToken those orphans never get deleted
        // and silently eat appDataFolder quota.
        // orderBy modifiedTime desc so findBackupFileId() / restore always pick the newest zip.
        val ids = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append(DRIVE_FILES_URL)
                append("?spaces=appDataFolder")
                append("&pageSize=100")
                append("&q=${Uri.encode("name = '$BACKUP_FILE_NAME' and trashed = false")}")
                append("&orderBy=${Uri.encode("modifiedTime desc")}")
                append("&fields=${Uri.encode("nextPageToken,files(id,name,modifiedTime)")}")
                if (!pageToken.isNullOrBlank()) {
                    append("&pageToken=${Uri.encode(pageToken)}")
                }
            }
            val request = Request.Builder().url(url).header("Authorization", "Bearer $accessToken").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Drive list failed: ${response.code}")
                val body = JSONObject(response.body?.string().orEmpty())
                val files = body.optJSONArray("files") ?: JSONArray()
                for (i in 0 until files.length()) {
                    ids.add(files.getJSONObject(i).getString("id"))
                }
                pageToken = body.optString("nextPageToken", "").ifBlank { null }
            }
        } while (pageToken != null)
        return ids
    }

    private fun deleteFile(accessToken: String, fileId: String) {
        val request = Request.Builder()
            .url("$DRIVE_FILES_URL/$fileId")
            .header("Authorization", "Bearer $accessToken")
            .delete()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) error("Drive delete failed: ${response.code}")
        }
    }

    /** Returns the newly-created file's Drive id, so the caller can tell it apart from any other
     * (old or orphaned) file sharing the same [BACKUP_FILE_NAME] when cleaning those up. */
    private fun uploadFile(accessToken: String, file: File): String {
        // Years of photos make a 100–512MB zip common. Multipart must restart from byte 0 after
        // any stall; resumable uploads 8MB chunks and can query the session after a blip.
        return if (file.length() <= RESUMABLE_UPLOAD_THRESHOLD_BYTES) {
            uploadFileMultipart(accessToken, file)
        } else {
            uploadFileResumable(accessToken, file)
        }
    }

    private fun uploadFileMultipart(accessToken: String, file: File): String {
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("parents", JSONArray().put("appDataFolder"))
        }
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                Headers.headersOf("Content-Type", "application/zip"),
                file.asRequestBody("application/zip".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=${Uri.encode("id")}")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Drive upload failed: ${response.code} $responseText")
            return JSONObject(responseText).getString("id")
        }
    }

    private fun uploadFileResumable(accessToken: String, file: File): String {
        val total = file.length()
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("parents", JSONArray().put("appDataFolder"))
        }.toString()
        val initRequest = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=resumable&fields=${Uri.encode("id")}")
            .header("Authorization", "Bearer $accessToken")
            .header("X-Upload-Content-Type", "application/zip")
            .header("X-Upload-Content-Length", total.toString())
            .header("Content-Type", "application/json; charset=UTF-8")
            .post(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()
        val sessionUri = httpClient.newCall(initRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("Drive resumable init failed: ${response.code} ${response.body?.string().orEmpty()}")
            }
            response.header("Location")
                ?: error("Drive resumable init missing Location header")
        }

        var offset = 0L
        var failures = 0
        while (offset < total) {
            val end = minOf(offset + RESUMABLE_CHUNK_BYTES, total) - 1
            val length = (end - offset + 1).toInt()
            val putRequest = Request.Builder()
                .url(sessionUri)
                .header("Content-Range", "bytes $offset-$end/$total")
                .put(fileSliceRequestBody(file, offset, length))
                .build()
            try {
                resumableHttpClient.newCall(putRequest).execute().use { response ->
                    when (response.code) {
                        200, 201 -> {
                            val responseText = response.body?.string().orEmpty()
                            return JSONObject(responseText).getString("id")
                        }
                        308 -> {
                            val range = response.header("Range")
                            if (range.isNullOrBlank()) {
                                // Google: no Range means retry from the same offset — don't assume
                                // the whole chunk landed.
                                failures++
                                if (failures > RESUMABLE_CHUNK_RETRIES) {
                                    error("Drive resumable stalled without a Range header")
                                }
                            } else {
                                failures = 0
                                offset = nextOffsetFromRangeHeader(range)
                            }
                        }
                        else -> {
                            val body = response.body?.string().orEmpty()
                            throw IllegalStateException(
                                "Drive resumable chunk failed: ${response.code} $body"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                failures++
                if (failures > RESUMABLE_CHUNK_RETRIES) throw e
                // Ask the session how far it got so a mid-chunk kill does not restart the zip.
                when (val progress = queryResumableProgress(sessionUri, total)) {
                    is ResumableProgress.Complete -> return progress.fileId
                    is ResumableProgress.Incomplete -> offset = progress.nextOffset
                }
            }
        }
        error("Drive resumable upload finished without a file id")
    }

    private sealed class ResumableProgress {
        data class Complete(val fileId: String) : ResumableProgress()
        data class Incomplete(val nextOffset: Long) : ResumableProgress()
    }

    /** Streams [length] bytes from [file] starting at [offset] — never buffers a whole 8MB chunk. */
    private fun fileSliceRequestBody(file: File, offset: Long, length: Int): RequestBody {
        val mediaType = "application/zip".toMediaType()
        return object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength() = length.toLong()
            override fun writeTo(sink: BufferedSink) {
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(offset)
                    val buffer = ByteArray(64 * 1024)
                    var remaining = length
                    while (remaining > 0) {
                        val read = raf.read(buffer, 0, minOf(buffer.size, remaining))
                        if (read <= 0) error("Drive upload could not read zip at offset $offset")
                        sink.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
        }
    }

    /** Range header is `bytes=0-N` (inclusive end). */
    private fun nextOffsetFromRangeHeader(rangeHeader: String): Long {
        val end = rangeHeader.substringAfterLast('-', missingDelimiterValue = "")
            .trim()
            .toLongOrNull()
            ?: error("Drive resumable Range unreadable: $rangeHeader")
        return end + 1
    }

    private fun queryResumableProgress(sessionUri: String, total: Long): ResumableProgress {
        val statusRequest = Request.Builder()
            .url(sessionUri)
            .header("Content-Range", "bytes */$total")
            .put(ByteArray(0).toRequestBody(null))
            .build()
        resumableHttpClient.newCall(statusRequest).execute().use { response ->
            return when (response.code) {
                200, 201 -> {
                    val responseText = response.body?.string().orEmpty()
                    ResumableProgress.Complete(JSONObject(responseText).getString("id"))
                }
                308 -> {
                    val range = response.header("Range")
                    if (range.isNullOrBlank()) {
                        ResumableProgress.Incomplete(0L)
                    } else {
                        ResumableProgress.Incomplete(nextOffsetFromRangeHeader(range))
                    }
                }
                else -> error(
                    "Drive resumable status failed: ${response.code} ${response.body?.string().orEmpty()}"
                )
            }
        }
    }

    /** Full "connect" flow shared by the onboarding step and the Settings entry point, so sign-in
     * + scope authorization + restore-or-first-upload only has one implementation. [resolveConsent]
     * is supplied by the caller since it needs an activity-result launcher, which can only be
     * registered by whatever Activity/Composable owns this call — [DriveSyncManager] itself has
     * no UI. Returns a [ConnectOutcome] — either already-finished [ConnectOutcome.Connected], or
     * [ConnectOutcome.ConflictFound] if the caller needs to resolve a same-account/non-empty-device
     * conflict via [resolveConflict] before the connection actually finishes. */
    suspend fun connectAndSync(
        activity: Activity,
        db: AppDatabase,
        prefs: SharedPreferences,
        resolveConsent: suspend (IntentSender) -> Intent?
    ): Result<ConnectOutcome> = runCatching {
        val email = signIn(activity).getOrThrow()
        val authOutcome = authorizeDriveAccess(activity)
        val token = when (authOutcome) {
            is AuthOutcome.Authorized -> authOutcome.accessToken
            is AuthOutcome.NeedsConsent -> {
                val data = resolveConsent(authOutcome.intentSender)
                accessTokenFromConsentResult(activity, data).getOrThrow()
            }
        }

        val dao = db.locationDao()
        val vehicleDao = db.vehicleDao()
        val spotPhotoDao = db.spotPhotoDao()
        val tagDao = db.tagDao()
        // Deliberately getOrThrow(), not getOrDefault(false): this check decides whether to
        // restore or upload, and a transient failure here (a network blip right after the OAuth
        // consent screen closes, a momentary 5xx from Drive's list endpoint) is NOT the same
        // thing as "no backup exists." Defaulting to false on a failed check used to mean a
        // flaky connection on a fresh, empty-vault phone would upload a blank backup and then
        // have uploadBackup's own stale-file cleanup delete the user's real prior backup outright
        // — a network hiccup silently destroying the one thing this whole feature exists to
        // protect. Propagating the failure instead just fails the connect attempt cleanly, so the
        // user sees "couldn't connect, try again" and can retry once the connection is stable.
        val hasBackup = hasRemoteBackup(token).getOrThrow()
        // this app has no per-account isolation, so if the signed-in Google account happens to
        // already have a backup sitting in appDataFolder (a prior install, a different phone, or
        // simply this exact feature having been exercised once before) while THIS device already
        // has real local spots, neither "restore over the device" nor "upload over the Drive
        // backup" is safe to pick automatically — either one can silently throw away real data.
        // That case is punted to the caller as ConflictFound instead of being decided here.
        val localVaultIsEmpty = withContext(Dispatchers.IO) { dao.countAllSpotsIncludingDeleted() == 0 }
        when {
            hasBackup && !localVaultIsEmpty -> return@runCatching ConnectOutcome.ConflictFound(email, token)
            hasBackup -> {
                val restoredCount = downloadAndRestore(activity, db, dao, vehicleDao, spotPhotoDao, tagDao, prefs, token).getOrThrow()
                finishConnecting(activity, prefs, email)
                ConnectOutcome.Connected(email, restoredCount)
            }
            else -> {
                // Nothing to restore — push what's on the device now instead of waiting up to 7
                // days for the periodic worker, so "connected" actually means "backed up" now.
                uploadBackup(activity, dao, vehicleDao, spotPhotoDao, tagDao, prefs, token).getOrThrow()
                finishConnecting(activity, prefs, email)
                ConnectOutcome.Connected(email, null)
            }
        }
    }

    /** Finishes a connect that [connectAndSync] left pending as [ConnectOutcome.ConflictFound] —
     * called once the user has explicitly picked a side. Nothing was written to Drive or to
     * prefs before this point, so an app kill/crash between the conflict prompt and the user's
     * choice just leaves the account not-yet-connected rather than in a half-resolved state. */
    suspend fun resolveConflict(
        context: Context,
        db: AppDatabase,
        prefs: SharedPreferences,
        email: String,
        accessToken: String,
        choice: ConflictChoice
    ): Result<Int?> = runCatching {
        val dao = db.locationDao()
        val vehicleDao = db.vehicleDao()
        val spotPhotoDao = db.spotPhotoDao()
        val tagDao = db.tagDao()
        val restoredCount = when (choice) {
            ConflictChoice.RESTORE_FROM_DRIVE ->
                downloadAndRestore(context, db, dao, vehicleDao, spotPhotoDao, tagDao, prefs, accessToken).getOrThrow()
            ConflictChoice.OVERWRITE_DRIVE_BACKUP -> {
                uploadBackup(context, dao, vehicleDao, spotPhotoDao, tagDao, prefs, accessToken).getOrThrow()
                null
            }
        }
        finishConnecting(context, prefs, email)
        restoredCount
    }

    private fun finishConnecting(context: Context, prefs: SharedPreferences, email: String) {
        prefs.edit()
            .putBoolean("drive_connected", true)
            .putString("drive_account_email", email)
            .apply()
        scheduleDriveAutoBackup(context)
    }

    /** For a re-trigger of an *already-connected* account — Settings' "Back Up Now" button.
     * Deliberately NOT [connectAndSync]: that function checks whether a backup already exists in
     * Drive and, if so, downloads and restores it (correct for a first-time connect, where the
     * local vault is what needs hydrating). Reusing it here would mean tapping "Back Up Now" a
     * second time — after the very first connect already created a Drive backup — would silently
     * overwrite the current local Vault with that old backup instead of pushing what's on the
     * device now, discarding anything saved since. This always uploads, never restores, and skips
     * the Credential Manager account picker too, since [authorizeDriveAccess] can silently reuse
     * the existing grant without any UI in the common case. */
    suspend fun backUpNow(
        activity: Activity,
        db: AppDatabase,
        prefs: SharedPreferences,
        resolveConsent: suspend (IntentSender) -> Intent?
    ): Result<Unit> = runCatching {
        val authOutcome = authorizeDriveAccess(activity)
        val token = when (authOutcome) {
            is AuthOutcome.Authorized -> authOutcome.accessToken
            is AuthOutcome.NeedsConsent -> {
                val data = resolveConsent(authOutcome.intentSender)
                accessTokenFromConsentResult(activity, data).getOrThrow()
            }
        }
        uploadBackup(activity, db.locationDao(), db.vehicleDao(), db.spotPhotoDao(), db.tagDao(), prefs, token).getOrThrow()
    }

    private fun downloadFile(accessToken: String, fileId: String, destination: File) {
        val request = Request.Builder()
            .url("$DRIVE_FILES_URL/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive download failed: ${response.code}")
            val body = response.body ?: error("Drive download returned an empty body")
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            destination.outputStream().use { out ->
                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > VaultBackupManager.MAX_BACKUP_TOTAL_BYTES) {
                            runCatching { destination.delete() }
                            error(
                                "Drive backup is larger than the ${VaultBackupManager.MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB limit"
                            )
                        }
                        out.write(buffer, 0, read)
                    }
                }
            }
        }
    }
}
