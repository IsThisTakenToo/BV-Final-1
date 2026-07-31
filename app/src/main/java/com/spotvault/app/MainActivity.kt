package com.spotvault.app

import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Edit

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import android.Manifest
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import android.annotation.SuppressLint
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
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
    val dialogShape = RoundedCornerShape(28.dp)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.9f).dp)
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
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                content()
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dismissButton != null) {
                                    dismissButton()
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {


    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var pendingActionIsCamera: Boolean? = null
    private var tempOcrTextForDialog: String = ""
    private var tempProminentOcrTextForDialog: String = ""
    private var isProcessingPhoto = mutableStateOf(false)
    private var instantLat: Double = 0.0
    private var instantLng: Double = 0.0

    private val isPinned = mutableStateOf(false)
    private val showConfirmationDialog = mutableStateOf<com.spotvault.app.LocationSpot?>(null)
    private val showTimerDialog = mutableStateOf(false)
    private val showHistoryDialog = mutableStateOf(false)
    private val showSettingsDialog = mutableStateOf(false)
    private val showPinnedPhotoViewer = mutableStateOf(false)
    private val cameraCountdown = mutableStateOf(-1)
    private var dialogSessionKey = 0L


    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: android.net.Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            prefs.edit().putString("alarm_sound_uri", uri?.toString() ?: "").apply()
        }
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            extractOcrAndShowDialog()
        } else {
            Toast.makeText(this, "Photo cancelled.", Toast.LENGTH_SHORT).show()
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

        if (pendingActionIsCamera == true) {
            if (cameraGranted && notifGranted && locGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Permissions required to capture and vault this spot.", Toast.LENGTH_SHORT).show()
            }
        } else if (pendingActionIsCamera == false) {
            if (notifGranted && locGranted) {
                photoFile = null
                tempOcrTextForDialog = ""
                tempProminentOcrTextForDialog = ""
                dialogSessionKey = System.currentTimeMillis()
                showTimerDialog.value = true
            } else {
                Toast.makeText(this, "Permissions required for Pin Location.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingActionIsCamera = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        isPinned.value = prefs.getBoolean("is_pinned", false)
        com.spotvault.app.SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(800)
                showSplash = false
            }
            
            SpotVaultTheme(darkTheme = true) {
                androidx.compose.animation.Crossfade(
                    targetState = showSplash,
                    animationSpec = androidx.compose.animation.core.tween(500),
                    label = "SplashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SpotVaultAmbientBackground()
                            Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                            SpotVaultBottomBar(
                                onVaultClick = { showHistoryDialog.value = true },
                                onSettingsClick = { showSettingsDialog.value = true }
/*
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
                                    */
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                SpotVaultColors.Primary.copy(alpha = 0.45f),
                                                SpotVaultColors.Teal.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Text(
                                text = "USE AT YOUR OWN RISK. BEACONVAULT IS NOT RESPONSIBLE FOR LOST LOCATIONS OR INACCURATE DATA.",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = SpotVaultColors.Muted.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                ) { innerPadding ->
                    SpotVaultScreen(
                        modifier = Modifier.padding(innerPadding),
                        isPinned = isPinned.value,
                        onSnapClick = { checkPermissionsAndAction(isCamera = true) },
                        onPinOnlyClick = { checkPermissionsAndAction(isCamera = false) },
                        onFoundClick = { clearPinnedSpot() },
                        onPhotoClick = { showPinnedPhotoViewer.value = true },
                        prefs = prefs
                    )

                    if (showPinnedPhotoViewer.value) {
                        FullScreenImageViewer(
                            imagePath = prefs.getString("photo_path", "") ?: "",
                            ocrText = "",
                            note = prefs.getString("location_details", "") ?: "",
                            timestampStr = System.currentTimeMillis().toString(),
                            lat = prefs.getFloat("lat", 0f),
                            lng = prefs.getFloat("lng", 0f),
                            onDismiss = { showPinnedPhotoViewer.value = false },
                            onFoundClick = {
                                showPinnedPhotoViewer.value = false
                                clearPinnedSpot()
                            },
                            category = prefs.getString("category", "Other") ?: "Other",
                            address = prefs.getString("current_address", "") ?: ""
                        )
                    }

                    if (showTimerDialog.value) {
                        androidx.compose.runtime.key(dialogSessionKey) {
                        TimerSelectionDialog(
                            prefs = prefs,
                            isCamera = (photoFile != null),
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
                            onPin = { mins: Int, note: String, finalOcr: String, category: String, isActiveTracking: Boolean ->
                                showTimerDialog.value = false
                                processPhotoAndPin(mins.toLong() * 60 * 1000L, note, finalOcr, category, isActiveTracking)
                            }
                        )
                        }
                    }

                    if (showHistoryDialog.value) {
                        HistoryDialog(onDismiss = { showHistoryDialog.value = false }, dao = AppDatabase.getDatabase(this@MainActivity).locationDao(), prefs = prefs, isPinned = isPinned.value)
                    }

                    if (showSettingsDialog.value) {
                        SettingsDialog(
                            onDismiss = { showSettingsDialog.value = false },
                            prefs = prefs,
                            dao = AppDatabase.getDatabase(this@MainActivity).locationDao(),
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
                                ringtonePickerLauncher.launch(intent)
                            }
                        )
                    }
                    
                    showConfirmationDialog.value?.let { savedSpot ->
                        PremiumDialog(
                            onDismissRequest = { showConfirmationDialog.value = null },
                            title = { Text("Spot Saved", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                            content = {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (savedSpot.imagePath.isNotEmpty()) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(java.io.File(savedSpot.imagePath)),
                                            contentDescription = "Saved Photo",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        )
                                    }
                                    
                                    if (savedSpot.locationDetails.isNotBlank()) {
                                        Text(
                                            text = savedSpot.locationDetails,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SpotVaultColors.OnSurface
                                        )
                                    }
                                    
                                    val finalAddress = if (savedSpot.address.isNotBlank()) savedSpot.address else prefs.getString("current_address", "") ?: ""
                                    if (finalAddress.isNotBlank() && finalAddress != "Loading address...") {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = finalAddress,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SpotVaultColors.Muted
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val finalAddress = if (savedSpot.address.isNotBlank()) savedSpot.address else prefs.getString("current_address", "") ?: ""
                                            shareLocation(this@MainActivity, savedSpot.lat, savedSpot.lng, finalAddress, savedSpot.locationDetails, savedSpot.imagePath)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Primary)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = SpotVaultColors.OnPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { showConfirmationDialog.value = null },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Done", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            dismissButton = null
                        )
                    }
                    
                    if (isProcessingPhoto.value) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
                            GlassSurface(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(0.7f),
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

                    if (cameraCountdown.value > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SpotVaultColors.Void.copy(alpha = 0.96f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth(0.92f),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                        .padding(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = SpotVaultColors.Teal,
                                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                                    )
                                    Text(
                                        text = "For best results: Get close to the sign, make sure the text is clear and well-lit, and fill most of the frame.",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 18.sp,
                                        color = SpotVaultColors.OnSurface,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Text(
                                        text = "Opening camera in ${cameraCountdown.value}...",
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 16.sp,
                                        color = SpotVaultColors.PrimaryBright
                                    )
                                }
                            }
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

    override fun onResume() {
        super.onResume()
        isPinned.value = prefs.getBoolean("is_pinned", false)
        com.spotvault.app.SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    @SuppressLint("MissingPermission")
    private fun lockInstantGps() {
        val locFineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locCoarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locFineGranted || locCoarseGranted) {
            lifecycleScope.launch {
                try {
                    var location = fusedLocationClient.lastLocation.await()
                    if (location == null) {
                        location = fusedLocationClient.getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 
                            null
                        ).await()
                    }
                    if (location != null) {
                        instantLat = location.latitude
                        instantLng = location.longitude
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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

        if (isCamera) {
            if (cameraGranted && notifGranted && locGranted) {
                launchCamera()
            } else {
                pendingActionIsCamera = true
                val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                requestPermissionsLauncher.launch(perms.toTypedArray())
            }
        } else {
            if (notifGranted && locGranted) {
                photoFile = null
                tempOcrTextForDialog = ""
                tempProminentOcrTextForDialog = ""
                dialogSessionKey = System.currentTimeMillis()
                showTimerDialog.value = true
            } else {
                pendingActionIsCamera = false
                val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                requestPermissionsLauncher.launch(perms.toTypedArray())
            }
        }
    }

    private fun launchCamera() {
        tempOcrTextForDialog = ""
        tempProminentOcrTextForDialog = ""
        lifecycleScope.launch {
            val tipShownCount = prefs.getInt("camera_tip_shown_count", 0)
            if (tipShownCount < 1) {
                cameraCountdown.value = 3
                while (cameraCountdown.value > 0) {
                    kotlinx.coroutines.delay(1000)
                    cameraCountdown.value -= 1
                }
                prefs.edit().putInt("camera_tip_shown_count", tipShownCount + 1).apply()
            } else {
                android.widget.Toast.makeText(this@MainActivity, "Tip: Get close and fill the frame for best results", android.widget.Toast.LENGTH_SHORT).show()
            }
            val imagesDir = File(cacheDir, "images").apply { mkdirs() }
            val newFile = File(imagesDir, "parkmark_${System.currentTimeMillis()}.jpg")
            photoFile = newFile
            val newUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", newFile)
            photoUri = newUri
            takePictureLauncher.launch(newUri)
        }
    }


    @SuppressLint("MissingPermission")
    private fun processPhotoAndPin(timeMs: Long, note: String, ocrText: String, category: String, isActiveTracking: Boolean = true) {
        lifecycleScope.launch {
            var lat = instantLat
            var lng = instantLng
            if (lat == 0.0 && lng == 0.0) {
                try {
                    val location = fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    if (location != null) {
                        lat = location.latitude
                        lng = location.longitude
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            instantLat = 0.0
            instantLng = 0.0
            val currentPhotoPath = photoFile?.absolutePath ?: ""
            val isCamera = currentPhotoPath.isNotEmpty()
            val initialLocationDetails = if (isCamera) ocrText else note
            
            val editor = prefs.edit()
                .putBoolean("is_pinned", true)
                .putString("photo_path", currentPhotoPath)
                .putFloat("lat", lat.toFloat())
                .putFloat("lng", lng.toFloat())
                .putString("location_details", initialLocationDetails)
                .putString("category", category)
                .putString("current_address", "Loading address...")
            if (timeMs > 0) {
                editor.putLong("timer_end_time", System.currentTimeMillis() + timeMs)
            } else {
                editor.remove("timer_end_time")
            }
            editor.apply()
            
            // Add to History Log
            val timestamp = System.currentTimeMillis()
            val dao = AppDatabase.getDatabase(this@MainActivity).locationDao()
            val newSpot = LocationSpot(
                imagePath = currentPhotoPath,
                locationDetails = initialLocationDetails,
                category = category,
                timestamp = timestamp,
                lat = lat,
                lng = lng,
                address = "",
                isFavorite = false
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
               dao.insertSpot(newSpot)
            }
            
            if (isActiveTracking) {
                isPinned.value = true
            } else {
                prefs.edit().putBoolean("is_pinned", false).remove("photo_path").remove("lat").remove("lng").apply()
                isPinned.value = false
                showConfirmationDialog.value = newSpot
            }
            
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@MainActivity)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@MainActivity, SpotVaultWidget::class.java))
            for (id in appWidgetIds) {
                SpotVaultWidget.updateAppWidget(this@MainActivity, appWidgetManager, id)
            }
            val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@MainActivity, CompactSpotVaultWidget::class.java))
            for (id in compactWidgetIds) {
                CompactSpotVaultWidget.updateAppWidget(this@MainActivity, appWidgetManager, id)
            }
            
            // Async reverse geocoding for history
            if (lat != 0.0 || lng != 0.0) {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    var addressStr = ""
                    try {
                        val geocoder = android.location.Geocoder(this@MainActivity, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val streetNum = address.subThoroughfare ?: ""
                            val streetName = address.thoroughfare ?: ""
                            val street = if (streetNum.isNotEmpty() && streetName.isNotEmpty()) "$streetNum $streetName" else if (streetName.isNotEmpty()) streetName else address.subAdminArea ?: ""
                            val city = address.locality ?: ""
                            val state = address.adminArea ?: ""
                            addressStr = listOf(street, city, state).filter { it.isNotEmpty() }.joinToString(", ")
                            if (addressStr.isEmpty() && !address.getAddressLine(0).isNullOrEmpty()) {
                                addressStr = address.getAddressLine(0) ?: ""
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    if (addressStr.isEmpty() || addressStr.contains("null")) {
                        val formatLat = String.format(java.util.Locale.US, "%.4f", lat)
                        val formatLng = String.format(java.util.Locale.US, "%.4f", lng)
                        addressStr = "Lat: $formatLat, Lng: $formatLng"
                    }
                    
                    prefs.edit().putString("current_address", addressStr).apply()
                    
                    val finalDetails = if (!isCamera) {
                        note
                    } else {
                        initialLocationDetails
                    }
                    
                    if (!isCamera) {
                        prefs.edit().putString("location_details", finalDetails).apply()
                    }
                    
                    val allSpots = dao.getHistoryList()
                    val spotToUpdate = allSpots.find { it.timestamp == timestamp }
                    if (spotToUpdate != null) {
                        dao.updateSpot(spotToUpdate.copy(address = addressStr, locationDetails = finalDetails))
                    }
                    
                    val updateIntent = Intent(this@MainActivity, TimerService::class.java).apply {
                        action = "UPDATE_DETAILS"
                    }
                    startService(updateIntent)
                }
            }
            if (isActiveTracking) {
                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    putExtra("TIME_MS", timeMs)
                    putExtra("PHOTO_PATH", currentPhotoPath)
                    putExtra("LAT", lat)
                    putExtra("LNG", lng)
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)
            }
        }
    }

    private fun extractOcrAndShowDialog() {
        val currentPhotoPath = photoFile?.absolutePath ?: ""
        if (currentPhotoPath.isEmpty()) {
            tempOcrTextForDialog = ""
            tempProminentOcrTextForDialog = ""
            showTimerDialog.value = true
            return
        }
        
        isProcessingPhoto.value = true
        lifecycleScope.launch {
            var extractedText = ""
            var prominentText = ""
            val rawBitmap = getUprightBitmap(currentPhotoPath)
            if (rawBitmap != null) {
                try {
                    val bitmap = enhanceBitmapForOCR(this@MainActivity, rawBitmap)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
                        extractedText = fullTextBuilder.toString().replace(Regex("[^A-Za-z0-9\\-\\s]"), " ").replace(Regex("\\s+"), " ").trim()
                        prominentText = prominentText.replace(Regex("[^A-Za-z0-9\\-\\s]"), " ").replace(Regex("\\s+"), " ").trim()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            tempOcrTextForDialog = extractedText
            tempProminentOcrTextForDialog = prominentText
            isProcessingPhoto.value = false
            showTimerDialog.value = true
        }
    }
    private fun enhanceBitmapForOCR(context: android.content.Context, original: Bitmap): Bitmap {
        val contrastBmp = Bitmap.createBitmap(original.width, original.height, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(contrastBmp)
        val cm = android.graphics.ColorMatrix()
        cm.setSaturation(0f) // Grayscale
        val cmContrast = android.graphics.ColorMatrix()
        val contrast = 2.0f
        val offset = (-.5f * contrast + .5f) * 255f
        cmContrast.set(floatArrayOf(
            contrast, 0f, 0f, 0f, offset,
            0f, contrast, 0f, 0f, offset,
            0f, 0f, contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(cmContrast)
        val paint = android.graphics.Paint()
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(original, 0f, 0f, paint)

        try {
            val rs = android.renderscript.RenderScript.create(context)
            val allocIn = android.renderscript.Allocation.createFromBitmap(rs, contrastBmp)
            val allocOut = android.renderscript.Allocation.createTyped(rs, allocIn.type)
            val script = android.renderscript.ScriptIntrinsicConvolve3x3.create(rs, android.renderscript.Element.U8_4(rs))
            script.setCoefficients(floatArrayOf(
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
            ))
            script.setInput(allocIn)
            script.forEach(allocOut)
            val outBmp = Bitmap.createBitmap(original.width, original.height, original.config ?: Bitmap.Config.ARGB_8888)
            allocOut.copyTo(outBmp)
            rs.destroy()
            return outBmp
        } catch(e: Exception) {
            e.printStackTrace()
            return contrastBmp
        }
    }

    private fun getUprightBitmap(filePath: String): Bitmap? {
        try {
            val file = File(filePath)
            if (!file.exists()) return null
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            val original = BitmapFactory.decodeFile(filePath)
            return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun clearPinnedSpot() {
        prefs.edit()
            .putBoolean("is_pinned", false)
            .remove("photo_path")
            .remove("lat")
            .remove("lng")
            .remove("location_details")
            .remove("category")
            .remove("timer_end_time")
            .remove("current_address")
            .apply()
        isPinned.value = false
        
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, SpotVaultWidget::class.java))
        for (id in appWidgetIds) {
            SpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
        }
        val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, CompactSpotVaultWidget::class.java))
        for (id in compactWidgetIds) {
            CompactSpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
        }
        
        val intent = Intent(this, TimerService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1)
        manager.cancel(2)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            
            val countdownChannel = android.app.NotificationChannel("TIMER_COUNTDOWN", "Timer Countdown", android.app.NotificationManager.IMPORTANCE_LOW)
            countdownChannel.setSound(null, null)
            manager.createNotificationChannel(countdownChannel)

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

fun saveImageToGallery(context: android.content.Context, imagePath: String) {
    if (imagePath.isEmpty()) return
    try {
        val file = java.io.File(imagePath)
        if (!file.exists()) return
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "SpotVault_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/SpotVault")
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
            }
            android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch(e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show()
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
    category: String = "",
    address: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current

    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        if (imagePath.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = java.io.File(imagePath),
                    contentDescription = "Spot Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Top gradient for status bar and close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    
                    Text(
                        text = "Spot Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), address, note, imagePath) },
                        modifier = Modifier.background(Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.3f).dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (category.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                            }
                        }

                        if (address.isNotEmpty()) {
                            Text(
                                text = address,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else if (lat != 0f && lng != 0f) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha=0.7f)
                            )
                        }

                        if (note.isNotBlank()) {
                            Text(
                                text = note, 
                                fontSize = 14.sp, 
                                color = Color.White.copy(alpha=0.9f),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        if (ocrText.isNotBlank()) {
                            Text(
                                text = "Text: $ocrText", 
                                fontSize = 12.sp, 
                                color = Color.White.copy(alpha=0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (lat != 0f && lng != 0f) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = SpotVaultColors.Primary,
                                    contentColor = SpotVaultColors.OnPrimary
                                )
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                Text("NAVIGATE", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (onFoundClick != null) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onFoundClick, 
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotVaultColors.PrimaryBright
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                Text("FOUND IT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // New centered detail view layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpotVaultColors.Deep)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape).border(1.dp, SpotVaultColors.Outline.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                    }
                    
                    Text(
                        text = "Spot Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = SpotVaultColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), address, note, imagePath) },
                        modifier = Modifier.background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape).border(1.dp, SpotVaultColors.Outline.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SpotVaultColors.OnSurface)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .statusBarsPadding()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 32.dp, vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(36.dp))
                    }
                    
                    if (category.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        }
                    }

                    if (address.isNotEmpty()) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.titleLarge,
                            color = SpotVaultColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (lat != 0f && lng != 0f) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpotVaultColors.Muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (note.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SpotVaultColors.Surface)
                                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.PrimaryBright, modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SpotVaultColors.OnSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (lat != 0f && lng != 0f) {
                        androidx.compose.material3.Button(
                            onClick = {
                                val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = SpotVaultColors.Primary,
                                contentColor = SpotVaultColors.OnPrimary
                            )
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("NAVIGATE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onFoundClick != null) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onFoundClick, 
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.PrimaryBright)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MARK AS FOUND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
@Composable
fun WishlistDialog(onDismiss: () -> Unit, dao: LocationDao, prefs: SharedPreferences) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")
    var customCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", emptySet())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + customCategories).distinct()

    val defaultTags = listOf("Level 1", "Level 2", "Must See", "Near Entrance", "Scenic", "Indoor", "Outdoor")
    var customTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", emptySet())?.toList() ?: emptyList()) }
    val allTags = (defaultTags + customTags).distinct()

    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).padding(end = 8.dp))
                Text("New Wishlist Spot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        content = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Aesthetic Bookstore)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                // Editable Category Dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded; if(categoryExpanded) { keyboardController?.hide(); focusManager.clearFocus() } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text("Choose a category") },
                        label = { Text("Folder / Category", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (categoryExpanded) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            allCategories.forEach { cat ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                            // Custom Option
                            var showCustomCategoryDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomCategoryDialog = true
                                }
                            )
                            
                            if (showCustomCategoryDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomCategoryDialog = false 
                                        categoryExpanded = false
                                    },
                                    title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                val cat = customInput.trim()
                                                if (!allCategories.contains(cat)) {
                                                    val updated = (prefs.getStringSet("custom_categories", emptySet()) ?: emptySet()) + cat
                                                    prefs.edit().putStringSet("custom_categories", updated).apply()
                                                    customCategories = updated.toList()
                                                }
                                                category = cat
                                            }
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }

                // Editable Tag Dropdown
                var tagExpanded by remember { mutableStateOf(false) }
                
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded; if(tagExpanded) { keyboardController?.hide(); focusManager.clearFocus() } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text("Select to add tags") },
                        label = { Text("Tags (Appended to notes)", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                        },
                        
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (tagExpanded) {
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            allTags.forEach { tag ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        note = if (note.isEmpty()) tag else "${note}, $tag"
                                        tagExpanded = false
                                    }
                                )
                            }
                            
                            // Custom Option
                            var showCustomTagDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomTagDialog = true
                                }
                            )
                            
                            if (showCustomTagDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomTagDialog = false 
                                        tagExpanded = false
                                    },
                                    title = { Text("Add Custom Tag", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                val tag = customInput.trim()
                                                if (!allTags.contains(tag)) {
                                                    val updated = (prefs.getStringSet("custom_tags", emptySet()) ?: emptySet()) + tag
                                                    prefs.edit().putStringSet("custom_tags", updated).apply()
                                                    customTags = updated.toList()
                                                }
                                                note = if (note.isEmpty()) tag else "${note}, $tag"
                                            }
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        dao.insertSpot(
                            LocationSpot(
                                imagePath = "",
                                locationDetails = note,
                                category = category,
                                timestamp = System.currentTimeMillis(),
                                lat = 0.0,
                                lng = 0.0,
                                address = "",
                                isFavorite = false,
                                title = title,
                                isWishlist = true,
                                isVisited = false
                            )
                        )
                    }
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryDialog(onDismiss: () -> Unit, dao: LocationDao, prefs: android.content.SharedPreferences, isPinned: Boolean) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewSpot by remember { mutableStateOf<LocationSpot?>(null) }

    if (viewSpot != null) {
        val spot = viewSpot!!
        FullScreenImageViewer(
            imagePath = spot.imagePath,
            ocrText = "",
            note = spot.locationDetails,
            timestampStr = spot.timestamp.toString(),
            lat = spot.lat.toFloat(),
            lng = spot.lng.toFloat(),
            onDismiss = { viewSpot = null },
            category = spot.category,
            address = spot.address
        )
    }
    
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotVaultColors.Surface,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = SpotVaultColors.Teal) },
        
    ) {
        HistoryDialogContent(onDismiss, dao, prefs, isPinned, onViewSpot = { viewSpot = it })
    }
}

@Composable
fun HistoryDialogContent(onDismiss: () -> Unit, dao: LocationDao, prefs: android.content.SharedPreferences, isPinned: Boolean, onViewSpot: (LocationSpot) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
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
    var showWishlistDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Newest") }
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete: LocationSpot? by remember { mutableStateOf(null) }
    var deleteAllMode by remember { mutableStateOf(false) }

    val filteredList = historyList.filter { item ->
        val matchSearch = if (searchQuery.isNotBlank()) {
            item.title.contains(searchQuery, ignoreCase = true) || 
            item.locationDetails.contains(searchQuery, ignoreCase = true) ||
            item.category.contains(searchQuery, ignoreCase = true) ||
            item.address.contains(searchQuery, ignoreCase = true)
        } else true
        val matchFav = if (showFavoritesOnly) item.isFavorite else true
        val matchTab = if (selectedTabIndex == 0) !item.isWishlist else item.isWishlist
        matchSearch && matchFav && matchTab
    }.sortedWith(Comparator { a, b ->
        when (sortBy) {
            "Newest" -> b.timestamp.compareTo(a.timestamp)
            "Oldest" -> a.timestamp.compareTo(b.timestamp)
            "Category" -> a.category.compareTo(b.category)
            else -> b.timestamp.compareTo(a.timestamp)
        }
    })

    if (showWishlistDialog) {
        WishlistDialog(onDismiss = { showWishlistDialog = false }, dao = dao, prefs = prefs)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("VAULT", fontWeight = FontWeight.Black, fontSize = 24.sp, color = SpotVaultColors.Teal, letterSpacing = 2.sp)
                Text("Your Secure Location History", fontSize = 12.sp, color = SpotVaultColors.Muted)
            }
            if (selectedTabIndex == 1) {
                Button(
                    onClick = { showWishlistDialog = true }, 
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = SpotVaultColors.Teal,
                        contentColor = SpotVaultColors.Ink
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("+ Wishlist", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Active Pin Section
        if (isPinned && selectedTabIndex == 0 && searchQuery.isEmpty()) {
            val cat = prefs.getString("category", "Other") ?: "Other"
            val addr = prefs.getString("current_address", "") ?: ""
            val title = prefs.getString("location_details", "") ?: ""
            val lat = prefs.getFloat("lat", 0f)
            val lng = prefs.getFloat("lng", 0f)
            
            Text("ACTIVE SPOT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = SpotVaultColors.PrimaryBright, modifier = Modifier.padding(bottom = 8.dp))
            androidx.compose.material3.ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).border(1.dp, SpotVaultColors.PrimaryBright, RoundedCornerShape(16.dp)),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(SpotVaultColors.PrimaryBright, androidx.compose.foundation.shape.CircleShape).size(8.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Now Tracking", fontSize = 10.sp, color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (title.isNotEmpty()) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpotVaultColors.OnSurface, modifier = Modifier.padding(top=8.dp))
                    }
                    if (addr.isNotEmpty()) {
                        Text(addr, fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top=4.dp))
                    }
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            val uri = "geo:0,0?q=$lat,$lng"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Deep, contentColor = SpotVaultColors.Teal)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp).padding(end=4.dp))
                        Text("Navigate to Active Spot")
                    }
                }
            }
        }

        // Tabs
        androidx.compose.material3.TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            containerColor = SpotVaultColors.Deep,
            contentColor = SpotVaultColors.Teal
        ) {
            androidx.compose.material3.Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0; selectedItems = emptySet() },
                text = { Text("History", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                selectedContentColor = SpotVaultColors.Teal,
                unselectedContentColor = SpotVaultColors.Muted
            )
            androidx.compose.material3.Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1; selectedItems = emptySet() },
                text = { Text("Wishlist", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                selectedContentColor = SpotVaultColors.Teal,
                unselectedContentColor = SpotVaultColors.Muted
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search & Filters
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Title, Note, Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Search", tint = SpotVaultColors.Muted) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SpotVaultColors.Outline,
                focusedBorderColor = SpotVaultColors.Teal
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            var expandedSort by remember { mutableStateOf(false) }
            Box {
                androidx.compose.material3.OutlinedButton(
                    onClick = { expandedSort = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Outline)
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp).padding(end=4.dp))
                    Text(sortBy, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = expandedSort,
                    onDismissRequest = { expandedSort = false }
                ) {
                    listOf("Newest", "Oldest", "Category").forEach { sortOption ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(sortOption) },
                            onClick = {
                                sortBy = sortOption
                                expandedSort = false
                            }
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { 
                    if (selectedItems.size == filteredList.size && filteredList.isNotEmpty()) selectedItems = emptySet() 
                    else selectedItems = filteredList.map { it.id }.toSet() 
                }) {
                    Text(if (selectedItems.size == filteredList.size && filteredList.isNotEmpty()) "Deselect All" else "Select All", color = SpotVaultColors.Teal, fontSize=12.sp)
                }
                Text("Favs Only", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SpotVaultColors.OnSurface)
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.Switch(
                    checked = showFavoritesOnly,
                    onCheckedChange = { showFavoritesOnly = it },
                    modifier = Modifier.scale(0.8f),
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = SpotVaultColors.Ink,
                        checkedTrackColor = SpotVaultColors.Teal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Bar
        if (selectedItems.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${selectedItems.size} Selected", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                Row {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.material3.IconButton(
                        onClick = {
                            val itemsToExport = historyList.filter { selectedItems.contains(it.id) }
                            val shareText = itemsToExport.joinToString("\n---\n") { item ->
                                val titlePrefix = if (item.title.isNotEmpty()) item.title else if (item.category != "Other") item.category else "SpotVault Location"
                                "📍 $titlePrefix: ${if (item.address.isNotBlank()) item.address else "Unnamed Location"}\n📝 Notes: ${if (item.locationDetails.isNotBlank()) item.locationDetails else "None"}\n🗺️ Google Maps: https://www.google.com/maps/search/?api=1&query=${item.lat},${item.lng}\n🍎 Apple Maps: http://maps.apple.com/?q=${item.lat},${item.lng}"
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Spots"))
                        }
                    ) { Icon(Icons.Default.Share, contentDescription = "Export", tint = SpotVaultColors.Teal) }
                    androidx.compose.material3.IconButton(
                        onClick = { deleteAllMode = false; showDeleteConfirmDialog = true }
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }

        // List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedTabIndex == 1) androidx.compose.material.icons.Icons.Default.Bookmark else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SpotVaultColors.Muted.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTabIndex == 1) "Your Wishlist is Empty" else if (showFavoritesOnly) "No Favorite Spots" else "Your Vault is Empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotVaultColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTabIndex == 1) "Save places you want to visit later." else "Snap a photo or drop a pin to save a spot.",
                        fontSize = 14.sp,
                        color = SpotVaultColors.Muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 500.dp).nestedScroll(nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredList) { item ->
                    val path = item.imagePath
                    val locationDetails = item.locationDetails
                    val category = item.category
                    val isFav = item.isFavorite
                    val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onViewSpot(item)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Thumbnail
                            val hasValidPhoto = path.isNotEmpty() && java.io.File(path).exists()
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SpotVaultColors.Glass)
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { onViewSpot(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasValidPhoto) {
                                    androidx.compose.foundation.Image(
                                        painter = coil.compose.rememberAsyncImagePainter(java.io.File(path)),
                                        contentDescription = "Thumb",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Muted.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("No photo", fontSize = 9.sp, color = SpotVaultColors.Muted.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                                }
                                Text(date, fontSize = 12.sp, color = SpotVaultColors.Muted)
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            if (item.title.isNotEmpty()) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = SpotVaultColors.OnSurface)
                            }
                            if (item.address.isNotEmpty()) {
                                Row(modifier = Modifier.padding(top=6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.address, fontSize = 13.sp, color = SpotVaultColors.OnSurface.copy(alpha = 0.9f), lineHeight = 18.sp)
                                }
                            }
                            if (locationDetails.isNotEmpty()) {
                                Row(modifier = Modifier.padding(top=6.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = SpotVaultColors.PrimaryBright.copy(alpha = 0.8f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(locationDetails, fontSize = 13.sp, color = SpotVaultColors.OnSurface.copy(alpha = 0.85f), lineHeight = 18.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row {
                                    IconButton(onClick = { 
                                        shareLocation(context, item.lat.toDouble(), item.lng.toDouble(), item.address, locationDetails, item.imagePath)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SpotVaultColors.Teal, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                dao.updateSpot(item.copy(isFavorite = !item.isFavorite))
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) Color.Red else SpotVaultColors.Muted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                androidx.compose.material3.Checkbox(
                                    checked = selectedItems.contains(item.id),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedItems = selectedItems + item.id
                                        else selectedItems = selectedItems - item.id
                                    },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = SpotVaultColors.Teal,
                                        uncheckedColor = SpotVaultColors.Outline
                                    )
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        PremiumDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Saved Spots?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface) },
            content = { Text("Are you sure you want to delete the selected spot(s)? This action cannot be undone.", color = SpotVaultColors.Muted) },
            confirmButton = {
                Button(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            if (deleteAllMode) {
                                if (selectedTabIndex == 0) dao.deleteNonFavoriteHistory()
                            } else {
                                val toDelete = historyList.filter { selectedItems.contains(it.id) }
                                toDelete.forEach {
                                    dao.deleteSpot(it)
                                    if (it.imagePath.isNotEmpty()) java.io.File(it.imagePath).delete()
                                }
                                selectedItems = emptySet()
                            }
                        }
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = SpotVaultColors.Teal) }
            }
        )
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TimerSelectionDialog(
    prefs: android.content.SharedPreferences,
    isCamera: Boolean, 
    ocrText: String, 
    prominentOcrText: String = "",
    onDismiss: () -> Unit, 
    onRetake: () -> Unit, 
    onPin: (Int, String, String, String, Boolean) -> Unit
) {
    val defaultMins = prefs.getString("default_timer_mins", "") ?: ""
    var timerMins by remember { mutableStateOf(defaultMins) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    val initialOcr = prominentOcrText.ifBlank { ocrText }
    var editedOcrText by remember { mutableStateOf(TextFieldValue(text = initialOcr, selection = TextRange(initialOcr.length))) }
    var category by remember { mutableStateOf("") }
    var useNumericKeyboard by remember { mutableStateOf(false) }
    val defaultTracking = prefs.getBoolean("default_active_tracking", true)
    var isActiveTracking by remember { mutableStateOf(defaultTracking) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")
    var savedCustomCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", setOf())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + savedCustomCategories).distinct()

    val defaultTags = listOf("Level 1", "Level 2", "Must See", "Near Entrance", "Scenic", "Indoor", "Outdoor")
    var savedCustomTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", setOf())?.toList() ?: emptyList()) }
    val allTags = (defaultTags + savedCustomTags).distinct()

    val suggestedMins = if (ocrText.contains("1 hour", ignoreCase=true) || ocrText.contains("60 min", ignoreCase=true)) 60 
                        else if (ocrText.contains("30 min", ignoreCase=true)) 30 
                        else if (ocrText.contains("2 hour", ignoreCase=true)) 120 
                        else null

    fun saveCustomCategory(cat: String) {
        if (cat.isNotBlank() && !defaultCategories.contains(cat) && !savedCustomCategories.contains(cat)) {
            val newSet = (savedCustomCategories + cat).toSet()
            prefs.edit().putStringSet("custom_categories", newSet).apply()
            savedCustomCategories = newSet.toList()
        }
    }

    fun saveCustomTag(tag: String) {
        if (tag.isNotBlank() && !defaultTags.contains(tag) && !savedCustomTags.contains(tag)) {
            val newSet = (savedCustomTags + tag).toSet()
            prefs.edit().putStringSet("custom_tags", newSet).apply()
            savedCustomTags = newSet.toList()
        }
    }

    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isCamera) Icons.Default.CameraAlt else Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(28.dp).padding(end = 8.dp))
                Text(if (isCamera) "Save Photo & Pin" else "Save Location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            }
        },
        content = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(vertical = 4.dp)) {
                if (isCamera) {
                    OutlinedTextField(
                        value = editedOcrText,
                        onValueChange = { editedOcrText = it },
                        label = { Text("Scanned Text (Edit if needed)") },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SpotVaultColors.Teal) }
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = SpotVaultColors.Primary.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isCamera) "Additional Notes (e.g., Level 4, Blue)" else "Location Notes") },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    keyboardOptions = if (useNumericKeyboard) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SpotVaultColors.Teal) }
                )
                
                // Editable Category Dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { 
                        categoryExpanded = !categoryExpanded
                        if (categoryExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { 
                             category = it 
                        },
                        readOnly = false,
                        placeholder = { Text("Choose a category") },
                        label = { Text("Folder / Category", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (categoryExpanded) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            allCategories.forEach { cat ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                            // Custom Option
                            var showCustomCategoryDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomCategoryDialog = true
                                }
                            )
                            
                            if (showCustomCategoryDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomCategoryDialog = false 
                                        categoryExpanded = false
                                    },
                                    title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                saveCustomCategory(customInput.trim())
                                                category = customInput.trim()
                                            }
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }

                // Editable Tag Dropdown
                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { 
                        tagExpanded = !tagExpanded
                        if (tagExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text("Select to add tags") },
                        label = { Text("Tags (Appended to notes)", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                        },
                        
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (tagExpanded) {
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            allTags.forEach { tag ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        val newText = if (isCamera) {
                                            if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                                        } else {
                                            if (note.text.isEmpty()) tag else "${note.text}, $tag"
                                        }
                                        if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        tagExpanded = false
                                    }
                                )
                            }
                            
                            // Custom Option
                            var showCustomTagDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomTagDialog = true
                                }
                            )
                            
                            if (showCustomTagDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomTagDialog = false 
                                        tagExpanded = false
                                    },
                                    title = { Text("Add Custom Tag", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                val tag = customInput.trim()
                                                saveCustomTag(tag)
                                                val newText = if (isCamera) {
                                                    if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                                                } else {
                                                    if (note.text.isEmpty()) tag else "${note.text}, $tag"
                                                }
                                                if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                            }
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }
                
                var showTimerInput by remember { mutableStateOf(false) }
                if (isCamera && suggestedMins != null && !showTimerInput) {
                    Button(
                        onClick = { timerMins = suggestedMins.toString(); showTimerInput = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = SpotVaultColors.Teal.copy(alpha=0.1f),
                            contentColor = SpotVaultColors.Teal
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                        Text("Suggested Timer: $suggestedMins mins", fontWeight = FontWeight.Bold)
                    }
                }
                if (showTimerInput || timerMins.isNotEmpty()) {
                    OutlinedTextField(
                        value = timerMins,
                        onValueChange = { timerMins = it },
                        label = { Text("Timer (minutes, optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = SpotVaultColors.Teal) },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = { showTimerInput = true }, 
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                        Text("Add Timer (Optional)", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isActiveTracking) {
                        Button(onClick = { isActiveTracking = true }, modifier = Modifier.weight(1f)) {
                            Text("Active Tracking")
                        }
                    } else {
                        OutlinedButton(onClick = { isActiveTracking = true }, modifier = Modifier.weight(1f)) {
                            Text("Active Tracking")
                        }
                    }
                    if (!isActiveTracking) {
                        Button(onClick = { isActiveTracking = false }, modifier = Modifier.weight(1f)) {
                            Text("Quiet Save")
                        }
                    } else {
                        OutlinedButton(onClick = { isActiveTracking = false }, modifier = Modifier.weight(1f)) {
                            Text("Quiet Save")
                        }
                    }
                }
                Text(
                    text = if (isActiveTracking) "Tracks location and alerts you in background." else "Saves silently to Vault without alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotVaultColors.Muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = timerMins.toIntOrNull() ?: 0
                val finalCategory = if (category.isNotBlank()) category else "Uncategorized"
                saveCustomCategory(finalCategory)
                onPin(mins, note.text, editedOcrText.text, finalCategory, isActiveTracking)
            }) { 
                Text("Pin Spot") 
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCamera) {
                    TextButton(onClick = onRetake) {
                        Text("Retake", color = SpotVaultColors.Teal)
                    }
                }
                TextButton(onClick = onDismiss) { 
                    Text("Cancel", color = SpotVaultColors.Muted) 
                }
            }
        }
    )
}
val SpotVaultTypography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 45.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
        fontSize = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    )
)

@Composable
fun SpotVaultTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val themeColors = when (ThemeState.currentTheme) {
        "neon_pink" -> NeonPinkColors
        "lime_magenta" -> LimeMagentaColors
        "crimson_gold" -> CrimsonGoldColors
        "gold_cobalt" -> GoldCobaltColors
        else -> PurpleTealColors
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
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = SpotVaultTypography,
        shapes = shapes,
        content = content
    )
    }
}

@Composable
fun SpotVaultScreen(
    modifier: Modifier = Modifier, 
    isPinned: Boolean,
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit,
    onFoundClick: () -> Unit,
    onPhotoClick: () -> Unit,
    prefs: android.content.SharedPreferences
) {
    var photoPath by remember { mutableStateOf(prefs.getString("photo_path", "") ?: "") }
    var locationDetails by remember { mutableStateOf(prefs.getString("location_details", "") ?: "") }
    var currentAddress by remember { mutableStateOf(prefs.getString("current_address", "") ?: "") }
    var isAlarmRinging by remember { mutableStateOf(prefs.getBoolean("is_alarm_ringing", false)) }
    var category by remember { mutableStateOf(prefs.getString("category", "Other") ?: "Other") }
    var lat by remember { mutableStateOf(prefs.getFloat("lat", 0f)) }
    var lng by remember { mutableStateOf(prefs.getFloat("lng", 0f)) }

    androidx.compose.runtime.LaunchedEffect(isPinned) {
        if (isPinned) {
            photoPath = prefs.getString("photo_path", "") ?: ""
            locationDetails = prefs.getString("location_details", "") ?: ""
            currentAddress = prefs.getString("current_address", "") ?: ""
            isAlarmRinging = prefs.getBoolean("is_alarm_ringing", false)
            category = prefs.getString("category", "Other") ?: "Other"
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
            } else if (key == "category" && sharedPreferences.contains(key)) {
                category = sharedPreferences.getString("category", "Other") ?: "Other"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 0.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpotVaultBrandHeader(
                modifier = Modifier.padding(bottom = 20.dp).padding(horizontal = 4.dp)
            )

            androidx.compose.animation.Crossfade(
                targetState = isPinned,
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                label = "PinStateTransition",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pinned ->
                if (pinned) {
                    PinnedStateView(
                        photoPath = photoPath,
                        locationDetails = locationDetails,
                        currentAddress = currentAddress,
                        category = category,
                        lat = lat.toDouble(),
                        lng = lng.toDouble(),
                        isAlarmRinging = isAlarmRinging,
                        onFoundClick = onFoundClick,
                        onPhotoClick = onPhotoClick,
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
}

@Composable
fun EmptyStateView(
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.35f))
        Text(
            text = "LOCK YOUR BEACON",
            style = MaterialTheme.typography.titleLarge.copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal)
                )
            ),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Capture a photo or drop a GPS pin — locked into your vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = SpotVaultColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 28.dp)
        )

        val needsDarkText = LocalSpotVaultColors.current.id == "lime_magenta" || LocalSpotVaultColors.current.id == "gold_cobalt"
        val customDarkColor = SpotVaultColors.Ink
        val iconTint = if (needsDarkText) customDarkColor else SpotVaultColors.PrimaryBright
        val titleColor = if (needsDarkText) customDarkColor else null

        GradientCtaCard(
            title = "Snap Beacon",
            subtitle = "Photo + OCR",
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onSnapClick()
            },
            height = 176.dp,
            tealDominant = false,
            titleColor = titleColor,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
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
                        modifier = Modifier.size(36.dp),
                        tint = iconTint
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        GradientCtaCard(
            title = "Pin Beacon",
            subtitle = "Instant GPS pin",
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onPinOnlyClick()
            },
            height = 118.dp,
            tealDominant = true,
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = SpotVaultColors.Ink
                )
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "GPS accuracy depends on your device and environment.",
            style = MaterialTheme.typography.bodySmall,
            color = SpotVaultColors.Muted.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
fun PinnedStateView(
    photoPath: String,
    locationDetails: String,
    currentAddress: String,
    category: String,
    lat: Double,
    lng: Double,
    isAlarmRinging: Boolean,
    onFoundClick: () -> Unit,
    onPhotoClick: () -> Unit,
    prefs: android.content.SharedPreferences
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
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
                    if (category.isNotEmpty() || isTimerActive) {
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (category.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                                }
                            }
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
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { onPhotoClick() }
                        )
                    }
                    
                }
            }
        }

        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onFoundClick()
                },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Primary,
                    contentColor = SpotVaultColors.OnPrimary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FOUND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            
            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (lat != 0.0 && lng != 0.0) {
                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Location coordinates not available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Teal,
                    contentColor = SpotVaultColors.Ink
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NAVIGATE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), currentAddress, locationDetails, photoPath) },
                modifier = Modifier.align(Alignment.CenterHorizontally).height(48.dp).fillMaxWidth(0.6f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.PrimaryBright)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Spot", fontWeight = FontWeight.SemiBold)
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
                    Button(onClick = {
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
        androidx.compose.material3.Button(
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

fun shareLocation(context: android.content.Context, lat: Double, lng: Double, address: String, notes: String, imagePath: String = "") {
    val displayAddress = if (address.isNotBlank()) address else "Unnamed Location"
    val displayNotes = if (notes.isNotBlank()) notes else "None"
    
    val formattedLat = String.format(java.util.Locale.US, "%.6f", lat)
    val formattedLng = String.format(java.util.Locale.US, "%.6f", lng)
    
    val shareText = "📍 $displayAddress\n📝 Notes: $displayNotes\n\n🗺️ Google Maps: https://www.google.com/maps?q=$formattedLat,$formattedLng\n🍎 Apple Maps: https://maps.apple.com/?q=$formattedLat,$formattedLng"
    
    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        if (imagePath.isNotEmpty()) {
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
    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Location"))
}


@Composable
fun SplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
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
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SpotVaultAmbientBackground()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    SpotVaultColors.Primary.copy(alpha = 0.55f),
                                    SpotVaultColors.Teal.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_spotvault_mark),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(136.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "BeaconVault",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = SpotVaultColors.OnSurface,
                letterSpacing = (-0.5).sp
            )
            
            Text(
                text = "SAVE · SHARE · NAVIGATE · RECALL",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Teal,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    prefs: android.content.SharedPreferences,
    dao: com.spotvault.app.LocationDao,
    onPickRingtone: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var amoledBlack by remember { mutableStateOf(prefs.getBoolean("amoled_black", false)) }
    var defaultActiveTracking by remember { mutableStateOf(prefs.getBoolean("default_active_tracking", true)) }
    var defaultTimerMins by remember { mutableStateOf(prefs.getString("default_timer_mins", "") ?: "") }
    
    var savedCustomCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", setOf())?.toList() ?: emptyList()) }
    var savedCustomTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", setOf())?.toList() ?: emptyList()) }
    
    var showClearDataConfirm by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotVaultColors.Deep)
                .padding(top = 48.dp, bottom = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotVaultColors.OnSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Appearance
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Appearance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        
                        Text("Color Theme", color = SpotVaultColors.OnSurface, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val themes = listOf(
                                Triple("purple_teal", Color(0xFF6B2FFF), Color(0xFF00F0FF)),
                                Triple("neon_pink", Color(0xFFFF008C), Color(0xFF00F0FF)),
                                Triple("lime_magenta", Color(0xFFC6FF00), Color(0xFFFF0080)),
                                Triple("crimson_gold", Color(0xFFFF2E63), Color(0xFFFFD100)),
                                Triple("gold_cobalt", Color(0xFF2962FF), Color(0xFFFFD100))
                            )
                            themes.forEach { (id, primary, teal) ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(primary, teal)), androidx.compose.foundation.shape.CircleShape)
                                        .border(2.dp, if (ThemeState.currentTheme == id) SpotVaultColors.OnSurface else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            ThemeState.currentTheme = id
                                            prefs.edit().putString("color_theme", id).apply()
                                        }
                                ) {
                                    if (ThemeState.currentTheme == id) {
                                        val checkColor = if (primary.luminance() > 0.4f) Color.Black else Color.White
                                        Icon(Icons.Default.Check, contentDescription = null, tint = checkColor, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AMOLED True Black", color = SpotVaultColors.OnSurface)
                            androidx.compose.material3.Switch(
                                checked = amoledBlack,
                                onCheckedChange = { 
                                    amoledBlack = it
                                    prefs.edit().putBoolean("amoled_black", it).apply()
                                    SpotVaultColors.updateAmoled(it)
                                }
                            )
                        }
                    }

                    // Notifications
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onPickRingtone() }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Alert Sound", color = SpotVaultColors.OnSurface, fontSize = 16.sp)
                                Text("Choose tone for timer alarms", color = SpotVaultColors.Muted, fontSize = 14.sp)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SpotVaultColors.Muted)
                        }
                    }

                    // Defaults
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Defaults", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Default Save Mode", color = SpotVaultColors.OnSurface)
                                Text(if (defaultActiveTracking) "Active Tracking" else "Quiet Save", color = SpotVaultColors.Muted, fontSize = 14.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = defaultActiveTracking,
                                onCheckedChange = { 
                                    defaultActiveTracking = it
                                    prefs.edit().putBoolean("default_active_tracking", it).apply()
                                }
                            )
                        }
                        
                        OutlinedTextField(
                            value = defaultTimerMins,
                            onValueChange = { 
                                defaultTimerMins = it 
                                prefs.edit().putString("default_timer_mins", it).apply()
                            },
                            label = { Text("Default Timer (Mins)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Manage Categories & Tags
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Manage Custom Data", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        
                        if (savedCustomCategories.isEmpty() && savedCustomTags.isEmpty()) {
                            Text("No custom categories or tags.", color = SpotVaultColors.Muted, fontSize = 14.sp)
                        } else {
                            if (savedCustomCategories.isNotEmpty()) {
                                Text("Custom Categories:", color = SpotVaultColors.OnSurface, fontSize = 14.sp)
                                savedCustomCategories.forEach { cat ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cat, color = SpotVaultColors.OnSurface, modifier = Modifier.padding(start = 8.dp))
                                        IconButton(onClick = {
                                            val newSet = savedCustomCategories.filter { it != cat }.toSet()
                                            prefs.edit().putStringSet("custom_categories", newSet).apply()
                                            savedCustomCategories = newSet.toList()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpotVaultColors.Danger)
                                        }
                                    }
                                }
                            }
                            if (savedCustomTags.isNotEmpty()) {
                                Text("Custom Tags:", color = SpotVaultColors.OnSurface, fontSize = 14.sp)
                                savedCustomTags.forEach { tag ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(tag, color = SpotVaultColors.OnSurface, modifier = Modifier.padding(start = 8.dp))
                                        IconButton(onClick = {
                                            val newSet = savedCustomTags.filter { it != tag }.toSet()
                                            prefs.edit().putStringSet("custom_tags", newSet).apply()
                                            savedCustomTags = newSet.toList()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpotVaultColors.Danger)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Clear Data
                    Button(
                        onClick = { showClearDataConfirm = true },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Danger.copy(alpha=0.2f), contentColor = SpotVaultColors.Danger),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Clear Vault Data")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        
        if (showClearDataConfirm) {
            AlertDialog(
                onDismissRequest = { showClearDataConfirm = false },
                containerColor = SpotVaultColors.Surface,
                titleContentColor = SpotVaultColors.OnSurface,
                textContentColor = SpotVaultColors.Muted,
                title = { Text("Clear Vault Data?") },
                text = { Text("This will permanently delete all saved locations and photos. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val spots = dao.getHistoryList()
                                spots.forEach { spot ->
                                    if (spot.imagePath.isNotEmpty()) {
                                        java.io.File(spot.imagePath).delete()
                                    }
                                }
                                dao.deleteAllHistory()
                            }
                            showClearDataConfirm = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Danger)
                    ) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataConfirm = false }) {
                        Text("Cancel", color = SpotVaultColors.Teal)
                    }
                }
            )
        }
    }
}
