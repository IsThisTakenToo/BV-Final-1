package com.spotvault.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/** Launched directly by a tile tap (see QuickActionTileService.trackingRoot()) — a tap on the
 * tile is itself a foreground user gesture on the watch, so unlike everything WearActionListenerService
 * does on the phone side, launching an Activity from here has no background-activity-launch
 * restriction to work around.
 *
 * A Tile (ProtoLayout) can't render a smoothly-rotating live arrow — no per-frame animation, no
 * live sensor binding — so this is a real Activity instead, reading the target coordinates
 * [TrackingStateStore] already has cached (synced by TrackingWearSync on the phone, see
 * QuickActionPaths.KEY_TARGET_LAT/LNG) and combining them with the watch's *own* current position
 * and heading, resolved independently of the phone. Needs the watch to have its own location
 * capability — most Wear OS 3+ hardware does, but a watch with no GPS/network positioning of its
 * own will just sit on the "Getting your location…" state indefinitely rather than something this
 * screen can work around; there's no reasonable "point me toward my car" answer without knowing
 * where "me" currently is. */
class CompassActivity : ComponentActivity() {

    private var hasLocationPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MaterialTheme {
                CompassScreen(
                    hasLocationPermission = hasLocationPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }
}

@Composable
private fun CompassScreen(hasLocationPermission: Boolean, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val target = remember { TrackingStateStore.targetCoordinates(context) }
    val unit = remember { TrackingStateStore.distanceUnit(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            target == null -> Text(
                text = "No active track",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )
            !hasLocationPermission -> PermissionPrompt(onRequestPermission)
            else -> LiveCompass(targetLat = target.first, targetLng = target.second, unit = unit)
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    LaunchedEffect(Unit) { onRequestPermission() }
    Text(
        text = "Location access needed to point toward your car",
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable
private fun LiveCompass(targetLat: Double, targetLng: Double, unit: String) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var azimuthDegrees by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    val needleRotation = remember { Animatable(0f) }

    DisposableEffect(context) {
        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    currentLocation = loc.latitude to loc.longitude
                }
            }
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
        onDispose { client.removeLocationUpdates(callback) }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasSensor = rotationVector != null || (accelerometer != null && magnetometer != null)

        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val accelValues = FloatArray(3)
        val magValues = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR ->
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelValues, 0, 3)
                        hasAccel = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magValues, 0, 3)
                        hasMag = true
                    }
                }
                if (rotationVector == null && hasAccel && hasMag) {
                    SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelValues, magValues)
                }
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val degrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                azimuthDegrees = (degrees + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val fix = currentLocation
    if (fix == null) {
        Text(
            text = if (hasSensor) "Getting your location…" else "No compass sensor on this watch",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )
        return
    }
    val (lat, lng) = fix

    // Same correction CompassNavigationScreen.kt applies on the phone — getOrientation() reports
    // azimuth relative to MAGNETIC north, but bearingDegrees() below is a pure lat/lng great-circle
    // calculation referenced to TRUE north.
    val declinationDegrees = remember(String.format(Locale.US, "%.3f,%.3f", lat, lng)) {
        GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
    }
    val trueAzimuth = (azimuthDegrees + declinationDegrees + 360f) % 360f
    val bearing = bearingDegrees(lat, lng, targetLat, targetLng).toFloat()
    val distanceMeters = haversineDistanceMeters(lat, lng, targetLat, targetLng)

    LaunchedEffect(bearing, trueAzimuth) {
        val targetNeedle = bearing - trueAzimuth
        val delta = shortestAngleDelta(needleRotation.value, targetNeedle)
        needleRotation.animateTo(
            needleRotation.value + delta,
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f)
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer { rotationZ = needleRotation.value }
        ) {
            val w = size.width
            val h = size.height
            val arrow = Path().apply {
                moveTo(w / 2f, 0f)
                lineTo(w * 0.72f, h * 0.62f)
                lineTo(w / 2f, h * 0.46f)
                lineTo(w * 0.28f, h * 0.62f)
                close()
            }
            drawPath(arrow, color = Color(0xFFF59E0B))
        }
        Text(
            text = formatDistanceAway(distanceMeters, unit),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
