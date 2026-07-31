import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    if (showHistoryDialog.value) {
                        HistoryDialog(onDismiss = { showHistoryDialog.value = false }, dao = AppDatabase.getDatabase(this@MainActivity).locationDao(), prefs = prefs, isPinned = isPinned.value)
                    }"""

replacement = """                    if (showHistoryDialog.value) {
                        HistoryDialog(onDismiss = { showHistoryDialog.value = false }, dao = AppDatabase.getDatabase(this@MainActivity).locationDao(), prefs = prefs, isPinned = isPinned.value)
                    }

                    if (showSettingsDialog.value) {
                        SettingsDialog(
                            onDismiss = { showSettingsDialog.value = false },
                            prefs = prefs,
                            dao = AppDatabase.getDatabase(this@MainActivity).locationDao(),
                            onPickRingtone = {
                                val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Alert Sound")
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    val currentUriStr = prefs.getString("alarm_sound_uri", null)
                                    if (currentUriStr != null && currentUriStr.isNotEmpty()) {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(currentUriStr))
                                    } else {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            }
                        )
                    }"""

if target in content:
    content = content.replace(target, replacement)
    
    # Also update onAlertsClick -> onSettingsClick in MainActivity
    content = content.replace("onAlertsClick = {", "onSettingsClick = { showSettingsDialog.value = true }\n/*")
    content = content.replace("ringtonePickerLauncher.launch(intent)\n                                }", "*/")

    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched MainActivity dialogs")
else:
    print("MainActivity dialogs target not found")
