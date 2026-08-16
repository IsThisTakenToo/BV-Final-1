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
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
object DriveSyncManager {
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val BACKUP_FILE_NAME = "droppinvault_backup.zip"
    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

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
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
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
            .addOnFailureListener { e -> cont.resumeWithException(e) }
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
                val token = result.accessToken
                if (result.hasResolution() || token == null) {
                    cont.resume(Result.failure(IllegalStateException("Drive access needs re-authorization; can't do that from the background")))
                } else {
                    cont.resume(Result.success(token))
                }
            }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
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
        prefs: SharedPreferences,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, "drive_upload_${System.currentTimeMillis()}.zip")
            try {
                VaultBackupManager.exportBackup(context, dao, vehicleDao, spotPhotoDao, prefs, Uri.fromFile(tempFile))
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
                val fingerprint = VaultBackupManager.computeFingerprint(dao, vehicleDao, spotPhotoDao, prefs)
                prefs.edit()
                    .putLong("drive_last_backup_success", System.currentTimeMillis())
                    .putString("drive_last_backup_fingerprint", fingerprint)
                    .apply()
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun downloadAndRestore(
        context: Context,
        db: AppDatabase,
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        prefs: SharedPreferences,
        accessToken: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val fileId = findBackupFileId(accessToken) ?: error("No backup found in Google Drive")
            val tempFile = File(context.cacheDir, "drive_restore_${System.currentTimeMillis()}.zip")
            try {
                downloadFile(accessToken, fileId, tempFile)
                // First-run restore only: an empty vault is being hydrated from the user's own
                // prior backup, not merged with anything — same "replace" semantics the manual
                // Import Backup flow uses when the user explicitly chooses to replace.
                VaultBackupManager.importBackup(context, db, dao, vehicleDao, spotPhotoDao, prefs, Uri.fromFile(tempFile), replaceExisting = true)
                    .getOrThrow()
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun findBackupFileId(accessToken: String): String? = findAllBackupFileIds(accessToken).firstOrNull()

    /** All files named [BACKUP_FILE_NAME] in the appDataFolder — normally just one, but a prior
     * interrupted upload (before the upload-then-delete reordering in [uploadBackup]) could have
     * left duplicates behind, silently eating into the user's Drive quota forever. Returning every
     * match lets [uploadBackup] clean all of them up, not just the first one found. */
    private fun findAllBackupFileIds(accessToken: String): List<String> {
        val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=${Uri.encode("name = '$BACKUP_FILE_NAME' and trashed = false")}&fields=${Uri.encode("files(id,name)")}"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $accessToken").get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive list failed: ${response.code}")
            val files = JSONObject(response.body?.string().orEmpty()).optJSONArray("files") ?: JSONArray()
            return List(files.length()) { i -> files.getJSONObject(i).getString("id") }
        }
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
        val localVaultIsEmpty = withContext(Dispatchers.IO) { dao.getAllHistoryIncludingDeleted().isEmpty() }
        when {
            hasBackup && !localVaultIsEmpty -> return@runCatching ConnectOutcome.ConflictFound(email, token)
            hasBackup -> {
                val restoredCount = downloadAndRestore(activity, db, dao, vehicleDao, spotPhotoDao, prefs, token).getOrThrow()
                finishConnecting(activity, prefs, email)
                ConnectOutcome.Connected(email, restoredCount)
            }
            else -> {
                // Nothing to restore — push what's on the device now instead of waiting up to 7
                // days for the periodic worker, so "connected" actually means "backed up" now.
                uploadBackup(activity, dao, vehicleDao, spotPhotoDao, prefs, token).getOrThrow()
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
        val restoredCount = when (choice) {
            ConflictChoice.RESTORE_FROM_DRIVE ->
                downloadAndRestore(context, db, dao, vehicleDao, spotPhotoDao, prefs, accessToken).getOrThrow()
            ConflictChoice.OVERWRITE_DRIVE_BACKUP -> {
                uploadBackup(context, dao, vehicleDao, spotPhotoDao, prefs, accessToken).getOrThrow()
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
        uploadBackup(activity, db.locationDao(), db.vehicleDao(), db.spotPhotoDao(), prefs, token).getOrThrow()
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
            destination.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }
}
