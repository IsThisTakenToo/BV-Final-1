package com.spotvault.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class IconGeneratorTest {

    @Test
    fun generateIcons() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fgDrawable = ContextCompat.getDrawable(context, R.drawable.ic_spotvault_mark)
        val bgDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)

        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        for ((name, size) in densities) {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Draw bg
            bgDrawable?.setBounds(0, 0, size, size)
            bgDrawable?.draw(canvas)

            // Draw fg with some padding to simulate adaptive icon
            val fgSize = (size * 0.7f).toInt()
            val padding = (size - fgSize) / 2
            fgDrawable?.setBounds(padding, padding, size - padding, size - padding)
            fgDrawable?.draw(canvas)

            val file = File("src/main/res/mipmap-$name/ic_launcher.png")
            file.parentFile.mkdirs()
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // For round, clip to circle
            val roundBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val roundCanvas = Canvas(roundBmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.BLACK
            roundCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            roundCanvas.drawBitmap(bmp, 0f, 0f, paint)
            
            val roundFile = File("src/main/res/mipmap-$name/ic_launcher_round.png")
            FileOutputStream(roundFile).use { out ->
                roundBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("Generated $name")
        }
    }
}
