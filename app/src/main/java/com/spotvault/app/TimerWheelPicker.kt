package com.spotvault.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * A compact "Add Timer" card built entirely from scrollable number wheels — no keyboard/keypad
 * ever appears. Hours (0-12) and 5-minute increments (0-55) scroll and snap like an iOS-style
 * picker; the currently centered value in each column is the selected one.
 */
@Composable
fun TimerWheelPicker(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SpotVaultColors.Elevated.copy(alpha = 0.9f))
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set a Timer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SpotVaultColors.OnSurface)
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SpotVaultColors.Deep)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear timer", tint = SpotVaultColors.Muted, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberWheelColumn(
                values = (0..12).toList(),
                selected = hours,
                onSelectedChange = onHoursChange,
                labelSuffix = "h",
                modifier = Modifier.width(76.dp)
            )
            Text(
                text = ":",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = SpotVaultColors.Muted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            NumberWheelColumn(
                values = (0..55 step 5).toList(),
                selected = minutes,
                onSelectedChange = onMinutesChange,
                labelSuffix = "m",
                modifier = Modifier.width(76.dp)
            )
        }

        val totalMins = hours * 60 + minutes
        Text(
            text = if (totalMins > 0) {
                if (hours > 0) "Alerts in ${hours}h ${minutes}m" else "Alerts in ${minutes}m"
            } else {
                "Scroll to set a reminder"
            },
            fontSize = 12.sp,
            color = SpotVaultColors.Muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun NumberWheelColumn(
    values: List<Int>,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    labelSuffix: String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 38.dp,
    visibleCount: Int = 3
) {
    val initialIndex = values.indexOf(selected).let { if (it < 0) 0 else it }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    // Native snap physics instead of a manual "settle after scroll stops, then animate to the
    // nearest item" effect — a real fling flies straight past the item it lands under otherwise,
    // since nothing arrests it until isScrollInProgress goes false well past where it "should"
    // have stopped.
    val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
    val coroutineScope = rememberCoroutineScope()
    var hasInitialized by remember { mutableStateOf(false) }

    // Re-syncs the wheel to `selected` whenever it changes from outside this composable — e.g. an
    // OCR-suggestion tap that sets the value directly rather than through a scroll here. Skipped
    // on first composition since rememberLazyListState above already starts at the right spot;
    // without that guard this would immediately re-trigger itself on mount.
    LaunchedEffect(selected) {
        if (!hasInitialized) {
            hasInitialized = true
        } else {
            val targetIndex = values.indexOf(selected)
            if (targetIndex >= 0 && !listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    // firstVisibleItemIndex is whichever item has any pixel visible at the very
                    // top of the viewport — with 3 items visible at once (top/center/bottom band)
                    // that's the item *above* the centered, highlighted one, not the centered one
                    // itself, except right at index 0. That made the reported selection lag one
                    // item behind the actually-centered value in whichever direction reveals a
                    // "new" top item first. Finding the visible item whose own center is nearest
                    // the viewport's center sidesteps the content-padding math entirely and always
                    // matches what's actually highlighted on screen.
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
                    }
                    val settled = centeredItem?.index?.coerceIn(0, values.lastIndex)
                    val value = settled?.let { values.getOrNull(it) }
                    if (value != null && value != selected) {
                        onSelectedChange(value)
                    }
                }
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(SpotVaultColors.Teal.copy(alpha = 0.12f))
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(values) { index, value ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch { listState.animateScrollToItem(index) }
                            onSelectedChange(value)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${value.toString().padStart(2, '0')}$labelSuffix",
                        fontSize = if (isSelected) 19.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Muted.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
