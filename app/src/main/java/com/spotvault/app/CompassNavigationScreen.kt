package com.spotvault.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Standalone compass navigation to a saved spot — no map required.
 * Uses [SensorEventListener] for a fluid heading and fused location for distance/bearing.
 */
@Composable
fun CompassNavigationScreen(
    targetLat: Double,
    targetLng: Double,
    targetTitle: String,
    targetSubtitle: String = "",
    prefs: android.content.SharedPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
    val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() }
    val unit = rememberDistanceUnit(prefs)
    // Reads ThemeState.compassStyle directly (rather than a local remember of the pref) so this
    // screen picks up a premium downgrade instantly if Premium is toggled off while it's open —
    // ThemeState is kept in sync with the pref by MainActivity's onCreate/onResume and by the
    // premium downgrade path in PremiumSettingsContent.
    val compassStyle = ThemeState.compassStyle

    val deviceAzimuthState = rememberDeviceAzimuth()
    val liveLocation = rememberLiveUserLocation()
    val userLocation = liveLocation?.latLng
    val locationServicesEnabled = rememberLocationServicesEnabled()

    val dialRotation = remember { Animatable(0f) }
    val needleRotation = remember { Animatable(0f) }

    val bearing = remember(userLocation, targetLat, targetLng) {
        userLocation?.let { (lat, lng) ->
            bearingDegrees(lat, lng, targetLat, targetLng).toFloat()
        }
    }

    // SensorManager.getOrientation() (feeding deviceAzimuthState) reports azimuth relative to
    // MAGNETIC north, but bearingDegrees() above is a pure lat/lng great-circle calculation
    // referenced to TRUE north — combining them with no correction was off by the local magnetic
    // declination everywhere except the handful of places on Earth where it happens to be zero.
    // GeomagneticField gives that correction for the user's actual location; until a GPS fix
    // exists there's nothing to correct against yet, so it's a no-op (0°) rather than guessing.
    // Declination drifts slowly enough that recomputing only when userLocation's *rounded*
    // position changes (not on every sub-meter GPS jitter) is plenty; it's not properly worth
    // recomputing on every single location update.
    val declinationDegrees = remember(
        userLocation?.let { (lat, lng) -> "%.3f,%.3f".format(Locale.US, lat, lng) }
    ) {
        userLocation?.let { (lat, lng) ->
            android.hardware.GeomagneticField(
                lat.toFloat(),
                lng.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        } ?: 0f
    }

    // On the (rare, but real) device with neither a rotation-vector sensor nor a magnetometer,
    // deviceAzimuthState.azimuthDegrees never moves from its initial 0° — the needle would
    // silently freeze pointing one fixed direction forever with nothing telling the user why.
    // GPS course-over-ground (Location.bearing) is a legitimate fallback heading, but only while
    // actually moving with some real speed — right after a fix, or standing still, providers
    // frequently report a "bearing" that's pure noise. Already true-north-referenced (it's a
    // real-world direction of travel over the ground), so no declination correction here.
    val gpsBearingUsable = !deviceAzimuthState.hasSensor &&
        liveLocation?.bearingDegrees != null &&
        (liveLocation.speedMps ?: 0f) >= MIN_GPS_BEARING_SPEED_MPS
    val trueDeviceAzimuth = when {
        deviceAzimuthState.hasSensor -> (deviceAzimuthState.azimuthDegrees + declinationDegrees + 360f) % 360f
        gpsBearingUsable -> liveLocation?.bearingDegrees ?: 0f
        else -> deviceAzimuthState.azimuthDegrees
    }
    val needsMotionForHeading = !deviceAzimuthState.hasSensor && !gpsBearingUsable

    LaunchedEffect(trueDeviceAzimuth) {
        val targetDial = -trueDeviceAzimuth
        val delta = shortestAngleDelta(dialRotation.value, targetDial)
        dialRotation.animateTo(
            dialRotation.value + delta,
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f)
        )
    }

    // Needle rotation must also compensate for device heading, the same way the dial's cardinal
    // ring does (dialRotationDegrees = -trueDeviceAzimuth) — VaultCompassDial draws the needle in
    // its own separate, non-nested canvas transform, not inside the dial ring's rotation, so
    // "bearing" alone (an absolute, true-north-referenced angle from bearingDegrees()) only
    // pointed the needle correctly at the exact moment the phone happened to be facing true
    // north. At any other heading it was off by up to 180° — turning to try to follow the needle
    // didn't make it track the target at all, since it never factored in which way the phone was
    // actually pointed. Re-triggers on trueDeviceAzimuth too, not just bearing, so it stays
    // correct continuously as the user turns, not just when their GPS fix updates.
    LaunchedEffect(bearing, trueDeviceAzimuth) {
        if (bearing != null) {
            val targetNeedle = bearing - trueDeviceAzimuth
            val delta = shortestAngleDelta(needleRotation.value, targetNeedle)
            needleRotation.animateTo(
                needleRotation.value + delta,
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f)
            )
        }
    }

    val distanceMeters = remember(userLocation, targetLat, targetLng) {
        userLocation?.let { (lat, lng) ->
            haversineDistanceMeters(lat, lng, targetLat, targetLng)
        }
    }

    val distanceLabel = remember(distanceMeters, unit) {
        distanceMeters?.let { formatCompassDistance(it, unit) } ?: "—"
    }

    val bearingLabel = remember(bearing) {
        bearing?.let { formatBearingLabel(it.toDouble()) } ?: "—"
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusPad + 8.dp, bottom = navPad + 16.dp)
        ) {
            CompassTopBar(
                title = targetTitle,
                subtitle = targetSubtitle,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // No scroll fallback before this — the 300dp dial, 52sp distance text, and
                    // bearing/GPS-status lines don't shrink with a larger system font size, so on
                    // a short portrait phone the stack could exceed the space left after the top
                    // bar and "Open in Maps" button, with Center arrangement silently cropping
                    // both ends instead of anything being reachable.
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassSurface(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        VaultCompassDial(
                            styleId = compassStyle,
                            dialRotationDegrees = { dialRotation.value },
                            needleRotationDegrees = { needleRotation.value },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = distanceLabel,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "straight-line",
                    style = MaterialTheme.typography.labelMedium,
                    color = SpotVaultColors.Muted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                val hasArrived = distanceMeters != null && distanceMeters < ARRIVED_THRESHOLD_METERS
                Text(
                    text = if (hasArrived) "You've arrived" else bearingLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasArrived) SpotVaultColors.PrimaryBright else SpotVaultColors.Teal
                )

                if (userLocation == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (locationServicesEnabled) {
                        Text(
                            text = "Waiting for GPS fix…",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotVaultColors.Muted.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        // Without this, the system-wide Location toggle being off left this
                        // screen stuck on "Waiting for GPS fix…" forever — requestLocationUpdates
                        // has no timeout of its own to fall back from, so there was never anything
                        // to tell the user *why* it wasn't resolving, or how to fix it.
                        Text(
                            text = "Location Services are turned off on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotVaultColors.PrimaryBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }) {
                            Text("Turn On Location", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Independent of the GPS-fix status above — this is about the compass sensor
                // specifically (nearby metal/magnets/speakers throwing off the magnetometer),
                // previously never surfaced anywhere even though onAccuracyChanged was already
                // telling the app exactly when this was happening.
                if (deviceAzimuthState.needsCalibration) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Compass needs calibration — wave your phone in a figure-8 motion",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpotVaultColors.PrimaryBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                // This device has no compass hardware at all (no rotation-vector sensor, no
                // magnetometer) and isn't moving fast enough yet for GPS course-over-ground to
                // stand in as a heading — without this, the needle just silently sits frozen with
                // nothing telling the user their phone can't do this the normal way.
                if (needsMotionForHeading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No compass sensor on this device — start walking and the needle will use your direction of travel",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpotVaultColors.PrimaryBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OpenInMapsButton(targetLat = targetLat, targetLng = targetLng)
        }
    }
}

private const val ARRIVED_THRESHOLD_METERS = 20.0

/** Minimum speed (~1.8 mph, a slow walk) before Location.bearing is trusted as a heading on a
 * device with no compass sensor — below this, providers frequently report a "bearing" that's
 * just GPS jitter between two nearly-identical points, not an actual direction of travel. */
private const val MIN_GPS_BEARING_SPEED_MPS = 0.8f

@Composable
private fun OpenInMapsButton(targetLat: Double, targetLng: Double) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    val uri = android.net.Uri.parse("google.navigation:q=$targetLat,$targetLng&mode=w")
                    try {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, "No navigation app found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            ),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = SpotVaultColors.Teal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open turn-by-turn backup",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface
                )
            }
        }
    }
}

@Composable
private fun CompassTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        GlassSurface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = SpotVaultColors.Teal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SpotVaultColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotVaultColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


/** SensorManager.getRotationMatrix()/getRotationMatrixFromVector() always produce a matrix in
 * the device's natural (portrait, for phones) orientation — they know nothing about the current
 * *display* rotation. Without remapping for it, the needle reads correctly in portrait and is
 * off by a multiple of 90° in landscape (or upside-down portrait), since screen "up" and sensor
 * "up" have parted ways. Read once per DisposableEffect run rather than observed continuously:
 * MainActivity has no android:configChanges override, so a rotation goes through a full
 * onStop→onDestroy→onCreate teardown (same reasoning documented on isChangingConfigurations
 * elsewhere in this app) — the whole Composable tree, this DisposableEffect included, restarts
 * from scratch on every orientation change, so there's no stale-rotation window to worry about. */
private fun currentDisplayRotation(context: Context): Int {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay
    }
    return display?.rotation ?: Surface.ROTATION_0
}

/** Minimum change (degrees) before a new sensor reading is published to Compose state — see the
 * lastPublishedAzimuth comment inside rememberDeviceAzimuth for why. Small enough that turning to
 * face a new direction is indistinguishable from unthrottled, large enough to absorb sensor
 * jitter while holding roughly still. */
private const val AZIMUTH_UPDATE_THRESHOLD_DEGREES = 1.5f

/** Device heading in degrees [0, 360), 0 = north, via [SensorEventListener]. Registration is
 * tied to the host lifecycle (ON_START/ON_STOP), not just composition — a bare
 * `DisposableEffect(context) { ... onDispose { unregister() } }` only unregisters when this
 * composable leaves composition, which doesn't happen just from backgrounding the app (e.g. the
 * Home button) while this screen is still on the back stack — SENSOR_DELAY_GAME listeners would
 * otherwise keep running indefinitely in the background. */
/** [azimuthDegrees] is raw MAGNETIC-north azimuth straight off the sensor — [CompassNavigationScreen]
 * corrects it to true north (see the declination comment there) before using it for anything.
 * [needsCalibration] mirrors whatever the least-reliable active sensor last reported via
 * onAccuracyChanged: true for SENSOR_STATUS_UNRELIABLE/SENSOR_STATUS_ACCURACY_LOW, the classic
 * "compass reading nearby metal/magnets, do a figure-8" state that was previously never surfaced
 * anywhere — onAccuracyChanged was a bare no-op, so a badly-calibrated reading looked identical
 * to a good one with no way for the user to tell why the needle seemed wrong. [hasSensor] is
 * false on the (rare but real) device with neither a rotation-vector sensor nor a magnetometer —
 * [azimuthDegrees] just silently stays 0 forever on those with no way to tell the needle isn't
 * actually tracking anything; [CompassNavigationScreen] falls back to a GPS-derived heading when
 * this is false. */
private data class DeviceAzimuthState(
    val azimuthDegrees: Float,
    val needsCalibration: Boolean,
    val hasSensor: Boolean
)

@Composable
private fun rememberDeviceAzimuth(): DeviceAzimuthState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var needsCalibration by remember { mutableStateOf(false) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasSensor = rotationVector != null || (accelerometer != null && magnetometer != null)

        val displayRotation = currentDisplayRotation(context)
        val (remapX, remapY) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        val rotationMatrix = FloatArray(9)
        val remappedRotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var lastAccel: FloatArray? = null
        var lastMag: FloatArray? = null
        // Plain local, not Compose state — SENSOR_DELAY_GAME fires up to ~50 times/sec, and
        // publishing every single one to `azimuth` would mean a recomposition (plus a
        // LaunchedEffect relaunch and spring re-target downstream in CompassNavigationScreen)
        // that often, almost entirely pure sensor jitter well under what's visible on a rotating
        // needle. NaN sentinel so the very first reading always publishes unconditionally.
        var lastPublishedAzimuth = Float.NaN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastAccel = event.values.copyOf()
                        if (lastMag != null &&
                            SensorManager.getRotationMatrix(
                                rotationMatrix,
                                inclinationMatrix,
                                lastAccel,
                                lastMag
                            )
                        ) {
                            // matrix updated
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lastMag = event.values.copyOf()
                        if (lastAccel != null &&
                            SensorManager.getRotationMatrix(
                                rotationMatrix,
                                inclinationMatrix,
                                lastAccel,
                                lastMag
                            )
                        ) {
                            // matrix updated
                        }
                    }
                }

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_ACCELEROMETER ||
                    event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD
                ) {
                    SensorManager.remapCoordinateSystem(rotationMatrix, remapX, remapY, remappedRotationMatrix)
                    SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
                    val degrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val newAzimuth = (degrees + 360f) % 360f
                    // Threshold-filtered, not published unconditionally — see lastPublishedAzimuth
                    // above. shortestAngleDelta (not a plain subtraction) so the filter doesn't
                    // misfire right at the 0°/360° wrap.
                    if (lastPublishedAzimuth.isNaN() ||
                        kotlin.math.abs(shortestAngleDelta(lastPublishedAzimuth, newAzimuth)) >= AZIMUTH_UPDATE_THRESHOLD_DEGREES
                    ) {
                        lastPublishedAzimuth = newAzimuth
                        azimuth = newAzimuth
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Only the sensor(s) actually feeding the azimuth above are relevant — on a
                // rotation-vector device that's just the one sensor; on the raw-fusion fallback
                // it's magnetometer specifically (the one actually vulnerable to nearby
                // metal/magnets — accelerometer accuracy isn't the "figure-8" kind of problem).
                val relevant = sensor?.type == Sensor.TYPE_ROTATION_VECTOR ||
                    (rotationVector == null && sensor?.type == Sensor.TYPE_MAGNETIC_FIELD)
                if (relevant) {
                    needsCalibration = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                        accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }

        fun startListening() {
            if (rotationVector != null) {
                sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
            } else if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        fun stopListening() {
            sensorManager.unregisterListener(listener)
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startListening()
                Lifecycle.Event.ON_STOP -> stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            stopListening()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return DeviceAzimuthState(azimuth, needsCalibration, hasSensor)
}

/** How old the seed fix from [fused.lastLocation] is allowed to be before the compass shows it
 * as a temporary starting point while waiting for the first live update. Unlike
 * resolveCurrentLocation's fast-path cache reuse (3s, for a one-shot pin save that has to be
 * right immediately), this only ever seeds a *continuous* display that the live 1-2s update
 * stream corrects moments later — the risk being guarded against here is a stale-but-plausible
 * cached fix (from wherever the phone last got a fix, possibly somewhere else entirely) briefly
 * pointing the compass in a wrong direction with no "still waiting" indication at all, not the
 * final accuracy of what's shown once live updates arrive. */
private const val COMPASS_SEED_MAX_AGE_MS = 10_000L

/** [bearingDegrees]/[speedMps] are only ever populated from a live update (never the
 * fused.lastLocation seed below — a cached fix's bearing is typically stale/absent and not worth
 * trusting for anything). Both null unless the provider actually reported them, which on most
 * devices only happens while genuinely moving — see [CompassNavigationScreen]'s
 * MIN_GPS_BEARING_SPEED_MPS gate for why a low/absent speed means this shouldn't be trusted as a
 * heading either. */
private data class LiveLocationFix(
    val latLng: Pair<Double, Double>,
    val bearingDegrees: Float?,
    val speedMps: Float?
)

/** Tracks the system-wide Location toggle (not permission — [isLocationServicesEnabled] checks
 * device-level GPS/network provider state), re-checked on every ON_START the same way permission
 * is above. Without this, a user with Location turned off entirely saw no different from one still
 * waiting on a slow fix: [rememberLiveUserLocation]'s requestLocationUpdates has no timeout of its
 * own to time out *from*, so "Waiting for GPS fix…" would just sit there forever with nothing
 * telling them why, or how to fix it. */
@Composable
private fun rememberLocationServicesEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isLocationServicesEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                enabled = isLocationServicesEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

@SuppressLint("MissingPermission")
@Composable
private fun rememberLiveUserLocation(): LiveLocationFix? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<LiveLocationFix?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Tied to the host lifecycle for the same reason as rememberDeviceAzimuth above — otherwise
    // a 1-2s PRIORITY_HIGH_ACCURACY location request keeps running while the app sits
    // backgrounded on this screen. Re-checking permission on every ON_START (rather than only
    // once, the first time this effect ran) also means granting location permission from system
    // Settings and returning to the app actually starts updates, instead of staying stuck on
    // "Waiting for GPS fix..." until the screen is left and reopened.
    DisposableEffect(context, lifecycleOwner) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    location = LiveLocationFix(
                        loc.latitude to loc.longitude,
                        bearingDegrees = if (loc.hasBearing()) loc.bearing else null,
                        speedMps = if (loc.hasSpeed()) loc.speed else null
                    )
                }
            }
        }

        var isListening = false

        fun startListening() {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (isListening || (!fineGranted && !coarseGranted)) return

            isListening = true
            fused.lastLocation.addOnSuccessListener { last ->
                // Unfiltered before this, a cached fix from wherever the phone last resolved
                // location — possibly hours old, possibly from an entirely different place —
                // would seed the compass immediately with no age/freshness check at all, briefly
                // pointing it in a plain wrong direction with the "Waiting for GPS fix…" status
                // never showing (that status is driven by `location == null`, which this seed
                // unconditionally cleared the instant it loaded, live update or not).
                val ageMs = last?.time?.let { System.currentTimeMillis() - it }
                if (last != null && ageMs != null && ageMs in 0..COMPASS_SEED_MAX_AGE_MS) {
                    location = LiveLocationFix(last.latitude to last.longitude, bearingDegrees = null, speedMps = null)
                }
            }
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }

        fun stopListening() {
            if (!isListening) return
            isListening = false
            fused.removeLocationUpdates(callback)
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startListening()
                Lifecycle.Event.ON_STOP -> stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            stopListening()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return location
}
