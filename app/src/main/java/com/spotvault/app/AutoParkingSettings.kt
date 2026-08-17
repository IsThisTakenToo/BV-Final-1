package com.spotvault.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@Composable
fun AutomaticParkingSettingsContent(
    prefs: SharedPreferences,
    dao: LocationDao,
    onNavigateToVehicles: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val linkedVehicles by vehicleDao.observeActive().collectAsState(initial = emptyList())

    val enabled by remember(prefs) { prefs.observeAutoParkEnabled() }
        .collectAsStateWithLifecycle(initialValue = isAutoParkEnabled(prefs))
    var quietZones by remember { mutableStateOf(loadAutoParkQuietZones(prefs)) }
    var vaultSpots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    val vehiclesWithBluetooth = remember(linkedVehicles) {
        linkedVehicles.filter { !it.bluetoothMac.isNullOrBlank() }
    }
    val legacyMac = remember { loadAutoParkCarMac(prefs) }

    var showBackgroundLocationPrimer by remember { mutableStateOf(false) }
    var showAddQuietZone by remember { mutableStateOf(false) }
    var showXiaomiAutostartNotice by remember { mutableStateOf(false) }

    // Shown once, right after the battery-optimization prompt, the first time Automatic Parking
    // is turned on on a Xiaomi/Redmi/POCO device — never again afterward on this install.
    fun maybeShowXiaomiAutostartNotice() {
        if (isXiaomiFamilyDevice() && !prefs.getBoolean("xiaomi_autostart_notice_seen", false)) {
            showXiaomiAutostartNotice = true
        }
    }

    val hasFineLocation = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    var fineLocationGranted by remember { mutableStateOf(hasFineLocation) }
    var backgroundLocationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                fineLocationGranted
            }
        )
    }
    var bluetoothConnectGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var batteryOptDisabled by remember {
        mutableStateOf(isBatteryOptimizationDisabled(context))
    }
    var backgroundRestricted by remember {
        mutableStateOf(isAppBackgroundRestricted(context))
    }
    val motionEnabled by remember(prefs) { prefs.observeMotionAutoParkEnabled() }
        .collectAsStateWithLifecycle(initialValue = isMotionAutoParkEnabled(prefs))
    var activityRecognitionGranted by remember { mutableStateOf(hasActivityRecognitionPermission(context)) }

    // Computed here (rather than down with the rest of the derived UI state below) so
    // activityRecognitionLauncher's callback, declared next, can reference it — that callback
    // needs to know whether the base Auto-Park prerequisites are satisfied to decide whether a
    // just-granted Motion permission should arm the watch immediately.
    val hasLinkedVehicle = vehiclesWithBluetooth.isNotEmpty() || !legacyMac.isNullOrBlank()
    val armed = enabled &&
        hasLinkedVehicle &&
        fineLocationGranted &&
        backgroundLocationGranted &&
        (bluetoothConnectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        fineLocationGranted = granted
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundLocationPrimer = true
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
        if (!batteryOptDisabled) {
            requestBatteryOptimizationExemption(context)
            maybeShowXiaomiAutostartNotice()
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        bluetoothConnectGranted = granted
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityRecognitionGranted = granted
        if (!granted) {
            prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, false).commit()
            stopMotionWatch(context, prefs)
        } else if (armed) {
            // The Motion toggle's own onCheckedChange below already tries this "arm right away"
            // call when the toggle is flipped on — but when ACTIVITY_RECOGNITION wasn't already
            // granted (the common first-time case), that call fires this permission request and,
            // in the same synchronous block, immediately calls armMotionWatchIfAlreadyConnected —
            // which reaches startMotionWatch, sees the permission isn't granted *yet* (the
            // request is still an async dialog at that point), and silently no-ops. Nothing
            // previously re-ran that arm attempt once the grant actually landed here, so the
            // watch stayed un-armed until the next Bluetooth disconnect/reconnect — precisely the
            // gap this whole "arm immediately" mechanism exists to close. Mirrors the toggle's own
            // branch exactly.
            vehiclesWithBluetooth.forEach { armMotionWatchIfAlreadyConnected(context, it.bluetoothMac!!) }
            legacyMac?.let { armMotionWatchIfAlreadyConnected(context, it) }
        }
    }

    val combinedPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results[Manifest.permission.ACCESS_FINE_LOCATION]?.let { fineLocationGranted = it }
        results[Manifest.permission.BLUETOOTH_CONNECT]?.let { bluetoothConnectGranted = it }
        if (fineLocationGranted && !backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundLocationPrimer = true
        } else if (!batteryOptDisabled) {
            requestBatteryOptimizationExemption(context)
            maybeShowXiaomiAutostartNotice()
        }
    }

    LaunchedEffect(Unit) {
        migrateLegacyCarVehicleIfNeeded(context, prefs, vehicleDao)
        vaultSpots = dao.getHistoryList().filter { !it.isWishlist }
    }

    LaunchedEffect(enabled) {
        // Only auto-chase missing permissions once Automatic Parking is actually turned on —
        // this used to fire unconditionally the instant the screen opened, before a first-time
        // visitor had touched anything, surfacing a surprise cascade of system permission
        // dialogs. Worse, if the user then tapped a toggle (e.g. Motion) while that cascade was
        // still mid-flight, Android silently drops a second permission request while one is
        // already showing — the toggle's own request never appeared until they backed out and
        // tried again. Gating on `enabled` means this only runs for someone who's deliberately
        // opting in, with nothing else competing for the permission dialog.
        if (!enabled) return@LaunchedEffect
        val missing = buildList {
            if (!fineLocationGranted) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothConnectGranted) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (missing.isNotEmpty()) {
            combinedPermissionLauncher.launch(missing.toTypedArray())
        } else if (fineLocationGranted && !backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundLocationPrimer = true
        } else if (!batteryOptDisabled) {
            requestBatteryOptimizationExemption(context)
            maybeShowXiaomiAutostartNotice()
        }
    }

    // Every granted/disabled flag above is a snapshot from whenever this screen last recomposed
    // for that value — accurate when the user grants something through one of this screen's own
    // launchers, but stale for battery optimization and any permission granted from a *system*
    // settings screen we navigated away to (there's no launcher callback for those, since the
    // user backs out on their own). Re-reading everything on RESUME is what makes "grant it in
    // system settings, come back" actually reflect in the UI without needing to leave and
    // reopen this screen a second time.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    fineLocationGranted
                }
                bluetoothConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                val stillGranted = hasActivityRecognitionPermission(context)
                // A revocation via system Settings (not this screen's own request launcher,
                // which already calls stopMotionWatch on denial) left the ActivityTransition
                // registration with Play Services alive indefinitely — this screen only ever
                // updated the status label to reflect the loss, the same leak already fixed for
                // the master-toggle-off case but not for a mid-session permission pull.
                if (activityRecognitionGranted && !stillGranted) {
                    stopMotionWatch(context, prefs)
                }
                activityRecognitionGranted = stillGranted
                batteryOptDisabled = isBatteryOptimizationDisabled(context)
                backgroundRestricted = isAppBackgroundRestricted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // hasLinkedVehicle/armed are computed earlier now (see the comment above
    // activityRecognitionLauncher) — reused here unchanged.
    // Independent of `enabled`/`hasLinkedVehicle` (unlike `armed`) — this is specifically "does
    // the Step 2 card have anything left to grant," so its header can drop the "Step 2" framing
    // once true, even if the master toggle is still off or no vehicle is linked yet. Includes
    // battery optimization since that's one of the four rows/buttons that card actually shows —
    // leaving it out would drop the "Step" framing while a live "Disable Battery Optimization"
    // button was still sitting right there.
    val allAutoParkPermissionsGranted = fineLocationGranted &&
        backgroundLocationGranted &&
        (bluetoothConnectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) &&
        batteryOptDisabled

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Auto-Park Trigger",
            subtitle = "When your phone disconnects from your car's Bluetooth, DropPin Vault saves a parking pin and starts Active Tracking — even if the app is closed."
        ) {
            SettingsToggleRow(
                title = "Enable Automatic Parking",
                subtitle = "Off by default. Requires permissions below.",
                checked = enabled,
                onCheckedChange = { on ->
                    prefs.edit().putBoolean(AUTO_PARK_ENABLED_PREF, on).commit()
                    WidgetThemeHelper.bumpWidgetRevision(prefs)
                    WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
                    if (!on) {
                        // CarBluetoothReceiver bails out before ever reaching stopMotionWatch()
                        // once isAutoParkEnabled is false, so that's not a path that can clean
                        // this up on its own — without calling it here, a watch armed while this
                        // was on (car already connected) stays registered with Play Services
                        // indefinitely, waking the app on every walk transition for no reason.
                        stopMotionWatch(context, prefs)
                    } else if (motionEnabled) {
                        // Otherwise, turning this on while already connected to a linked car
                        // would leave the watch un-armed until the next disconnect/reconnect.
                        vehiclesWithBluetooth.forEach { armMotionWatchIfAlreadyConnected(context, it.bluetoothMac!!) }
                        legacyMac?.let { armMotionWatchIfAlreadyConnected(context, it) }
                    }
                }
            )

            var quietSaveMode by remember { mutableStateOf(isAutoParkQuietSaveMode(prefs)) }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Auto-Park Mode",
                color = SpotVaultColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (quietSaveMode) {
                    "Silently saves to the Vault with no notifications."
                } else {
                    "Saves the spot and starts a live, persistent tracking notification."
                },
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            TrackingModeSegmentedToggle(
                isActiveTracking = !quietSaveMode,
                onModeChange = { activeTracking ->
                    quietSaveMode = !activeTracking
                    prefs.edit().putBoolean(AUTO_PARK_QUIET_SAVE_MODE_PREF, !activeTracking).apply()
                }
            )

            // Muted here used to mean two very different things: "genuinely off" and "you turned
            // this on, but it's not actually protecting you yet" — both looked identical apart
            // from the toggle itself, and the toggle's own teal "on" look didn't communicate
            // anything was wrong. Danger red for the second case makes it visually obvious the
            // feature isn't armed despite the switch reading on, without blocking the toggle
            // itself — someone flipping this on before adding their car is a reasonable setup
            // order, not something to prevent.
            val statusColor = when {
                armed -> SpotVaultColors.Teal
                enabled -> SpotVaultColors.Danger
                else -> SpotVaultColors.Muted
            }
            val statusText = when {
                !enabled -> "Disabled"
                !hasLinkedVehicle -> "Complete Step 1 below: link a vehicle's Bluetooth"
                !fineLocationGranted || !backgroundLocationGranted -> "Complete Step 2 below: grant location permission (including background)"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothConnectGranted ->
                    "Complete Step 2 below: grant Bluetooth permission to identify your car"
                vehiclesWithBluetooth.size == 1 ->
                    "Armed — will auto-save when ${vehiclesWithBluetooth.first().name} disconnects"
                else -> "Armed — ${vehiclesWithBluetooth.size} vehicles linked"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpotVaultColors.Elevated.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (enabled && !armed) Icons.Default.Warning else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text("Status", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        statusText,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = if (enabled && !armed) FontWeight.Bold else FontWeight.Normal
                    )
                    vehiclesWithBluetooth.forEach { vehicle ->
                        Text(
                            "${vehicleIconEmoji(vehicle.iconKey)} ${vehicle.name}: ${vehicle.bluetoothName ?: vehicle.bluetoothMac}",
                            color = SpotVaultColors.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (armed) {
                Spacer(modifier = Modifier.height(4.dp))
                AutoParkActionButton(
                    text = "Test Auto-Park Now",
                    icon = Icons.Default.Bluetooth,
                    onClick = {
                        val vehicleId = vehiclesWithBluetooth.first().id
                        enqueueAutoParkWorkForVehicle(context, vehicleId)
                        android.widget.Toast.makeText(
                            context,
                            "Testing…",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        // The old version of this button just fired the job and hoped — if the
                        // save was silently skipped (a Quiet Zone, a missing permission, Auto-Park
                        // toggled off mid-flight) there was no way to tell "it worked" from "it did
                        // nothing" apart from digging through the Vault yourself. Watching this
                        // exact job's WorkInfo and reading back what AutoParkWorker actually did
                        // turns that guesswork into a real answer.
                        coroutineScope.launch {
                            val outcome = WorkManager.getInstance(context)
                                .getWorkInfosForUniqueWorkFlow(autoParkManualTestWorkName(vehicleId))
                                .mapNotNull { infos -> infos.firstOrNull { it.state.isFinished } }
                                .first()
                                .outputData
                                .getString(AUTO_PARK_OUTCOME_KEY)
                            android.widget.Toast.makeText(
                                context,
                                autoParkTestOutcomeMessage(outcome),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
                Text(
                    "Runs the exact same save the real Bluetooth disconnect triggers, without needing to actually disconnect — good way to confirm everything's wired up.",
                    color = SpotVaultColors.Muted,
                    fontSize = 11.sp
                )
            }

            // Collapsed by default — a power-user tweak on a value that's already right for
            // almost everyone (8s), not something to surface to every visitor of this screen.
            var showAdvanced by remember { mutableStateOf(false) }
            var disconnectConfirmSeconds by remember { mutableStateOf(loadBtDisconnectConfirmSeconds(prefs)) }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Advanced", color = SpotVaultColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SpotVaultColors.Muted
                )
            }
            if (showAdvanced) {
                Text(
                    "Disconnect Confirmation Delay",
                    color = SpotVaultColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "How long your car's Bluetooth has to stay connected before a disconnect counts as real — filters out the brief drop-and-reconnect blip some head units do right after pairing or reconnecting. Higher is safer against false saves; lower reacts faster to a genuine, quick parking stop. Default: ${DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS}s.",
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (disconnectConfirmSeconds == 0) "Off (instant)" else "${disconnectConfirmSeconds}s",
                        color = SpotVaultColors.OnSurface,
                        fontSize = 13.sp
                    )
                    if (disconnectConfirmSeconds != DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS) {
                        TextButton(onClick = {
                            disconnectConfirmSeconds = DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS
                            prefs.edit().putInt(BT_DISCONNECT_CONFIRM_SECONDS_PREF, DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS).apply()
                        }) {
                            Text("Reset to default", fontSize = 12.sp, color = SpotVaultColors.Teal)
                        }
                    }
                }
                Slider(
                    value = disconnectConfirmSeconds.toFloat(),
                    onValueChange = { newValue ->
                        val seconds = newValue.toInt()
                        disconnectConfirmSeconds = seconds
                        prefs.edit().putInt(BT_DISCONNECT_CONFIRM_SECONDS_PREF, seconds).apply()
                    },
                    valueRange = 0f..MAX_BT_DISCONNECT_CONFIRM_SECONDS.toFloat(),
                    steps = MAX_BT_DISCONNECT_CONFIRM_SECONDS - 1
                )
            }
        }

        // Setup order matters here: a first-time visitor used to land on the master toggle and
        // status card, then have to scroll past Motion & Fitness and Quiet Zones — two entirely
        // optional refinements — before ever reaching the vehicle link and permissions that
        // actually determine whether "Armed" is even reachable. Vehicle Bluetooth Links and
        // Permissions & Reliability are the only two prerequisites, so they now come immediately
        // after the master toggle, numbered as Step 1 / Step 2, with the optional refinements
        // pushed below them.
        SettingsSectionCard(
            title = if (hasLinkedVehicle) "Linked Vehicle" else "Step 1 · Link a Vehicle",
            subtitle = "Automatic Parking watches for this vehicle's Bluetooth to disconnect — add or change it in Vehicles"
        ) {
            if (vehiclesWithBluetooth.isEmpty()) {
                Text(
                    "No vehicles with Bluetooth linked yet. Add a vehicle and link its car stereo or hands-free device there.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp
                )
            } else {
                vehiclesWithBluetooth.forEach { vehicle ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(vehicleIconEmoji(vehicle.iconKey), fontSize = 16.sp)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(vehicle.name, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                vehicle.bluetoothName ?: vehicle.bluetoothMac.orEmpty(),
                                color = SpotVaultColors.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            SpotVaultButton(
                onClick = onNavigateToVehicles,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                shape = spotVaultButtonShape(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Primary,
                    contentColor = SpotVaultColors.OnPrimary
                )
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    if (vehiclesWithBluetooth.isEmpty()) "Link a Bluetooth Device" else "Manage Vehicle Bluetooth Links",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        SettingsSectionCard(
            title = if (allAutoParkPermissionsGranted) "System Permissions" else "Step 2 · Grant Permissions",
            subtitle = "Required for background auto-park on Android 12–15+"
        ) {
            PermissionStatusRow(
                title = "Bluetooth (nearby devices)",
                granted = bluetoothConnectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                explanation = "Identifies when your car's Bluetooth disconnects."
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothConnectGranted) {
                AutoParkActionButton(
                    text = "Grant Bluetooth Permission",
                    onClick = { bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                )
            }

            PermissionStatusRow(
                title = "Location (while using app)",
                granted = fineLocationGranted,
                explanation = "Required before background location can be requested."
            )
            if (!fineLocationGranted) {
                AutoParkActionButton(
                    text = "Grant Location Permission",
                    onClick = { fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
            }

            PermissionStatusRow(
                title = "Location (all the time)",
                granted = backgroundLocationGranted,
                explanation = "DropPin Vault needs your parking location when the app is closed. Android requires this as a separate step from foreground location."
            )
            if (fineLocationGranted && !backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AutoParkActionButton(
                    text = "Grant Background Location",
                    onClick = { showBackgroundLocationPrimer = true }
                )
            }

            PermissionStatusRow(
                title = "Battery optimization disabled",
                granted = batteryOptDisabled,
                explanation = "Many phones kill background Bluetooth and WorkManager jobs unless DropPin Vault is exempted. This is the main reliability fix on Samsung, Xiaomi, etc."
            )
            if (!batteryOptDisabled) {
                AutoParkActionButton(
                    text = "Disable Battery Optimization",
                    icon = Icons.Default.BatteryAlert,
                    onClick = {
                        requestBatteryOptimizationExemption(context)
                        batteryOptDisabled = isBatteryOptimizationDisabled(context)
                    }
                )
            }
            // Generic, always-visible — standard battery-optimization exemption above doesn't
            // cover every OEM's own separate background-app allowlist (Samsung's "Sleeping apps,"
            // Huawei's "Protected apps," OPPO/vivo's Startup Manager/Autostart, etc.), and building
            // a detection+dialog for every manufacturer individually (the way the Xiaomi-specific
            // notice below does, since MIUI's Autostart is unusually persistent) isn't worth the
            // ongoing upkeep for OEMs where a one-line pointer is enough.
            Text(
                "Some phones also have their own separate battery/background app list (sometimes called Autostart, Protected Apps, or similar) — check your phone's battery settings if parking still isn't detected reliably after enabling this.",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (backgroundRestricted) {
                PermissionStatusRow(
                    title = "Background activity restricted",
                    granted = false,
                    explanation = "You've manually set DropPin Vault to \"Restricted\" in Android's Battery settings — a separate, stricter block than battery optimization that silently prevents auto-park from ever running in the background, even fully exempted. Change it to \"Unrestricted\" or \"Optimized\" on the app's Battery screen."
                )
                AutoParkActionButton(
                    text = "Open Battery Settings",
                    icon = Icons.Default.BatteryAlert,
                    onClick = { openAppSettings(context) }
                )
            }
        }

        SettingsSectionCard(
            title = "Motion & Fitness Detection (Optional)",
            subtitle = "Drops your parking pin the instant you start walking away from a linked car — no need to wait for Bluetooth to disconnect."
        ) {
            SettingsToggleRow(
                title = "Enable Motion & Fitness",
                subtitle = "Follows the same Quiet Zones and only watches vehicles linked above.",
                checked = motionEnabled,
                onCheckedChange = { on ->
                    if (on && !activityRecognitionGranted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            activityRecognitionGranted = true
                        }
                    }
                    prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, on).commit()
                    WidgetThemeHelper.bumpWidgetRevision(prefs)
                    WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
                    if (!on) {
                        stopMotionWatch(context, prefs)
                    } else if (armed) {
                        // Same reasoning as the Automatic Parking toggle above — arm right away
                        // instead of waiting for the next Bluetooth reconnect.
                        vehiclesWithBluetooth.forEach { armMotionWatchIfAlreadyConnected(context, it.bluetoothMac!!) }
                        legacyMac?.let { armMotionWatchIfAlreadyConnected(context, it) }
                    }
                }
            )
            val motionStatusColor = if (motionEnabled && activityRecognitionGranted && armed) SpotVaultColors.Teal else SpotVaultColors.Muted
            val motionStatusText = when {
                !motionEnabled -> "Disabled"
                !armed -> "Waiting on Steps 1–2 above to be fully complete"
                !activityRecognitionGranted -> "Motion & Fitness permission needed"
                else -> "Armed — watches for walking after your car's Bluetooth connects"
            }
            Text(motionStatusText, color = motionStatusColor, fontSize = 12.sp)
            if (motionEnabled && !activityRecognitionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AutoParkActionButton(
                    text = "Grant Motion & Fitness Permission",
                    onClick = { activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                )
            }
        }

        SettingsSectionCard(
            title = "Quiet Zones (Optional)",
            subtitle = "Skip or silence auto-park near home, work, or other saved areas"
        ) {
            if (quietZones.isEmpty()) {
                Text("No quiet zones yet.", color = SpotVaultColors.Muted, fontSize = 13.sp)
            } else {
                quietZones.forEach { zone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(zone.name, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            val modeLabel = when (zone.mode) {
                                AutoParkQuietZoneMode.SKIP -> "Don't save here"
                                AutoParkQuietZoneMode.SILENT -> "Save silently"
                            }
                            Text(
                                "$modeLabel · ${(zone.radiusMeters * 3.28084).toInt()} ft radius",
                                color = SpotVaultColors.Muted,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = {
                            quietZones = quietZones.filter { it.id != zone.id }
                            saveAutoParkQuietZones(prefs, quietZones)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete zone", tint = SpotVaultColors.Muted)
                        }
                    }
                }
            }
            AutoParkActionButton(text = "Add Zone", onClick = { showAddQuietZone = true })
        }
    }

    if (showBackgroundLocationPrimer) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationPrimer = false },
            title = { Text("Background location") },
            text = {
                Text(
                    "Automatic Parking saves your location when you leave your car, even if DropPin Vault isn't open — " +
                        "either when your car's Bluetooth disconnects, or (if Motion & Fitness is enabled) the moment " +
                        "motion sensors detect you've stepped away while still connected. " +
                        "Android requires you to allow location \"All the time\" in the next system screen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundLocationPrimer = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) {
                    Text("Continue", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationPrimer = false }) {
                    Text("Not now", color = SpotVaultColors.Muted)
                }
            }
        )
    }

    if (showXiaomiAutostartNotice) {
        AlertDialog(
            onDismissRequest = {
                showXiaomiAutostartNotice = false
                prefs.edit().putBoolean("xiaomi_autostart_notice_seen", true).apply()
            },
            title = { Text("Xiaomi Device Detected") },
            text = {
                Text(
                    "Because you are using a Xiaomi device, you will also need to manually find and enable " +
                        "\"Autostart\" for DropPin Vault in your phone's Security settings so background parking " +
                        "works. We wish we could open this screen for you automatically, but Xiaomi changes it " +
                        "too often for us to link it reliably!"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showXiaomiAutostartNotice = false
                    prefs.edit().putBoolean("xiaomi_autostart_notice_seen", true).apply()
                }) {
                    Text("Got it", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddQuietZone) {
        AddQuietZoneDialog(
            vaultSpots = vaultSpots,
            onDismiss = { showAddQuietZone = false },
            onSave = { zone ->
                quietZones = quietZones + zone
                saveAutoParkQuietZones(prefs, quietZones)
                showAddQuietZone = false
            },
            resolveCurrentLocation = {
                resolveCurrentLocation(context, prefs)
            }
        )
    }
}

@Composable
fun PermissionStatusRow(title: String, granted: Boolean, explanation: String) {
    val color = if (granted) SpotVaultColors.Teal else SpotVaultColors.Muted
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top, not CenterVertically — matters once the title can wrap to a second line (see
        // below): top-aligns the icon and the Granted/Needed label with the title's first line
        // instead of the vertical middle of the whole wrapped block. Identical to
        // CenterVertically for the common single-line case.
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (granted) Icons.Default.Check else Icons.Default.LocationOn,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                title,
                color = SpotVaultColors.OnSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                // Wraps instead of eliding — this used to be maxLines = 1 with
                // TextOverflow.Ellipsis, which silently cut "Battery optimization disabled" down
                // to "Battery opt…" even after fixing the row's weight split above to give it the
                // real available width, because the raised text-size setting (this app's own
                // "Default" is already 1.15x, "Extra Large" 1.3x) can still outgrow a single line
                // on top of that. Letting it wrap is the only fix that can't silently truncate
                // again at a larger scale or a longer title string added later.
                // A plain weight(1f) — not weight(1f, fill = false) plus a same-weight Spacer.
                // That split the row's leftover width 50/50 between title and spacer regardless
                // of whether the spacer actually needed any of it, capping the title at half the
                // real available width even when the row had plenty of room to spare. A single
                // weight(1f) here claims all of that leftover space for the title (still renders
                // left-aligned, so short titles look identical) and still pushes the Granted/
                // Needed label to the row's end on its own, no separate Spacer needed.
                modifier = Modifier.padding(start = 8.dp, end = 8.dp).weight(1f)
            )
            Text(if (granted) "Granted" else "Needed", color = color, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
        Text(explanation, color = SpotVaultColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 26.dp))
    }
}

/** Human-readable version of [AutoParkWorker]'s [AUTO_PARK_OUTCOME_KEY] output, for the "Test
 * Auto-Park Now" button's result toast. */
private fun autoParkTestOutcomeMessage(outcome: String?): String = when (outcome) {
    AUTO_PARK_OUTCOME_SAVED -> "Test saved — check your Vault!"
    AUTO_PARK_OUTCOME_QUIET_ZONE_SKIP -> "Test skipped — your current location is inside a Quiet Zone that suppresses saves here."
    AUTO_PARK_OUTCOME_NO_BACKGROUND_LOCATION -> "Test failed — background location permission isn't granted (see Step 2 below)."
    AUTO_PARK_OUTCOME_NO_VEHICLE -> "Test failed — couldn't find a linked vehicle."
    AUTO_PARK_OUTCOME_VEHICLE_ARCHIVED -> "Test skipped — that vehicle is archived."
    AUTO_PARK_OUTCOME_DISABLED -> "Test skipped — Automatic Parking is turned off."
    AUTO_PARK_OUTCOME_STILL_CONNECTED -> "Test skipped — that vehicle's Bluetooth is still connected."
    AUTO_PARK_OUTCOME_FAILED -> "Test ran but the save failed — try again."
    else -> "Test finished, but the result was unclear — check your Vault."
}

/** Every action button on this screen used to be a bare Material OutlinedButton, whose default
 * text color is the theme's primary purple — low contrast on the dark card backgrounds here.
 * This always uses Teal, which stays readable across every theme. */
@Composable
fun AutoParkActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SpotVaultOutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        shape = spotVaultButtonShape(),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddQuietZoneDialog(
    vaultSpots: List<LocationSpot>,
    onDismiss: () -> Unit,
    onSave: (AutoParkQuietZone) -> Unit,
    resolveCurrentLocation: suspend () -> Pair<Double, Double>?
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(150f) }
    var mode by remember { mutableStateOf(AutoParkQuietZoneMode.SKIP) }
    var centerLat by remember { mutableStateOf<Double?>(null) }
    var centerLng by remember { mutableStateOf<Double?>(null) }
    var centerLabel by remember { mutableStateOf("Not set") }
    var modeExpanded by remember { mutableStateOf(false) }
    var spotExpanded by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add quiet zone", fontWeight = FontWeight.Bold) },
        content = {
            // PremiumDialog caps content height (heightIn(max = 54% of screen)) but doesn't
            // scroll it itself — this dialog stacks a name field, locate button, an optional
            // vault-spot dropdown, radius slider, and mode dropdown, which routinely exceeds
            // that cap on a shorter phone or with a larger text-size setting. Without this, the
            // overflow was silently clipped instead of scrollable, cutting off the lower fields.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Zone name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { VoiceMicButton(onResult = { name = it }, prompt = "Speak zone name…") }
                )

                var locatingNow by remember { mutableStateOf(false) }
                AutoParkActionButton(
                    text = if (locatingNow) "Locating…" else "Use Current Location",
                    icon = Icons.Default.MyLocation,
                    onClick = {
                        locatingNow = true
                        scope.launch {
                            val loc = resolveCurrentLocation()
                            locatingNow = false
                            if (loc != null) {
                                centerLat = loc.first
                                centerLng = loc.second
                                centerLabel = "Current location"
                                locationError = null
                            } else {
                                locationError = "Could not get location — try again outdoors."
                            }
                        }
                    }
                )

                if (vaultSpots.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = spotExpanded, onExpandedChange = { spotExpanded = it }) {
                        OutlinedTextField(
                            value = "Pick from Vault",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spotExpanded) }
                        )
                        DropdownMenu(expanded = spotExpanded, onDismissRequest = { spotExpanded = false }) {
                            vaultSpots.take(30).forEach { spot ->
                                DropdownMenuItem(
                                    text = { Text(spot.locationDetails.ifBlank { spot.title }) },
                                    onClick = {
                                        centerLat = spot.lat
                                        centerLng = spot.lng
                                        centerLabel = spot.locationDetails.ifBlank { spot.title }
                                        spotExpanded = false
                                        locationError = null
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Center: $centerLabel", color = SpotVaultColors.Muted, fontSize = 13.sp)
                locationError?.let { Text(it, color = SpotVaultColors.Danger, fontSize = 12.sp) }

                var useFeet by remember { mutableStateOf(true) }
                // radius is always stored/persisted in meters (matches AutoParkQuietZone's own
                // radiusMeters field) — only the Slider's own value/range need to switch units
                // when useFeet flips, converting back to meters in onValueChange. Previously only
                // the label text converted; the Slider itself always stayed in raw 50f..500f
                // meters regardless of the ft/m toggle, so switching to "ft" relabeled the same
                // meter-range numbers as ~164–1640 ft with no way to actually select anything
                // below 164 ft despite the slider visually spanning its full track.
                val metersToFeet = 3.28084f
                val minRadiusMeters = 50f
                val maxRadiusMeters = 500f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayValue = if (useFeet) (radius * metersToFeet).toInt() else radius.toInt()
                    val displayUnit = if (useFeet) "ft" else "m"
                    Text("Radius: $displayValue $displayUnit", color = SpotVaultColors.OnSurface, fontSize = 14.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SpotVaultColors.Deep)
                            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                            .padding(2.dp)
                    ) {
                        listOf(true to "ft", false to "m").forEach { (isFeet, label) ->
                            val selected = useFeet == isFeet
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) SpotVaultColors.Teal else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { useFeet = isFeet }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) SpotVaultColors.Ink else SpotVaultColors.Muted
                                )
                            }
                        }
                    }
                }
                Slider(
                    value = if (useFeet) radius * metersToFeet else radius,
                    onValueChange = { newValue ->
                        radius = if (useFeet) newValue / metersToFeet else newValue
                    },
                    valueRange = if (useFeet) {
                        (minRadiusMeters * metersToFeet)..(maxRadiusMeters * metersToFeet)
                    } else {
                        minRadiusMeters..maxRadiusMeters
                    },
                    steps = 8
                )

                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
                    val modeLabel = when (mode) {
                        AutoParkQuietZoneMode.SKIP -> "Don't save here"
                        AutoParkQuietZoneMode.SILENT -> "Save silently (no heads-up alert)"
                    }
                    OutlinedTextField(
                        value = modeLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) }
                    )
                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Don't save here") },
                            onClick = { mode = AutoParkQuietZoneMode.SKIP; modeExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Save silently") },
                            onClick = { mode = AutoParkQuietZoneMode.SILENT; modeExpanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = centerLat
                    val lng = centerLng
                    if (name.isBlank() || lat == null || lng == null) {
                        locationError = "Name and center location are required."
                        return@TextButton
                    }
                    onSave(
                        AutoParkQuietZone(
                            name = name.trim(),
                            lat = lat,
                            lng = lng,
                            radiusMeters = radius.toDouble(),
                            mode = mode
                        )
                    )
                }
            ) {
                Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SpotVaultColors.Muted) }
        }
    )
}

// Not private — reused by TimerSettingsContent (SettingsCategoryContent.kt) so Active Tracking's
// own battery-exemption prompt doesn't duplicate this logic.
fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** True if the user has manually forced this app into Android's "Restricted" App Standby Bucket
 * (Settings → Apps → DropPin Vault → Battery → Restricted) — a separate, more aggressive OS
 * mechanism than battery-optimization exemption, and not implied by it either way: an app can be
 * fully exempted from Doze and still be Restricted, which silently starves WorkManager of
 * execution windows regardless. There's no runtime "request" API for this one (unlike battery
 * optimization's ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) — the user can only undo it from the
 * app's own details settings page, which is exactly where [openAppSettings] already deep-links. */
fun isAppBackgroundRestricted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    return am.isBackgroundRestricted
}

fun requestBatteryOptimizationExemption(context: Context) {
    if (isBatteryOptimizationDisabled(context)) return
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        // NEW_TASK forces this onto a fresh task stack, which is only needed when context isn't
        // an Activity. Adding it unconditionally is what caused the "first tap does nothing"
        // symptom on Samsung/Xiaomi — the settings screen here always has an Activity context, so
        // the flag was pure overhead that some OEM ROMs mishandle on the first launch attempt.
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}

/** Xiaomi's MIUI gates background execution behind its own separate "Autostart" permission, on
 * top of standard Android's battery-optimization exemption above — an app fully exempted from
 * Doze can still get killed on MIUI unless Autostart is also manually enabled in the Security
 * app. There's no official API to grant it, and no reliable way to deep-link to it either: the
 * settings path is an undocumented, MIUI-version-specific Activity that changes often enough to
 * silently break. This only detects the device to show an informational notice — it never
 * attempts to open or grant anything automatically. Checks both fields since Redmi/POCO devices
 * (Xiaomi sub-brands, same MIUI Autostart gate) don't always report "Xiaomi" in both. */
private fun isXiaomiFamilyDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    val markers = listOf("xiaomi", "redmi", "poco")
    return markers.any { it in manufacturer || it in brand }
}
