package com.spotvault.app

import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Edit

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import android.Manifest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File

@Composable
fun PremiumDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismissRequest)
    val dialogShape = spotVaultDialogShape()
    val maxDialogHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.88f).dp
    val maxContentHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.54f).dp
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        EnsureDialogEdgeToEdge()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
        val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() }
        val leftPad = with(density) { SystemBarInsets.leftPx.toDp() }
        val rightPad = with(density) { SystemBarInsets.rightPx.toDp() }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 24.dp)
                .padding(start = leftPad, top = statusPad, end = rightPad, bottom = navPad)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .adaptiveMaxContentWidth()
                    .heightIn(max = maxDialogHeight)
                    .clip(dialogShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(SpotVaultColors.Elevated, SpotVaultColors.Surface, SpotVaultColors.Deep)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                SpotVaultColors.PrimaryBright.copy(alpha = 0.55f),
                                SpotVaultColors.Teal.copy(alpha = 0.35f),
                                SpotVaultColors.Outline.copy(alpha = 0.3f)
                            )
                        ),
                        shape = dialogShape
                    )
            ) {
                CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides SpotVaultColors.OnSurface) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            SpotVaultColors.PrimaryDeep,
                                            SpotVaultColors.Primary,
                                            SpotVaultColors.Teal,
                                            SpotVaultColors.PrimaryBright
                                        )
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(22.dp)) {
                            title()
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxContentHeight)
                            ) {
                                content()
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dismissButton != null) {
                                    Box(modifier = Modifier.weight(1f, fill = false)) {
                                        dismissButton()
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Box(modifier = Modifier.heightIn(min = 48.dp)) {
                                    confirmButton()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ShareSpotPayload(
    val lat: Double,
    val lng: Double,
    val address: String,
    val notes: String,
    val imagePath: String = ""
)

private const val KEY_PHOTO_FILE_PATH = "photo_file_path"
private const val KEY_PHOTO_URI = "photo_uri"
private const val KEY_TEMP_OCR_TEXT = "temp_ocr_text"
private const val KEY_TEMP_PROMINENT_OCR_TEXT = "temp_prominent_ocr_text"
private const val KEY_DIALOG_SESSION_KEY = "dialog_session_key"
private const val KEY_SHOW_TIMER_DIALOG = "show_timer_dialog"

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : FragmentActivity() {


    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private var pendingTrackedQuickPinOption: QuickPinDef? = null
    private lateinit var prefs: SharedPreferences
    private var pendingActionIsCamera: Boolean? = null
    private var tempOcrTextForDialog: String = ""
    private var tempProminentOcrTextForDialog: String = ""
    private var isProcessingPhoto = mutableStateOf(false)
    private var instantLat: Double = 0.0
    private var instantLng: Double = 0.0

    private val isAppUnlocked = mutableStateOf(true)
    private var shouldRequireUnlockOnResume = false
    private var isShowingBiometricPrompt = false
    private val isPinned = mutableStateOf(false)
    private val showFoundMoment = mutableStateOf(false)
    private val showConfirmationDialog = mutableStateOf<com.spotvault.app.LocationSpot?>(null)
    private val showTimerDialog = mutableStateOf(false)
    private var dialogSessionKey = 0L

    private var pendingWidgetSpotId = -1
    private var pendingWidgetAction: String? = null
    private var pendingWidgetCompassLat: Double? = null
    private var pendingWidgetCompassLng: Double? = null
    private val widgetIntentVersion = mutableStateOf(0)
    private var pendingWidgetSharePhotoFile: File? = null


    private val driveOnboardingState = mutableStateOf<DriveOnboardingState>(DriveOnboardingState.Idle)
    private var driveConsentContinuation: ((Intent?) -> Unit)? = null
    private val driveConsentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val continuation = driveConsentContinuation
        driveConsentContinuation = null
        continuation?.invoke(result.data)
    }

    /** Onboarding's "Connect Google Drive" flow: sign in, request (or reuse) the drive.appdata
     * scope grant, then either restore an existing backup or upload the very first one right
     * away — so "Connected" actually means backed up immediately, not "backed up sometime in the
     * next 7 days" once [DriveAutoBackupWorker] happens to run. A [DriveSyncManager.ConnectOutcome.ConflictFound]
     * pauses here instead of auto-picking a side — [resolveDriveConflict] finishes the connect
     * once the user answers the dialog [WelcomeOnboardingScreen] shows for that state. */
    private fun connectGoogleDrive() {
        driveOnboardingState.value = DriveOnboardingState.Working
        // Same reasoning as GoogleDriveBackupSection's connect() in SettingsCategoryContent.kt —
        // this onboarding version was missing it. Held for the whole call, not just around the
        // consent launcher: the underlying Credential Manager sign-in sheet can take the
        // foreground on its own too, before intentSender is even reached, and App Lock's onStop
        // can't otherwise tell that apart from the user actually leaving the app. Without this, an
        // App-Lock user backgrounded mid-OAuth would come back to AppLockScreen instead of the
        // in-flight connect, which tears down (and loses) the pending driveConsentLauncher result.
        AppLockGate.begin()
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val result = DriveSyncManager.connectAndSync(this@MainActivity, db, prefs) { intentSender ->
                    suspendCancellableCoroutine { cont ->
                        driveConsentContinuation = { data -> cont.resume(data) }
                        driveConsentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                }
                driveOnboardingState.value = result.fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is DriveSyncManager.ConnectOutcome.Connected -> DriveOnboardingState.Connected(outcome.email, outcome.restoredCount)
                            is DriveSyncManager.ConnectOutcome.ConflictFound -> DriveOnboardingState.NeedsConflictResolution(outcome.email, outcome.accessToken)
                        }
                    },
                    onFailure = { DriveOnboardingState.Failed(it.message ?: "Couldn't connect to Google Drive") }
                )
            } finally {
                // finally, not right after the call — an unexpected exception here must still
                // release the gate, or App Lock would never re-lock again for the rest of the
                // session.
                AppLockGate.end()
            }
        }
    }

    private fun resolveDriveConflict(email: String, accessToken: String, choice: DriveSyncManager.ConflictChoice) {
        driveOnboardingState.value = DriveOnboardingState.Working
        // resolveConflict() itself reuses the access token from connectGoogleDrive() and never
        // launches a new consent screen today, so this isn't strictly needed yet — held anyway so
        // a future change to downloadAndRestore/uploadBackup that does need re-consent doesn't
        // silently reintroduce this exact bug by omission.
        AppLockGate.begin()
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainActivity)
                val result = DriveSyncManager.resolveConflict(this@MainActivity, db, prefs, email, accessToken, choice)
                driveOnboardingState.value = result.fold(
                    onSuccess = { restoredCount -> DriveOnboardingState.Connected(email, restoredCount) },
                    onFailure = { DriveOnboardingState.Failed(it.message ?: "Couldn't finish connecting Google Drive") }
                )
            } finally {
                AppLockGate.end()
            }
        }
    }

    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: android.net.Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            prefs.edit().putString("alarm_sound_uri", uri?.toString() ?: "").apply()
            // The 10-minute timer-alert channel is keyed by this sound's hash (see
            // createNotificationChannel/fireTenMinuteAlert) — Android notification channels are
            // immutable once created, so a new sound needs its own new channel id, and that
            // channel has to actually exist before TimerService posts to it later or the OS
            // silently drops the notification instead of showing it.
            createNotificationChannel()
        }
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            extractOcrAndShowDialog()
        } else {
            // pendingTrackedQuickPinOption is only ever cleared inside extractOcrAndShowDialog(),
            // which a cancelled capture never reaches — leaving it set here would silently divert
            // the *next* Snap/Pin (even an unrelated one) into completing this stale tracked
            // quick pin instead of opening its own timer dialog.
            pendingTrackedQuickPinOption = null
            Toast.makeText(this, "Photo cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val widgetSharePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingWidgetSharePhotoFile
        pendingWidgetSharePhotoFile = null
        if (success && file != null && file.exists()) {
            lifecycleScope.launch {
                val located = withContext(Dispatchers.IO) {
                    val coords = resolveCurrentLocation(this@MainActivity, prefs) ?: return@withContext null
                    val (lat, lng) = coords
                    val address = if (prefs.getBoolean("auto_fetch_address", true)) {
                        reverseGeocodeAddress(this@MainActivity, lat, lng).full
                    } else ""
                    Triple(lat, lng, address)
                }
                if (located == null) {
                    Toast.makeText(this@MainActivity, "Location unavailable — enable GPS to share.", Toast.LENGTH_SHORT).show()
                } else {
                    val (lat, lng, address) = located
                    shareLocation(this@MainActivity, lat, lng, address, notes = "", imagePath = file.absolutePath, includePhoto = true)
                }
            }
        } else {
            Toast.makeText(this, "Photo canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val widgetSharePhotoPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchWidgetSharePhotoCamera()
        } else if (isPermissionPermanentlyDenied(this, prefs, Manifest.permission.CAMERA)) {
            showPermissionSettingsDialog(this, "Camera access is needed to share a photo. Enable it for DropPin Vault in Settings.")
        } else {
            Toast.makeText(this, "Camera permission is required to share a photo.", Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingGalleryPhotoTarget: LocationSpot? = null

    private val galleryPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val spot = pendingGalleryPhotoTarget
        pendingGalleryPhotoTarget = null
        if (uri != null && spot != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val imagesDir = File(filesDir, "images").apply { mkdirs() }
                val destFile = File(imagesDir, "gallery_${System.currentTimeMillis()}.jpg")
                // A revoked URI grant, corrupt image, or a full disk (ENOSPC) here used to be
                // uncaught — a crash on Dispatchers.IO for exactly the low-storage devices most
                // likely to hit it — and openInputStream() returning null (stream unavailable)
                // fell through to attaching a file that was never actually written: a spot with
                // a permanently broken photo reference and no error shown either way.
                val copied = try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    destFile.exists() && destFile.length() > 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                if (copied) {
                    compressCapturedPhoto(destFile.absolutePath)
                    attachPhotoToSpot(spot, destFile.absolutePath)
                } else {
                    runCatching { destFile.delete() }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Couldn't add that photo. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun launchGalleryPickerForSpot(spot: LocationSpot) {
        pendingGalleryPhotoTarget = spot
        galleryPhotoLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private var pendingCameraPhotoTarget: LocationSpot? = null
    private var pendingCameraPhotoFile: File? = null

    private val addPhotoCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val spot = pendingCameraPhotoTarget
        val file = pendingCameraPhotoFile
        pendingCameraPhotoTarget = null
        pendingCameraPhotoFile = null
        if (success && spot != null && file != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                compressCapturedPhoto(file.absolutePath)
                attachPhotoToSpot(spot, file.absolutePath)
            }
        } else if (!success) {
            Toast.makeText(this, "Photo cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraForSpot(spot: LocationSpot) {
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val newFile = File(imagesDir, "spot_photo_${System.currentTimeMillis()}.jpg")
        pendingCameraPhotoTarget = spot
        pendingCameraPhotoFile = newFile
        val newUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", newFile)
        // Camera is optional hardware (see the manifest's <uses-feature required="false">) — a
        // device that installed anyway with no camera app has nothing to resolve TakePicture()'s
        // ACTION_IMAGE_CAPTURE, and launch() throws synchronously rather than failing through the
        // result callback.
        try {
            addPhotoCameraLauncher.launch(newUri)
        } catch (e: android.content.ActivityNotFoundException) {
            pendingCameraPhotoTarget = null
            pendingCameraPhotoFile = null
            Toast.makeText(this, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    /** First photo ever added becomes the spot's cover ([LocationSpot.imagePath]); every photo
     * after that is appended to [SpotPhoto] so the detail screen can show all of them. */
    private suspend fun attachPhotoToSpot(spot: LocationSpot, absolutePath: String) {
        val db = AppDatabase.getDatabase(this@MainActivity)
        if (spot.imagePath.isBlank()) {
            db.locationDao().updateSpot(spot.copy(imagePath = absolutePath))
        } else {
            db.spotPhotoDao().insert(SpotPhoto(spotId = spot.id, path = absolutePath))
        }
    }

    fun startTrackedQuickPin(option: QuickPinDef, allowPhoto: Boolean) {
        if (allowPhoto) {
            pendingTrackedQuickPinOption = option
            launchCamera()
        } else {
            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()
                val result = withContext(Dispatchers.IO) {
                    quietSaveTacticalPin(this@MainActivity, dao, prefs, option)
                }
                when (result) {
                    is QuietSaveResult.Saved -> {
                        startPinnedNotificationForSpot(
                            spotId = result.spotId,
                            photoPath = "",
                            label = "${option.emoji} ${option.label}"
                        )
                    }
                    QuietSaveResult.Failed -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not save pin. Try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    QuietSaveResult.NeedsNotificationPermission -> Unit
                }
            }
        }
    }

    private suspend fun completeTrackedQuickPinWithPhoto(option: QuickPinDef, photoPath: String, details: String) {
        val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()
        val result = withContext(Dispatchers.IO) {
            val saveResult = quietSaveTacticalPin(this@MainActivity, dao, prefs, option)
            if (saveResult is QuietSaveResult.Saved && photoPath.isNotEmpty()) {
                val spot = dao.getHistoryList().find { it.id == saveResult.spotId }
                if (spot != null) {
                    dao.updateSpot(spot.copy(imagePath = photoPath, locationDetails = details))
                }
            }
            saveResult
        }
        when (result) {
            is QuietSaveResult.Saved -> {
                startPinnedNotificationForSpot(
                    spotId = result.spotId,
                    photoPath = photoPath,
                    label = "${option.emoji} ${option.label}"
                )
            }
            QuietSaveResult.Failed -> {
                Toast.makeText(
                    this@MainActivity,
                    "Could not save pin. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            QuietSaveResult.NeedsNotificationPermission -> Unit
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val locCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val locGranted = locFineGranted || locCoarseGranted
        
        var notifGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        }

        // See checkPermissionsAndAction's comment — notification permission is requested here
        // when missing, but (deliberately, same reasoning) not required for either branch below.
        if (pendingActionIsCamera == true) {
            if (cameraGranted && locGranted) {
                launchCamera()
            } else {
                // A Toast alone left a user who'd permanently denied either permission
                // permanently stuck — Android will never show the system prompt again once
                // that's happened, so the only way back in is the app's own Settings page.
                val permanentlyDenied = (!cameraGranted && isPermissionPermanentlyDenied(this, prefs, Manifest.permission.CAMERA)) ||
                    (!locGranted && isPermissionPermanentlyDenied(this, prefs, Manifest.permission.ACCESS_FINE_LOCATION))
                if (permanentlyDenied) {
                    showPermissionSettingsDialog(this, "Camera and location access are needed to Snap. Enable them for DropPin Vault in Settings.")
                } else {
                    Toast.makeText(this, "Camera and location permission are required to Snap.", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (pendingActionIsCamera == false) {
            if (locGranted) {
                photoFile = null
                tempOcrTextForDialog = ""
                tempProminentOcrTextForDialog = ""
                dialogSessionKey = System.currentTimeMillis()
                showTimerDialog.value = true
            } else {
                if (isPermissionPermanentlyDenied(this, prefs, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermissionSettingsDialog(this, "Location access is needed to Pin. Enable it for DropPin Vault in Settings.")
                } else {
                    Toast.makeText(this, "Location permission is required to Pin.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingActionIsCamera = null
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            resumePinnedTimerService()
        } else if (::prefs.isInitialized && prefs.getBoolean("is_pinned", false)) {
            Toast.makeText(
                this,
                "Timer alerts need notification permission. Enable it in system Settings to keep tracking visible.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun resumePinnedTimerService() {
        if (!::prefs.isInitialized || !prefs.getBoolean("is_pinned", false)) return
        val resumeIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        try {
            ContextCompat.startForegroundService(this, resumeIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureNotificationPermissionForActivePin() {
        if (!::prefs.isInitialized || !prefs.getBoolean("is_pinned", false)) return
        if (hasNotificationPermission()) {
            resumePinnedTimerService()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Blocks screenshots/screen-recording and swaps the Recent Apps thumbnail for a blank
     * placeholder — without this, App Lock only gated re-entry into the app; anyone with the
     * device could still flip to the app switcher and see the last-rendered Vault/photo content
     * without ever unlocking. Only applied while App Lock is actually on, so a user who doesn't
     * use App Lock keeps the ability to screenshot their own saved spots to share them. */
    private fun applySecureFlag(appLockEnabled: Boolean) {
        if (appLockEnabled) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        AppIconManager.migrateLegacyIconIfNeeded(this, prefs)
        ingestWidgetIntent(intent)
        QuickPinStore.load(prefs)
        isPinned.value = prefs.getBoolean("is_pinned", false)
        ThemeState.currentTheme = loadColorThemeFromPrefs(prefs)
        ThemeState.vaultIconStyle = premiumGatedId(
            prefs,
            when (val saved = prefs.getString("vault_icon_style", "classic") ?: "classic") {
                "gear" -> "maglock"
                else -> saved
            },
            PremiumFreeTier.freeVaultIconStyleId
        )
        if (prefs.getString("vault_icon_style", null) == "gear") {
            prefs.edit().putString("vault_icon_style", "maglock").apply()
        }
        if (prefs.getString("compass_style", null) == "star_chart") {
            prefs.edit().putString("compass_style", CompassStyle.LASER_DOT.id).apply()
        }
        ThemeState.compassStyle = premiumGatedId(
            prefs,
            prefs.getString("compass_style", CompassStyle.CLASSIC.id) ?: CompassStyle.CLASSIC.id,
            PremiumFreeTier.freeCompassStyleId
        )
        ThemeState.buttonStyle = loadButtonStyleFromPrefs(prefs)
        ThemeState.reduceAnimations = prefs.getBoolean("reduce_animations", false)
        ThemeState.customPrimaryArgb = prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())
        ThemeState.customAccentArgb = prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())
        ThemeState.textScale = prefs.getFloat("text_size_scale", 1.15f)
        ThemeState.fontFamilyId = prefs.getString("app_font_family", AppFontOptions.DEFAULT_ID) ?: AppFontOptions.DEFAULT_ID
        ThemeState.backgroundPatternId = premiumGatedId(
            prefs,
            normalizeBackgroundPatternId(prefs.getString("background_pattern", BackgroundPattern.GRADIENT_MESH.id)),
            PremiumFreeTier.freeBackgroundPatternId
        )
        ThemeState.customOnPrimaryArgb = textModeToArgb(prefs.getString("custom_on_primary_mode", "auto") ?: "auto")
        ThemeState.customOnAccentArgb = textModeToArgb(prefs.getString("custom_on_accent_mode", "auto") ?: "auto")
        ThemeState.dynamicColorEnabled = prefs.getBoolean("dynamic_color_enabled", false)
        refreshDynamicColors(applicationContext)
        com.spotvault.app.SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))
        isAppUnlocked.value = !prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)
        applySecureFlag(prefs.getBoolean(APP_LOCK_ENABLED_PREF, false))

        // scheduleDriveAutoBackup() is otherwise only called at the moment of connecting (see
        // DriveSyncManager.finishConnecting) — re-asserting it here on every launch for anyone
        // already connected means a future constraint change (or WorkManager's schedule getting
        // cleared by an aggressive OEM battery manager) self-heals on next app open instead of
        // silently going stale until the user disconnects and reconnects by hand.
        // enqueueUniquePeriodicWork(..., UPDATE) is idempotent, so this is a safe no-op on every
        // launch where nothing actually changed.
        if (prefs.getBoolean("drive_connected", false)) {
            scheduleDriveAutoBackup(this)
        }

        // Restores the Snap/Pin flow across a configuration change (rotation, dark/light system
        // switch, or a resizeableActivity="true" multi-window resize — there's no orientation
        // lock or configChanges override, so all of those recreate this Activity by default).
        // Without this, a photo the camera had already written to disk went silently orphaned —
        // the file stays on disk forever, never attached to anything, with the timer dialog just
        // gone and no trace of what the user was doing.
        savedInstanceState?.let { state ->
            state.getString(KEY_PHOTO_FILE_PATH)?.let { path -> photoFile = File(path) }
            state.getString(KEY_PHOTO_URI)?.let { photoUri = Uri.parse(it) }
            tempOcrTextForDialog = state.getString(KEY_TEMP_OCR_TEXT, "")
            tempProminentOcrTextForDialog = state.getString(KEY_TEMP_PROMINENT_OCR_TEXT, "")
            dialogSessionKey = state.getLong(KEY_DIALOG_SESSION_KEY, 0L)
            showTimerDialog.value = state.getBoolean(KEY_SHOW_TIMER_DIALOG, false)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            migrateLegacyCarVehicleIfNeeded(this@MainActivity, prefs, db.vehicleDao())
        }

        // Every launch, not gated behind a one-time flag — a spot missing city/state (stuck on
        // the "Lat: x, Lng: y" fallback, or an older spot saved before those columns existed)
        // deserves another attempt whenever the app is next opened, not exactly one retry ever
        // for the entire lifetime of the install. See resolveStuckSpotAddresses's own doc for why
        // this replaced the old one-time text-guessing migration — cheap either way, since it
        // only touches rows still genuinely missing city/state, and each one drops out of its own
        // filter the moment it resolves.
        lifecycleScope.launch(Dispatchers.IO) {
            resolveStuckSpotAddresses(this@MainActivity, AppDatabase.getDatabase(this@MainActivity).locationDao())
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.locationDao()
            val spotPhotoDao = db.spotPhotoDao()
            val toPurge = dao.getDeletedOlderThan(cutoff)
            toPurge.forEach { spot ->
                if (spot.imagePath.isNotEmpty()) {
                    runCatching { File(spot.imagePath).delete() }
                }
                // spot_photos rows cascade-delete with the spot below, but that only removes the
                // DB rows — the actual image files on disk are only reachable here, before the
                // purge, or they'd be orphaned forever.
                spotPhotoDao.getForSpot(spot.id).forEach { photo ->
                    runCatching { File(photo.path).delete() }
                }
            }
            dao.purgeDeletedOlderThan(cutoff)
            // Cascades away the cross-refs for every tag those spots carried, but nothing else
            // ever decrements usageCount for a hard-deleted spot — left unrecomputed, every tag's
            // count only ever climbs, never falls, on this exact auto-purge that runs on every
            // single app launch. See TagDao.recomputeAllUsageCounts's own doc for the full picture
            // (this is one of four call sites that needed it).
            if (toPurge.isNotEmpty()) {
                db.tagDao().recomputeAllUsageCounts()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val exportsDir = File(filesDir, "exports")
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            exportsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    runCatching { file.delete() }
                }
            }
        }

        // Sweeps files/images/ for photos no spot (or spot_photos row) actually references —
        // a capture whose save was interrupted (cancelled, a crash, or process death between the
        // camera returning and the timer dialog's Save) leaves its JPEG on disk with nothing ever
        // cleaning it up otherwise, a slow unbounded storage leak on exactly the low-storage
        // devices most likely to have caused the interruption in the first place. 24h old is a
        // safety margin so this can never race a capture that's still legitimately in progress
        // (e.g. sitting in the timer dialog, not yet saved).
        lifecycleScope.launch(Dispatchers.IO) {
            val imagesDir = File(filesDir, "images")
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val candidates = imagesDir.listFiles()?.filter { it.lastModified() < cutoff } ?: emptyList()
            if (candidates.isEmpty()) return@launch

            val db = AppDatabase.getDatabase(this@MainActivity)
            val allSpots = db.locationDao().getAllHistoryIncludingDeleted()
            val spotPhotoDao = db.spotPhotoDao()
            val referencedPaths = buildSet {
                allSpots.forEach { spot ->
                    if (spot.imagePath.isNotEmpty()) add(File(spot.imagePath).absolutePath)
                    spotPhotoDao.getForSpot(spot.id).forEach { photo -> add(File(photo.path).absolutePath) }
                }
                photoFile?.absolutePath?.let { add(File(it).absolutePath) }
            }
            candidates.forEach { file ->
                if (file.absolutePath !in referencedPaths) {
                    runCatching { file.delete() }
                }
            }
        }

        if (prefs.getBoolean("widget_refresh_on_open", true)) {
            WidgetThemeHelper.refreshAllWidgets(this)
        }

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * ThemeState.textScale
                )
            ) {
            // Measured once here, against the main Activity's own root view — the one window
            // guaranteed to receive correct WindowInsets — and shared app-wide via
            // SystemBarInsets so every full-screen Dialog can rely on real bar heights instead
            // of re-querying WindowInsets from inside its own (sometimes unreliable) window.
            val rootView = LocalView.current
            DisposableEffect(rootView) {
                val barTypes = WindowInsetsCompat.Type.statusBars()
                val navTypes = WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.tappableElement() or
                    WindowInsetsCompat.Type.systemGestures()
                // displayCutout() folded into the horizontal read specifically — a camera cutout
                // sits on the top edge in portrait (already covered by barTypes) but moves to a
                // *side* edge in landscape, which neither barTypes nor navTypes' .bottom read ever
                // caught.
                val sideTypes = navTypes or WindowInsetsCompat.Type.displayCutout()
                fun apply(current: WindowInsetsCompat) {
                    SystemBarInsets.statusBarPx = current.getInsets(barTypes).top
                    SystemBarInsets.navigationBarPx = current.getInsets(navTypes).bottom
                    val sides = current.getInsets(sideTypes)
                    SystemBarInsets.leftPx = sides.left
                    SystemBarInsets.rightPx = sides.right
                }
                val listener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
                    apply(insets)
                    insets
                }
                ViewCompat.setOnApplyWindowInsetsListener(rootView, listener)
                ViewCompat.requestApplyInsets(rootView)
                ViewCompat.getRootWindowInsets(rootView)?.let { current -> apply(current) }
                onDispose { ViewCompat.setOnApplyWindowInsetsListener(rootView, null) }
            }
            val splashStyle = remember {
                val saved = SplashStyle.fromId(prefs.getString("splash_style", SplashStyle.DEFAULT.id))
                if (saved.id !in PremiumFreeTier.freeSplashStyleIds && !isPremiumUnlocked(prefs)) SplashStyle.DEFAULT else saved
            }
            var showSplash by remember { mutableStateOf(splashStyle != SplashStyle.NONE) }
            var showWelcome by remember { mutableStateOf(!prefs.getBoolean("has_seen_welcome", false)) }
            val splashAppIcon = remember {
                AppIcon.fromId(AppIconManager.currentIconId(prefs))
            }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(splashStyle.totalDisplayMillis())
                showSplash = false
            }

            // No eager batch permission request here on purpose — checkPermissionsAndAction()
            // already asks for exactly what's missing (camera + location for Snap, location alone
            // for Pin) the moment the user actually taps one of those buttons, and notifications
            // gets its own narrower ask when Active Tracking is actually chosen (see
            // processPhotoAndPin's isActiveTracking check). Firing every permission at once right
            // after onboarding — before the user had done anything — meant up to four system
            // dialogs stacked back to back with no context for why any of them mattered.

            SpotVaultTheme(darkTheme = true) {
                androidx.compose.animation.Crossfade(
                    targetState = showSplash,
                    animationSpec = androidx.compose.animation.core.tween(500),
                    label = "SplashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            style = splashStyle,
                            appIcon = splashAppIcon,
                            onDismiss = { showSplash = false }
                        )
                    } else if (showWelcome) {
                        WelcomeOnboardingScreen(
                            driveState = driveOnboardingState.value,
                            onConnectDrive = { connectGoogleDrive() },
                            onResolveConflict = { choice ->
                                val state = driveOnboardingState.value
                                if (state is DriveOnboardingState.NeedsConflictResolution) {
                                    resolveDriveConflict(state.email, state.accessToken, choice)
                                }
                            },
                            onFinish = {
                                prefs.edit().putBoolean("has_seen_welcome", true).apply()
                                showWelcome = false
                            }
                        )
                    } else {
                        val appLockEnabled = prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)
                        val unlocked = isAppUnlocked.value
                        if (appLockEnabled && !unlocked) {
                            AppLockScreen(
                                onUnlock = { requestAppUnlock() }
                            )
                            LaunchedEffect(appLockEnabled, unlocked) {
                                if (appLockEnabled && !isAppUnlocked.value) {
                                    kotlinx.coroutines.delay(300)
                                    if (appLockEnabled && !isAppUnlocked.value) {
                                        requestAppUnlock()
                                    }
                                }
                            }
                        } else {
                        val widgetVersion = widgetIntentVersion.value
                        val widgetSpotId = remember(widgetVersion) {
                            pendingWidgetSpotId.also { pendingWidgetSpotId = -1 }
                        }
                        val widgetNavRoute = remember(widgetVersion) {
                            when (pendingWidgetAction) {
                                PremiumWidgetIntents.ACTION_PREMIUM -> SpotVaultRoutes.SETTINGS_PREMIUM
                                PremiumWidgetIntents.ACTION_VAULT -> SpotVaultRoutes.VAULT
                                else -> null
                            }
                        }
                        val widgetCompassLat = remember(widgetVersion) { pendingWidgetCompassLat }
                        val widgetCompassLng = remember(widgetVersion) { pendingWidgetCompassLng }

                        LaunchedEffect(widgetVersion, showSplash, showWelcome, isAppUnlocked.value) {
                            if (showSplash || showWelcome) return@LaunchedEffect
                            if (prefs.getBoolean(APP_LOCK_ENABLED_PREF, false) && !isAppUnlocked.value) return@LaunchedEffect
                            when (val action = pendingWidgetAction) {
                                PremiumWidgetIntents.ACTION_SNAP -> {
                                    pendingWidgetAction = null
                                    checkPermissionsAndAction(isCamera = true)
                                }
                                PremiumWidgetIntents.ACTION_PIN_ONLY -> {
                                    pendingWidgetAction = null
                                    checkPermissionsAndAction(isCamera = false)
                                }
                                PremiumWidgetIntents.ACTION_SHARE_PHOTO -> {
                                    pendingWidgetAction = null
                                    startWidgetSharePhoto()
                                }
                                PremiumWidgetIntents.ACTION_PREMIUM -> {
                                    pendingWidgetAction = null
                                }
                                PremiumWidgetIntents.ACTION_COMPASS -> {
                                    pendingWidgetAction = null
                                }
                                PremiumWidgetIntents.ACTION_VAULT -> {
                                    pendingWidgetAction = null
                                }
                                PremiumWidgetIntents.ACTION_NAVIGATE_MAPS -> {
                                    pendingWidgetAction = null
                                    // Same google.navigation walking-mode intent TimerService's
                                    // "Navigate to" notification action used to fire directly —
                                    // deferred to here, behind the App Lock gate this
                                    // LaunchedEffect already sits behind, instead of handing the
                                    // tracked location to Maps straight from a locked device.
                                    val mapUri = android.net.Uri.parse(
                                        "google.navigation:q=$widgetCompassLat,$widgetCompassLng&mode=w"
                                    )
                                    try {
                                        // No setPackage() — see TimerService's identical fix for why
                                        // locking this to Google Maps specifically only narrows
                                        // which devices this can succeed on, with no upside.
                                        startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri)
                                        )
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        Toast.makeText(this@MainActivity, "No navigation app found.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> Unit
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            SpotVaultAmbientBackground()
                            LocationAcquisitionBanner(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .zIndex(10f)
                            )
                            SpotVaultMainScaffold(
                                isPinned = isPinned.value,
                                prefs = prefs,
                                dao = AppDatabase.getDatabase(this@MainActivity).locationDao(),
                                initialNavRoute = widgetNavRoute,
                                initialSpotId = widgetSpotId,
                                initialCompassLat = widgetCompassLat,
                                initialCompassLng = widgetCompassLng,
                                onSnapClick = { checkPermissionsAndAction(isCamera = true) },
                                onPinOnlyClick = { checkPermissionsAndAction(isCamera = false) },
                                onFoundClick = { clearPinnedSpot() },
                                onShareRequest = { payload ->
                                    val hasPhoto = payload.imagePath.isNotEmpty() && File(payload.imagePath).exists()
                                    shareLocation(
                                        this@MainActivity,
                                        payload.lat,
                                        payload.lng,
                                        payload.address,
                                        payload.notes,
                                        payload.imagePath,
                                        includePhoto = hasPhoto
                                    )
                                },
                                onPickRingtone = {
                                    val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Alert Sound")
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        val currentUriStr = prefs.getString("alarm_sound_uri", null)
                                        if (currentUriStr != null && currentUriStr.isNotEmpty()) {
                                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(currentUriStr))
                                        } else {
                                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                                        }
                                    }
                                    // ACTION_RINGTONE_PICKER is near-universal but not guaranteed —
                                    // some stripped-down custom ROMs or kiosk-mode Android devices
                                    // have no Settings app implementing it, and launch() throws
                                    // synchronously (same as startActivity()) rather than failing
                                    // through the result callback.
                                    try {
                                        ringtonePickerLauncher.launch(intent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        Toast.makeText(this@MainActivity, "No sound picker available on this device.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAppLockChanged = { enabled ->
                                    applySecureFlag(enabled)
                                    if (!enabled) {
                                        isAppUnlocked.value = true
                                        shouldRequireUnlockOnResume = false
                                    }
                                },
                                onRequestLocationPermission = {
                                    requestPermissionsLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onClearPinnedSpot = { clearPinnedSpot() },
                                contentOverlays = { bottomInset ->
                    if (showFoundMoment.value) {
                        FoundSplashOverlay(
                            style = loadFoundSplashStyleFromPrefs(prefs),
                            title = loadFoundSplashTitleFromPrefs(prefs),
                            subtitle = loadFoundSplashSubtitleFromPrefs(prefs),
                            onFinished = { showFoundMoment.value = false }
                        )
                    }
                    if (showTimerDialog.value) {
                        androidx.compose.runtime.key(dialogSessionKey) {
                        TimerSelectionDialog(
                            prefs = prefs,
                            isCamera = (photoFile != null),
                            photoPath = photoFile?.absolutePath ?: "",
                            ocrText = tempOcrTextForDialog,
                            prominentOcrText = tempProminentOcrTextForDialog,
                            onDismiss = {
                                showTimerDialog.value = false
                                photoFile?.delete()
                                photoFile = null
                            },
                            onRetake = {
                                showTimerDialog.value = false
                                photoFile?.delete()
                                photoFile = null
                                checkPermissionsAndAction(isCamera = true)
                            },
                            onPin = { mins: Int, title: String, note: String, isActiveTracking: Boolean, vehicleId: Int?, tags: List<String> ->
                                showTimerDialog.value = false
                                processPhotoAndPin(mins.toLong() * 60 * 1000L, title, note, isActiveTracking, vehicleId, tags)
                            }
                        )
                        }
                    }


                    // Quiet Save (no active tracking) used to pop a whole second dialog here —
                    // photo/address preview, a Share button, and a Done button to dismiss —
                    // right after the Snap/Pin screen it came from already has its own Share
                    // button. That made saving quietly feel like it needed one more step. This
                    // is purely informational now: no buttons, auto-dismisses on its own.
                    showConfirmationDialog.value?.let { savedSpot ->
                        SavedConfirmationBanner(
                            spotHasPhoto = savedSpot.imagePath.isNotEmpty(),
                            onDismiss = { showConfirmationDialog.value = null }
                        )
                    }
                    
                    if (isProcessingPhoto.value) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
                            GlassSurface(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(0.7f).adaptiveMaxContentWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = SpotVaultColors.Teal)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Securing your pin…", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Mounted here (inside the Scaffold's own content, via contentOverlays) rather
                    // than as a sibling of SpotVaultMainScaffold, so bottomInset — the Scaffold's
                    // own measured bottom-bar height — is actually available; a plain full-screen
                    // Box outside the Scaffold has no way to know that height and used to render
                    // this flush against the raw screen edge, behind both the app's bottom nav bar
                    // and the system nav bar.
                    VaultUndoSnackbarHost(bottomInset = bottomInset)
                                }
                            )
                        }
                        }
                    }
                }
            }
            }
            }
        }
        createNotificationChannel()
        setupWatchdog()
    }

    private fun setupWatchdog() {
        val workRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SpotVaultWatchdog",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }



    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "is_pinned") {
            isPinned.value = sharedPreferences.getBoolean("is_pinned", false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // See the matching restore in onCreate — this is what actually survives a configuration
        // change; without writing it out here there'd be nothing to restore from.
        photoFile?.absolutePath?.let { outState.putString(KEY_PHOTO_FILE_PATH, it) }
        photoUri?.toString()?.let { outState.putString(KEY_PHOTO_URI, it) }
        outState.putString(KEY_TEMP_OCR_TEXT, tempOcrTextForDialog)
        outState.putString(KEY_TEMP_PROMINENT_OCR_TEXT, tempProminentOcrTextForDialog)
        outState.putLong(KEY_DIALOG_SESSION_KEY, dialogSessionKey)
        outState.putBoolean(KEY_SHOW_TIMER_DIALOG, showTimerDialog.value)
    }

    override fun onStop() {
        super.onStop()
        // isChangingConfigurations() — a screen rotation (this Activity has no configChanges
        // override, so rotation goes through the standard onStop→onDestroy→onCreate teardown) or
        // entering/exiting multi-window is not the user leaving the app; re-arming the lock here
        // anyway meant every rotation while App Lock was on threw up a fresh biometric prompt and
        // tore down whatever Compose state (an open dialog, in-progress text) hadn't been
        // explicitly saved.
        if (isShowingBiometricPrompt || AppLockGate.suppressed || isChangingConfigurations) return
        if (::prefs.isInitialized && prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            shouldRequireUnlockOnResume = true
            isAppUnlocked.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ingestWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isPinned.value = prefs.getBoolean("is_pinned", false)
        ThemeState.currentTheme = loadColorThemeFromPrefs(prefs)
        ThemeState.vaultIconStyle = premiumGatedId(
            prefs,
            when (val saved = prefs.getString("vault_icon_style", "classic") ?: "classic") {
                "gear" -> "maglock"
                else -> saved
            },
            PremiumFreeTier.freeVaultIconStyleId
        )
        if (prefs.getString("vault_icon_style", null) == "gear") {
            prefs.edit().putString("vault_icon_style", "maglock").apply()
        }
        if (prefs.getString("compass_style", null) == "star_chart") {
            prefs.edit().putString("compass_style", CompassStyle.LASER_DOT.id).apply()
        }
        ThemeState.compassStyle = premiumGatedId(
            prefs,
            prefs.getString("compass_style", CompassStyle.CLASSIC.id) ?: CompassStyle.CLASSIC.id,
            PremiumFreeTier.freeCompassStyleId
        )
        ThemeState.buttonStyle = loadButtonStyleFromPrefs(prefs)
        ThemeState.reduceAnimations = prefs.getBoolean("reduce_animations", false)
        ThemeState.customPrimaryArgb = prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())
        ThemeState.customAccentArgb = prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())
        ThemeState.textScale = prefs.getFloat("text_size_scale", 1.15f)
        ThemeState.fontFamilyId = prefs.getString("app_font_family", AppFontOptions.DEFAULT_ID) ?: AppFontOptions.DEFAULT_ID
        ThemeState.backgroundPatternId = premiumGatedId(
            prefs,
            normalizeBackgroundPatternId(prefs.getString("background_pattern", BackgroundPattern.GRADIENT_MESH.id)),
            PremiumFreeTier.freeBackgroundPatternId
        )
        ThemeState.customOnPrimaryArgb = textModeToArgb(prefs.getString("custom_on_primary_mode", "auto") ?: "auto")
        ThemeState.customOnAccentArgb = textModeToArgb(prefs.getString("custom_on_accent_mode", "auto") ?: "auto")
        ThemeState.dynamicColorEnabled = prefs.getBoolean("dynamic_color_enabled", false)
        refreshDynamicColors(applicationContext)
        com.spotvault.app.SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))
        if (!prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            isAppUnlocked.value = true
            shouldRequireUnlockOnResume = false
        } else if (shouldRequireUnlockOnResume) {
            isAppUnlocked.value = false
        }
        if (prefs.getBoolean("is_pinned", false)) {
            ensureNotificationPermissionForActivePin()
        }
        if (prefs.getBoolean("keep_screen_on", false) && prefs.getBoolean("is_pinned", false)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    fun requestAppUnlock() {
        if (!prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            isAppUnlocked.value = true
            shouldRequireUnlockOnResume = false
            return
        }
        if (isShowingBiometricPrompt) return

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // This means neither a biometric NOR a device PIN/pattern/password is enrolled —
                // the exact same "no usable lock method" situation the else branch below already
                // self-heals from, just reached via a different BiometricManager result code.
                // Only showing a toast and returning here (the old behavior) left isAppUnlocked
                // false with no way to progress: the app itself has no other unlock path, so the
                // user was stuck on AppLockScreen until they made an unrelated device-wide
                // security change. Disabling App Lock instead guarantees a way back in.
                prefs.edit().putBoolean(APP_LOCK_ENABLED_PREF, false).apply()
                isAppUnlocked.value = true
                shouldRequireUnlockOnResume = false
                Toast.makeText(
                    this,
                    "No screen lock or fingerprint set up — App Lock has been turned off. Set one up in Android Settings to turn it back on.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            else -> {
                // No usable lock method on this device at all — disable App Lock instead
                // of permanently trapping the user on the lock screen with no way back in.
                prefs.edit().putBoolean(APP_LOCK_ENABLED_PREF, false).apply()
                isAppUnlocked.value = true
                shouldRequireUnlockOnResume = false
                Toast.makeText(
                    this,
                    "App Lock isn't supported on this device and has been turned off.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        isShowingBiometricPrompt = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isShowingBiometricPrompt) {
                isShowingBiometricPrompt = false
            }
        }, 4000L)
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isShowingBiometricPrompt = false
                    isAppUnlocked.value = true
                    shouldRequireUnlockOnResume = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isShowingBiometricPrompt = false
                    isAppUnlocked.value = false
                }

                override fun onAuthenticationFailed() {
                    isAppUnlocked.value = false
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ${loadVaultDisplayNameFromPrefs(prefs)}")
            .setSubtitle("Confirm your identity to access your vault")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    /** Warms up the GPS radio the moment Snap/Pin is tapped, so it has a head start on a lock
     * by the time the user actually confirms — but the confirmed save always re-fetches fresh
     * in [processPhotoAndPin]. This value is only ever used as a last-resort fallback if that
     * later fetch fails outright. */
    @SuppressLint("MissingPermission")
    private fun lockInstantGps() {
        val locFineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locCoarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locFineGranted || locCoarseGranted) {
            lifecycleScope.launch {
                val location = resolveCurrentLocation(this@MainActivity, prefs)
                if (location != null) {
                    instantLat = location.first
                    instantLng = location.second
                }
            }
        }
    }

    private fun checkPermissionsAndAction(isCamera: Boolean) {
        lockInstantGps()
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locFineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locCoarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locGranted = locFineGranted || locCoarseGranted
        
        var notifGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }

        // Notification permission is still requested below when missing (nice to ask for it
        // upfront), but it no longer blocks Pin/Snap from proceeding — it isn't needed to save a
        // spot at all, only for the persistent alert if the user goes on to pick Active Tracking
        // in the timer dialog, and that path already has its own correct, narrower handling (see
        // processPhotoAndPin's isActiveTracking check). Requiring it here meant a user who denies
        // notifications — permanently, after Android auto-blocks re-prompting — could never Pin
        // or Snap again at all, for a permission the save itself never actually needed.
        if (isCamera) {
            if (cameraGranted && locGranted) {
                launchCamera()
            } else {
                pendingActionIsCamera = true
                val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                markPermissionRequested(prefs, *perms.toTypedArray())
                requestPermissionsLauncher.launch(perms.toTypedArray())
            }
        } else {
            if (locGranted) {
                photoFile = null
                tempOcrTextForDialog = ""
                tempProminentOcrTextForDialog = ""
                dialogSessionKey = System.currentTimeMillis()
                showTimerDialog.value = true
            } else {
                pendingActionIsCamera = false
                val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                markPermissionRequested(prefs, *perms.toTypedArray())
                requestPermissionsLauncher.launch(perms.toTypedArray())
            }
        }
    }

    private fun launchCamera() {
        tempOcrTextForDialog = ""
        tempProminentOcrTextForDialog = ""
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val newFile = File(imagesDir, "parkmark_${System.currentTimeMillis()}.jpg")
        photoFile = newFile
        val newUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", newFile)
        photoUri = newUri
        try {
            takePictureLauncher.launch(newUri)
        } catch (e: android.content.ActivityNotFoundException) {
            // Same cleanup as takePictureLauncher's own cancel path above — an immediate launch
            // failure never reaches that callback at all, so this has to clear
            // pendingTrackedQuickPinOption itself or the next unrelated Snap/Pin would silently
            // divert into completing this stale tracked quick pin instead of its own flow.
            pendingTrackedQuickPinOption = null
            Toast.makeText(this, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ingestWidgetIntent(intent: Intent?) {
        if (intent == null) return
        var changed = false
        val spotId = intent.getIntExtra(PremiumWidgetIntents.EXTRA_SPOT_ID, -1)
        if (spotId >= 0) {
            pendingWidgetSpotId = spotId
            changed = true
        }
        intent.getStringExtra(PremiumWidgetIntents.EXTRA_WIDGET_ACTION)?.let { action ->
            pendingWidgetAction = action
            changed = true
        }
        if (intent.hasExtra(PremiumWidgetIntents.EXTRA_COMPASS_LAT)) {
            pendingWidgetCompassLat = intent.getDoubleExtra(PremiumWidgetIntents.EXTRA_COMPASS_LAT, 0.0)
            pendingWidgetCompassLng = intent.getDoubleExtra(PremiumWidgetIntents.EXTRA_COMPASS_LNG, 0.0)
            changed = true
        }
        if (changed) widgetIntentVersion.value++
    }

    private fun hasLocationPermissionForWidget(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun launchWidgetSharePhotoCamera() {
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val newFile = File(imagesDir, "widget_share_${System.currentTimeMillis()}.jpg")
        pendingWidgetSharePhotoFile = newFile
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", newFile)
        try {
            widgetSharePhotoLauncher.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            pendingWidgetSharePhotoFile = null
            Toast.makeText(this, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startWidgetSharePhoto() {
        if (!hasLocationPermissionForWidget()) {
            Toast.makeText(this, "Grant location access from Snap or Pin first.", Toast.LENGTH_SHORT).show()
            return
        }
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            launchWidgetSharePhotoCamera()
        } else {
            markPermissionRequested(prefs, Manifest.permission.CAMERA)
            widgetSharePhotoPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    @SuppressLint("MissingPermission")
    private fun processPhotoAndPin(
        timeMs: Long,
        title: String,
        note: String,
        isActiveTracking: Boolean = true,
        vehicleId: Int? = null,
        tags: List<String> = emptyList()
    ) {
        lifecycleScope.launch {
            val vehicleDao = AppDatabase.getDatabase(this@MainActivity).vehicleDao()
            val pinWork = withContext(Dispatchers.IO) {
                // Fetch fresh right now, at the moment the user actually confirms the pin —
                // not back when they tapped Snap/Pin. Filling out notes, category, and timer
                // can take a while, and re-using that earlier fix risked saving where they
                // were standing minutes ago instead of where they are now. The early prewarm
                // fetch (instantLat/instantLng) is kept only as a last-resort fallback.
                val fresh = resolveCurrentLocation(this@MainActivity, prefs)
                val lat = fresh?.first ?: instantLat
                val lng = fresh?.second ?: instantLng

                val currentPhotoPath = photoFile?.absolutePath ?: ""
                val isCamera = currentPhotoPath.isNotEmpty()
                // Notes is the same field/state for both flows now — Snap's scanned text goes to
                // title instead (see TimerSelectionDialog's initialTitle), so this no longer
                // branches on isCamera the way it used to when Snap had its own separate
                // "Scanned Text" field feeding locationDetails.
                val initialLocationDetails = note
                val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()

                val timestamp = System.currentTimeMillis()
                // The dialog always has an explicit vehicle selection (including "None") by the
                // time Save is tappable — unlike Quick Pin/Track, it should never silently
                // auto-resolve to some other vehicle here.
                val resolvedVehicleId = vehicleId
                val pinnedVehicle = resolvedVehicleId?.let { vehicleDao.getById(it) }

                val editor = prefs.edit()
                    .putBoolean("is_pinned", true)
                    .putString("photo_path", currentPhotoPath)
                    .putFloat("lat", lat.toFloat())
                    .putFloat("lng", lng.toFloat())
                    .putString("location_details", initialLocationDetails)
                    .putString("current_address", "Loading address...")
                    .also { applyPinnedVehiclePrefs(it, pinnedVehicle) }
                if (timeMs > 0) {
                    editor.putLong("timer_end_time", System.currentTimeMillis() + timeMs)
                } else {
                    editor.remove("timer_end_time")
                }
                editor.apply()

                val newSpot = LocationSpot(
                    imagePath = currentPhotoPath,
                    locationDetails = initialLocationDetails,
                    timestamp = timestamp,
                    lat = lat,
                    lng = lng,
                    address = "",
                    isFavorite = false,
                    title = title,
                    vehicleId = resolvedVehicleId
                )
                // Needs the real generated id (not the 0 default still sitting on newSpot) — tags
                // are assigned through the junction table by locationId, so this has to be the
                // actual row Room just created, not a fresh insert-and-forget.
                val insertedId = dao.insertSpotAndGetId(newSpot).toInt()
                if (tags.isNotEmpty()) {
                    val tagDao = AppDatabase.getDatabase(this@MainActivity).tagDao()
                    tags.forEach { tagDao.assignTag(insertedId, it) }
                }

                PinWorkResult(
                    lat = lat,
                    lng = lng,
                    photoPath = currentPhotoPath,
                    timestamp = timestamp,
                    isCamera = isCamera,
                    note = note,
                    timeMs = timeMs,
                    isActiveTracking = isActiveTracking,
                    insertedId = insertedId,
                    newSpot = newSpot
                )
            }

            instantLat = 0.0
            instantLng = 0.0

            if (pinWork.lat == 0.0 && pinWork.lng == 0.0) {
                val reason = if (!isLocationServicesEnabled(this@MainActivity)) {
                    "Location Services is off — turn it on in system settings for accurate pins."
                } else {
                    "Couldn't get an accurate GPS fix — pin saved without exact coordinates."
                }
                android.widget.Toast.makeText(this@MainActivity, reason, android.widget.Toast.LENGTH_LONG).show()
            }

            if (pinWork.isActiveTracking) {
                isPinned.value = true
            } else {
                prefs.edit().putBoolean("is_pinned", false).remove("photo_path").remove("lat").remove("lng").apply()
                isPinned.value = false
                showConfirmationDialog.value = pinWork.newSpot
            }

            withContext(Dispatchers.IO) {
                WidgetThemeHelper.refreshAllWidgets(this@MainActivity)
            }

            if ((pinWork.lat != 0.0 || pinWork.lng != 0.0) && prefs.getBoolean("auto_fetch_address", true)) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()
                    val geocoded = reverseGeocodeAddress(this@MainActivity, pinWork.lat, pinWork.lng)

                    prefs.edit().putString("current_address", geocoded.full).apply()

                    // Snap and Pin share the same notes field now (Snap's scanned text goes to
                    // title instead) — pinWork.note is always what got saved as locationDetails,
                    // for both flows, so there's nothing left to branch on here.
                    val finalDetails = pinWork.note
                    if (!pinWork.isCamera) {
                        prefs.edit().putString("location_details", finalDetails).apply()
                    }

                    val spotToUpdate = dao.getSpotById(pinWork.insertedId)
                    if (spotToUpdate != null) {
                        dao.updateSpot(
                            spotToUpdate.copy(
                                address = geocoded.full,
                                locationDetails = finalDetails,
                                city = geocoded.city,
                                state = geocoded.state
                            )
                        )
                    }

                    startService(
                        Intent(this@MainActivity, TimerService::class.java).apply {
                            action = TimerService.ACTION_UPDATE_DETAILS
                        }
                    )
                }
            }

            if (pinWork.isActiveTracking) {
                if (!hasNotificationPermission()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Notification permission is required for timer tracking alerts.",
                        Toast.LENGTH_LONG
                    ).show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                NotificationGuard.schedule(this@MainActivity)
                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    putExtra("TIME_MS", pinWork.timeMs)
                    putExtra("PHOTO_PATH", pinWork.photoPath)
                    putExtra("LAT", pinWork.lat)
                    putExtra("LNG", pinWork.lng)
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)
                }
            }
        }
    }

    private data class PinWorkResult(
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val photoPath: String = "",
        val timestamp: Long = 0L,
        val isCamera: Boolean = false,
        val note: String = "",
        val timeMs: Long = 0L,
        val isActiveTracking: Boolean = true,
        val insertedId: Int = 0,
        val newSpot: LocationSpot? = null
    )

    @SuppressLint("MissingPermission")
    private fun startPinnedNotificationForSpot(spotId: Int, photoPath: String, label: String) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()
            val spot = withContext(Dispatchers.IO) {
                dao.getHistoryList().find { it.id == spotId }
            } ?: return@launch

            val effectivePhotoPath = photoPath.ifEmpty { spot.imagePath }
            val effectiveDetails = spot.locationDetails.ifBlank { label }
            val lat = spot.lat
            val lng = spot.lng

            prefs.edit()
                .putBoolean("is_pinned", true)
                .putString("photo_path", effectivePhotoPath)
                .putFloat("lat", lat.toFloat())
                .putFloat("lng", lng.toFloat())
                .putString("location_details", effectiveDetails)
                .putString("current_address", spot.address.ifBlank { "Loading address..." })
                .remove("timer_end_time")
                .apply()

            isPinned.value = true

            withContext(Dispatchers.IO) {
                WidgetThemeHelper.refreshAllWidgets(this@MainActivity)
            }

            if ((lat != 0.0 || lng != 0.0) && spot.address.isBlank() && prefs.getBoolean("auto_fetch_address", true)) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val geocoded = reverseGeocodeAddress(this@MainActivity, lat, lng)
                    prefs.edit().putString("current_address", geocoded.full).apply()

                    val spotToUpdate = dao.getHistoryList().find { it.id == spotId }
                    if (spotToUpdate != null) {
                        dao.updateSpot(spotToUpdate.copy(address = geocoded.full, city = geocoded.city, state = geocoded.state))
                    }

                    startService(
                        Intent(this@MainActivity, TimerService::class.java).apply {
                            action = TimerService.ACTION_UPDATE_DETAILS
                        }
                    )
                }
            }

            if (!hasNotificationPermission()) {
                Toast.makeText(
                    this@MainActivity,
                    "Notification permission is required for timer tracking alerts.",
                    Toast.LENGTH_LONG
                ).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                NotificationGuard.schedule(this@MainActivity)
                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    putExtra("TIME_MS", 0L)
                    putExtra("PHOTO_PATH", effectivePhotoPath)
                    putExtra("LAT", lat)
                    putExtra("LNG", lng)
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)
            }
        }
    }

    private fun extractOcrAndShowDialog() {
        val currentPhotoPath = photoFile?.absolutePath ?: ""
        val trackedOption = pendingTrackedQuickPinOption
        if (trackedOption != null && currentPhotoPath.isEmpty()) {
            pendingTrackedQuickPinOption = null
            lifecycleScope.launch {
                completeTrackedQuickPinWithPhoto(trackedOption, photoPath = "", details = trackedOption.details)
            }
            return
        }
        if (currentPhotoPath.isEmpty()) {
            tempOcrTextForDialog = ""
            tempProminentOcrTextForDialog = ""
            showTimerDialog.value = true
            return
        }

        isProcessingPhoto.value = true
        lifecycleScope.launch {
            // Downscale + re-encode the fresh camera capture before OCR/storage.
            // Existing vault photos are never touched — only this new capture path.
            withContext(Dispatchers.IO) {
                compressCapturedPhoto(currentPhotoPath)
            }

            val ocrResult = withContext(Dispatchers.Default) {
                var extractedText = ""
                var prominentText = ""
                val rawBitmap = getUprightBitmap(currentPhotoPath)
                if (rawBitmap != null) {
                    // A new client is created per capture (there's no long-lived shared instance
                    // to reuse), so each one has to close itself when done — ML Kit's own docs
                    // call this out as necessary to free the underlying detector resources;
                    // leaving it open here leaked one on every single photo capture.
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    // Was recycled inline right after the OCR text loop — meaning any exception
                    // thrown by recognizer.process(), or while walking result.textBlocks (both
                    // already anticipated by the catch below), skipped past that recycle() call
                    // entirely and leaked the enhanced bitmap. Tracked in a var and recycled in
                    // `finally` instead, so every exit path — success or exception — releases it.
                    var workingBitmap: Bitmap? = null
                    try {
                        val bitmap = enhanceBitmapForOCR(rawBitmap)
                        workingBitmap = bitmap
                        if (bitmap != rawBitmap) rawBitmap.recycle()
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val result = recognizer.process(image).await()

                        var maxBoxHeight = 0
                        val fullTextBuilder = java.lang.StringBuilder()

                        for (block in result.textBlocks) {
                            for (line in block.lines) {
                                val bbox = line.boundingBox
                                if (bbox != null) {
                                    val height = bbox.bottom - bbox.top
                                    if (height > maxBoxHeight) {
                                        maxBoxHeight = height
                                        prominentText = line.text
                                    }
                                }
                                fullTextBuilder.append(line.text).append(" ")
                            }
                        }

                        if (fullTextBuilder.isNotEmpty()) {
                            extractedText = fullTextBuilder.toString()
                                .replace(Regex("[^A-Za-z0-9\\-\\s]"), " ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            prominentText = prominentText
                                .replace(Regex("[^A-Za-z0-9\\-\\s]"), " ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        workingBitmap?.recycle()
                        recognizer.close()
                    }
                }
                extractedText to prominentText
            }

            tempOcrTextForDialog = ocrResult.first
            tempProminentOcrTextForDialog = ocrResult.second
            isProcessingPhoto.value = false
            val pendingTrackOption = pendingTrackedQuickPinOption
            if (pendingTrackOption != null) {
                pendingTrackedQuickPinOption = null
                val details = ocrResult.first.ifBlank { pendingTrackOption.details }
                completeTrackedQuickPinWithPhoto(pendingTrackOption, currentPhotoPath, details)
                return@launch
            }
            showTimerDialog.value = true
        }
    }

    private fun enhanceBitmapForOCR(original: Bitmap): Bitmap {
        val contrastBmp = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(contrastBmp)
        val cm = android.graphics.ColorMatrix()
        cm.setSaturation(0f)
        val cmContrast = android.graphics.ColorMatrix()
        val contrast = 1.8f
        val offset = (-.5f * contrast + .5f) * 255f
        cmContrast.set(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(cmContrast)
        val paint = android.graphics.Paint()
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(original, 0f, 0f, paint)
        return contrastBmp
    }

    private fun getUprightBitmap(filePath: String, maxDimension: Int = 1600): Bitmap? {
        try {
            val file = File(filePath)
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, boundsOptions)
            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return null

            var inSampleSize = 1
            while (width / inSampleSize > maxDimension * 2 || height / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val decoded = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            var bitmap = if (matrix.isIdentity) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            }

            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > maxDimension) {
                val scale = maxDimension.toFloat() / longest
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun clearPinnedSpot() {
        FoundCelebration.play(this, withToast = false)
        ActiveTrackingHelper.clearActiveTracking(this)
        isPinned.value = false
        showFoundMoment.value = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(android.app.NotificationManager::class.java)

            // The countdown/silent channels themselves are TimerService.ensureNotificationChannels()'s
            // job now, not this function's — this used to also create its own "TIMER_COUNTDOWN"
            // channel here, the pre-rename id TimerService abandoned in favor of "TIMER_COUNTDOWN_V2"
            // (a channel's importance is locked in the first time it's created and never updates on
            // reinstall, which is exactly why that rename happened). This copy never got updated
            // alongside it, so it kept silently recreating the old, now-unused channel on every
            // launch — a dead entry cluttering the user's system notification settings, sitting
            // right next to the real "Timer Countdown" channel TimerService actually posts to.

            val prefs = getSharedPreferences("SpotVaultPrefs", android.content.Context.MODE_PRIVATE)
            val soundUriStr = prefs.getString("alarm_sound_uri", null)
            val baseId = "TIMER_ALERT"
            val channelId = if (soundUriStr != null) "${baseId}_${soundUriStr.hashCode()}" else baseId
            
            val alertChannel = android.app.NotificationChannel(channelId, "Timer Alert", android.app.NotificationManager.IMPORTANCE_HIGH)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
                
            if (soundUriStr != null && soundUriStr.isNotEmpty()) {
                alertChannel.setSound(android.net.Uri.parse(soundUriStr), audioAttributes)
            } else {
                alertChannel.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            }
            
            manager.createNotificationChannel(alertChannel)
        }
    }
}

/**
 * Replaces a freshly captured camera JPEG with a downscaled quality-85 copy
 * (max 1600px on the long edge). Safe for OCR at that size. Only call this on
 * new captures — never on existing vault photo paths.
 */
fun compressCapturedPhoto(
    filePath: String,
    maxDimension: Int = 1600,
    quality: Int = 85
): Boolean {
    // Tracks whichever decoded/rotated/scaled bitmap is currently "live" so `finally` can always
    // free it — the old inline recycle() calls only ran on the exact success/compress-failure
    // paths that reached them, so an exception anywhere past decoding (a corrupt image tripping up
    // the rotation matrix, an IOException from bitmap.compress() itself) leaked whichever bitmap
    // was live at that point instead of ever being recycled.
    var liveBitmap: Bitmap? = null
    return try {
        val file = File(filePath)
        if (!file.exists()) return false

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return false

        var sample = 1
        while (srcW / sample > maxDimension * 2 || srcH / sample > maxDimension * 2) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            filePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return false
        liveBitmap = decoded

        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        var bitmap = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
        liveBitmap = bitmap

        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
            liveBitmap = bitmap
        }

        val temp = File(file.parentFile, "${file.nameWithoutExtension}_cmp.jpg")
        temp.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                temp.delete()
                return false
            }
        }

        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }

        // Rotation is baked into pixels — mark EXIF normal so viewers don't double-rotate.
        runCatching {
            ExifInterface(filePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        liveBitmap?.recycle()
    }
}

private tailrec fun android.content.Context.findHostActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Composable
private fun rememberNavigationBarBottomPadding(minimum: Dp = 48.dp, extra: Dp = 32.dp): Dp {
    val density = LocalDensity.current

    EnsureDialogEdgeToEdge()

    // Read from the app-wide SystemBarInsets (measured against the main Activity's window)
    // rather than this composable's own hosting window, since Dialog windows can't be trusted
    // to report accurate WindowInsets on every device.
    val bottomPx = SystemBarInsets.navigationBarPx
    return with(density) {
        maxOf(bottomPx.toDp() + extra, minimum)
    }
}

@Composable
private fun FullScreenScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue <= 0) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 6.dp)
            .width(3.dp)
    ) {
        val density = LocalDensity.current
        val viewportHeightPx = constraints.maxHeight.toFloat()
        if (viewportHeightPx <= 0f) return@BoxWithConstraints

        val contentHeightPx = scrollState.maxValue + viewportHeightPx
        val minThumbPx = with(density) { 40.dp.toPx() }
        val thumbHeightPx = maxOf(
            minThumbPx,
            viewportHeightPx * (viewportHeightPx / contentHeightPx)
        )
        val trackRangePx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
        val thumbOffsetPx = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()) * trackRangePx

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { androidx.compose.ui.unit.IntOffset(0, thumbOffsetPx.toInt()) }
                .width(3.dp)
                .height(with(density) { thumbHeightPx.toDp() })
                .background(SpotVaultColors.Teal.copy(alpha = 0.6f), RoundedCornerShape(50))
        )
    }
}

@Composable
fun FullScreenImageViewer(
    imagePath: String, 
    ocrText: String, 
    note: String, 
    timestampStr: String,
    lat: Float,
    lng: Float,
    onDismiss: () -> Unit,
    onFoundClick: (() -> Unit)? = null,
    address: String = "",
    prefs: SharedPreferences,
    onShareRequest: (ShareSpotPayload) -> Unit = {},
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    spotId: Int = -1,
    onNotesPersisted: ((String) -> Unit)? = null,
    onNavigateToCompass: (() -> Unit)? = null,
    spotItem: LocationSpot? = null,
    onEditRequest: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(onBack = onDismiss)

    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showPhotoExpanded by remember { mutableStateOf(false) }

    var displayNote by remember(note) { mutableStateOf(note) }
    LaunchedEffect(note) { displayNote = note }

    // Live observation, not a one-shot fetch — imagePath/extra photos used to be a snapshot
    // taken once at composition time, so a photo attached from this very screen's own "Add
    // Photo" button never appeared until the screen was closed and reopened. Observing the spot
    // (for imagePath, which goes from blank to set on the very first photo) and its extra-photos
    // table live means both the first photo and any additional ones show up immediately.
    val locationDao = remember { AppDatabase.getDatabase(context).locationDao() }
    val spotPhotoDao = remember { AppDatabase.getDatabase(context).spotPhotoDao() }
    val liveSpot: LocationSpot? by produceState(initialValue = spotItem, spotId) {
        if (spotId >= 0) {
            locationDao.observeSpotById(spotId).collect { value = it }
        }
    }
    val effectiveImagePath = if (spotId >= 0) (liveSpot?.imagePath ?: "") else imagePath
    val extraPhotoPaths: List<SpotPhoto> by produceState(initialValue = emptyList(), spotId) {
        if (spotId >= 0) {
            spotPhotoDao.observeForSpot(spotId).collect { value = it }
        }
    }
    val allPhotoPaths = remember(effectiveImagePath, extraPhotoPaths) {
        (if (effectiveImagePath.isNotEmpty()) listOf(effectiveImagePath) else emptyList()) +
            extraPhotoPaths.map { it.path }
    }
    var selectedImagePath by remember(spotId, imagePath) { mutableStateOf(imagePath) }
    LaunchedEffect(allPhotoPaths) {
        if (selectedImagePath !in allPhotoPaths) {
            selectedImagePath = allPhotoPaths.firstOrNull().orEmpty()
        }
    }

    val bottomPad = rememberNavigationBarBottomPadding(minimum = 52.dp, extra = 44.dp)
    val actionBottomPad = bottomPad
    val topPad = with(LocalDensity.current) {
        SystemBarInsets.statusBarPx.toDp()
    }.coerceIn(12.dp, 28.dp)
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpotVaultColors.Void)
    ) {
        AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(SpotVaultColors.Surface.copy(alpha = 0.75f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                    }

                    Text(
                        text = "Spot Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = SpotVaultColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        if (onEditRequest != null) {
                            IconButton(
                                onClick = onEditRequest,
                                modifier = Modifier
                                    .background(SpotVaultColors.Surface.copy(alpha = 0.75f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit spot", tint = SpotVaultColors.OnSurface)
                            }
                        }
                    }
                }

                // Extracted so the photo/details block and the action buttons below can be laid
                // out either stacked (portrait — unchanged, see the Box/Column below) or side by
                // side (landscape, or any short window). Stacking them in a short landscape
                // window used to leave the scrollable details area (with a fixed 180dp photo)
                // eating most of the available height, pushing Navigate down to around 75% of
                // the screen with only a sliver left for it and the rest of the button stack —
                // which, since none of those buttons shrink for a short window, then rendered
                // comically large relative to what little room was left.
                val detailsBlock: @Composable () -> Unit = {
                    if (allPhotoPaths.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allPhotoPaths.forEach { path ->
                                val isSelected = path == selectedImagePath
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedImagePath = path }
                                ) {
                                    AsyncImage(
                                        model = java.io.File(path),
                                        contentDescription = "Photo thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            // Lets more photos be added even when one (or several) already
                            // exist — previously "Add Photo" only ever showed up on the
                            // no-photo placeholder, with no way back to it once a spot had one.
                            if (spotItem != null) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SpotVaultColors.Elevated.copy(alpha = 0.6f))
                                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { showAddPhotoDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = "Add another photo",
                                        tint = SpotVaultColors.Teal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedImagePath.isNotEmpty()) {
                        Box(
                            modifier = imageModifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showPhotoExpanded = true }
                        ) {
                            AsyncImage(
                                model = java.io.File(selectedImagePath),
                                contentDescription = "Spot Image — tap to enlarge",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Tap to enlarge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Box(
                            modifier = imageModifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SpotVaultColors.Elevated.copy(alpha = 0.5f))
                                .let { base ->
                                    if (spotItem != null) {
                                        base.clickable { showAddPhotoDialog = true }
                                    } else {
                                        base
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = vaultSpotCategoryEmoji(
                                        LocationSpot(
                                            imagePath = "",
                                            locationDetails = displayNote,
                                            timestamp = 0L,
                                            lat = lat.toDouble(),
                                            lng = lng.toDouble(),
                                            address = address
                                        )
                                    ),
                                    fontSize = 40.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No photo attached", color = SpotVaultColors.Muted, fontSize = 13.sp)
                                if (spotItem != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = null,
                                            tint = SpotVaultColors.Teal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Photo", color = SpotVaultColors.Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(SpotVaultColors.Void, SpotVaultColors.Deep.copy(alpha = 0.98f))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (address.isNotEmpty()) {
                            Text(
                                text = address,
                                style = MaterialTheme.typography.titleMedium,
                                color = SpotVaultColors.OnSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else if (lat != 0f && lng != 0f) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpotVaultColors.Muted
                            )
                        }

                        SpotDistanceLabel(
                            spotLat = lat.toDouble(),
                            spotLng = lng.toDouble(),
                            prefs = prefs,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (displayNote.isNotBlank()) {
                            Text(
                                text = displayNote,
                                fontSize = 14.sp,
                                color = SpotVaultColors.OnSurface.copy(alpha = 0.9f),
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (spotItem != null) {
                            Text(
                                text = "No notes yet",
                                fontSize = 14.sp,
                                color = SpotVaultColors.Muted,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (ocrText.isNotBlank()) {
                            Text(
                                text = "Text: $ocrText",
                                fontSize = 12.sp,
                                color = SpotVaultColors.Muted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val isCompact = needsCompactHeightLayout()

                // Buttons, extracted the same way — a shared Column of rows so the split below
                // doesn't duplicate any of the click handling. Show Map/Compass/Share switch from
                // a fixed 130dp width (fine in portrait's full screen width) to weighted in the
                // compact-height right column, which is narrower — a fixed width there risked the
                // same "second button clipped off the edge" overflow already found and fixed on
                // the tracking screen's equivalent buttons.
                val buttonsBlock: @Composable ColumnScope.() -> Unit = {
                    if (lat != 0f && lng != 0f || onFoundClick != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (lat != 0f && lng != 0f) {
                                SpotVaultButton(
                                    onClick = {
                                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                                        try {
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            android.widget.Toast.makeText(context, "No navigation app found", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = spotVaultButtonShape(),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = SpotVaultColors.Primary
                                    )
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("NAVIGATE", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }

                            if (onFoundClick != null) {
                                SpotVaultOutlinedButton(
                                    onClick = onFoundClick,
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = spotVaultButtonShape(),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                                    // OnPrimary, not the flat PrimaryBright — an outlined button's
                                    // "glass" shell is tinted from this same contentColor, so a
                                    // color that's unconditionally bright (PrimaryBright, by design,
                                    // on every theme) produced a shell tinted just as bright as the
                                    // text sitting on it once Yin Yang's whole palette leaned that
                                    // direction too.
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = SpotVaultColors.OnPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                    Text("FOUND IT", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (lat != 0f && lng != 0f || onNavigateToCompass != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isCompact) {
                                Arrangement.spacedBy(10.dp)
                            } else {
                                Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                            }
                        ) {
                            val smallButtonModifier = if (isCompact) {
                                Modifier.weight(1f).height(40.dp)
                            } else {
                                Modifier.width(130.dp).height(40.dp)
                            }
                            if (lat != 0f && lng != 0f) {
                                SpotVaultOutlinedButton(
                                    onClick = {
                                        val label = address.ifBlank { "Saved Spot" }
                                        val uri = android.net.Uri.parse("geo:0,0?q=$lat,$lng(${android.net.Uri.encode(label)})")
                                        try {
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            android.widget.Toast.makeText(context, "No map app found", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = smallButtonModifier,
                                    shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha = 0.5f)),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.ButtonLabel)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Show Map", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Show Map", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                }
                            }

                            if (onNavigateToCompass != null) {
                                SpotVaultOutlinedButton(
                                    onClick = { onNavigateToCompass() },
                                    modifier = smallButtonModifier,
                                    shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha = 0.5f)),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.ButtonLabel)
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = "Open compass", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compass", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isCompact) Arrangement.spacedBy(10.dp) else Arrangement.Center
                    ) {
                        SpotVaultOutlinedButton(
                            onClick = {
                                onShareRequest(
                                    ShareSpotPayload(
                                        lat = lat.toDouble(),
                                        lng = lng.toDouble(),
                                        address = address,
                                        notes = displayNote,
                                        imagePath = imagePath
                                    )
                                )
                            },
                            modifier = if (isCompact) Modifier.weight(1f).height(40.dp) else Modifier.width(130.dp).height(40.dp),
                            shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }

                if (isCompact) {
                    // Side by side instead of stacked — see the comment above detailsBlock. Each
                    // side gets its own scroll: the details column can still run taller than a
                    // short window, and the buttons column now holds five buttons instead of the
                    // two or three portrait's taller layout has room for at once.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .verticalScroll(scrollState)
                        ) {
                            detailsBlock()
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = actionBottomPad),
                            verticalArrangement = Arrangement.Center
                        ) {
                            buttonsBlock()
                        }
                    }
                } else {
                    // weight(1f, fill = false) instead of a bare fillMaxWidth(): unweighted, this Box
                    // had no upper bound at all, so a full-width photo (contentScale = FillWidth, no
                    // fixed height) could report any height its aspect ratio implied — a portrait
                    // capture routinely taller than the whole screen — and verticalScroll had nothing
                    // to actually bound the viewport against, so nothing scrolled and the Navigate/
                    // Compass/Found row below was pushed off-screen with no way to reach it. fill =
                    // false still lets short content (a small photo, few notes) hug its own size
                    // rather than being force-stretched — same "no dead space" goal as before — while
                    // capping the max at the remaining screen height so verticalScroll has a real
                    // viewport to clip against once content grows past it.
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            detailsBlock()
                        }
                        FullScreenScrollIndicator(
                            scrollState = scrollState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpotVaultColors.Void)
                            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = actionBottomPad)
                    ) {
                        buttonsBlock()
                    }
                }
            }
        }
    }

    if (showAddPhotoDialog && spotItem != null) {
        VaultAddPhotoDialog(item = spotItem, onDismiss = { showAddPhotoDialog = false })
    }

    if (showPhotoExpanded && selectedImagePath.isNotEmpty()) {
        BackHandler(onBack = { showPhotoExpanded = false })
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPhotoExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            EnsureDialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showPhotoExpanded = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = java.io.File(selectedImagePath),
                    contentDescription = "Enlarged spot photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showPhotoExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryDialogContent(
    onDismiss: () -> Unit,
    dao: LocationDao,
    prefs: android.content.SharedPreferences,
    isPinned: Boolean,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val spotPhotoDao = remember { AppDatabase.getDatabase(context).spotPhotoDao() }
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                return available
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                return androidx.compose.ui.unit.Velocity.Zero
            }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                return available
            }
        }
    }
    val historyList by dao.getAllHistory().collectAsState(initial = emptyList())
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember {
        mutableStateOf(
            when (val saved = prefs.getString("default_sort_order", VaultSortOption.NEWEST) ?: VaultSortOption.NEWEST) {
                "Category" -> VaultSortOption.ALPHABETICAL
                else -> saved
            }
        )
    }
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var selectedVehicleId by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingSwipeDeleteSpot by remember { mutableStateOf<LocationSpot?>(null) }
    var vaultViewMode by remember { mutableStateOf(loadVaultViewMode(prefs)) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var selectedCalendarDay by remember { mutableStateOf<Long?>(null) }
    var showRecentlyDeleted by remember { mutableStateOf(false) }
    var showArchivedSpots by remember { mutableStateOf(false) }
    var showLocationBrowser by remember { mutableStateOf(false) }
    var autoDeleteEnabled by remember { mutableStateOf(prefs.getBoolean("auto_delete_enabled", false)) }
    var showVaultMenu by remember { mutableStateOf(false) }
    var showAutoDeleteIntervalDialog by remember { mutableStateOf(false) }
    var deletedCount by remember { mutableStateOf(0) }

    LaunchedEffect(historyList, showRecentlyDeleted) {
        deletedCount = withContext(Dispatchers.IO) { dao.getRecentlyDeleted().size }
    }

    if (showRecentlyDeleted) {
        RecentlyDeletedDialog(dao = dao, spotPhotoDao = spotPhotoDao, onDismiss = { showRecentlyDeleted = false })
    }

    if (showArchivedSpots) {
        ArchivedSpotsDialog(dao = dao, spotPhotoDao = spotPhotoDao, onDismiss = { showArchivedSpots = false })
    }

    if (showAutoDeleteIntervalDialog) {
        AutoDeleteIntervalPickerDialog(
            initialInterval = AutoDeleteInterval.fromId(prefs.getString("auto_delete_interval", AutoDeleteInterval.MONTH.id)),
            onConfirm = { interval ->
                autoDeleteEnabled = true
                prefs.edit()
                    .putBoolean("auto_delete_enabled", true)
                    .putString("auto_delete_interval", interval.id)
                    .apply()
                scheduleAutoDelete(context)
                showAutoDeleteIntervalDialog = false
            },
            onDismiss = { showAutoDeleteIntervalDialog = false }
        )
    }

    BackHandler(enabled = showRecentlyDeleted) { showRecentlyDeleted = false }
    BackHandler(enabled = showArchivedSpots) { showArchivedSpots = false }
    BackHandler(enabled = showAutoDeleteIntervalDialog) { showAutoDeleteIntervalDialog = false }
    BackHandler(enabled = showDeleteConfirmDialog) { showDeleteConfirmDialog = false }
    BackHandler(enabled = showCalendarDialog) { showCalendarDialog = false }
    BackHandler(enabled = selectedCalendarDay != null) { selectedCalendarDay = null }

    val vaultHistorySpots = remember(historyList) { historyList.filter { !it.isWishlist } }

    if (showLocationBrowser) {
        VaultLocationBrowserDialog(
            historyList = vaultHistorySpots,
            dao = dao,
            prefs = prefs,
            onDismiss = { showLocationBrowser = false },
            onViewSpot = onViewSpot,
            onShareRequest = onShareRequest,
            coroutineScope = coroutineScope
        )
    }

    if (showCalendarDialog) {
        VaultCalendarDialog(
            historySpots = vaultHistorySpots,
            onDismiss = { showCalendarDialog = false },
            onDaySelected = { dayStart ->
                selectedCalendarDay = dayStart
                showCalendarDialog = false
            }
        )
    }

    selectedCalendarDay?.let { dayStart ->
        VaultCalendarDayResultsDialog(
            dayStartMillis = dayStart,
            historySpots = vaultHistorySpots,
            prefs = prefs,
            dao = dao,
            onDismiss = { selectedCalendarDay = null },
            onViewSpot = onViewSpot,
            onShareRequest = onShareRequest,
            onSwipeDelete = { spot ->
                pendingSwipeDeleteSpot = spot
                if (prefs.getBoolean("confirm_delete", true)) {
                    showDeleteConfirmDialog = true
                } else {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        dao.softDeleteSpot(spot.id)
                    }
                    pendingSwipeDeleteSpot = null
                }
            },
            selectedItems = selectedItems,
            onSelectionChange = { selectedItems = it },
            onShowDeleteConfirm = {
                pendingSwipeDeleteSpot = null
                if (prefs.getBoolean("confirm_delete", true)) {
                    showDeleteConfirmDialog = true
                } else {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val toDelete = vaultHistorySpots.filter { selectedItems.contains(it.id) }
                        toDelete.forEach { dao.softDeleteSpot(it.id) }
                    }
                    selectedItems = emptySet()
                }
            },
            coroutineScope = coroutineScope
        )
    }

    // Pulled out as a slot passed into HistoryVaultTabPage (rather than rendered directly here)
    // so landscape can place it above the filters in a left-hand column instead of full width
    // above everything else — see isLandscapeOrientation() inside HistoryVaultTabPage.
    val vaultHeaderRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val vaultAmber = Color(0xFFFFB300)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            (if (autoDeleteEnabled) vaultAmber else SpotVaultColors.Teal).copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showVaultMenu = true }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "VAULT",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = if (autoDeleteEnabled) vaultAmber else SpotVaultColors.Teal,
                        letterSpacing = 2.sp
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Vault view menu",
                        tint = if (autoDeleteEnabled) vaultAmber else SpotVaultColors.Teal,
                        modifier = Modifier.size(24.dp)
                    )
                    // HUD indicator — appended next to the header instead of taking its own row,
                    // so a temporary-spots warning is always visible without spending vertical
                    // space on a dedicated banner.
                    if (autoDeleteEnabled) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.AutoDelete,
                            contentDescription = "Auto Delete is on",
                            tint = vaultAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showVaultMenu,
                    onDismissRequest = { showVaultMenu = false },
                    containerColor = SpotVaultColors.Elevated
                ) {
                    DropdownMenuItem(
                        text = { Text("Active Spots", color = SpotVaultColors.OnSurface) },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = SpotVaultColors.Teal) },
                        onClick = { showVaultMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Archived Spots", color = SpotVaultColors.OnSurface) },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = SpotVaultColors.Teal) },
                        onClick = {
                            showVaultMenu = false
                            showArchivedSpots = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recently Deleted", color = SpotVaultColors.OnSurface)
                                if (deletedCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(SpotVaultColors.Teal.copy(alpha = 0.28f), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("$deletedCount", fontSize = 10.sp, color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = SpotVaultColors.Teal) },
                        onClick = {
                            showVaultMenu = false
                            showRecentlyDeleted = true
                        }
                    )
                    HorizontalDivider(color = SpotVaultColors.Outline.copy(alpha = 0.25f))
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    if (autoDeleteEnabled) "Auto Delete: On" else "Auto Delete",
                                    color = if (autoDeleteEnabled) vaultAmber else SpotVaultColors.OnSurface
                                )
                                Text(
                                    "Favorites are never touched",
                                    fontSize = 11.sp,
                                    color = SpotVaultColors.Muted
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoDelete,
                                contentDescription = null,
                                tint = if (autoDeleteEnabled) vaultAmber else SpotVaultColors.Teal
                            )
                        },
                        trailingIcon = {
                            // onCheckedChange = null: the Switch is purely a visual reflection of
                            // autoDeleteEnabled here — the row's own onClick below is the single
                            // source of truth for the toggle. A live onCheckedChange on the Switch
                            // itself, nested inside an already-clickable DropdownMenuItem row,
                            // would give a tap on the switch two separate handlers to fire (the
                            // switch's own plus the row's), which could double-toggle back to no
                            // visible change instead of turning it on/off as expected.
                            Switch(
                                checked = autoDeleteEnabled,
                                onCheckedChange = null,
                                colors = spotVaultSwitchColors()
                            )
                        },
                        onClick = {
                            if (autoDeleteEnabled) {
                                // Turning off needs no confirmation — only enabling it does.
                                autoDeleteEnabled = false
                                prefs.edit().putBoolean("auto_delete_enabled", false).apply()
                                cancelAutoDelete(context)
                            } else {
                                showVaultMenu = false
                                showAutoDeleteIntervalDialog = true
                            }
                        }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(
                    onClick = { showLocationBrowser = true },
                    modifier = Modifier
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Browse by Location", tint = SpotVaultColors.Teal)
                }
                IconButton(
                    onClick = { showCalendarDialog = true },
                    modifier = Modifier
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = SpotVaultColors.Teal)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        HistoryVaultTabPage(
            headerContent = vaultHeaderRow,
            historyList = historyList,
            prefs = prefs,
            dao = dao,
            isPinned = isPinned,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            showFavoritesOnly = showFavoritesOnly,
            onShowFavoritesOnlyChange = { showFavoritesOnly = it },
            selectedVehicleId = selectedVehicleId,
            onSelectedVehicleIdChange = { selectedVehicleId = it },
            selectedItems = selectedItems,
            onSelectedItemsChange = { selectedItems = it },
            onShowDeleteConfirm = {
                pendingSwipeDeleteSpot = null
                if (prefs.getBoolean("confirm_delete", true)) {
                    showDeleteConfirmDialog = true
                } else {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val toDelete = historyList.filter { selectedItems.contains(it.id) }
                        toDelete.forEach { dao.softDeleteSpot(it.id) }
                    }
                    selectedItems = emptySet()
                }
            },
            onSwipeDeleteSpot = { spot ->
                pendingSwipeDeleteSpot = spot
                if (prefs.getBoolean("confirm_delete", true)) {
                    showDeleteConfirmDialog = true
                } else {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        dao.softDeleteSpot(spot.id)
                    }
                    pendingSwipeDeleteSpot = null
                }
            },
            onShareRequest = onShareRequest,
            onViewSpot = onViewSpot,
            vaultViewMode = vaultViewMode,
            onVaultViewModeChange = { vaultViewMode = it },
            nestedScrollConnection = nestedScrollConnection,
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showDeleteConfirmDialog) {
        PremiumDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                pendingSwipeDeleteSpot = null
            },
            title = { Text("Delete Saved Spots?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface) },
            content = { Text("Are you sure you want to delete the selected spot(s)? This action cannot be undone.", color = SpotVaultColors.Muted) },
            confirmButton = {
                SpotVaultButton(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val toDelete = pendingSwipeDeleteSpot?.let { listOf(it) }
                                ?: historyList.filter { selectedItems.contains(it.id) }
                            toDelete.forEach { dao.softDeleteSpot(it.id) }
                            selectedItems = emptySet()
                            pendingSwipeDeleteSpot = null
                        }
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    pendingSwipeDeleteSpot = null
                }) { Text("Cancel", color = SpotVaultColors.Teal) }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveScreenSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SpotVaultColors.Teal,
        letterSpacing = 0.4.sp,
        modifier = modifier
    )
}

/** Tag picker for a spot that doesn't exist yet — the spot only gets a real id once Pin/Save is
 * tapped, so this just collects plain tag names locally; the caller assigns them via TagDao after
 * the insert actually happens. Typing filters existing tags into tappable suggestions (the "smart"
 * list), and anything not already a tag can be added as a brand-new one from the same field. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SaveScreenTagField(
    selectedTags: List<String>,
    onSelectedTagsChange: (List<String>) -> Unit,
    allTags: List<TagEntity>
) {
    val context = LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val coroutineScope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedTags.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedTags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpotVaultColors.Teal.copy(alpha = 0.18f))
                            .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove $tag",
                            tint = SpotVaultColors.Teal,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onSelectedTagsChange(selectedTags.filterNot { it == tag }) }
                        )
                    }
                }
            }
        }
        // A compact button opening a picker sheet, not an inline search box — the search field
        // plus its suggestion cloud used to always be on screen here even collapsed/empty,
        // eating a chunk of the card's height on every single Snap/Pin save regardless of
        // whether the user was tagging this spot at all.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sell, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Tag", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }

    if (showPicker) {
        SaveScreenTagPickerSheet(
            selectedTags = selectedTags,
            onSelectedTagsChange = onSelectedTagsChange,
            allTags = allTags,
            tagDao = tagDao,
            coroutineScope = coroutineScope,
            onDismiss = { showPicker = false }
        )
    }
}

/** Same smart tag cloud as the Vault's "Filter by Tag" sheet (search, Frequently Used / All
 * Tags split, create-new-from-search) — except chips toggle multiple tags on/off instead of
 * single-selecting and dismissing, since a spot can carry any number of tags. Tag creation still
 * goes through [TagDao.createTag] so it's immediately visible everywhere else, but nothing is
 * assigned to a spot here — the spot doesn't exist yet at Snap/Pin save time, so the picked
 * names just flow back into [onSelectedTagsChange]'s local list until the spot is actually saved. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SaveScreenTagPickerSheet(
    selectedTags: List<String>,
    onSelectedTagsChange: (List<String>) -> Unit,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val filteredTags = remember(allTags, search) {
        if (search.isBlank()) allTags else allTags.filter { searchTextMatches(it.name, search) }
    }
    val frequentTags = remember(allTags) { allTags.sortedByDescending { it.usageCount }.take(10) }
    val remainingTagsAlpha = remember(allTags, frequentTags) {
        val frequentIds = frequentTags.map { it.id }.toSet()
        allTags.filter { it.id !in frequentIds }.sortedBy { it.name.lowercase() }
    }
    val trimmedSearch = search.trim()
    val exactMatchExists = remember(allTags, trimmedSearch) {
        trimmedSearch.isNotEmpty() && allTags.any { it.name.equals(trimmedSearch, ignoreCase = true) }
    }

    fun toggleTag(name: String) {
        val exists = selectedTags.any { it.equals(name, ignoreCase = true) }
        onSelectedTagsChange(
            if (exists) selectedTags.filterNot { it.equals(name, ignoreCase = true) } else selectedTags + name
        )
    }

    fun createTagFromSearch() {
        val name = trimmedSearch
        if (name.isEmpty() || exactMatchExists) return
        coroutineScope.launch(Dispatchers.IO) { tagDao.createTag(name) }
        toggleTag(name)
        search = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotVaultColors.Elevated,
        contentColor = SpotVaultColors.OnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                // ModalBottomSheet doesn't auto-scroll oversized content — it just clips whatever
                // doesn't fit. With a large tag library the ALL TAGS chip cloud plus the Done
                // button below it could exceed the sheet's available height and become
                // permanently unreachable without this.
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Add Tags",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search or add a tag…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                            IconButton(onClick = { createTagFromSearch() }) {
                                Icon(Icons.Default.Add, contentDescription = "Create tag \"$trimmedSearch\"", tint = SpotVaultColors.Teal)
                            }
                        }
                        VoiceMicButton(onResult = { search = it }, prompt = "Speak a tag…")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = SpotVaultColors.OnSurface,
                    focusedTextColor = SpotVaultColors.OnSurface,
                    unfocusedBorderColor = SpotVaultColors.Outline,
                    focusedBorderColor = SpotVaultColors.Teal,
                    cursorColor = SpotVaultColors.Teal
                )
            )
            if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                Text(
                    "Tap + to create \"$trimmedSearch\" as a new tag",
                    color = SpotVaultColors.Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            when {
                allTags.isEmpty() -> Text(
                    "No tags yet — create one above.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                filteredTags.isEmpty() && trimmedSearch.isNotEmpty() -> Text(
                    "No tags match \"$search\"",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                trimmedSearch.isNotEmpty() -> androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredTags.forEach { tag ->
                        SaveScreenPickerTagChip(tag = tag, selectedTags = selectedTags, onToggle = ::toggleTag)
                    }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (frequentTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "FREQUENTLY USED",
                                color = SpotVaultColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                frequentTags.forEach { tag ->
                                    SaveScreenPickerTagChip(tag = tag, selectedTags = selectedTags, onToggle = ::toggleTag)
                                }
                            }
                        }
                    }
                    if (remainingTagsAlpha.isNotEmpty()) {
                        if (frequentTags.isNotEmpty()) {
                            androidx.compose.material3.HorizontalDivider(color = SpotVaultColors.Outline.copy(alpha = 0.25f))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (frequentTags.isNotEmpty()) {
                                Text(
                                    "ALL TAGS",
                                    color = SpotVaultColors.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                remainingTagsAlpha.forEach { tag ->
                                    SaveScreenPickerTagChip(tag = tag, selectedTags = selectedTags, onToggle = ::toggleTag)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SpotVaultButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = spotVaultButtonShape()
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SaveScreenPickerTagChip(tag: TagEntity, selectedTags: List<String>, onToggle: (String) -> Unit) {
    val selected = selectedTags.any { it.equals(tag.name, ignoreCase = true) }
    FilterChip(
        selected = selected,
        onClick = { onToggle(tag.name) },
        label = { Text(tag.name, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = vaultTagChipColors(),
        border = vaultTagChipBorder(selected),
        shape = RoundedCornerShape(10.dp)
    )
}

private fun formatReminderDuration(mins: Int): String {
    if (mins <= 0) return "No Reminder"
    val h = mins / 60
    val m = mins % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

/** The "Reminder" section label *is* the control now — no separate label above a dropdown-style
 * box. Unset, it reads "REMINDER +" (matching the same bold/teal/letter-spaced style every other
 * section label on this card uses); once a duration is picked, that same text is replaced by the
 * duration itself (e.g. "1h 30m"), and tapping it again reopens the wheel picker to change it.
 * Either way it's a single line that never grows the card — the wheels themselves live in a popup
 * dialog, not inline. */
@Composable
private fun SaveScreenReminderControl(
    selectedMins: Int,
    suggestedMins: Int?,
    isCamera: Boolean,
    onSelect: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val hasReminder = selectedMins > 0
    val hasSuggestion = isCamera && suggestedMins != null && suggestedMins != selectedMins

    Row(
        modifier = Modifier.clickable { showPicker = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasReminder) {
            Text(
                text = formatReminderDuration(selectedMins),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.4.sp
            )
        } else {
            Text(
                text = "REMINDER",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.AddCircle,
                contentDescription = "Add reminder",
                tint = SpotVaultColors.Teal,
                modifier = Modifier.size(16.dp)
            )
        }
        if (hasSuggestion) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "★ OCR: ${formatReminderDuration(suggestedMins!!)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = SpotVaultColors.PrimaryBright,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }

    if (showPicker) {
        ReminderWheelPickerDialog(
            selectedMins = selectedMins,
            suggestedMins = if (isCamera) suggestedMins else null,
            onDismiss = { showPicker = false },
            onConfirm = { mins ->
                onSelect(mins)
                showPicker = false
            }
        )
    }
}

@Composable
private fun ReminderWheelPickerDialog(
    selectedMins: Int,
    suggestedMins: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Staged locally, same as before — TimerWheelPicker itself applies live to these two as the
    // wheels scroll, but nothing reaches the actual reminder (onConfirm) until Done is tapped, so
    // scrolling around and then backing out via Cancel/tap-outside still discards cleanly.
    var hours by remember { mutableStateOf((selectedMins / 60).coerceIn(0, 12)) }
    var minutes by remember { mutableStateOf((((selectedMins % 60) + 2) / 5 * 5).coerceIn(0, 55)) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(spotVaultDialogShape())
                .background(SpotVaultColors.Elevated)
                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), spotVaultDialogShape())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (suggestedMins != null) {
                Text(
                    "OCR suggests ${formatReminderDuration(suggestedMins)} — tap to use",
                    fontSize = 11.sp,
                    color = SpotVaultColors.Teal.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        hours = (suggestedMins / 60).coerceIn(0, 12)
                        minutes = (((suggestedMins % 60) + 2) / 5 * 5).coerceIn(0, 55)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            TimerWheelPicker(
                hours = hours,
                minutes = minutes,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
                onClear = { hours = 0; minutes = 0 },
                modifier = Modifier.width(240.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpotVaultOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = spotVaultButtonShape()
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, color = SpotVaultColors.Muted)
                }
                SpotVaultButton(
                    onClick = { onConfirm(hours * 60 + minutes) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = spotVaultButtonShape()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// internal, not private — reused by AutoParkingSettings.kt for the "Auto-Park Mode" setting,
// same Active Tracking/Quiet Save choice, same control, so the two places in the app that ask
// this exact question don't drift into two different-looking answers to it.
@Composable
internal fun TrackingModeSegmentedToggle(
    isActiveTracking: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // True stadium/pill shape (percent-based corner radius, not a fixed dp) — the ends round
    // fully regardless of the row's actual height, instead of the fixed-dp rounded-rect language
    // used elsewhere on this form (GlassSurface, dropdowns, tag chips). A segmented toggle reads
    // as a single switch rather than two buttons specifically because of that pill silhouette.
    val pillShape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(SpotVaultColors.Deep)
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), pillShape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(true to "Active Tracking", false to "Quiet Save").forEach { (active, label) ->
            val selected = isActiveTracking == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(pillShape)
                    .background(if (selected) SpotVaultColors.Teal else Color.Transparent)
                    .clickable { onModeChange(active) }
                    .padding(vertical = 7.dp, horizontal = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) SpotVaultColors.Ink else SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveScreenCompactDropdown(
    label: String,
    displayValue: String,
    placeholder: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingIconTint: Color = SpotVaultColors.Teal,
    menuContent: @Composable androidx.compose.material3.ExposedDropdownMenuBoxScope.() -> Unit
) {
    // Same compact chip language as the "Add a Vehicle" row — a bordered pill, not a full
    // Material3 OutlinedTextField, which carries a much taller built-in min touch height.
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = leadingIconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = displayValue.ifBlank { placeholder },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (displayValue.isBlank()) SpotVaultColors.Muted else SpotVaultColors.OnSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = SpotVaultColors.Teal,
                modifier = Modifier.size(16.dp)
            )
        }
        menuContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSelectionDialog(
    prefs: android.content.SharedPreferences,
    isCamera: Boolean,
    photoPath: String = "",
    ocrText: String,
    prominentOcrText: String = "",
    onDismiss: () -> Unit,
    onRetake: () -> Unit,
    onPin: (Int, String, String, Boolean, Int?, List<String>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val vehicleLocationDao = remember { AppDatabase.getDatabase(context).locationDao() }
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    var selectedTags by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val activeVehicles by vehicleDao.observeActive().collectAsState(initial = emptyList())
    // rememberSaveable everywhere below that holds anything the user actually typed or picked —
    // a bare remember loses its value across a configuration change (rotation being the most
    // common; MainActivity also has no orientation lock and android:resizeableActivity="true", so
    // an unhandled multi-window resize hits the same path), silently discarding a half-filled
    // Snap/Pin form with no warning.
    var selectedVehicleId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAddVehicle by rememberSaveable { mutableStateOf(false) }

    // Always pre-selects the Primary Default vehicle from Vehicles settings (or no vehicle, if
    // none is set as default) — never whichever vehicle was picked on a previous save. Manual
    // tagging needs to be predictable: a "remembers last used" default meant the chip shown here
    // could silently be whatever was picked for a completely unrelated save, easy to miss and
    // easy to tag the wrong vehicle with.
    LaunchedEffect(activeVehicles) {
        selectedVehicleId = activeVehicles.firstOrNull { it.isDefault }?.id
    }

    val defaultMins = prefs.getString("default_timer_mins", "") ?: ""
    var timerMins by rememberSaveable { mutableStateOf(defaultMins) }
    // Explicit name for the pin, separate from Notes below it. On the Snap flow it starts
    // prefilled with the scanned text (still fully editable) so OCR output lands as the pin's
    // name instead of in Notes; on the Pin flow (no OCR to draw from) it starts blank. Without
    // this, every spot saved from this dialog kept LocationSpot's blank title default and showed
    // up in the Vault as generic "Saved spot" (see vaultSpotDisplayTitle's ifBlank fallback).
    val initialTitle = if (isCamera) prominentOcrText.ifBlank { ocrText } else ""
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = initialTitle, selection = TextRange(initialTitle.length)))
    }
    var note by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val defaultTracking = prefs.getBoolean("default_active_tracking", true)
    var isActiveTracking by rememberSaveable { mutableStateOf(defaultTracking) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val shareScope = rememberCoroutineScope()

    val suggestedMins = if (ocrText.contains("1 hour", ignoreCase = true) || ocrText.contains("60 min", ignoreCase = true)) 60
    else if (ocrText.contains("30 min", ignoreCase = true)) 30
    else if (ocrText.contains("2 hour", ignoreCase = true)) 120
    else null

    var vehicleExpanded by rememberSaveable { mutableStateOf(false) }
    var showPhotoExpanded by rememberSaveable { mutableStateOf(false) }

    // Category is gone from this screen — organizing a spot now happens with tags, picked right
    // here instead of a single fixed label chosen up front. "Drop Pinned" is the same generic
    // fallback title category already fell back to whenever it was left blank, so spots saved
    // from here keep exactly the same default they always did.
    fun confirmPin() {
        val mins = timerMins.toIntOrNull() ?: 0
        onPin(mins, title.text, note.text, isActiveTracking, selectedVehicleId, selectedTags)
    }

    // Same share this screen replaced "Quick Share"/"Share Photo" for — text-only for the Pin
    // flow, with the just-taken photo attached for the Snap flow — just reachable from here
    // instead of a dedicated home-screen row now.
    fun shareCurrentPin() {
        shareScope.launch {
            val located = withContext(Dispatchers.IO) {
                val coords = resolveCurrentLocation(context, prefs) ?: return@withContext null
                val (lat, lng) = coords
                val address = if (prefs.getBoolean("auto_fetch_address", true)) {
                    reverseGeocodeAddress(context, lat, lng).full
                } else ""
                Triple(lat, lng, address)
            }
            if (located == null) {
                Toast.makeText(context, "Location unavailable — enable GPS to share.", Toast.LENGTH_SHORT).show()
            } else {
                val (lat, lng, address) = located
                shareLocation(
                    context, lat, lng, address,
                    notes = note.text,
                    imagePath = if (isCamera) photoPath else "",
                    includePhoto = isCamera
                )
            }
        }
    }

    BackHandler(onBack = onDismiss)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        EnsureDialogEdgeToEdge()
        // Dialog windows can't trust WindowInsets.navigationBars — read the Activity's
        // measured inset from SystemBarInsets instead (same fix as NotepadEditorDialog).
        val density = LocalDensity.current
        val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }.coerceAtLeast(24.dp)
        val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() }.coerceAtLeast(24.dp) + 12.dp
        // Left/right — this screen never accounted for either edge before, so the header's Pin
        // button and the photo/Retake column both rendered flush against the raw physical screen
        // edge with zero clearance: exactly what showed up as the Pin button crammed into the
        // corner and the photo bleeding off the left edge in landscape.
        val leftPad = with(density) { SystemBarInsets.leftPx.toDp() }.coerceAtLeast(8.dp)
        val rightPad = with(density) { SystemBarInsets.rightPx.toDp() }.coerceAtLeast(8.dp)

        // The non-compact layout already handles the keyboard — its fixed bottom action bar
        // carries its own .imePadding() further down. The compact-height branch (short/landscape
        // windows, where that bottom bar is hidden entirely) had no IME handling anywhere: the
        // Title/Notes fields could end up hidden behind the keyboard with no way to see what's
        // being typed, since this Dialog's decorFitsSystemWindows = false means the window never
        // auto-pans on its own. Scoped to only the compact branch rather than the shared root, so
        // it can't double up with the bottom bar's own imePadding() in the already-working case.
        val needsImeFixHere = needsCompactHeightLayout()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotVaultColors.Void)
                .padding(start = leftPad, end = rightPad)
                .then(if (needsImeFixHere) Modifier.imePadding() else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SpotVaultColors.Elevated,
                                SpotVaultColors.Void.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(top = (statusPad - 10.dp).coerceAtLeast(0.dp))
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.Muted)
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCamera) Icons.Default.CameraAlt else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SpotVaultColors.Teal,
                                modifier = Modifier.size(20.dp).padding(end = 6.dp)
                            )
                            Text(
                                if (isCamera) "Save Photo & Pin" else "Save Location",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpotVaultColors.OnSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        // Compact-height only: the fixed bottom button stack (Share/Pin/Retake/
                        // Cancel) is hidden entirely there — the giant bar was eating most of a
                        // short window's vertical space, pushing Notes/Tags off screen before the
                        // form even started. Pin and Share both move up here instead — Share used
                        // to live at the bottom of the scrollable form card, but on real landscape
                        // hardware that fold was proving unreachable, so it's unconditionally
                        // visible here now rather than depending on scroll working correctly.
                        // Cancel is the X already on the left; Retake moves down into the body
                        // (see photoBlock below).
                        if (needsCompactHeightLayout()) {
                            IconButton(onClick = { shareCurrentPin() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = SpotVaultColors.Muted, modifier = Modifier.size(18.dp))
                            }
                            TextButton(onClick = { confirmPin() }) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pin", fontWeight = FontWeight.ExtraBold, color = SpotVaultColors.Teal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SpotVaultColors.PrimaryDeep,
                                        SpotVaultColors.Primary,
                                        SpotVaultColors.Teal,
                                        SpotVaultColors.PrimaryBright
                                    )
                                )
                            )
                    )
            }

            val scrollState = rememberScrollState()
            // BoxWithConstraints wraps the photo+card stack below purely so the card can size
            // itself against real available height rather than being force-stretched. The
            // photo/placeholder is now a fixed-height media header (see mediaHeaderHeight below)
            // instead of a weighted share of the screen, so the card simply claims 100% of
            // whatever's left underneath it — no more hasRealPhoto-driven weight split here.
            val hasRealPhoto = isCamera && photoPath.isNotEmpty()

            // Extracted so the photo/placeholder and the input card can be laid out either
            // stacked (portrait — see the BoxWithConstraints/weight split below) or
            // side by side (landscape, or any short window) without duplicating either block's
            // own internals. Stacking them vertically in a short landscape window used to leave
            // the fixed-height photo/placeholder eating almost the entire available height,
            // leaving the notes/tags/vehicle card only a sliver to work with — this is what made
            // "the card" effectively disappear there.
            val isCompact = needsCompactHeightLayout()
            // Unified media header shape for the Pin flow's gradient placeholder and the Snap
            // flow's photo — they used to size completely differently in the portrait/stacked
            // layout (photo: weight(0.4f) of whatever vertical space was left, often 250-350dp;
            // placeholder: a fixed 140dp). VaultNoPhotoPlaceholder renders its graphic AT
            // whatever exact box size it's given rather than stretching a fixed-aspect image
            // into it, so that unusually wide/short 140dp box is what actually produced the
            // "squashed" placeholder graphic — not a stretched bitmap, but one rendered to an
            // aspect ratio it was never designed for. Only applies to the portrait/stacked
            // layout below — compact-height (landscape/short) already sizes both cases
            // identically via weight(1f) of its own column, so it isn't affected by this bug and
            // a fixed height there would fight the whole reason that side-by-side layout exists.
            // Mutable, not a fixed val — the non-compact BoxWithConstraints below overwrites this
            // with a height scaled to its own real available space before invoking photoBlock(),
            // so a short window (a real Note 20-class phone, or a compact multi-window slice) gets
            // a smaller header instead of this fixed 200dp alone forcing the Title/Notes/Tags/
            // Vehicles/Reminder/Tracking card below it into a scroll it wouldn't otherwise need.
            // Untouched (stays 200dp) on anything roomy — every larger phone and tablet renders
            // pixel-identical to before this.
            var mediaHeaderHeight = 200.dp
            val photoBlock: @Composable () -> Unit = {
                Column(
                    modifier = if (isCompact) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
                ) {
                    val mediaModifier = if (isCompact) {
                        Modifier.fillMaxWidth().weight(1f)
                    } else {
                        Modifier.fillMaxWidth().height(mediaHeaderHeight)
                    }
                    if (hasRealPhoto) {
                        Box(
                            modifier = mediaModifier
                                .background(SpotVaultColors.Void)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showPhotoExpanded = true }
                        ) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(java.io.File(photoPath)),
                                contentDescription = "Captured photo — tap to enlarge",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Tap to enlarge",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Retake now lives on the photo itself, opposite "Tap to enlarge" —
                            // its own clickable, nested inside the photo's — Compose resolves a
                            // tap to whichever clickable is innermost, so this consumes the tap
                            // before it reaches the photo's own "tap to expand" handler. Freeing
                            // up the separate full-width row this used to sit in (below the whole
                            // photo block) means the card underneath gets that space back instead.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(onClick = onRetake)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Retake Photo",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // Covers both the Pin flow (no photo by design) and the rare case where a
                        // Snap capture reaches this dialog with no photo file.
                        VaultNoPhotoPlaceholder(
                            modifier = mediaModifier.clip(RoundedCornerShape(16.dp))
                        )
                        // No photo to overlay Retake onto here, so it falls back to its own row
                        // below — only reachable when a Snap capture somehow reaches this dialog
                        // with no photo file at all (isCamera true, hasRealPhoto false).
                        if (isCamera) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = onRetake) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retake Photo", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            val cardBlock: @Composable () -> Unit = {
                var saveScreenViewportPx by remember { mutableStateOf(0) }
                Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { saveScreenViewportPx = it.height }
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .padding(end = 10.dp),
                            // Pin flow (no real photo) in the non-compact/portrait layout: the
                            // card's content is naturally shorter than the weight(1f) height it's
                            // given here, and top-alignment (the default) left the leftover space
                            // as a bare gap between the Tracking toggle and the fixed Share/Pin bar
                            // below it. Bottom-anchoring puts that same leftover space above the
                            // card instead — next to the placeholder graphic, where it reads as
                            // normal breathing room instead of a stray gap right before the action
                            // buttons. Only one child (GlassSurface) sits in this Column, so this
                            // is purely about where slack space goes, not about spacing between
                            // siblings.
                            verticalArrangement = if (!isCompact && !hasRealPhoto) Arrangement.Bottom else Arrangement.spacedBy(6.dp)
                        ) {
                            GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Explicit name for the pin — separate from Notes/Scanned Text
                                // below, and from LocationSpot.title, which this dialog left blank
                                // until now (every spot saved here showed up in the Vault as the
                                // generic "Saved spot" fallback). singleLine keeps it to one row
                                // regardless of how much is typed.
                                VaultCompactNotesFieldWithExpand(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = "Title",
                                    placeholder = "Name this pin",
                                    minLines = 1,
                                    maxLines = 1,
                                    singleLine = true
                                )
                                // Same Notes field for both flows now — Snap's scanned text goes
                                // to Title above (see initialTitle), so there's no separate
                                // "Scanned Text" field left to branch on. No leadingIcon, matching
                                // Title right above it.
                                VaultCompactNotesFieldWithExpand(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = "Notes",
                                    placeholder = "Bathroom L2, beach by red buoy…",
                                    minLines = 1,
                                    maxLines = 4
                                )

                                androidx.compose.material3.HorizontalDivider(
                                    color = SpotVaultColors.Outline.copy(alpha = 0.22f),
                                    thickness = 1.dp
                                )

                                // Tags and Vehicles side by side, one column each, same headers —
                                // two full-width stacked sections (each with its own label, chip
                                // row/dropdown, and divider) was the single biggest driver of this
                                // card's height. Reminder tucks under Vehicles in the right column
                                // rather than getting a third column of its own: it's already just
                                // a single compact inline control (no separate section label — the
                                // "REMINDER" text IS the control), and a third narrow column here
                                // would be the first thing to break on a narrow phone. No font
                                // sizes changed anywhere in this block.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SaveScreenSectionLabel("Tags")
                                        SaveScreenTagField(
                                            selectedTags = selectedTags,
                                            onSelectedTagsChange = { selectedTags = it },
                                            allTags = allTags
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SaveScreenSectionLabel("Vehicles")
                                        if (activeVehicles.isNotEmpty()) {
                                            val selectedVehicle = activeVehicles.firstOrNull { it.id == selectedVehicleId }
                                            SaveScreenCompactDropdown(
                                                label = "Vehicle",
                                                displayValue = selectedVehicle?.name ?: "None",
                                                placeholder = "Vehicle",
                                                expanded = vehicleExpanded,
                                                onExpandedChange = {
                                                    vehicleExpanded = it
                                                    if (vehicleExpanded) {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                    }
                                                },
                                                leadingIcon = selectedVehicle?.let { vehicleIconVector(it.iconKey) },
                                                leadingIconTint = selectedVehicle?.let { Color(it.colorArgb) } ?: SpotVaultColors.Teal,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                ExposedDropdownMenu(
                                                    expanded = vehicleExpanded,
                                                    onDismissRequest = { vehicleExpanded = false },
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(SpotVaultColors.Elevated)
                                                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                ) {
                                                    PolishedDropdownItem(
                                                        label = "None",
                                                        icon = Icons.Default.Close,
                                                        selected = selectedVehicleId == null,
                                                        onClick = {
                                                            selectedVehicleId = null
                                                            vehicleExpanded = false
                                                        }
                                                    )
                                                    PolishedDropdownDivider()
                                                    activeVehicles.forEach { vehicle ->
                                                        PolishedDropdownItem(
                                                            label = vehicle.name,
                                                            icon = vehicleIconVector(vehicle.iconKey),
                                                            iconTint = Color(vehicle.colorArgb),
                                                            selected = vehicle.id == selectedVehicleId,
                                                            onClick = {
                                                                selectedVehicleId = vehicle.id
                                                                vehicleExpanded = false
                                                            }
                                                        )
                                                    }
                                                    PolishedDropdownDivider()
                                                    PolishedDropdownItem(
                                                        label = "Add Vehicle…",
                                                        icon = Icons.Default.Add,
                                                        accent = true,
                                                        onClick = {
                                                            vehicleExpanded = false
                                                            showAddVehicle = true
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                                    .clickable { showAddVehicle = true }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = SpotVaultColors.Teal,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    "Add Vehicle",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SpotVaultColors.OnSurface,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Centered across the full card width, not tucked inside the
                                // Vehicles column — left-aligned within that column put it flush
                                // against the card's right half, reading as oddly shoved to one
                                // side. Still the same row in the sequence (right after Tags/
                                // Vehicles, before Tracking) — only its horizontal position moved.
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    SaveScreenReminderControl(
                                        selectedMins = timerMins.toIntOrNull() ?: 0,
                                        suggestedMins = if (isCamera) suggestedMins else null,
                                        isCamera = isCamera,
                                        onSelect = { mins -> timerMins = if (mins > 0) mins.toString() else "" }
                                    )
                                }

                                androidx.compose.material3.HorizontalDivider(
                                    color = SpotVaultColors.Outline.copy(alpha = 0.22f),
                                    thickness = 1.dp
                                )

                                SaveScreenSectionLabel("Tracking")
                                TrackingModeSegmentedToggle(
                                    isActiveTracking = isActiveTracking,
                                    onModeChange = { isActiveTracking = it }
                                )

                            }
                        }
                    }

                    if (scrollState.maxValue > 0) {
                        PremiumVerticalScrollbar(
                            scrollState = scrollState,
                            viewportHeightPx = saveScreenViewportPx,
                            emphasizeHint = true,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .width(8.dp)
                        )
                    }

                    if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - 8) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            SpotVaultColors.Void.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll for more",
                            tint = SpotVaultColors.Teal.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            if (isCompact) {
                // Side by side instead of stacked — see the comment above photoBlock/cardBlock.
                // The photo/placeholder no longer competes with the card for a share of a short
                // window's scarce height; each gets the full height of its own column instead,
                // with the card's own scroll (already built in above) as the safety net for
                // anything that still doesn't fit.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) { photoBlock() }
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) { cardBlock() }
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // 30% of the real space available to photo+card combined, clamped so it never
                    // grows past the original fixed 200dp (rooms with real height to spare stay
                    // exactly as they were) or shrinks below 130dp (VaultNoPhotoPlaceholder renders
                    // its graphic straight into whatever box it's given — see the squashing note
                    // above — and 130dp is close to what that placeholder used pre-unification, so
                    // it's a floor already known to look fine, not a new guess).
                    mediaHeaderHeight = (maxHeight * 0.3f).coerceIn(130.dp, 200.dp)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // photoBlock is unwrapped now — it sizes its own media element to
                        // mediaHeaderHeight internally (same value, hasRealPhoto or not), so it no
                        // longer needs an outer Box competing for a weighted share of this column's
                        // height. The card takes 100% of whatever's left below it, always — no more
                        // hasRealPhoto branch here either.
                        photoBlock()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            cardBlock()
                        }
                    }
                }
            }

            // Hidden entirely in the compact-height layout — Pin moved into the header, Cancel is
            // the X already there, and Share/Retake moved into the body (cardBlock/photoBlock
            // above). This whole bar used to be the single biggest consumer of a short window's
            // vertical space, routinely pushing Notes and Tags off screen before the form even
            // started.
            if (!isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpotVaultColors.Surface.copy(alpha = 0.98f))
                        .border(
                            width = 1.dp,
                            color = SpotVaultColors.Outline.copy(alpha = 0.35f)
                        )
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = navPad),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Was inside the scrollable card, right after the Tracking toggle — its position
                    // depended on scroll offset and how much other content was above it. Living in the
                    // fixed bottom bar instead means it's always right above the Pin/Share buttons,
                    // regardless of scroll position or card content.
                    Text(
                        text = if (isActiveTracking) {
                            "Live background tracking & alerts"
                        } else {
                            "Silent save — no alerts"
                        },
                        fontSize = 10.sp,
                        color = SpotVaultColors.Muted.copy(alpha = 0.85f),
                        lineHeight = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Small square button on the left — does the same share (text-only for Pin,
                        // with the photo attached for Snap) that "Quick Share"/"Share Photo" used to
                        // do from the home screen, now reachable right from this confirmation screen
                        // instead. Pin keeps its own height but no longer spans the full width, so
                        // this has somewhere to sit.
                        SpotVaultButton(
                            onClick = { shareCurrentPin() },
                            modifier = Modifier
                                // Was 0.62f — "Share" (5 letters) is the longer of the two labels
                                // but had the smaller share of the row's width, which is exactly
                                // backwards and is what pushed it into truncating to "Shar…" on a
                                // narrower/differently-scaled screen. Still meaningfully smaller
                                // than Pin's 1f (Share stays the visually secondary action), just
                                // no longer working against its own label length.
                                .weight(0.8f)
                                .height(52.dp),
                            shape = spotVaultButtonShape(),
                            // Tighter than Material3's default 24dp/side ContentPadding — same fix
                            // as the home screen's Quick Pin/Quick Track row: two icon+text buttons
                            // sharing a row don't have room to spare for a single-button's worth of
                            // padding on each one, and this is what was actually squeezing "Share"
                            // into "Shar…" (the 0.8f/1f weight rebalance alone wasn't the full story).
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            // No explicit contentColor — same as the home screen's Quick Track button
                            // (also a Teal-filled SpotVaultButton), which lets Material3 derive the
                            // text/icon color from the active theme's palette instead of forcing a
                            // fixed white that ignored whatever theme was picked.
                            colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Teal)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share this spot", modifier = Modifier.padding(end = 6.dp))
                            Text("Share", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SpotVaultButton(
                            onClick = { confirmPin() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = spotVaultButtonShape(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Pin", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // Retake now lives directly under the photo (photoBlock above) and Cancel is
                    // gone entirely — the system back gesture and the X in the header already
                    // cancel this screen, so a duplicate full-width button for it was pure
                    // vertical space spent on something every Android user already has a faster
                    // way to do. The bottom block is Share/Pin only now.
                }
            }
        }
    }

    if (showAddVehicle) {
        BackHandler(onBack = { showAddVehicle = false })
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddVehicle = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            EnsureDialogEdgeToEdge()
            val density = androidx.compose.ui.platform.LocalDensity.current
            val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
            val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpotVaultColors.Void)
                    .padding(top = statusPad, bottom = navPad)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                VehicleEditScreen(
                    vehicleId = null,
                    vehicleDao = vehicleDao,
                    locationDao = vehicleLocationDao,
                    onBack = { showAddVehicle = false }
                )
            }
        }
    }

    if (showPhotoExpanded && isCamera && photoPath.isNotEmpty()) {
        BackHandler(onBack = { showPhotoExpanded = false })
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPhotoExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            EnsureDialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showPhotoExpanded = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = java.io.File(photoPath),
                    contentDescription = "Enlarged captured photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showPhotoExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                }
            }
        }
    }
}

/** Compact, themed dropdown row shared by the category/tag pickers on the save screen. */
@Composable
private fun PolishedDropdownItem(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = SpotVaultColors.Teal,
    selected: Boolean = false,
    accent: Boolean = false
) {
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = when {
                    accent -> SpotVaultColors.Teal
                    selected -> SpotVaultColors.PrimaryBright
                    else -> SpotVaultColors.OnSurface
                },
                fontWeight = if (selected || accent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp)) }
        } else null,
        trailingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(14.dp)) }
        } else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
        onClick = { performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs); onClick() },
        modifier = Modifier
            .heightIn(min = 32.dp)
            .padding(horizontal = 4.dp, vertical = 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.background(SpotVaultColors.Primary.copy(alpha = 0.14f)) else Modifier
            )
    )
}

@Composable
private fun PolishedDropdownDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        color = SpotVaultColors.Outline.copy(alpha = 0.3f)
    )
}

fun spotVaultTypography(fontFamily: androidx.compose.ui.text.font.FontFamily): androidx.compose.material3.Typography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 45.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        fontSize = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    // labelLarge is what Material3 buttons default their text style to — leaving these three
    // unset (as this function used to) meant every button silently fell back to Typography's
    // own default FontFamily no matter what font was picked in Settings, even though every
    // display/headline/title/body style above it was already correctly overridden. That's
    // exactly why titles and body text picked up a font change but buttons never did.
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = fontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun SpotVaultTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val themeColors = if (ThemeState.dynamicColorEnabled && ThemeState.dynamicColorAvailable) {
        ThemeState.dynamicScheme()
    } else {
        when (ThemeState.currentTheme) {
            "neon_pink" -> NeonPinkColors
            "yin_yang" -> YinYangColors
            "crimson_gold" -> CrimsonGoldColors
            "gold_cobalt" -> GoldCobaltColors
            "camo_hunt" -> CamoHuntColors
            "forest_nature" -> ForestNatureColors
            "halloween" -> HalloweenColors
            "christmas" -> ChristmasColors
            "custom" -> ThemeState.customScheme()
            else -> PurpleTealColors
        }
    }
    
    androidx.compose.runtime.CompositionLocalProvider(LocalSpotVaultColors provides themeColors) {
        val darkColors = darkColorScheme(
            background = LocalSpotVaultColors.current.Void,
            onBackground = LocalSpotVaultColors.current.OnSurface,
            surface = LocalSpotVaultColors.current.Surface,
            onSurface = LocalSpotVaultColors.current.OnSurface,
            surfaceVariant = LocalSpotVaultColors.current.Elevated,
            onSurfaceVariant = LocalSpotVaultColors.current.Muted,
            primary = LocalSpotVaultColors.current.Primary,
            onPrimary = SpotVaultColors.OnPrimary,
            primaryContainer = LocalSpotVaultColors.current.PrimaryDeep,
            onPrimaryContainer = LocalSpotVaultColors.current.PrimaryBright,
            secondary = LocalSpotVaultColors.current.Teal,
            onSecondary = LocalSpotVaultColors.current.Ink,
            secondaryContainer = LocalSpotVaultColors.current.TealDeep,
            onSecondaryContainer = LocalSpotVaultColors.current.TealSoft,
            outline = LocalSpotVaultColors.current.Outline,
            surfaceTint = LocalSpotVaultColors.current.Primary,
            error = LocalSpotVaultColors.current.Danger,
            onError = Color.White
        )
    
    val lightColors = lightColorScheme(
        background = Color(0xFFF7F2FF),
        onBackground = Color(0xFF1A1228),
        surface = Color(0xFFF0EAF8),
        onSurface = Color(0xFF1A1228),
        surfaceVariant = Color(0xFFE4DCF0),
        onSurfaceVariant = Color(0xFF4A4458),
        primary = Color(0xFF6B2FFF),
        onPrimary = SpotVaultColors.OnPrimary,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF00838F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB2EBF2),
        onSecondaryContainer = Color(0xFF001F24),
        outline = Color(0xFF79747E),
        surfaceTint = Color(0xFF6B2FFF)
    )

    val shapes = androidx.compose.material3.Shapes(
        extraSmall = spotVaultThemeButtonShape(SpotVaultButtonShapeSize.Small),
        small = spotVaultThemeButtonShape(SpotVaultButtonShapeSize.Medium),
        medium = spotVaultThemeButtonShape(SpotVaultButtonShapeSize.Large),
        large = spotVaultThemeButtonShape(SpotVaultButtonShapeSize.Dialog),
        extraLarge = spotVaultThemeButtonShape(SpotVaultButtonShapeSize.Dialog)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = spotVaultTypography(AppFontOptions.familyFor(ThemeState.fontFamilyId)),
        shapes = shapes,
        content = {
            content()
        }
    )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SpotVaultScreen(
    modifier: Modifier = Modifier, 
    isPinned: Boolean,
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit,
    onFoundClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit = {},
    prefs: android.content.SharedPreferences,
    dao: LocationDao,
    onRequestLocationPermission: () -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    onNavigateToCompass: (spotId: Int, lat: Double, lng: Double) -> Unit = { _, _, _ -> },
    onOpenAutoParkSettings: () -> Unit = {},
    onOpenHelpSettings: () -> Unit = {}
) {
    var photoPath by remember { mutableStateOf(prefs.getString("photo_path", "") ?: "") }
    var locationDetails by remember { mutableStateOf(prefs.getString("location_details", "") ?: "") }
    var currentAddress by remember { mutableStateOf(prefs.getString("current_address", "") ?: "") }
    var isAlarmRinging by remember { mutableStateOf(prefs.getBoolean("is_alarm_ringing", false)) }
    var lat by remember { mutableStateOf(prefs.getFloat("lat", 0f)) }
    var lng by remember { mutableStateOf(prefs.getFloat("lng", 0f)) }
    var vaultDisplayName by remember { mutableStateOf(loadVaultDisplayNameFromPrefs(prefs)) }
    val context = LocalContext.current
    val autoParkEnabled by remember(prefs) { prefs.observeAutoParkEnabled() }
        .collectAsStateWithLifecycle(initialValue = isAutoParkEnabled(prefs))
    val motionAutoParkEnabled by remember(prefs) { prefs.observeMotionAutoParkEnabled() }
        .collectAsStateWithLifecycle(initialValue = isMotionAutoParkEnabled(prefs))
    var showAutoParkToggleSheet by remember { mutableStateOf(false) }
    var showMotionToggleSheet by remember { mutableStateOf(false) }
    // Needed only to arm the motion watch immediately when this sheet's own toggle flips Motion &
    // Fitness on — see that onCheckedChange below for why. Mirrors AutoParkingSettings.kt's own
    // vehiclesWithBluetooth/legacyMac exactly.
    val homeVehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val homeLinkedVehicles by homeVehicleDao.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val homeVehiclesWithBluetooth = remember(homeLinkedVehicles) {
        homeLinkedVehicles.filter { !it.bluetoothMac.isNullOrBlank() }
    }
    val homeLegacyMac = remember { loadAutoParkCarMac(prefs) }
    // Requesting ACTIVITY_RECOGNITION directly from this sheet instead of bouncing to Automatic
    // Parking settings — Motion only needs this one runtime permission (unlike the master
    // Auto-Park toggle's longer location/Bluetooth/battery chain), so there's no reason flipping
    // it on here can't just prompt in place. Mirrors AutoParkingSettings.kt's own
    // activityRecognitionLauncher grant handling.
    val homeActivityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, true).commit()
            homeVehiclesWithBluetooth.forEach { armMotionWatchIfAlreadyConnected(context, it.bluetoothMac!!) }
            homeLegacyMac?.let { armMotionWatchIfAlreadyConnected(context, it) }
            WidgetThemeHelper.bumpWidgetRevision(prefs)
            WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
        }
    }

    // Automatic Parking's actual background pipeline (CarBluetoothReceiver) hard-requires
    // ACCESS_BACKGROUND_LOCATION to do anything — without it every save silently fails, forever,
    // with no error the user would ever see. Unlike Motion's single ACTIVITY_RECOGNITION prompt,
    // this is a multi-step chain (fine location, then a background-location primer, then Bluetooth
    // connect, then a battery-optimization exemption ask) that already exists in full on the
    // Automatic Parking settings screen — stacking that whole sequence as system dialogs over a
    // small home-screen sheet would be a worse experience than just sending the user there. So
    // this sheet only ever flips the switch on when every prerequisite is already satisfied;
    // otherwise it routes to Settings to complete the chain, the same way Motion's toggle used to
    // for its one permission. Mirrors AutoParkingSettings.kt's own `armed` computation exactly.
    var homeFineLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var homeBackgroundLocationGranted by remember { mutableStateOf(hasBackgroundLocationPermission(context)) }
    var homeBluetoothConnectGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val homeHasLinkedVehicle = homeVehiclesWithBluetooth.isNotEmpty() || !homeLegacyMac.isNullOrBlank()
    val homeAutoParkArmable = homeHasLinkedVehicle &&
        homeFineLocationGranted &&
        homeBackgroundLocationGranted &&
        homeBluetoothConnectGranted

    // Permissions granted from the system Settings screen this sheet redirects to have no launcher
    // callback here — re-reading on resume is what makes "grant it, come back" actually update
    // homeAutoParkArmable without needing to leave and reopen the app.
    val homeLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(homeLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                homeFineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                homeBackgroundLocationGranted = hasBackgroundLocationPermission(context)
                homeBluetoothConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        homeLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { homeLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.runtime.LaunchedEffect(isPinned) {
        if (isPinned) {
            photoPath = prefs.getString("photo_path", "") ?: ""
            locationDetails = prefs.getString("location_details", "") ?: ""
            currentAddress = prefs.getString("current_address", "") ?: ""
            isAlarmRinging = prefs.getBoolean("is_alarm_ringing", false)
            lat = prefs.getFloat("lat", 0f)
            lng = prefs.getFloat("lng", 0f)
        }
    }
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "current_address" && sharedPreferences.contains(key)) {
                currentAddress = sharedPreferences.getString("current_address", "") ?: ""
            } else if (key == "location_details" && sharedPreferences.contains(key)) {
                locationDetails = sharedPreferences.getString("location_details", "") ?: ""
            } else if (key == "is_alarm_ringing" && sharedPreferences.contains(key)) {
                isAlarmRinging = sharedPreferences.getBoolean("is_alarm_ringing", false)
            } else if (key == "lat" && sharedPreferences.contains(key)) {
                lat = sharedPreferences.getFloat("lat", 0f)
            } else if (key == "lng" && sharedPreferences.contains(key)) {
                lng = sharedPreferences.getFloat("lng", 0f)
            } else if (key == "photo_path" && sharedPreferences.contains(key)) {
                photoPath = sharedPreferences.getString("photo_path", "") ?: ""
            } else if (key == VAULT_DISPLAY_NAME_PREF) {
                vaultDisplayName = loadVaultDisplayNameFromPrefs(sharedPreferences)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Short screens need every dp they can get for the CTA cards, so the brand
    // header gives up its breathing room first.
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val headerSpacing = if (screenHeightDp < 620) 8.dp else 18.dp

    if (showAutoParkToggleSheet) {
        AlertDialog(
            onDismissRequest = { showAutoParkToggleSheet = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Bluetooth Auto Disconnect") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            if (autoParkEnabled) "On — saves a pin when your car disconnects" else "Off",
                            color = SpotVaultColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Respects your Quiet Zones and only watches linked vehicles.",
                            fontSize = 12.sp,
                            color = SpotVaultColors.Muted
                        )
                    }
                    Switch(
                        checked = autoParkEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !homeAutoParkArmable) {
                                showAutoParkToggleSheet = false
                                onOpenAutoParkSettings()
                                return@Switch
                            }
                            prefs.edit().putBoolean(AUTO_PARK_ENABLED_PREF, enabled).commit()
                            WidgetThemeHelper.bumpWidgetRevision(prefs)
                            WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
                        },
                        colors = spotVaultSwitchColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoParkToggleSheet = false; onOpenAutoParkSettings() }) {
                    Text("Open Settings", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoParkToggleSheet = false }) {
                    Text("Done", color = SpotVaultColors.Muted)
                }
            }
        )
    }

    if (showMotionToggleSheet) {
        AlertDialog(
            onDismissRequest = { showMotionToggleSheet = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Motion & Fitness") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            if (motionAutoParkEnabled) "On — drops a pin the moment you start walking away" else "Off",
                            color = SpotVaultColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Only active while connected to a linked car's Bluetooth, and follows the same Quiet Zones.",
                            fontSize = 12.sp,
                            color = SpotVaultColors.Muted
                        )
                    }
                    Switch(
                        checked = motionAutoParkEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasActivityRecognitionPermission(context)) {
                                homeActivityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                return@Switch
                            }
                            prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, enabled).commit()
                            if (!enabled) {
                                stopMotionWatch(context, prefs)
                            } else {
                                // Without this, flipping the toggle on here (rather than from the
                                // Automatic Parking settings screen, which already does this) left
                                // the watch silently unarmed for the whole current trip if the car
                                // was already connected — it would only pick up on the *next*
                                // Bluetooth reconnect, despite this switch showing "On" right now.
                                homeVehiclesWithBluetooth.forEach { armMotionWatchIfAlreadyConnected(context, it.bluetoothMac!!) }
                                homeLegacyMac?.let { armMotionWatchIfAlreadyConnected(context, it) }
                            }
                            WidgetThemeHelper.bumpWidgetRevision(prefs)
                            WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
                        },
                        colors = spotVaultSwitchColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMotionToggleSheet = false; onOpenAutoParkSettings() }) {
                    Text("Open Settings", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMotionToggleSheet = false }) {
                    Text("Done", color = SpotVaultColors.Muted)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val homeScreenColumn: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(
                            androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(top = headerSpacing, bottom = 0.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = headerSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    SpotVaultBrandHeader(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        displayName = vaultDisplayName
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HomeHeaderTogglePill(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help & Guide",
                            active = false,
                            onClick = onOpenHelpSettings
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        HomeHeaderTogglePill(
                            icon = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Auto Disconnect",
                            active = autoParkEnabled,
                            onClick = { showAutoParkToggleSheet = true },
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        HomeHeaderTogglePill(
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = "Motion & Fitness",
                            active = motionAutoParkEnabled,
                            onClick = { showMotionToggleSheet = true },
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        }
                    }
                }

                androidx.compose.animation.AnimatedContent(
                    targetState = isPinned,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(tween(320, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(240, easing = FastOutSlowInEasing))
                    },
                    label = "homeModeSwitch"
                ) { pinned ->
                    if (pinned) {
                        PinnedStateView(
                            photoPath = photoPath,
                            locationDetails = locationDetails,
                            currentAddress = currentAddress,
                            lat = lat.toDouble(),
                            lng = lng.toDouble(),
                            isAlarmRinging = isAlarmRinging,
                            onFoundClick = onFoundClick,
                            onPhotoClick = onPhotoClick,
                            onShareRequest = onShareRequest,
                            onNavigateToCompass = onNavigateToCompass,
                            prefs = prefs
                        )
                    } else {
                        EmptyStateView(
                            onSnapClick = onSnapClick,
                            onPinOnlyClick = onPinOnlyClick
                        )
                    }
                }
            }
        }
        AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
            homeScreenColumn()
        }
    }
}

@Composable
private fun HomeHeaderTogglePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp
) {
    val tint = if (active) SpotVaultColors.Teal else SpotVaultColors.Muted
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) SpotVaultColors.Teal.copy(alpha = 0.16f) else SpotVaultColors.Elevated.copy(alpha = 0.5f))
            .border(1.dp, tint.copy(alpha = if (active) 0.6f else 0.25f), CircleShape)
            // On/off state used to be conveyed by tint/background alone (an unlabeled teal vs.
            // muted circle) — invisible to TalkBack, which read the same static description
            // regardless of state, and unreliable for colorblind users. stateDescription makes
            // "on"/"off" part of what actually gets announced.
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = if (active) "On" else "Off"
                this.role = androidx.compose.ui.semantics.Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Purely informational, self-dismissing confirmation for a Quiet Save — no buttons, since the
 * Snap/Pin screen it followed already has its own Share button right there. */
@Composable
private fun SavedConfirmationBanner(
    spotHasPhoto: Boolean,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(1600)
        visible = false
        kotlinx.coroutines.delay(220)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            GlassSurface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SpotVaultColors.Teal.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (spotHasPhoto) "Photo & spot saved" else "Spot saved",
                        color = SpotVaultColors.OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstantActionsRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    val dao = remember { AppDatabase.getDatabase(context).locationDao() }
    val scope = rememberCoroutineScope()

    var pendingTrackPhotoPath by remember { mutableStateOf("") }
    // Neither button changed appearance or blocked input while its save was in flight — with GPS
    // resolution taking anywhere from near-instant to several seconds, an impatient second tap
    // (very plausible with zero visual feedback in that window) launched a second independent save
    // coroutine, landing two Vault entries for what was meant to be one tap. Both buttons below
    // gate on this and pass it to `enabled`, and every exit path (saved, failed, permission denied,
    // photo canceled) resets it so a real failure never leaves the buttons stuck disabled.
    var isSaving by remember { mutableStateOf(false) }

    fun handleQuietResult(result: QuietSaveResult) {
        when (result) {
            is QuietSaveResult.Saved -> Toast.makeText(context, "📍 Pin saved — find it in your Vault", Toast.LENGTH_SHORT).show()
            QuietSaveResult.Failed -> Toast.makeText(context, "Could not save pin. Try again.", Toast.LENGTH_SHORT).show()
            QuietSaveResult.NeedsNotificationPermission -> Unit
        }
    }

    fun runQuickPin(photoPath: String = "") {
        isSaving = true
        scope.launch {
            try {
                handleQuietResult(quietSaveTacticalPin(context, dao, prefs, GENERIC_QUICK_PIN, photoPath = photoPath))
            } finally {
                isSaving = false
            }
        }
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            isSaving = false
            Toast.makeText(
                context,
                "Notification permission is required for active tracking.",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val option = quickTrackOption()
                when (quickActiveTrackPin(context, dao, prefs, option, photoPath = pendingTrackPhotoPath)) {
                    is QuietSaveResult.Saved -> Toast.makeText(context, "🎯 Tracking pin saved", Toast.LENGTH_SHORT).show()
                    QuietSaveResult.Failed -> Toast.makeText(context, "Could not save pin. Try again.", Toast.LENGTH_SHORT).show()
                    QuietSaveResult.NeedsNotificationPermission -> Unit
                }
            } finally {
                isSaving = false
            }
        }
    }

    fun runQuickTrack(photoPath: String = "") {
        isSaving = true
        scope.launch {
            val option = quickTrackOption()
            when (val result = quickActiveTrackPin(context, dao, prefs, option, photoPath = photoPath)) {
                is QuietSaveResult.Saved -> {
                    Toast.makeText(context, "🎯 Tracking pin saved", Toast.LENGTH_SHORT).show()
                    isSaving = false
                }
                QuietSaveResult.NeedsNotificationPermission -> {
                    // Not terminal — isSaving stays true through the permission round trip,
                    // reset by notificationPermissionLauncher's callback above once that resolves.
                    pendingTrackPhotoPath = photoPath
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Toast.makeText(context, "Could not start tracking notifications.", Toast.LENGTH_SHORT).show()
                        isSaving = false
                    }
                }
                QuietSaveResult.Failed -> {
                    Toast.makeText(context, "Could not save pin. Try again.", Toast.LENGTH_SHORT).show()
                    isSaving = false
                }
            }
        }
    }

    // Camera capture shared by both buttons — which save happens on the resulting photo is
    // decided by pendingPhotoAction, set right before the permission/capture round trip starts.
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoAction by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        // Matches the AppLockGate.begin() in startCamera() below — this pending state
        // (pendingPhotoFile/pendingPhotoAction) lives in Compose remember{}, not an Activity
        // field, so if App Lock re-locked while the camera app had the foreground, AppLockScreen
        // swapping in would tear this composable down and silently lose the photo/pin it was for.
        AppLockGate.end()
        val file = pendingPhotoFile
        val action = pendingPhotoAction
        pendingPhotoFile = null
        pendingPhotoAction = null
        if (success && file != null && file.exists()) {
            when (action) {
                "pin" -> runQuickPin(file.absolutePath)
                "track" -> runQuickTrack(file.absolutePath)
            }
        } else {
            isSaving = false
            Toast.makeText(context, "Photo canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCamera() {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val newFile = File(imagesDir, "quickpin_${System.currentTimeMillis()}.jpg")
        pendingPhotoFile = newFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
        try {
            takePictureLauncher.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            // Mirrors takePictureLauncher's own callback cleanup — an immediate launch failure
            // never reaches that callback, so this has to release AppLockGate itself (requestCameraThenCapture
            // already called begin() before this ran) or App Lock's re-lock-on-background check
            // would stay suppressed for the rest of the session.
            pendingPhotoFile = null
            pendingPhotoAction = null
            isSaving = false
            AppLockGate.end()
            Toast.makeText(context, "No camera app found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            // Round trip ends here on denial — startCamera() (and its own AppLockGate.end() in
            // takePictureLauncher) never runs in this branch.
            AppLockGate.end()
            isSaving = false
            val activity = context as? Activity
            if (activity != null && isPermissionPermanentlyDenied(activity, prefs, Manifest.permission.CAMERA)) {
                showPermissionSettingsDialog(activity, "Camera access is needed to attach a photo. Enable it for DropPin Vault in Settings.")
            } else {
                Toast.makeText(context, "Camera permission is required to attach a photo.", Toast.LENGTH_SHORT).show()
            }
            pendingPhotoAction = null
        }
    }

    fun requestCameraThenCapture(action: String) {
        isSaving = true
        pendingPhotoAction = action
        // Covers both the permission dialog and the camera capture that can follow it — either
        // one taking the foreground while App Lock is on must not let AppLockScreen swap in and
        // tear down this composable's pendingPhotoFile/pendingPhotoAction state mid-flight.
        AppLockGate.begin()
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            startCamera()
        } else {
            markPermissionRequested(prefs, Manifest.permission.CAMERA)
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Centered so Quick Track's shadow (see below) has equal breathing room top and bottom
    // instead of being clipped flush against the row's own bounds — that clipping was what made
    // the bottom of both buttons look "cut off": Quick Track's glow ran straight into the row's
    // exact-height edge with zero margin, and Quick Pin matched the row at 100% height too.
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpotVaultButton(
            onClick = {
                if (!isSaving) {
                    if (prefs.getBoolean("quick_pin_auto_photo", false)) {
                        requestCameraThenCapture("pin")
                    } else {
                        runQuickPin()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.weight(1f).fillMaxHeight(0.88f),
            shape = spotVaultButtonShape(),
            // Material3's default ButtonDefaults.ContentPadding (24dp each side) is sized for a
            // single full-width button, not two sharing a row with an icon each — on a narrower
            // phone that alone ate enough width to ellipsize "Quick Pin" down to "Quick …" even
            // though the row itself had fillMaxWidth(). Tighter padding here gives the label room
            // without touching the row's own sizing.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Primary, contentColor = SpotVaultColors.ButtonLabel)
        ) {
            Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Quick Pin", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // A bit wider than Quick Pin — teal glass shell reads as the recommended pick.
        SpotVaultButton(
            onClick = {
                if (!isSaving) {
                    if (prefs.getBoolean("quick_track_auto_photo", false)) {
                        requestCameraThenCapture("track")
                    } else {
                        runQuickTrack()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .weight(1.22f)
                .fillMaxHeight(0.88f),
            shape = spotVaultButtonShape(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Teal)
        ) {
            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(20.dp))
            // maxLines=1 here, not on the row itself — the row still grows via IntrinsicSize.Max
            // for someone with a large system font scale (that's deliberate, see the comment where
            // InstantActionsRow is placed), this only stops the label from silently wrapping to two
            // lines at *normal* text scale on a narrower phone, which was stretching the row taller
            // than the tier system's spacing accounted for.
            Text("Quick Track", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun EmptyStateView(
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    if (needsCompactHeightLayout()) {
        // Side by side instead of the portrait tier system below (which shrinks the cards purely
        // by available height and stacks them regardless) — a landscape phone, or any short
        // window even in portrait (compact split-screen, a small device), has the same problem:
        // plenty of width and not much height, so Snap and Pin sit next to each other here to use
        // that width instead of squeezing down through the tiers.
        val showInstantRow = prefs.getBoolean("show_instant_actions_row", true)
        val isWildButtons = ThemeState.buttonStyle == "wild"
        // OnPrimary, not the flat ButtonLabel white — matches pinContentColor's own OnTeal right
        // below, which already auto-contrasts correctly; this one didn't, which is why Snap (but
        // not Pin) went unreadable on the Yin Yang theme's near-white Primary.
        val snapContentColor = if (isWildButtons) Color.White else SpotVaultColors.OnPrimary
        val pinContentColor = if (isWildButtons) Color.White else SpotVaultColors.OnTeal
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Sized from the real available height instead of a fixed guess — a fixed 140dp card
            // (sized for a typical portrait screen) turned out taller than plenty of real compact
            // landscape windows, forcing exactly the scroll this layout exists to avoid. Clamped
            // to a sane range so cards neither vanish on the shortest window nor balloon on a
            // roomier one. The floor (84dp) isn't arbitrary — it's the real minimum GradientCtaCard
            // needs in dense mode with no subtitle: 16dp vertical padding + a 34dp icon box + a
            // 6dp spacer + one ~24dp title line at default font scale. A higher floor here (the
            // 92dp this used to use) undercounted that minimum once the subtitle line was added
            // back in, so the card silently grew past its "budget" and pushed Quick Pin/Quick
            // Track below it off screen — which is exactly why the subtitle is dropped entirely
            // below instead of only past some height threshold: predictable minimum height matters
            // more here than the extra label.
            val verticalPad = 8.dp
            val rowGap = 8.dp
            val instantRowHeight = 48.dp
            val reservedForInstant = if (showInstantRow) rowGap + instantRowHeight else 0.dp
            val cardHeight = (maxHeight - verticalPad * 2 - reservedForInstant)
                .coerceIn(84.dp, 120.dp)
            val cardTitleFontSize = if (cardHeight >= 100.dp) 20.sp else 16.sp
            val iconBoxSize = if (cardHeight >= 100.dp) 44.dp else 34.dp
            val iconSize = if (cardHeight >= 100.dp) 26.dp else 20.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(vertical = verticalPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    // Capped on tablets/unfolded foldables — otherwise these two buttons stretch
                    // to fill the whole screen width, comically wide on anything 600dp+.
                    modifier = Modifier.fillMaxWidth().adaptiveMaxContentWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GradientCtaCard(
                        title = "Snap",
                        subtitle = "Photo + OCR",
                        onClick = {
                            performAppHaptic(context, prefs)
                            AppSounds.playClickSound(context, prefs)
                            onSnapClick()
                        },
                        modifier = Modifier.weight(1f),
                        height = cardHeight,
                        tealDominant = false,
                        titleColor = if (isWildButtons) null else snapContentColor,
                        dense = true,
                        showSubtitle = false,
                        titleFontSize = cardTitleFontSize,
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(iconBoxSize)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                SpotVaultColors.Primary.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = snapContentColor
                                )
                            }
                        }
                    )
                    GradientCtaCard(
                        title = "Pin",
                        subtitle = "Instant GPS pin",
                        onClick = {
                            performAppHaptic(context, prefs)
                            AppSounds.playClickSound(context, prefs)
                            onPinOnlyClick()
                        },
                        modifier = Modifier.weight(1f),
                        height = cardHeight,
                        tealDominant = true,
                        titleColor = if (isWildButtons) null else pinContentColor,
                        dense = true,
                        showSubtitle = false,
                        titleFontSize = cardTitleFontSize,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize),
                                tint = pinContentColor
                            )
                        }
                    )
                }
                if (showInstantRow) {
                    Spacer(modifier = Modifier.height(rowGap))
                    InstantActionsRow(modifier = Modifier.fillMaxWidth(0.75f).height(instantRowHeight))
                }
            }
        }
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Both CTA cards have to stay on screen down to small hardware, so the
        // supporting copy is dropped first and the cards step down through four
        // tiers sized to their measured content heights. The column also scrolls,
        // which is what saves the very shortest windows (~533dp tall) where even
        // the smallest tier does not quite fit.
        val availableHeight = maxHeight
        val showHeadline = availableHeight >= 380.dp

        // Taller than before (was 60.dp) — Quick Pin/Quick Track are now the only quick-action
        // row (Quick Share/Share Photo were removed), so they get the vertical room that row used
        // to occupy instead of leaving it as empty space.
        val instantRowHeight = 76.dp
        val showInstantRow = prefs.getBoolean("show_instant_actions_row", true)

        // Tier is picked by checking each tier's REAL total layout cost against availableHeight —
        // the previous version only subtracted instantRowHeight and a flat 60dp for the headline,
        // which ignored topPad, headlineGap, cardGap, and both footerGap spacers around the
        // instant row entirely. That let a roomier tier get picked on screens with enough height
        // for its cards but not its own spacing on top of them — the "spare" space those spacers
        // were counting on didn't exist, so centeredInstantGap below collapsed to ~0 and Quick
        // Pin/Quick Track ended up flush against the Pin card (and the headline's own gap looked
        // like dead air above it, since tier 0's generous 20dp/28dp were still being used even
        // though the screen didn't truly have room to spare for them).
        fun requiredHeightForTier(t: Int): Dp {
            val tSnapCardHeight = when (t) { 0 -> 176.dp; 1 -> 118.dp; 2 -> 106.dp; else -> 80.dp }
            val tPinCardHeight = when (t) { 0 -> 118.dp; 1 -> 94.dp; 2 -> 90.dp; else -> 68.dp }
            val tCardGap = if (t == 0) 18.dp else 10.dp
            val tHeadlineGap = if (t == 0) 28.dp else 14.dp
            val tFooterGap = if (t == 0) 24.dp else 14.dp
            val tTopPad = if (t == 0) 20.dp else 10.dp
            val headlineBlock = if (showHeadline) 60.dp + tHeadlineGap else 0.dp
            val instantBlock = if (showInstantRow) instantRowHeight + tFooterGap * 2 else 0.dp
            return tTopPad + headlineBlock + tSnapCardHeight + tCardGap + tPinCardHeight + instantBlock
        }
        val tier = (0..3).firstOrNull { availableHeight >= requiredHeightForTier(it) } ?: 3

        val dense = tier >= 1
        val showCardSubtitle = tier <= 2
        val snapCardHeight = when (tier) {
            0 -> 176.dp
            1 -> 118.dp
            2 -> 106.dp
            else -> 80.dp
        }
        val pinCardHeight = when (tier) {
            0 -> 118.dp
            1 -> 94.dp
            2 -> 90.dp
            else -> 68.dp
        }
        val snapIconBox = when (tier) {
            0 -> 76.dp
            1 -> 62.dp
            2 -> 48.dp
            else -> 40.dp
        }
        val snapIconSize = when (tier) {
            0 -> 44.dp
            1 -> 36.dp
            2 -> 30.dp
            else -> 26.dp
        }
        val pinIconSize = when (tier) {
            0 -> 42.dp
            1 -> 34.dp
            2 -> 30.dp
            else -> 26.dp
        }
        val cardGap = if (tier == 0) 18.dp else 10.dp
        val headlineGap = if (tier == 0) 28.dp else 14.dp
        val topPad = if (tier == 0) 20.dp else 10.dp

        // On a tall phone (or tablet) the tier-0/1 cards are nowhere near tall enough to fill the
        // screen, so whatever's left over needs to go *somewhere*. An earlier version of this only
        // centered Quick Pin/Track in the gap below the cards, which left the headline+cards
        // themselves pinned to the top with a big dead strip below Quick Pin/Track. A later version
        // over-corrected and dumped nearly all of that slack above the headline instead to chase
        // thumb reach — that fixed the dead-strip-below complaint but produced an equally awkward,
        // lopsided void *above* the headline on anything with real slack to spare (a taller phone,
        // a tablet). True centering is what actually reads as intentional on every size: split
        // whatever's left after the cluster's own fixed size evenly above and below it, so
        // headline+cards+row sit centered between the top bar and the footer instead of hugging
        // either edge. Both spacers floor at 0 on the shortest screens, collapsing back to the same
        // tight top-anchored layout as before (with verticalScroll, already on this Column, as the
        // safety net) — this only changes anything once there's real slack to redistribute.
        //
        // This can't be done with Modifier.weight(1f) spacers (the more idiomatic Compose way to
        // center a block between two others) because this Column also carries verticalScroll as
        // that safety net for the shortest screens, and a Column can't mix weight() children with
        // verticalScroll — the scroll gives children unbounded height to measure against, which
        // weight() has no finite budget left to divide up. Computing the split as plain Dp instead
        // gets the same centered result without that conflict.
        val instantRowBlock = if (showInstantRow) cardGap + instantRowHeight else 0.dp
        val clusterHeight = topPad +
            (if (showHeadline) 60.dp + headlineGap else 0.dp) +
            snapCardHeight + cardGap + pinCardHeight + instantRowBlock
        val totalSlack = (availableHeight - clusterHeight).coerceAtLeast(0.dp)
        val topSlack = totalSlack / 2
        val bottomSlack = totalSlack - topSlack

        Column(
            // Capped on tablets/unfolded foldables — otherwise Snap/Pin stretch to fill the
            // whole screen width, comically wide on anything 600dp+.
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveMaxContentWidth()
                .heightIn(min = availableHeight)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(topPad + topSlack))
            if (showHeadline) {
                Text(
                    text = "DROP YOUR PIN",
                    style = MaterialTheme.typography.titleLarge.copy(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal)
                        )
                    ),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = headlineGap)
                )
            }

            val isWildButtons = ThemeState.buttonStyle == "wild"
            // Same OnPrimary fix as EmptyStateView's compact/landscape layout above.
            val snapContentColor = if (isWildButtons) Color.White else SpotVaultColors.OnPrimary
            val pinContentColor = if (isWildButtons) Color.White else SpotVaultColors.OnTeal

            GradientCtaCard(
                title = "Snap",
                subtitle = "Photo + OCR",
                onClick = {
                    performAppHaptic(context, prefs)
                    AppSounds.playClickSound(context, prefs)
                    onSnapClick()
                },
                height = snapCardHeight,
                tealDominant = false,
                titleColor = if (isWildButtons) null else snapContentColor,
                dense = dense,
                showSubtitle = showCardSubtitle,
                titleFontSize = if (dense) 22.sp else 28.sp,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(snapIconBox)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        SpotVaultColors.Primary.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(snapIconSize),
                            tint = snapContentColor
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(cardGap))

            GradientCtaCard(
                title = "Pin",
                subtitle = "Instant GPS pin",
                onClick = {
                    performAppHaptic(context, prefs)
                    AppSounds.playClickSound(context, prefs)
                    onPinOnlyClick()
                },
                height = pinCardHeight,
                tealDominant = true,
                titleColor = if (isWildButtons) null else pinContentColor,
                dense = dense,
                showSubtitle = showCardSubtitle,
                titleFontSize = if (dense) 22.sp else 28.sp,
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(pinIconSize),
                        tint = pinContentColor
                    )
                }
            )

            if (showInstantRow) {
                Spacer(modifier = Modifier.height(cardGap))
                // heightIn(min) + IntrinsicSize.Max instead of a hard .height(instantRowHeight) —
                // the buttons inside fill a fraction of this row's height, and a hard-fixed row
                // height clipped their label text (via the button's own .clip(shape)) at large
                // system font sizes / the app's text-scale setting instead of letting the row
                // grow. IntrinsicSize.Max is what makes fillMaxHeight(fraction) children resolve
                // against the *tallest actual child*, not just whatever fixed number was picked.
                InstantActionsRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = instantRowHeight)
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Max)
                )
            }
            // Mirrors topSlack — see the centering comment above.
            Spacer(modifier = Modifier.height(bottomSlack))
        }
    }
}

@Composable
fun PinnedStateView(
    photoPath: String,
    locationDetails: String,
    currentAddress: String,
    lat: Double,
    lng: Double,
    isAlarmRinging: Boolean,
    onFoundClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit = {},
    onNavigateToCompass: (spotId: Int, lat: Double, lng: Double) -> Unit = { _, _, _ -> },
    prefs: android.content.SharedPreferences
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Card wraps its own content instead of being force-stretched via weight(1f) — that used to
    // reserve the full remaining screen height for this zone even when the card itself (no
    // photo, no notes, no timer) was much shorter, leaving dead space between the card and the
    // action buttons below on anything but the tallest content.
    val cardContent: @Composable () -> Unit = {
            // Smaller in a short window (landscape, or any compact-height case) — the fixed
            // 140dp photo was sized for portrait's much taller viewport and, stacked under the
            // address/category/notes above it, regularly pushed the photo itself below the fold,
            // needing a scroll just to see the one thing this card exists to show.
            val photoHeight = if (needsCompactHeightLayout()) 100.dp else 140.dp
            Spacer(modifier = Modifier.height(4.dp))
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(SpotVaultColors.Primary, SpotVaultColors.PrimaryDeep)
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.OnPrimary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (currentAddress.isNotEmpty()) currentAddress else "Location Pinned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpotVaultColors.OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val isTimerActive = prefs.getLong("timer_end_time", 0L) > System.currentTimeMillis() || isAlarmRinging
                    if (isTimerActive) {
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimerComponent(prefs = prefs, isAlarmRinging = isAlarmRinging, isPinned = true)
                        }
                    }

                    if (locationDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SpotVaultColors.Deep.copy(alpha = 0.85f))
                                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = locationDetails,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                color = SpotVaultColors.OnSurface
                            )
                        }
                    }

                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(java.io.File(photoPath)),
                            contentDescription = "Saved Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(photoHeight)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { onPhotoClick() }
                        )
                    } else {
                        // Same footprint as the real photo (8dp spacer + photoHeight image), filled
                        // with the app's existing no-photo placeholder graphic instead of blank
                        // space — so the card ends up the same height either way and the
                        // Found/Navigate buttons below always land at the same vertical position,
                        // instead of sitting high up right under a much shorter card whenever
                        // there's no photo. Height-based, not screen-based, so it holds on any
                        // phone or tablet the same way the photo case already does.
                        Spacer(modifier = Modifier.height(8.dp))
                        VaultNoPhotoPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(photoHeight)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        )
                    }
                }
            }
    }

    // Bottom Actions row (Found It, Navigate), Show Map/Compass, and Share below.
    val buttonsContent: @Composable ColumnScope.() -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

            SpotVaultButton(
                onClick = onFoundClick,
                modifier = Modifier.weight(1f).height(60.dp),
                shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FOUND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            
            SpotVaultButton(
                onClick = {
                    if (lat != 0.0 && lng != 0.0) {
                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                        try {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                        } catch (e: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(context, "No navigation app found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Location coordinates not available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Teal
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NAVIGATE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotVaultOutlinedButton(
                    onClick = {
                        if (lat != 0.0 && lng != 0.0) {
                            val label = currentAddress.ifBlank { "Saved Spot" }
                            val uri = android.net.Uri.parse("geo:0,0?q=$lat,$lng(${android.net.Uri.encode(label)})")
                            try {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                            } catch (e: android.content.ActivityNotFoundException) {
                                android.widget.Toast.makeText(context, "No map app found", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Location coordinates not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    // Matches the Compass button beside it, rather than its own Teal — the two
                    // used to look like they belonged to different button families despite sitting
                    // in the same row.
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha = 0.5f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.ButtonLabel)
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Show Map", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Show Map", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
                SpotVaultOutlinedButton(
                    onClick = { onNavigateToCompass(-1, lat.toDouble(), lng.toDouble()) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha = 0.5f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.ButtonLabel)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Compass", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compass", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SpotVaultOutlinedButton(
                    onClick = {
                        onShareRequest(
                            ShareSpotPayload(
                                lat = lat,
                                lng = lng,
                                address = currentAddress,
                                notes = locationDetails,
                                imagePath = photoPath
                            )
                        )
                    },
                    // widthIn(max), not a flat width — a fixed 130dp doesn't shrink, and this
                    // button sits alone in a fillMaxWidth() Row that can end up narrower than
                    // 130dp on the tightest windows (e.g. a phone's cover screen). max lets it
                    // shrink to fit there instead of clipping past the edge, while still keeping
                    // its normal 130dp pill size everywhere it comfortably fits.
                    modifier = Modifier.widthIn(max = 130.dp).height(40.dp),
                    shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Large),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    // Matches Show Map/Compass above, rather than its own Teal.
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha = 0.5f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.ButtonLabel)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
            }
    }

    if (needsCompactHeightLayout()) {
        // Side-by-side instead of stacked: on a phone rotated to landscape — or any short window
        // even in portrait (compact split-screen, a small device) — there's much less height
        // than this screen was designed around, and stacking the card above four rows of buttons
        // used to push Found/Navigate up near the top with a long scroll below them, or worse,
        // off the bottom with nothing below to scroll them into view. Both columns scroll
        // independently so neither the card nor the buttons can ever end up unreachable,
        // regardless of how tall the card's own content (photo, notes, timer) gets.
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                cardContent()
            }
            Column(
                // weight(1f) + a max cap instead of a flat 320dp — a fixed width doesn't shrink,
                // so on a window narrower than 320dp total (a phone's cover screen forced into
                // cover-screen app mode, or a very narrow split-screen slice) this used to demand
                // more width than the Row actually had, clipping the second button in
                // buttonsContent's Show Map/Compass row off the edge instead of ever letting it
                // shrink to fit. buttonsContent's own fixed-width buttons were converted to
                // weight() for the same reason — capping this column alone doesn't help if what's
                // inside it still assumes a fixed minimum.
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 320.dp)
                    .fillMaxHeight()
                    .padding(start = 16.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                buttonsContent()
            }
        }
    } else {
        // Buttons stay in their own fixed, always-visible block below the card rather than
        // sharing one scroll with it — merging them into a single scrolling Column used to start
        // scrolled to the top, so on a normal-height phone any decently sized card (photo + notes
        // + timer) pushed Found/Navigate below the fold on every single pin, not just on the rare
        // short-window case this was meant to guard against. weight(1f, fill = false) still lets
        // the card scroll internally if its own content ever runs taller than the room left after
        // the buttons — it just no longer drags the buttons down along with it.
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    cardContent()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                buttonsContent()
            }
        }
    }
}

@Composable
fun TimerComponent(
    prefs: android.content.SharedPreferences, 
    isAlarmRinging: Boolean, 
    isPinned: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentEndTime by remember(isPinned) { mutableStateOf(prefs.getLong("timer_end_time", 0L)) }
    
    if (currentEndTime > System.currentTimeMillis() || isAlarmRinging) {
        Column(modifier = modifier) {
            if (currentEndTime > System.currentTimeMillis()) {
        var minsLeft by remember(currentEndTime) { mutableStateOf((currentEndTime - System.currentTimeMillis()) / 60000) }
        var showEditTimer by remember { mutableStateOf(false) }
        
        if (showEditTimer) {
            var newMins by remember { mutableStateOf("") }
            PremiumDialog(
                onDismissRequest = { showEditTimer = false },
                title = { Text("Edit Timer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                content = {
                    androidx.compose.material3.OutlinedTextField(
                        value = newMins,
                        onValueChange = { newMins = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        label = { Text("New Duration (minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                },
                confirmButton = {
                    SpotVaultButton(onClick = {
                        val mins = newMins.toLongOrNull() ?: 0L
                        if (mins > 0) {
                            val newEndTime = System.currentTimeMillis() + (mins * 60000L)
                            prefs.edit().putLong("timer_end_time", newEndTime).apply()
                            currentEndTime = newEndTime
                            
                            val intent = android.content.Intent(context, TimerService::class.java).apply {
                                putExtra("TIME_MS", mins * 60000L)
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                        showEditTimer = false
                    }) { Text("Update") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showEditTimer = false }) { Text("Cancel") }
                }
            )
        }
        
        var remainingText by remember { mutableStateOf("") }
        androidx.compose.runtime.LaunchedEffect(currentEndTime) {
            while(true) {
                val left = currentEndTime - System.currentTimeMillis()
                if (left <= 0) {
                    currentEndTime = 0L
                    break
                }
                val m = left / 60000
                val s = (left % 60000) / 1000
                remainingText = String.format(java.util.Locale.US, "%02d:%02d", m, s)
                kotlinx.coroutines.delay(1000)
            }
        }
        
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(8.dp),
            color = SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f),
            contentColor = SpotVaultColors.TealSoft,
            modifier = Modifier
                .border(
                    1.dp,
                    SpotVaultColors.PrimaryBright.copy(alpha = 0.6f),
                    RoundedCornerShape(8.dp)
                )
                .animateContentSize(androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ))
        ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = SpotVaultColors.PrimaryBright)
                Spacer(modifier = Modifier.width(6.dp))
                Text(remainingText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Edit, contentDescription = "Edit Timer", tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(16.dp).padding(2.dp).clickable { showEditTimer = true })
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Close, contentDescription = "Cancel Timer", tint = SpotVaultColors.Danger, modifier = Modifier.size(16.dp).padding(2.dp).clickable { 
                    currentEndTime = 0L
                    val intent = android.content.Intent(context, TimerService::class.java).apply {
                        action = "CANCEL_TIMER"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                })
            }
        }
    }
    
    // We need to keep a way to turn off alarm ringing if it is ringing
    // (In MainActivity it's managed by a state, so we update it here via sharedprefs or just a simple button)
    androidx.compose.animation.AnimatedVisibility(visible = isAlarmRinging) {
        SpotVaultButton(
            onClick = {
                val intent = android.content.Intent(context, TimerService::class.java).apply {
                    action = "SILENCE_ALARM"
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                // MainActivity should ideally update isAlarmRinging via Prefs, but we'll assume it handles it
                // We'll update the prefs here directly as well
                prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Silence Alarm", color = MaterialTheme.colorScheme.onError)
        }
    }
        }
    }
}


fun shareLocation(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    address: String,
    notes: String,
    imagePath: String = "",
    includePhoto: Boolean = false
) {
    val shareText = buildShareText(lat, lng, address, notes)

    // Copied to the clipboard unconditionally, in addition to whatever the user picks below — a
    // paste anywhere still works even if they cancel out of the chooser or the picked app doesn't
    // prefill EXTRA_TEXT the way Messages/Gmail do.
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Location", shareText))

    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        if (includePhoto && imagePath.isNotEmpty()) {
            val file = java.io.File(imagePath)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        } else {
            type = "text/plain"
        }
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
    }
    val chooser = android.content.Intent.createChooser(shareIntent, "Share Location").apply {
        // Needed when called from a non-Activity context (e.g. a widget tap) — harmless
        // when called from an Activity too.
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}


@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotVaultColors.Void),
        contentAlignment = Alignment.Center
    ) {
        SpotVaultAmbientBackground()
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(SpotVaultColors.Surface.copy(alpha = 0.9f), CircleShape)
                    .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = SpotVaultColors.Teal,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                "Vault Locked",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = SpotVaultColors.OnSurface,
                textAlign = TextAlign.Center
            )
            Text(
                "Authenticate with your fingerprint, face, or device PIN to continue.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = SpotVaultColors.Muted,
                textAlign = TextAlign.Center
            )
            SpotVaultButton(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = spotVaultButtonShape(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Primary
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Vault", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DistanceUnitSegmentedToggle(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SpotVaultColors.Deep)
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("miles" to "Miles", "km" to "Kilometers").forEach { (value, label) ->
            val selected = selectedUnit == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SpotVaultColors.Teal else Color.Transparent)
                    .clickable { onUnitSelected(value) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) SpotVaultColors.Ink else SpotVaultColors.Muted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    style: SplashStyle = SplashStyle.DEFAULT,
    appIcon: AppIcon = AppIcon.DEFAULT,
    onDismiss: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vaultDisplayName = remember {
        loadVaultDisplayNameFromPrefs(context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE))
    }
    var startAnimation by remember { mutableStateOf(false) }
    val effectDurationMs = remember(style) { style.effectDurationMillis() }
    val effectProgress = remember(style) { androidx.compose.animation.core.Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(style) {
        if (effectDurationMs > 0) {
            effectProgress.animateTo(
                1f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = effectDurationMs.toInt(),
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
        }
        startAnimation = true
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "splashScale"
    )
    
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 550,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "splashAlpha"
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Scale every splash element off the shortest edge so 320dp phones and
        // tablets both get a balanced lockup.
        val shortestEdge = minOf(maxWidth, maxHeight)
        val markSize = (shortestEdge * 0.34f).coerceIn(88.dp, 148.dp)
        val glowSize = markSize * 1.55f
        val titleStyle = if (shortestEdge < 340.dp) {
            MaterialTheme.typography.headlineLarge
        } else {
            MaterialTheme.typography.displayMedium
        }
        val taglineSpacing = if (shortestEdge < 340.dp) 1.4.sp else 2.2.sp

        SpotVaultAmbientBackground()
        if (style != SplashStyle.DEFAULT) {
            SplashStyleEffect(style = style, progress = effectProgress.value, modifier = Modifier.fillMaxSize())
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(glowSize)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    SpotVaultColors.Teal.copy(alpha = 0.5f),
                                    SpotVaultColors.Primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                val iconProgress = effectProgress.value
                val iconModifier = Modifier
                    .size(markSize)
                    .then(
                        when (style) {
                            SplashStyle.VAULT_DOOR -> Modifier
                                .scale(splashVaultDoorIconScale(iconProgress))
                                .rotate(splashVaultDoorIconRotation(iconProgress))
                                .alpha(0.2f + splashVaultDoorIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.SIGNAL_FORGE -> Modifier
                                .scale(splashSignalForgeIconScale(iconProgress))
                                .rotate(splashSignalForgeIconRotation(iconProgress))
                                .alpha(0.2f + splashSignalForgeIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.RADAR -> Modifier
                                .scale(splashRadarIconScale(iconProgress))
                                .rotate(splashRadarIconRotation(iconProgress))
                                .alpha(0.2f + splashRadarIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.TRIANGULATE -> Modifier
                                .scale(splashTriangulateIconScale(iconProgress))
                                .rotate(splashTriangulateIconRotation(iconProgress))
                                .alpha(0.2f + splashTriangulateIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.SIGNAL_STORM -> Modifier
                                .scale(splashSignalStormIconScale(iconProgress))
                                .rotate(splashSignalStormIconRotation(iconProgress))
                                .alpha(0.2f + splashSignalStormIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.VAULT_BLOOM -> Modifier
                                .scale(splashVaultBloomIconScale(iconProgress))
                                .rotate(splashVaultBloomIconRotation(iconProgress))
                                .alpha(0.2f + splashVaultBloomIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.HALLOWEEN -> Modifier
                                .scale(splashHalloweenIconScale(iconProgress))
                                .rotate(splashHalloweenIconRotation(iconProgress))
                                .alpha(0.2f + splashHalloweenIconAlpha(iconProgress) * 0.8f)
                            SplashStyle.CHRISTMAS -> Modifier
                                .scale(splashChristmasIconScale(iconProgress))
                                .rotate(splashChristmasIconRotation(iconProgress))
                                .alpha(0.2f + splashChristmasIconAlpha(iconProgress) * 0.8f)
                            else -> Modifier
                        }
                    )
                Box(modifier = iconModifier, contentAlignment = Alignment.Center) {
                    if (style == SplashStyle.VAULT_BLOOM &&
                        splashVaultBloomIconAlpha(iconProgress) > 0.01f
                    ) {
                        VaultBloomWings(
                            progress = iconProgress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppIconMark(
                        appIcon = appIcon,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = vaultDisplayName,
                style = titleStyle,
                fontWeight = FontWeight.Black,
                color = SpotVaultColors.OnSurface,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = "SAVE · NAVIGATE · RECALL",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = taglineSpacing,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Teal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
internal fun SettingsSectionCard(
    title: String,
    subtitle: String? = null,
    padding: Dp = 14.dp,
    contentSpacing: Dp = 8.dp,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    titleBlockSpacing: Dp = 3.dp,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onExpandChange: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            val titleBlock: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(titleBlockSpacing)) {
                    Text(title, fontSize = titleFontSize, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = subtitleFontSize, color = SpotVaultColors.Muted)
                    }
                }
            }
            if (collapsible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onExpandChange?.invoke() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) { titleBlock() }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = SpotVaultColors.Teal
                    )
                }
                if (expanded) {
                    content()
                }
            } else {
                titleBlock()
                content()
            }
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = SpotVaultColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = SpotVaultColors.Muted, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs); onCheckedChange(it) },
            colors = spotVaultSwitchColors()
        )
    }
}

@Composable
internal fun SettingsSelectRow(
    title: String,
    subtitle: String? = null,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = SpotVaultColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = SpotVaultColors.Muted, fontSize = 13.sp)
            }
        }
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SpotVaultColors.Deep)
                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(selectedLabel, color = SpotVaultColors.Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(
                    androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SpotVaultColors.Teal,
                    modifier = Modifier.size(18.dp)
                )
            }
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs)
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = { performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs); onClick() })
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = SpotVaultColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = SpotVaultColors.Muted, fontSize = 13.sp)
            }
        }
        trailing?.invoke() ?: Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SpotVaultColors.Muted)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoryListScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) SettingsCategories
        else SettingsCategories.filter { cat ->
            cat.title.contains(searchQuery, ignoreCase = true) ||
                cat.subtitle.contains(searchQuery, ignoreCase = true) ||
                cat.keywords.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    val scrollState = rememberScrollState()
    val canScroll by remember { derivedStateOf { scrollState.maxValue > 0 } }
    val atBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue - 8 } }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val navBarBottom = with(LocalDensity.current) { SystemBarInsets.navigationBarPx.toDp() }
    // LargeTopAppBar's expanded height (~152dp) is a portrait-tuned constant — on a short
    // landscape window it ate a disproportionate slice of the available height, and since the
    // Column below sits in the Scaffold's remaining `padding` space (not the top of the actual
    // screen), that read as "the list doesn't scroll to the top" even though nothing was actually
    // stuck. A plain TopAppBar has no expanded/collapsed size difference to eat that space.
    val isCompact = needsCompactHeightLayout()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            if (isCompact) {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SpotVaultColors.Elevated.copy(alpha = 0.78f),
                        scrolledContainerColor = SpotVaultColors.Elevated.copy(alpha = 0.92f),
                        titleContentColor = SpotVaultColors.OnSurface
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = SpotVaultColors.Elevated.copy(alpha = 0.78f),
                        scrolledContainerColor = SpotVaultColors.Elevated.copy(alpha = 0.92f),
                        titleContentColor = SpotVaultColors.OnSurface
                    )
                )
            }
        }
    ) { padding ->
        // Two nested Boxes on purpose: Scaffold's own `padding` already sits above the system
        // nav bar, so the scroll hint — which has its own SystemBarInsets-based bottom padding
        // built into PremiumScrollMoreHint precisely so it can sit flush near the real screen
        // edge — was getting pushed up a second time when it lived inside the pre-inset Box,
        // leaving a visible gap above the true bottom instead of sitting near it. The hint now
        // lives in the outer, unpadded Box so only its own inset applies.
        Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(end = if (canScroll) 10.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search settings…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotVaultColors.Teal,
                        unfocusedBorderColor = SpotVaultColors.Outline.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                filtered.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotVaultColors.Elevated.copy(alpha = 0.6f))
                            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { onCategoryClick(cat.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpotVaultColors.Teal.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(cat.icon, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.title, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(cat.subtitle, color = SpotVaultColors.Muted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SpotVaultColors.Muted, modifier = Modifier.size(20.dp))
                    }
                }
                if (filtered.isEmpty()) {
                    Text("No settings match \"$searchQuery\"", color = SpotVaultColors.Muted, modifier = Modifier.padding(top = 24.dp))
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(navBarBottom + 24.dp))
            }
            if (canScroll) {
                PremiumVerticalScrollbar(
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    emphasizeHint = true,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp).width(6.dp)
                )
            }
        }
            // A translucent gradient fade + small bouncing arrow overlaid on the content,
            // instead of a solid-color Scaffold bottomBar — that bar reserved real layout
            // height for every settings screen (shrinking content) and painted an opaque
            // rectangle across the full width even for a small hint, which read as an
            // unnecessarily large black bar. This overlay disappears the instant there's
            // nothing left to scroll, and never affects layout since it's drawn on top. Lives in
            // the outer, un-inset Box (see comment above) so it actually sits near the physical
            // bottom edge instead of floating above it.
            if (canScroll && !atBottom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp + navBarBottom)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SpotVaultColors.Void.copy(alpha = 0.55f),
                                    SpotVaultColors.Void.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
                PremiumScrollMoreHint(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoryDetailScreen(
    categoryId: String,
    prefs: SharedPreferences,
    dao: LocationDao,
    onBack: () -> Unit,
    onPickRingtone: () -> Unit,
    onAppLockChanged: (Boolean) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    val category = SettingsCategories.first { it.id == categoryId }
    val scrollState = rememberScrollState()
    val canScroll by remember { derivedStateOf { scrollState.maxValue > 0 } }
    val atBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue - 8 } }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val navBarBottom = with(LocalDensity.current) { SystemBarInsets.navigationBarPx.toDp() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(category.title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotVaultColors.Elevated.copy(alpha = 0.78f),
                    titleContentColor = SpotVaultColors.OnSurface
                )
            )
        }
    ) { padding ->
        // See the matching comment in SettingsCategoryListScreen — the scroll hint needs the
        // outer, un-inset Box so its own built-in nav-bar padding isn't stacked on top of
        // Scaffold's already-inset content padding.
        Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(end = if (canScroll) 10.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                when (categoryId) {
                    "appearance" -> AppearanceSettingsContent(prefs, onNavigateToCategory = onNavigateToCategory)
                    "widgets" -> WidgetsSettingsContent(prefs)
                    "vault" -> VaultSettingsContent(prefs, dao)
                    "quickpin" -> QuickPinSettingsContent(prefs)
                    "timer" -> TimerSettingsContent(prefs, onPickRingtone = onPickRingtone, onNavigateToCategory = onNavigateToCategory)
                    "gps" -> GpsSettingsContent(prefs)
                    "autopark" -> AutomaticParkingSettingsContent(prefs, dao, onNavigateToVehicles = { onNavigateToCategory("vehicles") })
                    "vehicles" -> VehiclesSettingsContent(prefs, dao)
                    "data" -> DataSettingsContent(prefs, dao, onAppLockChanged = onAppLockChanged)
                    "premium" -> PremiumSettingsContent(prefs)
                    "help" -> HelpGuideSettingsContent()
                    "about" -> AboutSettingsContent()
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(navBarBottom + 24.dp))
            }
            if (canScroll) {
                PremiumVerticalScrollbar(
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    emphasizeHint = true,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp).width(6.dp)
                )
            }
        }
            // Gradient fade + bouncing arrow overlaid on content instead of a solid Scaffold
            // bottomBar — see the matching comment in SettingsCategoryListScreen for why. Lives
            // in the outer, un-inset Box so it sits near the real bottom edge.
            if (canScroll && !atBottom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(52.dp + navBarBottom)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SpotVaultColors.Void.copy(alpha = 0.55f),
                                    SpotVaultColors.Void.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
                PremiumScrollMoreHint(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    prefs: android.content.SharedPreferences,
    dao: com.spotvault.app.LocationDao,
    onPickRingtone: () -> Unit,
    onAppLockChanged: (Boolean) -> Unit = {},
    embeddedInMainNav: Boolean = false,
    initialCategoryId: String? = null
) {
    val settingsContext = LocalContext.current
    // A real back-stack, not just the current category — Settings screens can navigate into each
    // other (Automatic Parking's "Add Vehicle" prompt, any locked feature's "Premium" link), and
    // hardware back needs to unwind one hop at a time back to wherever you actually came from,
    // not always straight to the root category list.
    //
    // [floorStack] is where this particular SettingsDialog instance started. When opened from the
    // root category list (initialCategoryId null), the floor is the empty stack, so back can still
    // land on that root list — it's a screen this session actually visited. But when opened via a
    // direct shortcut that skips the root list entirely (e.g. the home screen's Appearance/Help
    // icons, which pass initialCategoryId straight in), the floor is that one category — the root
    // list was never shown, so back from it should dismiss straight back to Home in one press
    // instead of surfacing a screen the user never navigated through.
    val floorStack = remember { listOfNotNull(initialCategoryId) }
    var settingsCategoryStack by remember { mutableStateOf(floorStack) }
    var settingsSearchQuery by remember { mutableStateOf("") }
    val selectedSettingsCategory = settingsCategoryStack.lastOrNull()

    BackHandler {
        if (settingsCategoryStack.size > floorStack.size) {
            settingsCategoryStack = settingsCategoryStack.dropLast(1)
        } else {
            onDismiss()
        }
    }

    @Composable
    fun SettingsNavShell() {
        androidx.compose.animation.Crossfade(targetState = selectedSettingsCategory, label = "SettingsNav") { category ->
            if (category == null) {
                SettingsCategoryListScreen(
                    searchQuery = settingsSearchQuery,
                    onSearchQueryChange = { settingsSearchQuery = it },
                    onCategoryClick = { settingsCategoryStack = settingsCategoryStack + it }
                )
            } else {
                SettingsCategoryDetailScreen(
                    categoryId = category,
                    prefs = prefs,
                    dao = dao,
                    onBack = { settingsCategoryStack = settingsCategoryStack.dropLast(1) },
                    onPickRingtone = onPickRingtone,
                    onAppLockChanged = onAppLockChanged,
                    onNavigateToCategory = { settingsCategoryStack = settingsCategoryStack + it }
                )
            }
        }
    }


    if (embeddedInMainNav) {
        AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
            SettingsNavShell()
        }
    } else {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { onDismiss() },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            if (!embeddedInMainNav) EnsureDialogEdgeToEdge()
            AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
                SettingsNavShell()
            }
        }
    }
}

