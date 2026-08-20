package com.spotvault.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Soft cap so a huge paste can't balloon Room + undo history for the life of the install. */
const val NOTEPAD_MAX_CHARS = 50_000
private const val NOTEPAD_UNDO_MAX_ENTRIES = 40
private const val NOTEPAD_UNDO_MAX_CHARS = 200_000

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VaultCompactNotesFieldWithExpand(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    contextCategory: String = "",
    contextAddress: String = "",
    minLines: Int = 1,
    maxLines: Int = 4,
    singleLine: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    voiceInputEnabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SpotVaultColors.Muted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // Built from BasicTextField + OutlinedTextFieldDefaults.DecorationBox directly instead of
        // the high-level OutlinedTextField composable — same outlined look/colors, but the
        // high-level composable forces a `.defaultMinSize(minHeight = 56.dp)` internally that no
        // modifier or contentPadding override can shrink (same intrinsic-minimum problem
        // VaultSearchSortRow hit and worked around for the Vault search bar). Going through
        // DecorationBox directly means the real height comes from contentPadding + the actual text
        // line height instead, which is what actually lets this come down to something closer to
        // the search bar's own compact size instead of towering over it.
        val interactionSource = remember { MutableInteractionSource() }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = SpotVaultColors.OnSurface,
            focusedTextColor = SpotVaultColors.OnSurface,
            unfocusedBorderColor = SpotVaultColors.Outline,
            unfocusedLeadingIconColor = SpotVaultColors.Teal,
            focusedBorderColor = SpotVaultColors.Teal,
            focusedLeadingIconColor = SpotVaultColors.Teal,
            cursorColor = SpotVaultColors.Teal
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                color = SpotVaultColors.OnSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(SpotVaultColors.Teal),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = interactionSource
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value.text,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    // maxLines/overflow here, not just minLines on the field itself — an unclipped
                    // placeholder wraps across the field's width on its own and pushes the field
                    // taller to fit, even with minLines = 1, whenever the hint text is longer than
                    // one line's worth of space (a narrow phone, a larger system font size, a long
                    // hint string). The field should only grow because of what the user actually
                    // typed, never because of its own empty-state hint.
                    Text(
                        placeholder,
                        color = SpotVaultColors.Muted.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = leadingIcon,
                trailingIcon = if (voiceInputEnabled) {
                    {
                        VoiceMicButton(
                            onResult = { spoken ->
                                val current = value.text
                                val combined = if (current.isEmpty() || current.endsWith("\n") || current.endsWith(" ")) {
                                    current + spoken
                                } else {
                                    "$current $spoken"
                                }
                                onValueChange(TextFieldValue(combined, TextRange(combined.length)))
                            },
                            prompt = "Dictate $label…"
                        )
                    }
                } else null,
                colors = fieldColors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun NotepadEditorDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    fieldLabel: String = "Notes",
    contextCategory: String = "",
    contextAddress: String = "",
    conditionsSnapshot: String? = null
) {
    var draft by remember(initialNotes) {
        mutableStateOf(TextFieldValue(initialNotes, TextRange(initialNotes.length)))
    }
    var savedPulse by remember { mutableStateOf(false) }
    var lastEditedAt by remember { mutableStateOf<Long?>(null) }
    val isDirty = draft.text != initialNotes
    val saveEnabled = isDirty && !savedPulse

    // The debounced auto-save below only fires 1200ms after the last keystroke — closing sooner
    // than that (tap X, or hit back, right after finishing a quick edit) used to cancel that
    // pending save outright when this composable left composition, silently discarding whatever
    // was typed. Flushing any still-dirty text here first means closing can never race the debounce.
    val closeAndFlush = {
        if (isDirty) onSave(draft.text)
        onDismiss()
    }
    BackHandler(onBack = closeAndFlush)

    val checkAlpha by animateFloatAsState(
        targetValue = if (savedPulse) 1f else 0f,
        animationSpec = tween(250),
        label = "notepadSavedCheck"
    )

    LaunchedEffect(savedPulse) {
        if (savedPulse) {
            delay(1800)
            savedPulse = false
            // closeAndFlush(), not onDismiss() directly — typing more within this 1.8s window
            // (a completely normal continuation right after tapping Save) restarts the separate
            // 1200ms autosave debounce below without resetting this timer, so it used to be
            // possible for this to fire and close the dialog via bare onDismiss() — which never
            // saves — while a debounce for those extra keystrokes was still pending. Whatever was
            // typed in that last stretch was silently discarded with no warning, and the Save
            // button was already disabled the whole time so there was no way to save it manually
            // either. closeAndFlush() flushes any still-dirty text first, same as every other way
            // of closing this dialog (back button, X button).
            closeAndFlush()
        }
    }

    LaunchedEffect(draft.text) {
        if (draft.text != initialNotes) {
            delay(1200)
            onSave(draft.text)
            // Previously only set by the manual Save button's own onClick — a note saved purely
            // via this autosave path (typing, then closing via back/X without ever tapping Save)
            // showed no "Last edited" timestamp at all, even though it was in fact saved.
            lastEditedAt = System.currentTimeMillis()
        }
    }

    val lastEditedLabel = remember(lastEditedAt) {
        lastEditedAt?.let { millis ->
            "Last edited ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))}"
        }
    }

    val history = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }
    var fontScaleIndex by remember { mutableStateOf(1) } // 0=Small, 1=Medium, 2=Large
    val fontSizeSp = when (fontScaleIndex) { 0 -> 15.sp; 2 -> 20.sp; else -> 17.sp }
    val lineHeightSp = when (fontScaleIndex) { 0 -> 24.sp; 2 -> 32.sp; else -> 28.sp }

    fun clampNote(text: String): String =
        if (text.length <= NOTEPAD_MAX_CHARS) text else text.take(NOTEPAD_MAX_CHARS)

    fun pushHistory() {
        // Store text only — keeping 40 full TextFieldValue snapshots of a long note was a soft OOM path.
        history.add(TextFieldValue(draft.text))
        while (
            history.size > NOTEPAD_UNDO_MAX_ENTRIES ||
            history.sumOf { it.text.length } > NOTEPAD_UNDO_MAX_CHARS
        ) {
            if (history.isEmpty()) break
            history.removeAt(0)
        }
        redoStack.clear()
    }

    fun insertAtCursor(text: String, newlineBefore: Boolean = true) {
        pushHistory()
        val cursor = draft.selection.end.coerceIn(0, draft.text.length)
        val currentText = draft.text
        val needsNewline = newlineBefore && cursor > 0 && currentText.getOrNull(cursor - 1) != '\n'
        val insertion = (if (needsNewline) "\n" else "") + text
        val newText = clampNote(currentText.substring(0, cursor) + insertion + currentText.substring(cursor))
        draft = TextFieldValue(newText, TextRange(newText.length.coerceAtMost(cursor + insertion.length)))
    }

    // Voice dictation always appends to the very end rather than at the cursor — so stringing
    // multiple dictations together reads as one continuous note instead of possibly interleaving
    // with whatever the user was mid-edit on elsewhere in the text.
    fun appendVoiceText(spoken: String) {
        pushHistory()
        val current = draft.text
        val combined = clampNote(
            if (current.isEmpty() || current.endsWith("\n")) current + spoken else "$current $spoken"
        )
        draft = TextFieldValue(combined, TextRange(combined.length))
    }

    fun undo() {
        if (history.isNotEmpty()) {
            redoStack.add(draft)
            draft = history.removeAt(history.size - 1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            history.add(draft)
            draft = redoStack.removeAt(redoStack.size - 1)
        }
    }

    val wordCount = remember(draft.text) {
        draft.text.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }
    }

    val launchVoiceInput = rememberVoiceInputLauncher(prompt = "Dictate note…") { spoken ->
        appendVoiceText(spoken)
    }

    Dialog(
        onDismissRequest = closeAndFlush,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        EnsureDialogEdgeToEdge()
        // Read the real nav/status bar heights from the app-wide SystemBarInsets rather than
        // this Dialog's own WindowInsets — this dialog's window cannot be trusted to report
        // them accurately on every device, which is exactly why the Save button was ending up
        // unreachable behind the on-screen navigation bar. A generous extra margin is added on
        // top of the measured inset so the Save button always sits clearly above it.
        val density = LocalDensity.current
        val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
        val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() } + 20.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SpotVaultColors.Void, SpotVaultColors.Deep, SpotVaultColors.Surface)
                    )
                )
                .padding(top = statusPad, bottom = navPad)
                .imePadding()
        ) {
            // Caps width on tablets/landscape (same as the photo viewer and Settings) — without
            // this, the toolbar and ruled-paper lines stretched edge-to-edge on wide screens,
            // making long lines of notes hard to read and spreading the toolbar icons far apart.
            AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = closeAndFlush,
                        modifier = Modifier
                            .size(40.dp)
                            .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fieldLabel,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = SpotVaultColors.Teal,
                            letterSpacing = 0.5.sp
                        )
                        if (contextCategory.isNotBlank() || contextAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (contextCategory.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                SpotVaultColors.Teal.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = contextCategory,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SpotVaultColors.Teal
                                        )
                                    }
                                }
                                if (contextAddress.isNotBlank()) {
                                    Text(
                                        text = contextAddress,
                                        fontSize = 12.sp,
                                        color = SpotVaultColors.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Formatting toolbar — quick-insert bullets, checklists, numbered lines,
                // dividers, and a timestamp, plus undo and a text-size toggle. Everything
                // here inserts plain text (no rich text/markup), so it stays completely free
                // and renders identically anywhere the note is later shown or exported.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotepadToolbarButton(Icons.Default.Mic, "Dictate note") {
                        launchVoiceInput()
                    }
                    NotepadToolbarButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet point") {
                        insertAtCursor("• ")
                    }
                    NotepadToolbarButton(Icons.Default.CheckBox, "Checklist item") {
                        insertAtCursor("☐ ")
                    }
                    NotepadToolbarButton(Icons.Default.FormatListNumbered, "Numbered line") {
                        val lines = draft.text.substring(0, draft.selection.end.coerceIn(0, draft.text.length))
                            .lines()
                        val lastNum = lines.lastOrNull { it.trim().firstOrNull()?.isDigit() == true }
                            ?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                        insertAtCursor("${lastNum + 1}. ")
                    }
                    NotepadToolbarButton(Icons.Default.HorizontalRule, "Divider") {
                        insertAtCursor("───────────")
                    }
                    NotepadToolbarButton(Icons.Default.AccessTime, "Insert timestamp") {
                        val stamp = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
                        insertAtCursor(stamp)
                    }
                    NotepadToolbarButton(
                        Icons.AutoMirrored.Filled.Undo,
                        "Undo",
                        enabled = history.isNotEmpty()
                    ) { undo() }
                    NotepadToolbarButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        enabled = redoStack.isNotEmpty(),
                        onClick = { redo() }
                    )
                    if (conditionsSnapshot != null) {
                        NotepadToolbarButton(
                            icon = Icons.Default.WbSunny,
                            contentDescription = "Insert current conditions",
                            onClick = { insertAtCursor(conditionsSnapshot) }
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotVaultColors.Deep)
                            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("S", "M", "L").forEachIndexed { idx, label ->
                            val selected = fontScaleIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable { fontScaleIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Muted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SpotVaultColors.Elevated.copy(alpha = 0.92f))
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                ) {
                    val scrollState = rememberScrollState()
                    val density = LocalDensity.current
                    val lineHeightPx = with(density) { lineHeightSp.toPx() }
                    val topPaddingPx = with(density) { 16.dp.toPx() }
                    val ruleColor = SpotVaultColors.Outline.copy(alpha = 0.22f)
                    val marginColor = SpotVaultColors.Danger.copy(alpha = 0.28f)

                    // Ruled-paper effect: faint horizontal rules spaced to the active line
                    // height, purely decorative, drawn behind the text field.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val marginPx = 18.dp.toPx()
                        var y = topPaddingPx + lineHeightPx
                        while (y < size.height) {
                            drawLine(
                                color = ruleColor,
                                start = Offset(marginPx, y),
                                end = Offset(size.width - marginPx, y),
                                strokeWidth = 1f
                            )
                            y += lineHeightPx
                        }
                        // Warm margin rule, like a real notepad's left-side red line.
                        val marginRuleX = 46.dp.toPx()
                        drawLine(
                            color = marginColor,
                            start = Offset(marginRuleX, 0f),
                            end = Offset(marginRuleX, size.height),
                            strokeWidth = 1.5f
                        )
                    }

                    BasicTextField(
                        value = draft,
                        onValueChange = { next ->
                            draft = if (next.text.length <= NOTEPAD_MAX_CHARS) next
                            else next.copy(text = next.text.take(NOTEPAD_MAX_CHARS))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(start = 56.dp, end = 18.dp, top = 16.dp, bottom = 34.dp),
                        textStyle = TextStyle(
                            color = SpotVaultColors.OnSurface,
                            fontSize = fontSizeSp,
                            lineHeight = lineHeightSp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(SpotVaultColors.Teal),
                        decorationBox = { inner ->
                            if (draft.text.isEmpty()) {
                                Text(
                                    text = "Write your notes here… tap the toolbar above for bullets, checklists, and more.",
                                    style = TextStyle(
                                        color = SpotVaultColors.Muted.copy(alpha = 0.65f),
                                        fontSize = fontSizeSp,
                                        lineHeight = lineHeightSp
                                    )
                                )
                            }
                            inner()
                        }
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "$wordCount words",
                            fontSize = 11.sp,
                            color = SpotVaultColors.Muted.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${draft.text.length} chars",
                            fontSize = 11.sp,
                            color = SpotVaultColors.Muted.copy(alpha = 0.85f)
                        )
                    }
                }

                if (lastEditedLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = lastEditedLabel,
                        fontSize = 11.sp,
                        color = SpotVaultColors.Muted.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(spotVaultButtonShape())
                        .background(
                            brush = if (saveEnabled || savedPulse) {
                                Brush.linearGradient(listOf(SpotVaultColors.Primary, SpotVaultColors.Teal))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        SpotVaultColors.Muted.copy(alpha = 0.32f),
                                        SpotVaultColors.Muted.copy(alpha = 0.22f)
                                    )
                                )
                            }
                        )
                        .then(
                            if (saveEnabled) {
                                Modifier.clickable {
                                    onSave(draft.text)
                                    lastEditedAt = System.currentTimeMillis()
                                    savedPulse = true
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gated on savedPulse (a plain boolean, changes once per save) rather than
                        // checkAlpha > 0.01f (the animated value itself) — the latter recomposed
                        // this whole Row on every frame of the 250ms tween just to re-evaluate the
                        // threshold. checkAlpha crosses 0.01 within milliseconds of savedPulse
                        // flipping true, so the icon still appears at effectively the same moment;
                        // the .alpha(checkAlpha) below moved into graphicsLayer{} so the animated
                        // value itself is now read at draw time instead of composable-body scope.
                        if (savedPulse) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SpotVaultColors.ButtonLabel,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                                    .graphicsLayer { alpha = checkAlpha }
                            )
                        }
                        Text(
                            text = if (savedPulse) "Notes saved" else "Save Notes",
                            fontWeight = FontWeight.Bold,
                            color = if (saveEnabled || savedPulse) {
                                SpotVaultColors.ButtonLabel
                            } else {
                                SpotVaultColors.Muted.copy(alpha = 0.75f)
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun NotepadToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SpotVaultColors.Deep.copy(alpha = if (enabled) 1f else 0.5f))
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) SpotVaultColors.Teal else SpotVaultColors.Muted.copy(alpha = 0.5f),
            modifier = Modifier.size(19.dp)
        )
    }
}
