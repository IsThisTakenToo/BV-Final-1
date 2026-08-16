package com.spotvault.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Layers the user's chosen launcher icon (background + foreground) exactly like the home screen. */
@Composable
fun AppIconMark(
    appIcon: AppIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(appIcon.backgroundRes()),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(appIcon.foregroundRes()),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Live preview strip — preset icons, optional saved custom tile, and a New button. */
@Composable
fun AppIconPicker(
    selectedIconId: String,
    onIconSelected: (AppIcon) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {},
    premiumUnlocked: Boolean = true
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppIcon.entries.forEach { icon ->
                AppIconPickerCell(
                    icon = icon,
                    selected = icon.id == selectedIconId,
                    locked = isLocked(icon.id),
                    onClick = { if (isLocked(icon.id)) onLockedClick() else onIconSelected(icon) }
                )
            }
        }
        TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun AppIconPickerCell(
    icon: AppIcon,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .alpha(if (locked) 0.45f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
        ) {
            AppIconMark(appIcon = icon, modifier = Modifier.fillMaxSize())
            if (locked) {
                PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
                maxLines = 1
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = SpotVaultColors.Teal,
                    modifier = Modifier.padding(start = 4.dp).size(14.dp)
                )
            }
        }
    }
}
