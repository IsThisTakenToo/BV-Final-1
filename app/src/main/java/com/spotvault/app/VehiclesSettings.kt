package com.spotvault.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private sealed class VehiclesScreen {
    data object List : VehiclesScreen()
    data class Edit(val vehicleId: Int?) : VehiclesScreen()
}

@Composable
fun VehiclesSettingsContent(prefs: SharedPreferences, dao: LocationDao) {
    val context = LocalContext.current
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<VehiclesScreen>(VehiclesScreen.List) }
    var showArchived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        migrateLegacyCarVehicleIfNeeded(context, prefs, vehicleDao)
    }

    when (val current = screen) {
        VehiclesScreen.List -> VehicleListScreen(
            vehicleDao = vehicleDao,
            showArchived = showArchived,
            onShowArchivedChange = { showArchived = it },
            onAdd = { screen = VehiclesScreen.Edit(null) },
            onEdit = { id -> screen = VehiclesScreen.Edit(id) }
        )
        is VehiclesScreen.Edit -> VehicleEditScreen(
            vehicleId = current.vehicleId,
            vehicleDao = vehicleDao,
            locationDao = dao,
            onBack = { screen = VehiclesScreen.List }
        )
    }
}

@Composable
private fun VehicleListScreen(
    vehicleDao: VehicleDao,
    showArchived: Boolean,
    onShowArchivedChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val vehicles by vehicleDao.observeAll().collectAsState(initial = emptyList())
    val visible = remember(vehicles, showArchived) {
        if (showArchived) vehicles else vehicles.filter { !it.isArchived }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsSectionCard(
            title = "Vehicles",
            subtitle = "Organize saves and Bluetooth auto-park by vehicle",
            contentSpacing = 6.dp
        ) {
            if (visible.isEmpty()) {
                Text(
                    "Add a vehicle to tag saves, filter your Vault, and link Bluetooth auto-park.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else {
                visible.forEach { vehicle ->
                    VehicleListRow(vehicle = vehicle, onClick = { onEdit(vehicle.id) })
                }
            }
            SpotVaultButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = spotVaultButtonShape(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Primary,
                    contentColor = SpotVaultColors.OnPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Add Vehicle", fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
            }
            if (vehicles.any { it.isArchived }) {
                SettingsToggleRow(
                    title = "Show archived",
                    checked = showArchived,
                    onCheckedChange = onShowArchivedChange
                )
            }
        }
    }
}

@Composable
private fun VehicleListRow(vehicle: Vehicle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(SpotVaultColors.Elevated.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(vehicleDisplayColor(vehicle.colorArgb))
        )
        Icon(
            vehicleIconVector(vehicle.iconKey),
            contentDescription = null,
            tint = vehicleDisplayColor(vehicle.colorArgb),
            modifier = Modifier.padding(start = 8.dp).size(16.dp)
        )
        Text(
            vehicle.name,
            color = SpotVaultColors.OnSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp)
        )
        if (vehicle.isDefault && !vehicle.isArchived) {
            Text(
                "Default",
                color = SpotVaultColors.Teal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (vehicle.isArchived) {
            Text(
                "Archived",
                color = SpotVaultColors.Muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (!vehicle.bluetoothMac.isNullOrBlank()) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = "Bluetooth linked",
                tint = SpotVaultColors.Teal.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 8.dp).size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.Default.Edit, contentDescription = null, tint = SpotVaultColors.Muted, modifier = Modifier.size(16.dp))
    }
}

/** Single-line name field matching the app's compact bordered-pill inputs elsewhere — a plain
 * Material OutlinedTextField carries far more built-in vertical padding than this form needs. */
@Composable
private fun CompactVehicleNameField(value: String, onValueChange: (String) -> Unit, isError: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vehicle Name", color = SpotVaultColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(" *", color = SpotVaultColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isError) 1.5.dp else 1.dp,
                    color = if (isError) SpotVaultColors.Danger else SpotVaultColors.Outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isError) SpotVaultColors.Danger else SpotVaultColors.Teal,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("e.g. My Truck, Blue Honda…", color = SpotVaultColors.Muted, fontSize = 14.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = SpotVaultColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(SpotVaultColors.Teal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (isError) {
            Text("A name is required to save this vehicle.", color = SpotVaultColors.Danger, fontSize = 11.sp)
        }
    }
}

/** Motion & Fitness's watch tracks only one MAC globally at a time (see
 * [currentMotionWatchMac]/[stopMotionWatch] in MotionAutoParkManager) — only tears it down if
 * it's actually armed for [mac], so unlinking/archiving/deleting one vehicle can never disarm a
 * watch that's currently armed for a *different*, still-linked vehicle. */
private fun stopMotionWatchIfArmedFor(context: Context, prefs: SharedPreferences, mac: String?) {
    if (!mac.isNullOrBlank() && currentMotionWatchMac(prefs)?.equals(mac, ignoreCase = true) == true) {
        stopMotionWatch(context, prefs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(
    vehicleId: Int?,
    vehicleDao: VehicleDao,
    locationDao: LocationDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<Vehicle?>(null) }
    var name by remember { mutableStateOf("") }
    var colorArgb by remember { mutableStateOf(VehicleColorOptions.first()) }
    var iconKey by remember { mutableStateOf("car") }
    var isDefault by remember { mutableStateOf(false) }
    var bluetoothMac by remember { mutableStateOf<String?>(null) }
    var bluetoothName by remember { mutableStateOf<String?>(null) }
    var bondedDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var bluetoothConnectGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    // getBondedDevices() returns an empty set when Bluetooth is off — same empty result as "no
    // permission" and "genuinely zero paired devices," with nothing in the old single Text
    // message to tell those apart. Tracked separately so the empty-state message (and the
    // Refresh toast below) can say which one it actually is.
    var bluetoothEnabled by remember { mutableStateOf(BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        bluetoothConnectGranted = granted
        if (granted) {
            bluetoothEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
            bondedDevices = loadBondedBluetoothDevices(context)
        }
    }

    var canSetDefault by remember { mutableStateOf(true) }

    LaunchedEffect(vehicleId) {
        canSetDefault = vehicleDao.countActive() > 0 || vehicleId == null
        if (vehicleId != null) {
            existing = vehicleDao.getById(vehicleId)
            existing?.let { v ->
                name = v.name
                colorArgb = v.colorArgb
                iconKey = v.iconKey
                isDefault = v.isDefault
                bluetoothMac = v.bluetoothMac
                bluetoothName = v.bluetoothName
            }
        } else {
            isDefault = vehicleDao.countActive() == 0
            // colorArgb otherwise stays at its initial VehicleColorOptions.first() for every
            // new vehicle — with no auto-assignment, two vehicles added without anyone manually
            // touching the color row end up rendered identically everywhere (Vault filter chips,
            // list badges, the Vehicles list itself), defeating the point of vehicle colors.
            val usedColors = vehicleDao.getActiveList().map { it.colorArgb }.toSet()
            colorArgb = VehicleColorOptions.firstOrNull { it !in usedColors } ?: VehicleColorOptions.first()
        }
        if (bluetoothConnectGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
            bondedDevices = loadBondedBluetoothDevices(context)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpotVaultColors.OnSurface)
            }
            Text(
                if (vehicleId == null) "Add Vehicle" else "Edit Vehicle",
                color = SpotVaultColors.OnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        CompactVehicleNameField(
            value = name,
            onValueChange = { if (it.length <= 40) name = it; nameError = false },
            isError = nameError
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Color", color = SpotVaultColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleColorOptions.forEach { color ->
                    val selected = colorArgb == color
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) SpotVaultColors.OnSurface else SpotVaultColors.Outline.copy(alpha = 0f),
                                shape = CircleShape
                            )
                            .clickable { colorArgb = color }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Icon", color = SpotVaultColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleIconOptions.forEach { (key, _) ->
                    val selected = iconKey == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Color(colorArgb).copy(alpha = 0.2f)
                                else SpotVaultColors.Elevated.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                color = if (selected) Color(colorArgb) else SpotVaultColors.Outline.copy(alpha = 0f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { iconKey = key }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            vehicleIconVector(key),
                            contentDescription = null,
                            tint = if (selected) Color(colorArgb) else SpotVaultColors.Muted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        if (canSetDefault) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as default vehicle", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = spotVaultSwitchColors()
                    )
                }
                Text(
                    "Your default vehicle gets auto-tagged on every Quick Pin, Quick Track, and automatic parking save — until you set a different vehicle as default.",
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        var bluetoothExpanded by remember(bluetoothMac) { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bluetoothExpanded = !bluetoothExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                Text(
                    if (bluetoothMac.isNullOrBlank()) "Link a Bluetooth device (optional)" else "Linked: ${bluetoothName ?: bluetoothMac}",
                    color = SpotVaultColors.OnSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Icon(
                    if (bluetoothExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SpotVaultColors.Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (bluetoothExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VehicleBluetoothDevicePicker(
                        bondedDevices = bondedDevices,
                        selectedMac = bluetoothMac,
                        selectedName = bluetoothName,
                        hasBluetoothPermission = bluetoothConnectGranted,
                        isBluetoothEnabled = bluetoothEnabled,
                        onRefresh = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothConnectGranted) {
                                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                bluetoothEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
                                bondedDevices = loadBondedBluetoothDevices(context)
                                // The inline empty-state message already explains this, but a
                                // toast right when they tap Refresh — the moment they're actively
                                // looking for an answer — is the "why is nothing showing up"
                                // signal that was missing entirely before.
                                if (!bluetoothEnabled) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Bluetooth is turned off — turn it on to see your paired devices.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onSelect = { mac, deviceName ->
                            bluetoothMac = mac
                            bluetoothName = deviceName
                        },
                        onClear = {
                            bluetoothMac = null
                            bluetoothName = null
                        }
                    )
                }
            }
        }

        SpotVaultButton(
            onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@SpotVaultButton
                }
                scope.launch {
                    // Motion & Fitness's watch tracks only one MAC globally (see
                    // MotionAutoParkManager) — unlinking or changing this vehicle's Bluetooth
                    // device out from under an armed watch would otherwise leave that watch (and
                    // its Play Services registration) running indefinitely for a device the app
                    // no longer associates with any vehicle, the same leak the "Automatic Parking"
                    // master-toggle-off cleanup already exists to prevent, just not for this path.
                    val oldMac = existing?.bluetoothMac
                    if (!oldMac.isNullOrBlank() && !oldMac.equals(bluetoothMac, ignoreCase = true)) {
                        stopMotionWatchIfArmedFor(context, prefs, oldMac)
                    }
                    val savedId = saveVehicle(
                        vehicleDao,
                        Vehicle(
                            id = vehicleId ?: 0,
                            name = name.trim(),
                            colorArgb = colorArgb,
                            iconKey = iconKey,
                            notes = existing?.notes ?: "",
                            isDefault = isDefault,
                            isArchived = existing?.isArchived == true,
                            bluetoothMac = bluetoothMac,
                            bluetoothName = bluetoothName,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                    if (isDefault) vehicleDao.setDefault(savedId)
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = spotVaultButtonShape(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = SpotVaultColors.Primary,
                contentColor = SpotVaultColors.OnPrimary
            )
        ) {
            Text("Save Vehicle", fontWeight = FontWeight.Bold)
        }

        if (vehicleId != null && existing?.isArchived != true) {
            SpotVaultOutlinedButton(
                onClick = {
                    scope.launch {
                        stopMotionWatchIfArmedFor(context, prefs, existing?.bluetoothMac)
                        archiveVehicle(vehicleDao, vehicleId)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = spotVaultButtonShape(),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Archive vehicle", fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (vehicleId != null) {
            TextButton(onClick = { showDeleteConfirm = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = SpotVaultColors.Danger, modifier = Modifier.size(16.dp))
                Text("Delete vehicle", fontSize = 13.sp, color = SpotVaultColors.Danger, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    if (showDeleteConfirm && vehicleId != null) {
        PremiumDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete vehicle?", fontWeight = FontWeight.Bold) },
            content = {
                Text(
                    "Saved spots for this vehicle will stay in your Vault — they become unassigned and keep all other details. This cannot be undone.",
                    color = SpotVaultColors.OnSurface,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        stopMotionWatchIfArmedFor(context, prefs, existing?.bluetoothMac)
                        deleteVehicleKeepingHistory(vehicleDao, locationDao, vehicleId)
                        showDeleteConfirm = false
                        onBack()
                    }
                }) {
                    Text("Delete", color = SpotVaultColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }
}

@Composable
fun VehicleBluetoothDevicePicker(
    bondedDevices: List<Pair<String, String>>,
    selectedMac: String?,
    selectedName: String?,
    hasBluetoothPermission: Boolean,
    isBluetoothEnabled: Boolean,
    onRefresh: () -> Unit,
    onSelect: (mac: String, name: String) -> Unit,
    onClear: () -> Unit
) {
    if (bondedDevices.isEmpty()) {
        // getBondedDevices() comes back empty for three completely different reasons — no
        // permission, Bluetooth off, or genuinely zero paired devices — and used to collapse
        // into one generic message that never actually named Bluetooth-off as the cause, which
        // is exactly the case with no popup or explanation of any kind.
        val message = when {
            !hasBluetoothPermission -> "Bluetooth permission needed to see paired devices — tap Refresh to grant it."
            !isBluetoothEnabled -> "Bluetooth is turned off — turn it on, then tap Refresh to see your paired devices."
            else -> "No paired devices found. Pair your car's Bluetooth in Android Settings, then tap Refresh."
        }
        Text(
            message,
            color = SpotVaultColors.Muted,
            fontSize = 13.sp
        )
    } else {
        bondedDevices.forEach { (mac, name) ->
            val selected = mac.equals(selectedMac, ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mac, name) }
                    .background(
                        if (selected) SpotVaultColors.Teal.copy(alpha = 0.12f) else SpotVaultColors.Elevated.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(name, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(mac, color = SpotVaultColors.Muted, fontSize = 12.sp)
                }
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = SpotVaultColors.Teal)
                }
            }
        }
    }
    SpotVaultOutlinedButton(
        onClick = onRefresh,
        modifier = Modifier.height(38.dp),
        shape = spotVaultButtonShape(),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f)),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal)
    ) {
        Text("Refresh paired devices", fontSize = 13.sp)
    }
    if (!selectedMac.isNullOrBlank()) {
        TextButton(onClick = onClear) {
            Text("Unlink Bluetooth device", color = SpotVaultColors.Muted)
        }
    }
}

@Composable
fun VehicleSpotBadge(vehicle: Vehicle?, modifier: Modifier = Modifier) {
    if (vehicle == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(vehicleDisplayColor(vehicle.colorArgb))
        )
        Text(
            vehicle.name,
            fontSize = 12.sp,
            color = SpotVaultColors.Muted,
            maxLines = 1
        )
    }
}

@SuppressLint("MissingPermission")
fun loadBondedBluetoothDevices(context: Context): List<Pair<String, String>> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
    }
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
    return adapter.bondedDevices
        ?.mapNotNull { device ->
            val mac = device.address ?: return@mapNotNull null
            val name = device.name?.takeIf { it.isNotBlank() } ?: mac
            mac to name
        }
        ?.sortedBy { it.second.lowercase() }
        ?: emptyList()
}
