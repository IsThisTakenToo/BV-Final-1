package com.spotvault.app

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.Process

private const val CONNECTED_AT_PREF_PREFIX = "bt_connected_at_"
internal const val AUTO_PARK_PENDING_SPOT_PREF_PREFIX = "auto_park_pending_spot_"

/** True if [mac] is currently marked connected in [CarBluetoothReceiver]'s own per-device
 * tracking — i.e. it reconnected some time after whatever disconnect triggered the caller's
 * in-flight save, meaning that save no longer reflects reality. [AutoParkWorker] checks this
 * right before actually writing, since [cancelAutoParkWork]'s WorkManager cancellation on
 * reconnect isn't guaranteed to stop an already-running, non-cooperative suspend chain in time —
 * this is the backstop for whenever it doesn't. */
internal fun isMacCurrentlyConnected(prefs: SharedPreferences, mac: String): Boolean =
    prefs.contains(CONNECTED_AT_PREF_PREFIX + mac.uppercase())

/**
 * Drops per-MAC auto-park prefs whose MAC no longer belongs to any active vehicle — pairing
 * changes over years otherwise leave `bt_connected_at_*` / `auto_park_pending_spot_*` keys
 * accumulating forever in SpotVaultPrefs.
 */
fun pruneStaleAutoParkMacPrefs(prefs: SharedPreferences, activeMacs: Collection<String>) {
    val keep = activeMacs.mapNotNull { it.takeIf { m -> m.isNotBlank() }?.uppercase() }.toSet()
    val editor = prefs.edit()
    var changed = false
    prefs.all.keys.forEach { key ->
        val mac = when {
            key.startsWith(CONNECTED_AT_PREF_PREFIX) ->
                key.removePrefix(CONNECTED_AT_PREF_PREFIX)
            key.startsWith(AUTO_PARK_PENDING_SPOT_PREF_PREFIX) ->
                key.removePrefix(AUTO_PARK_PENDING_SPOT_PREF_PREFIX)
            else -> return@forEach
        }
        if (mac !in keep) {
            editor.remove(key)
            changed = true
        }
    }
    if (changed) editor.apply()
}

// The minimum-hold-duration threshold itself now lives in AutoParkingStore
// (loadBtDisconnectConfirmSeconds) — user-adjustable in Automatic Parking settings, 8s by
// default. Some head units briefly drop and re-establish the ACL link while negotiating HFP/A2DP
// profiles right after pairing or reconnecting — that blip's ACTION_ACL_DISCONNECTED is
// indistinguishable from a real one by action alone, and was firing a save the moment the car
// had barely finished connecting.

/**
 * Manifest-declared receiver for car Bluetooth disconnect.
 *
 * Deliberately does nothing but read a couple of SharedPreferences values and enqueue
 * [AutoParkWorker] — no database access here. A BroadcastReceiver.onReceive() runs on the main
 * thread under a strict system time budget (historically ~10s before Android can kill the
 * process); a prior version used `runBlocking` to look up the vehicle by MAC via Room before
 * enqueueing, which risked the receiver being killed — silently dropping the auto-park trigger
 * — before the WorkManager job was ever scheduled. The vehicle lookup now happens inside
 * [AutoParkWorker.doWork], which runs with WorkManager's own execution budget instead.
 *
 * Explicitly tracks the CONNECTED state per-MAC (via [CONNECTED_AT_PREF_PREFIX]) rather than
 * reacting to whichever action shows up — a save only ever fires from ACTION_ACL_DISCONNECTED,
 * and only once this receiver can show the same device actually held a CONNECTED state first,
 * long enough for the disconnect to plausibly be real rather than handshake noise.
 */
class CarBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != BluetoothDevice.ACTION_ACL_DISCONNECTED && action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        // Exported so the system Bluetooth stack can deliver ACL events while the process is
        // dead — but any app can also forge an explicit Intent with EXTRA_DEVICE. Reject
        // non-system / non-Bluetooth senders so a third party can't enqueue Auto-Park saves.
        if (!isTrustedBluetoothBroadcastSender(context)) return

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs)) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val mac = device.address?.uppercase() ?: return
        val connectedAtKey = CONNECTED_AT_PREF_PREFIX + mac

        if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            // Explicitly holds "this device is CONNECTED as of now" — read back below when the
            // matching disconnect arrives, so a same-device reconnect right after pairing doesn't
            // masquerade as a real departure.
            prefs.edit().putLong(connectedAtKey, System.currentTimeMillis()).apply()
            // Covers a quick reconnect (user got back in the car moments after disconnecting):
            // without this, a save already enqueued/running from that disconnect would still go
            // through even though the phone is back with the car.
            cancelAutoParkWork(appContext, mac)
            // Covers the slower "gas station" reconnect — cancelAutoParkWork only stops a save
            // that's still pending/running; this cleans up one that already finished and landed
            // in the Vault while the engine was briefly off. See its own doc for the full reasoning.
            enqueueAutoParkReconnectCleanup(appContext, mac)
            if (isMotionAutoParkEnabled(prefs)) {
                enqueueMotionWatchStart(appContext, mac)
            }
            return
        }

        // Disconnect. Toggling the phone's own Bluetooth adapter off (Quick Settings tile,
        // airplane mode) fires this exact same ACTION_ACL_DISCONNECTED for every currently
        // connected device — indistinguishable by action alone from the car genuinely driving out
        // of range. A real departure always happens with the adapter still on; only the adapter
        // itself powering down looks like this. Checked before touching connectedAtKey at all
        // (rather than just returning after clearing it) so a false trigger doesn't disturb the
        // bookkeeping a genuine disconnect around the same time would still need.
        val adapterState = android.bluetooth.BluetoothAdapter.getDefaultAdapter()?.state
        if (adapterState == android.bluetooth.BluetoothAdapter.STATE_OFF ||
            adapterState == android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF
        ) {
            // Still drop this device's connect bookkeeping — leaving bt_connected_at_* behind on
            // every adapter power-off accumulated forever across years of pairing with any
            // peripheral while Auto Park was on. Do not enqueue a park save (that would false-fire).
            prefs.edit().remove(connectedAtKey).apply()
            return
        }

        // If this receiver saw the matching connect and it's held for less than
        // MIN_CONNECTED_DURATION_MILLIS, treat it as a handshake blip, not a real departure — no
        // save. A missing record (no connect ever observed here — e.g. Automatic Parking was
        // turned on while already sitting in a connected car, or the app process was killed and
        // restarted mid-drive) intentionally still falls through to a real save below: there's no
        // evidence *against* it being genuine, and the pre-existing behavior for that case was
        // already correct.
        val connectedAt = prefs.getLong(connectedAtKey, -1L)
        prefs.edit().remove(connectedAtKey).apply()
        val minConnectedDurationMillis = loadBtDisconnectConfirmSeconds(prefs) * 1000L
        if (connectedAt >= 0L && System.currentTimeMillis() - connectedAt < minConnectedDurationMillis) {
            return
        }

        // This receiver has no foreground presence of its own — on API 29+, enqueueing the save
        // anyway without ACCESS_BACKGROUND_LOCATION would just have AutoParkWorker's location
        // fetch silently denied by the OS every time, retrying indefinitely for a permission that
        // was never going to appear on its own. The Settings screen already reflects this via
        // "Armed" requiring backgroundLocationGranted — this makes the actual save pipeline agree
        // with that instead of still firing (and failing) underneath it regardless.
        if (!hasBackgroundLocationPermission(appContext)) return

        // Read the bookmark before stopMotionWatch, which also clears it — order matters here.
        // The watch itself only ever tracks one MAC globally (MOTION_WATCH_MAC_KEY in
        // MotionAutoParkManager) — with two Bluetooth-linked vehicles, connecting to car B while
        // still connected to car A re-arms the watch for B, so car A disconnecting afterward must
        // NOT tear that down: it belongs to B's still-in-progress trip now, not A's. Only stop it
        // when it's actually still armed for the MAC that just disconnected.
        val cachedLocation = if (isMotionAutoParkEnabled(prefs)) {
            consumeValidMotionBookmark(prefs, mac).also {
                if (currentMotionWatchMac(prefs)?.equals(mac, ignoreCase = true) == true) {
                    stopMotionWatch(appContext, prefs)
                }
            }
        } else {
            null
        }
        enqueueAutoParkWork(appContext, mac, cachedLocation)
    }
}

/** True when the delivering UID is the system, this app, or a package that owns Bluetooth. */
private fun isTrustedBluetoothBroadcastSender(context: Context): Boolean {
    val uid = Binder.getCallingUid()
    if (uid == Process.SYSTEM_UID || uid == Process.myUid()) return true
    val packages = context.packageManager.getPackagesForUid(uid) ?: return false
    return packages.any { pkg ->
        pkg == "com.android.bluetooth" ||
            pkg.startsWith("com.android.bluetooth.") ||
            pkg == "com.google.android.bluetooth" ||
            pkg.endsWith(".bluetooth")
    }
}

/** Arms the motion watch immediately if [mac] is already connected right now — used when the
 * user turns Motion & Fitness (or Automatic Parking) on from Settings while already sitting in
 * the car. Without this, [CarBluetoothReceiver] only arms the watch on the *next*
 * ACTION_ACL_CONNECTED broadcast, which won't fire again until the car disconnects and
 * reconnects, leaving the feature silently un-armed for the whole current trip despite Settings
 * showing it as "Armed". BluetoothDevice has no public isConnected(); the hidden method is the
 * only way to check without standing up a profile proxy connection. */
@android.annotation.SuppressLint("MissingPermission")
fun armMotionWatchIfAlreadyConnected(context: Context, mac: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return
    val device = adapter.bondedDevices?.firstOrNull { it.address?.uppercase() == mac.uppercase() } ?: return
    val isConnected = try {
        val method = device.javaClass.getMethod("isConnected")
        method.invoke(device) as? Boolean ?: false
    } catch (e: Exception) {
        false
    }
    if (isConnected) {
        enqueueMotionWatchStart(context.applicationContext, mac)
    }
}
