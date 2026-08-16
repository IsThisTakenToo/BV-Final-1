package com.spotvault.app

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** MIME types accepted when importing GPX via Storage Access Framework. */
val GPX_IMPORT_MIME_TYPES = arrayOf(
    "application/gpx+xml",
    "application/xml",
    "text/xml",
    "*/*"
)

/**
 * Launch a SAF "Open document" flow to import GPX waypoints into [LocationDao].
 * Imported spots have no photo — coordinates and metadata only.
 *
 * [onComplete] still fires on a cancelled picker (with `null`) rather than being skipped
 * entirely — callers use it to release [AppLockGate], and skipping it on cancel would leave
 * that gate stuck open for the rest of the session. `null` just tells the caller "nothing to
 * report," as opposed to a real [Result.failure] worth surfacing to the user.
 */
@Composable
fun rememberGpxImportLauncher(
    dao: LocationDao,
    onComplete: (Result<Int>?) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onComplete(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Some providers do not grant persistable permission.
                    }
                    GpxParser.importSpotsFromUri(context, dao, uri)
                }
            }
            onComplete(result)
        }
    }
}

/** Convenience helper to start GPX import with standard MIME filters. */
fun ManagedActivityResultLauncher<Array<String>, Uri?>.launchGpxImport() {
    launch(GPX_IMPORT_MIME_TYPES)
}
