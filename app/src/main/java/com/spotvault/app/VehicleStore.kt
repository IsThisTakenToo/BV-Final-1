package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val PINNED_VEHICLE_ID_PREF = "pinned_vehicle_id"

/** (key, fallback emoji) — the emoji is only used where a plain string glyph is needed
 * (e.g. [QuickPinDef.emoji]); on-screen vehicle icons use [vehicleIconVector] instead so
 * they can be tinted with the vehicle's chosen color. Every key maps to a visually distinct
 * icon — no two options should ever render identically.
 *
 * "van" doubles as "SUV / Van" and "rv" was retired (folded into "other") — both are kept
 * out of this picker list but still resolve to an icon via [VehicleIconResIds] so any
 * vehicle saved with an old iconKey still renders correctly. */
val VehicleIconOptions = listOf(
    "car" to "🚗",
    "truck" to "🛻",
    "van" to "🚙",
    "motorcycle" to "🏍️",
    "scooter" to "🛵",
    "bicycle" to "🚲",
    "boat" to "🛥️",
    "electric" to "🔋",
    "other" to "🎡"
)

/** Drawable resource for each vehicle icon key — tintable with the vehicle's color via
 * [vehicleIconVector]. "rv" is a legacy key (pre-consolidation) mapped to the same
 * drawable as "other" so old data keeps rendering something sensible. */
private val VehicleIconResIds: Map<String, Int> = mapOf(
    "car" to R.drawable.ic_vehicle_car,
    "truck" to R.drawable.ic_vehicle_pickup,
    "van" to R.drawable.ic_vehicle_suv_van,
    "motorcycle" to R.drawable.ic_vehicle_motorcycle,
    "scooter" to R.drawable.ic_vehicle_scooter,
    "bicycle" to R.drawable.ic_vehicle_bicycle,
    "boat" to R.drawable.ic_vehicle_boat,
    "electric" to R.drawable.ic_vehicle_electric,
    "other" to R.drawable.ic_vehicle_other,
    "rv" to R.drawable.ic_vehicle_other
)

fun vehicleIconEmoji(iconKey: String): String =
    VehicleIconOptions.firstOrNull { it.first == iconKey }?.second ?: "🚗"

@Composable
fun vehicleIconVector(iconKey: String): ImageVector =
    ImageVector.vectorResource(id = VehicleIconResIds[iconKey] ?: R.drawable.ic_vehicle_other)

// Real-world car colors first (what most vehicles actually come in), then the original
// neon/accent set after — those still work fine for anyone who wants their vehicle chip to
// pop rather than match reality, they just no longer have to scroll past them to find "White".
val VehicleColorOptions = listOf(
    0xFFF5F5F0.toInt(), // White
    0xFF262626.toInt(), // Black
    0xFFC0C0C0.toInt(), // Silver
    0xFF808A8F.toInt(), // Gray
    0xFFD2B48C.toInt(), // Tan
    0xFFD4AF37.toInt(), // Gold
    0xFFB71C1C.toInt(), // Red
    0xFF7B1E1E.toInt(), // Maroon
    0xFF1976D2.toInt(), // Blue
    0xFF1A237E.toInt(), // Navy
    0xFF2E7D32.toInt(), // Green
    0xFF00F0FF.toInt(),
    0xFF6B2FFF.toInt(),
    0xFF00E676.toInt(),
    0xFFFF9100.toInt(),
    0xFFFF5252.toInt(),
    0xFF448AFF.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFE040FB.toInt()
)

fun vehicleDisplayColor(colorArgb: Int): Color = Color(colorArgb)

suspend fun migrateLegacyCarVehicleIfNeeded(
    context: Context,
    prefs: SharedPreferences,
    vehicleDao: VehicleDao
) = withContext(Dispatchers.IO) {
    if (vehicleDao.countAll() > 0) return@withContext
    val mac = loadAutoParkCarMac(prefs) ?: return@withContext
    val name = loadAutoParkCarName(prefs)?.takeIf { it.isNotBlank() } ?: "My Car"
    vehicleDao.insert(
        Vehicle(
            name = name,
            colorArgb = VehicleColorOptions.first(),
            iconKey = "car",
            isDefault = true,
            bluetoothMac = mac,
            bluetoothName = name,
            createdAt = System.currentTimeMillis()
        )
    )
    // The migrated Vehicle row is now the sole source of truth for this MAC — left populated,
    // these legacy prefs become a stale fallback AutoParkWorker's legacyVehicleFromPrefs() keeps
    // matching against forever, including after the user later unlinks this exact vehicle's
    // Bluetooth device (which only ever clears the Vehicle row's own bluetoothMac, never these).
    // That resurrects auto-park detections for a device the user explicitly told the app to stop
    // tracking. Clearing them the moment they've served their one-time migration purpose closes
    // that permanently instead of trying to catch every possible future "unlink" call site.
    prefs.edit().remove(AUTO_PARK_CAR_MAC_PREF).remove(AUTO_PARK_CAR_NAME_PREF).apply()
}

/** Resolves which vehicle a hands-off save (Quick Pin, Quick Track, Bluetooth auto-park, etc.)
 * should be tagged with. Always the user's globally-set Primary Default vehicle from Vehicles
 * settings — or no vehicle at all if none is set — never whichever vehicle happened to be
 * picked on a previous save. A "remembers last used" fallback here would let a save silently
 * inherit a vehicle nobody actually chose for it, which is exactly the sticky-state trap this
 * is meant to avoid. */
suspend fun resolveVehicleForQuickSave(
    vehicleDao: VehicleDao,
    explicitVehicleId: Int? = null
): Int? = withContext(Dispatchers.IO) {
    val active = vehicleDao.getActiveList()
    if (active.isEmpty()) return@withContext null
    explicitVehicleId?.let { id ->
        active.firstOrNull { it.id == id }?.let { return@withContext it.id }
    }
    vehicleDao.getDefault()?.id
}

fun applyPinnedVehiclePrefs(editor: SharedPreferences.Editor, vehicle: Vehicle?) {
    if (vehicle != null) {
        editor.putInt(PINNED_VEHICLE_ID_PREF, vehicle.id)
    } else {
        editor.remove(PINNED_VEHICLE_ID_PREF)
    }
}

suspend fun promoteDefaultVehicleIfNeeded(vehicleDao: VehicleDao) = withContext(Dispatchers.IO) {
    if (vehicleDao.getDefault() != null) return@withContext
    val next = vehicleDao.getMostRecentActive() ?: return@withContext
    vehicleDao.setDefault(next.id)
}

suspend fun deleteVehicleKeepingHistory(vehicleDao: VehicleDao, locationDao: LocationDao, vehicleId: Int) =
    withContext(Dispatchers.IO) {
        locationDao.clearVehicleId(vehicleId)
        val wasDefault = vehicleDao.getById(vehicleId)?.isDefault == true
        vehicleDao.deleteById(vehicleId)
        if (wasDefault) promoteDefaultVehicleIfNeeded(vehicleDao)
    }

suspend fun archiveVehicle(vehicleDao: VehicleDao, vehicleId: Int) = withContext(Dispatchers.IO) {
    val vehicle = vehicleDao.getById(vehicleId) ?: return@withContext
    val wasDefault = vehicle.isDefault
    vehicleDao.archive(vehicleId)
    if (wasDefault) promoteDefaultVehicleIfNeeded(vehicleDao)
}

fun autoParkOptionForVehicle(vehicle: Vehicle): QuickPinDef = QuickPinDef(
    id = "auto_park_${vehicle.id}",
    emoji = vehicleIconEmoji(vehicle.iconKey),
    label = "Auto-Parked",
    details = "Auto-saved via Bluetooth — ${vehicle.name}",
    section = "General",
    sortOrder = 0
)

suspend fun insertNewVehicle(vehicleDao: VehicleDao, vehicle: Vehicle): Int = withContext(Dispatchers.IO) {
    // Respects whatever the user actually chose on the Default toggle — forcing the first
    // vehicle to always be default (regardless of that toggle) meant unchecking it on your only
    // vehicle was silently ignored, so quick-saves kept auto-tagging it anyway.
    val rowId = vehicleDao.insert(vehicle).toInt()
    if (vehicle.isDefault) {
        vehicleDao.setDefault(rowId)
    }
    rowId
}

/** After vehicle create/edit/delete, drop orphaned per-MAC auto-park prefs so SpotVaultPrefs
 * doesn't accumulate forever across years of pairing changes. */
suspend fun pruneAutoParkMacPrefsAfterVehicleChange(
    context: Context,
    vehicleDao: VehicleDao
) = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
    val activeMacs = vehicleDao.getActiveList().mapNotNull { it.bluetoothMac }
    pruneStaleAutoParkMacPrefs(prefs, activeMacs)
}

suspend fun saveVehicle(vehicleDao: VehicleDao, vehicle: Vehicle): Int = withContext(Dispatchers.IO) {
    if (vehicle.id == 0) {
        insertNewVehicle(vehicleDao, vehicle)
    } else {
        // Same self-healing archiveVehicle()/deleteVehicleKeepingHistory() already do when a
        // default vehicle goes away — unchecking "Set as default" here is just as much a default
        // vehicle going away, but this update path had no equivalent, so it silently left every
        // future hands-off save (resolveVehicleForQuickSave) with no vehicle to tag at all.
        val wasDefault = vehicleDao.getById(vehicle.id)?.isDefault == true
        vehicleDao.update(vehicle)
        if (vehicle.isDefault) {
            vehicleDao.setDefault(vehicle.id)
        } else if (wasDefault) {
            promoteDefaultVehicleIfNeeded(vehicleDao)
        }
        vehicle.id
    }
}
