package com.spotvault.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

// -----------------------------------------------------------------------------------------------
// Crash handler — installed once from BeaconVaultApplication.onCreate(). Replaces the OS's default
// "unfortunately, X has stopped" dialog with CrashRecoveryActivity below, so a fatal bug reads as
// a brief, on-brand hiccup instead of the app looking broken. See BeaconVaultApplication's own
// comment for why this has to chain to whatever handler was already installed rather than simply
// overwriting it.
// -----------------------------------------------------------------------------------------------

private const val CRASH_TIMESTAMPS_PREF = "crash_recent_timestamps"
/** More than this many crashes inside [CRASH_LOOP_WINDOW_MILLIS] means restarting into MainActivity
 * is just going to crash again immediately — CrashRecoveryActivity shows a different, more honest
 * message and a way out (System App Info, for Clear Storage/Uninstall) instead of the same cheerful
 * "tap Restart" loop the user has already tried and watched fail. */
private const val CRASH_LOOP_THRESHOLD = 3
private const val CRASH_LOOP_WINDOW_MILLIS = 60_000L
/** Bounded, not unbounded — this only ever needs to answer "how many crashes very recently," not
 * hold a growing history (Crashlytics, once correctly chained, is the actual crash log). */
private const val MAX_TRACKED_CRASH_TIMESTAMPS = 5

/** Installs a custom [Thread.UncaughtExceptionHandler] that launches [CrashRecoveryActivity]
 * before the process dies, instead of leaving a fatal, uncaught exception to the OS's own default
 * handling (the "keeps stopping" dialog).
 *
 * Deliberately chains to whatever handler was already registered (see [previousHandler] below) —
 * this app already ships Firebase Crashlytics, which installs its own handler during its early,
 * automatic init (before this ever runs). Simply overwriting the default handler here, the way a
 * standalone crash-screen implementation normally would, would silently stop every future crash
 * from ever reaching Crashlytics again — a regression nobody would notice until the dashboard goes
 * quiet. Calling the previous handler lets Crashlytics still record the report; this app's own
 * work (the timestamp write + launching the recovery Activity) always happens first regardless of
 * what that chained call does afterward, and the process is unconditionally killed at the end
 * either way — a corrupted process must never be left alive on the off chance nothing upstream
 * already killed it. */
fun installCrashHandler(context: Context) {
    val appContext = context.applicationContext
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        // Every step below is independently try/caught — a bug in this handler itself must never
        // suppress the crash entirely or prevent the guaranteed kill at the bottom from running.
        runCatching { recordCrashTimestamp(appContext) }
        runCatching { launchRecoveryActivity(appContext) }
        runCatching { previousHandler?.uncaughtException(thread, throwable) }
        Process.killProcess(Process.myPid())
        kotlin.system.exitProcess(10)
    }
}

/** Records that a crash just happened, for [isCrashLooping]'s benefit — timestamps only, not the
 * stack trace itself (Crashlytics already owns that, once correctly chained above). Uses
 * `commit()`, not `apply()`, deliberately: the process is about to be killed, possibly within
 * milliseconds, and `apply()`'s deferred disk write could simply never happen. */
private fun recordCrashTimestamp(context: Context) {
    val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val existing = runCatching {
        val raw = prefs.getString(CRASH_TIMESTAMPS_PREF, null) ?: return@runCatching emptyList<Long>()
        val array = JSONArray(raw)
        (0 until array.length()).map { array.getLong(it) }
    }.getOrDefault(emptyList())
    val updated = (existing + now)
        .filter { now - it < CRASH_LOOP_WINDOW_MILLIS }
        .takeLast(MAX_TRACKED_CRASH_TIMESTAMPS)
    val json = JSONArray().apply { updated.forEach { put(it) } }
    prefs.edit().putString(CRASH_TIMESTAMPS_PREF, json.toString()).commit()
}

/** True if [CRASH_LOOP_THRESHOLD]+ crashes have landed within [CRASH_LOOP_WINDOW_MILLIS] —
 * read fresh (not cached) since this only ever runs once, right as CrashRecoveryActivity opens. */
private fun isCrashLooping(context: Context): Boolean {
    val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
    return runCatching {
        val raw = prefs.getString(CRASH_TIMESTAMPS_PREF, null) ?: return@runCatching false
        val array = JSONArray(raw)
        val now = System.currentTimeMillis()
        var recentCount = 0
        for (i in 0 until array.length()) {
            if (now - array.getLong(i) < CRASH_LOOP_WINDOW_MILLIS) recentCount++
        }
        recentCount >= CRASH_LOOP_THRESHOLD
    }.getOrDefault(false)
}

/** Clears the crash-loop counter — called once CrashRecoveryActivity's own Restart button has
 * been used, so a single isolated crash weeks apart from the last one never counts toward a new
 * loop just because [CRASH_TIMESTAMPS_PREF] still had old entries sitting in it. */
private fun clearCrashTimestamps(context: Context) {
    context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        .edit().remove(CRASH_TIMESTAMPS_PREF).apply()
}

private fun launchRecoveryActivity(context: Context) {
    val intent = Intent(context, CrashRecoveryActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
}

// -----------------------------------------------------------------------------------------------
// Recovery UI — runs in its own process (see the `android:process=":crash"` manifest entry) so it
// doesn't depend on whatever state in the main process just crashed. Deliberately touches nothing
// beyond a single boolean read from prefs: no Room/AppDatabase, no ViewModel, no business logic —
// a crash handler that can itself crash is the worst possible outcome, since that just forfeits to
// the exact OS dialog this whole feature exists to avoid.
// -----------------------------------------------------------------------------------------------

class CrashRecoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val looping = isCrashLooping(this)
        setContent {
            CrashRecoveryScreen(
                looping = looping,
                onRestart = {
                    clearCrashTimestamps(this)
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                    finish()
                },
                onOpenAppInfo = { openAppSettings(this) }
            )
        }
    }
}

@Composable
private fun CrashRecoveryScreen(
    looping: Boolean,
    onRestart: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotVaultColors.Void),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Same modifier order as AppLockScreen's own width cap (padding, then widthIn, then
            // fillMaxWidth last) — not the app's usual AdaptiveTabletContainer/WindowSizeClass
            // machinery, since this screen deliberately touches nothing beyond a single boolean
            // prefs read (see the file-level comment above CrashRecoveryActivity) so a bug in the
            // crash handler itself can never cause a second crash. Matching AppLockScreen's exact
            // order rather than a different one that happens to look equivalent — fillMaxWidth()
            // has to be the innermost modifier for the *outer* reported size to actually shrink to
            // the capped width, which is what lets this Box's contentAlignment = Center do
            // anything; put outermost, it'd report full screen width and the narrower content
            // would render pinned to the left edge instead of centered on a wide screen.
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = if (looping) SpotVaultColors.Danger else SpotVaultColors.Teal,
                modifier = Modifier.height(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (looping) "Still having trouble" else "We hit a pothole",
                color = SpotVaultColors.OnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (looping) {
                    "The vault ran into the same problem again right away. Restarting probably won't help this time — clearing the app's storage from Settings usually will."
                } else {
                    "The vault needs to recalibrate. Your saved spots are safe — this won't take a second."
                },
                color = SpotVaultColors.Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            if (looping) {
                SpotVaultButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Open App Settings", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onRestart) {
                    Text("Try Restarting Anyway", color = SpotVaultColors.Muted)
                }
            } else {
                SpotVaultButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Restart Vault", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
