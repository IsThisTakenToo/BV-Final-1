package com.spotvault.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Detects the "never ask again" permission trap and offers a way out — without this, a user who
 * permanently denies Location or Camera (or has an OEM auto-deny it) gets stuck on a Toast that
 * tells them to grant a permission the system will never prompt for again, with tapping Pin/Snap
 * doing nothing every time after.
 *
 * [ActivityCompat.shouldShowRequestPermissionRationale] returns false both before a permission
 * has ever been requested AND after it's been permanently denied — those two states are
 * otherwise indistinguishable, so [markPermissionRequested] persists a "we've asked at least
 * once" flag per permission to tell them apart.
 */
private fun requestedPermissionPrefKey(permission: String) = "requested_perm_$permission"

/** Call this right before launching a permission request, for every permission in that request. */
fun markPermissionRequested(prefs: SharedPreferences, vararg permissions: String) {
    prefs.edit().apply {
        permissions.forEach { putBoolean(requestedPermissionPrefKey(it), true) }
    }.apply()
}

/** True once [permission] has been asked before (per [markPermissionRequested]), is still
 * denied, and the system will no longer show its own rationale prompt for it — i.e. the user
 * chose "Don't ask again," or the OEM silently auto-denied it. The only way back in from here is
 * the app's own OS Settings page. */
fun isPermissionPermanentlyDenied(activity: Activity, prefs: SharedPreferences, permission: String): Boolean {
    val alreadyRequested = prefs.getBoolean(requestedPermissionPrefKey(permission), false)
    val granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    return alreadyRequested && !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/** Native (non-Compose) dialog — used from ActivityResult permission callbacks, which aren't
 * inside a Composable, so there's no Compose dialog state to hang this off of. */
fun showPermissionSettingsDialog(activity: Activity, message: String) {
    android.app.AlertDialog.Builder(activity)
        .setTitle("Permission Needed")
        .setMessage(message)
        .setPositiveButton("Open Settings") { _, _ -> openAppSettings(activity) }
        .setNegativeButton("Cancel", null)
        .show()
}
