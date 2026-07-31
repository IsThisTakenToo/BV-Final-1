import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    private fun launchCamera() {
        tempOcrTextForDialog = ""
        tempProminentOcrTextForDialog = ""
        lifecycleScope.launch {
            val launchCount = prefs.getInt("camera_launch_count", 0)
            if (launchCount < 2) {
                cameraCountdown.value = 3
                while (cameraCountdown.value > 0) {
                    kotlinx.coroutines.delay(1000)
                    cameraCountdown.value -= 1
                }
                prefs.edit().putInt("camera_launch_count", launchCount + 1).apply()
            }
            val imagesDir = File(cacheDir, "images").apply { mkdirs() }"""

replacement = """    private fun launchCamera() {
        tempOcrTextForDialog = ""
        tempProminentOcrTextForDialog = ""
        lifecycleScope.launch {
            val tipShownCount = prefs.getInt("camera_tip_shown_count", 0)
            if (tipShownCount < 3) {
                cameraCountdown.value = 3
                while (cameraCountdown.value > 0) {
                    kotlinx.coroutines.delay(1000)
                    cameraCountdown.value -= 1
                }
                prefs.edit().putInt("camera_tip_shown_count", tipShownCount + 1).apply()
            } else {
                android.widget.Toast.makeText(this@MainActivity, "Tip: Get close and fill the frame for best results", android.widget.Toast.LENGTH_SHORT).show()
            }
            val imagesDir = File(cacheDir, "images").apply { mkdirs() }"""

if target in content:
    content = content.replace(target, replacement)
    print("Found and replaced with target1")
else:
    # maybe it's the old version?
    target2 = """    private fun launchCamera() {
        tempOcrTextForDialog = ""
        tempProminentOcrTextForDialog = ""
        lifecycleScope.launch {
            cameraCountdown.value = 3
            while (cameraCountdown.value > 0) {
                kotlinx.coroutines.delay(1000)
                cameraCountdown.value -= 1
            }
            val imagesDir = File(cacheDir, "images").apply { mkdirs() }"""
    if target2 in content:
        content = content.replace(target2, replacement)
        print("Found and replaced with target2")
    else:
        print("COULD NOT FIND EITHER TARGET")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

