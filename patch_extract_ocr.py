import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    private fun extractOcrAndShowDialog() {
        val currentPhotoPath = photoFile?.absolutePath ?: ""
        if (currentPhotoPath.isEmpty()) {
            tempOcrTextForDialog = ""
            tempProminentOcrTextForDialog = ""
            showTimerDialog.value = true
            return
        }
        
        isProcessingPhoto.value = true
        lifecycleScope.launch {
            var extractedText = ""
            val rawBitmap = getUprightBitmap(currentPhotoPath)
            if (rawBitmap != null) {
                try {
                    val bitmap = enhanceBitmapForOCR(this@MainActivity, rawBitmap)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    if (result.text.isNotEmpty()) {
                        extractedText = result.text.replace(Regex("[^A-Za-z0-9\\\\-\\\\s]"), " ").replace(Regex("\\\\s+"), " ").trim()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            tempOcrTextForDialog = extractedText
            isProcessingPhoto.value = false
            showTimerDialog.value = true
        }
    }"""

replacement = """    private fun extractOcrAndShowDialog() {
        val currentPhotoPath = photoFile?.absolutePath ?: ""
        if (currentPhotoPath.isEmpty()) {
            tempOcrTextForDialog = ""
            tempProminentOcrTextForDialog = ""
            showTimerDialog.value = true
            return
        }
        
        isProcessingPhoto.value = true
        lifecycleScope.launch {
            var extractedText = ""
            var prominentText = ""
            val rawBitmap = getUprightBitmap(currentPhotoPath)
            if (rawBitmap != null) {
                try {
                    val bitmap = enhanceBitmapForOCR(this@MainActivity, rawBitmap)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    
                    var maxBoxHeight = 0
                    val fullTextBuilder = java.lang.StringBuilder()
                    
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            val bbox = line.boundingBox
                            if (bbox != null) {
                                val height = bbox.bottom - bbox.top
                                if (height > maxBoxHeight) {
                                    maxBoxHeight = height
                                    prominentText = line.text
                                }
                            }
                            fullTextBuilder.append(line.text).append(" ")
                        }
                    }
                    
                    if (fullTextBuilder.isNotEmpty()) {
                        extractedText = fullTextBuilder.toString().replace(Regex("[^A-Za-z0-9\\\\-\\\\s]"), " ").replace(Regex("\\\\s+"), " ").trim()
                        prominentText = prominentText.replace(Regex("[^A-Za-z0-9\\\\-\\\\s]"), " ").replace(Regex("\\\\s+"), " ").trim()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            tempOcrTextForDialog = extractedText
            tempProminentOcrTextForDialog = prominentText
            isProcessingPhoto.value = false
            showTimerDialog.value = true
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Replaced extractOcrAndShowDialog")
else:
    print("Target not found!")
