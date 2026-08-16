package com.spotvault.app

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val AUTO_PARK_ENABLED_PREF = "auto_park_enabled"
const val AUTO_PARK_CAR_MAC_PREF = "auto_park_car_mac"
const val AUTO_PARK_CAR_NAME_PREF = "auto_park_car_name"
const val AUTO_PARK_QUIET_ZONES_PREF = "auto_park_quiet_zones_json"
/** When true, the next pinned notification post uses a minimal/silent channel (see TimerService). */
const val PINNED_NOTIFICATION_SILENT_PREF = "pinned_notification_silent"

const val BT_DISCONNECT_CONFIRM_SECONDS_PREF = "bt_disconnect_confirm_seconds"
/** How long (seconds) a car's Bluetooth must hold a CONNECTED state before its disconnect is
 * trusted as real rather than handshake noise — see the full reasoning in
 * CarBluetoothReceiver's doc comment. 8s by default; user-adjustable in Automatic Parking
 * settings (Advanced) for power users whose head unit needs more — or less — margin. */
const val DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS = 8
const val MAX_BT_DISCONNECT_CONFIRM_SECONDS = 30

fun loadBtDisconnectConfirmSeconds(prefs: SharedPreferences): Int =
    prefs.getInt(BT_DISCONNECT_CONFIRM_SECONDS_PREF, DEFAULT_BT_DISCONNECT_CONFIRM_SECONDS)
        .coerceIn(0, MAX_BT_DISCONNECT_CONFIRM_SECONDS)

/** Whether an auto-park save starts a live, persistent Active Tracking notification (default —
 * matches every prior version's behavior) or saves to the Vault with no notification at all.
 * Independent of "default_active_tracking" (the Save/Snap screen's own tracking-mode default) —
 * a user can reasonably want manual saves to always track live while wanting the recurring,
 * unattended auto-park trigger specifically to stay quiet, or vice versa. */
const val AUTO_PARK_QUIET_SAVE_MODE_PREF = "auto_park_quiet_save_mode"

fun isAutoParkQuietSaveMode(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(AUTO_PARK_QUIET_SAVE_MODE_PREF, false)

enum class AutoParkQuietZoneMode(val prefValue: String) {
    SKIP("skip"),
    SILENT("silent");

    companion object {
        fun fromPref(value: String?): AutoParkQuietZoneMode =
            entries.firstOrNull { it.prefValue == value } ?: SKIP
    }
}

data class AutoParkQuietZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double = 150.0,
    val mode: AutoParkQuietZoneMode = AutoParkQuietZoneMode.SKIP
)

enum class AutoParkQuietZoneDecision {
    SAVE_NORMAL,
    SAVE_SILENT,
    SKIP
}

fun isAutoParkEnabled(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(AUTO_PARK_ENABLED_PREF, false)

fun SharedPreferences.observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
        if (updatedKey == key || updatedKey == null) {
            trySend(prefs.getBoolean(key, defaultValue))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    trySend(getBoolean(key, defaultValue))
    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}

fun SharedPreferences.observeAutoParkEnabled(): Flow<Boolean> =
    observeBoolean(AUTO_PARK_ENABLED_PREF, false)

fun SharedPreferences.observeMotionAutoParkEnabled(): Flow<Boolean> =
    observeBoolean(MOTION_AUTOPARK_ENABLED_PREF, false)

fun loadAutoParkCarMac(prefs: SharedPreferences): String? =
    prefs.getString(AUTO_PARK_CAR_MAC_PREF, null)?.takeIf { it.isNotBlank() }

fun loadAutoParkCarName(prefs: SharedPreferences): String? =
    prefs.getString(AUTO_PARK_CAR_NAME_PREF, null)?.takeIf { it.isNotBlank() }

fun loadAutoParkQuietZones(prefs: SharedPreferences): List<AutoParkQuietZone> {
    val raw = prefs.getString(AUTO_PARK_QUIET_ZONES_PREF, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AutoParkQuietZone(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.getString("name"),
                lat = o.getDouble("lat"),
                lng = o.getDouble("lng"),
                radiusMeters = o.optDouble("radiusMeters", 150.0),
                mode = AutoParkQuietZoneMode.fromPref(o.optString("mode"))
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveAutoParkQuietZones(prefs: SharedPreferences, zones: List<AutoParkQuietZone>) {
    val arr = JSONArray()
    zones.forEach { zone ->
        arr.put(
            JSONObject().apply {
                put("id", zone.id)
                put("name", zone.name)
                put("lat", zone.lat)
                put("lng", zone.lng)
                put("radiusMeters", zone.radiusMeters)
                put("mode", zone.mode.prefValue)
            }
        )
    }
    prefs.edit().putString(AUTO_PARK_QUIET_ZONES_PREF, arr.toString()).apply()
}

fun evaluateAutoParkQuietZones(
    lat: Double,
    lng: Double,
    zones: List<AutoParkQuietZone>
): AutoParkQuietZoneDecision {
    var silent = false
    zones.forEach { zone ->
        val dist = haversineDistanceMeters(lat, lng, zone.lat, zone.lng)
        if (dist <= zone.radiusMeters) {
            when (zone.mode) {
                AutoParkQuietZoneMode.SKIP -> return AutoParkQuietZoneDecision.SKIP
                AutoParkQuietZoneMode.SILENT -> silent = true
            }
        }
    }
    return if (silent) AutoParkQuietZoneDecision.SAVE_SILENT else AutoParkQuietZoneDecision.SAVE_NORMAL
}
