package com.spotvault.app

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.atomic.AtomicInteger

/**
 * App-wide "acquiring GPS fix" signal. [resolveCurrentLocation] is the single chokepoint every
 * Snap/Pin, Quick Pin, Quick Track, Quick Share, and Share Photo flow funnels through to get a
 * location, so toggling this there — instead of at each of those five call sites — covers all of
 * them for free and can't drift out of sync as new callers are added.
 */
object LocationAcquisitionStatus {
    private val activeCountAtomic = AtomicInteger(0)
    private val activeCount = mutableStateOf(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val derivedIsActive = derivedStateOf { activeCount.value > 0 }
    val isActive: State<Boolean> get() = derivedIsActive

    fun begin() {
        publish(activeCountAtomic.incrementAndGet())
    }

    fun end() {
        publish(activeCountAtomic.updateAndGet { (it - 1).coerceAtLeast(0) })
    }

    /** Compose state must only mutate on the main thread — resolveCurrentLocation / AutoPark /
     * MotionBookmark call begin/end from Dispatchers.IO. */
    private fun publish(count: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            activeCount.value = count
        } else {
            mainHandler.post { activeCount.value = count }
        }
    }
}

/**
 * Large, hard-to-miss card centered in the screen while a GPS fix is being acquired/averaged for
 * accuracy — that averaging is deliberate (see [resolveCurrentLocation]) and can take a few
 * seconds, especially indoors or in a parking garage, so this exists purely to reassure the user
 * something is happening rather than to speed anything up. Shown only after a brief debounce so
 * the common instant/cached-fix case never flashes it.
 */
@Composable
fun LocationAcquisitionBanner(modifier: Modifier = Modifier) {
    val isActive by LocationAcquisitionStatus.isActive
    var showBanner by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            kotlinx.coroutines.delay(300)
            if (LocationAcquisitionStatus.isActive.value) showBanner = true
        } else {
            showBanner = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.88f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.88f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SpotVaultColors.Elevated, SpotVaultColors.Surface)
                        )
                    )
                    .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 32.dp, vertical = 26.dp)
            ) {
                CircularProgressIndicator(
                    color = SpotVaultColors.Teal,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Pinpointing your exact location…",
                    color = SpotVaultColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Getting the most accurate fix possible",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
