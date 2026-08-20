package com.spotvault.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/** One expandable entry in Help & Guide. [settingsCategoryId] points at the real Settings
 * category this concept lives in (matching an id in [SettingsCategories]) when there is a real
 * toggle to jump to — null for pure how-it-works/usage topics that have no dedicated settings
 * screen, which fall back to landing inside this Help list itself. */
data class HelpItem(
    val icon: ImageVector,
    val question: String,
    val answer: String,
    val settingsCategoryId: String? = null
)

/** The single source of truth for Help & Guide's content, also doubled as the fine-grained half
 * of Settings search (see [buildSettingsSearchResults]) — every entry here is one thing a user
 * might type a word from and expect to find, independent of whether it has its own settings
 * screen. Kept as one flat top-level list (not nested inside HelpGuideSettingsContent) so it can
 * be searched without composing the Help screen itself. */
val HelpTopics: List<Pair<String, List<HelpItem>>> = listOf(
    "Getting Started" to listOf(
        HelpItem(Icons.Default.CameraAlt, "Snap", "Takes a photo and saves your exact location together — the fastest way to remember a spot you'll want to find again. Reads any visible text in the photo automatically (OCR) so it's searchable later."),
        HelpItem(Icons.Default.LocationOn, "Pin", "Saves your location without a photo, if you just need the spot marked quickly."),
        HelpItem(Icons.Default.Inventory2, "The Vault", "Every spot you've saved lives here — searchable, filterable, sortable, and organized by date. Switch between list and grid view from the toolbar."),
        HelpItem(Icons.Default.FlashOn, "Quick Pin & Quick Track (home screen)", "Two one-tap buttons on the home screen for when you're in a hurry — Quick Pin saves your spot immediately with no dialog, Quick Track does the same but also starts an active tracking timer with a persistent notification. Both also work from the home-screen widget. Both also ask for location permission the first time you use them, same as Snap and Pin.", settingsCategoryId = "quickpin"),
        HelpItem(Icons.Default.Share, "Share button (Snap/Pin confirmation)", "The small share button next to Pin on the Snap/Pin confirmation screen shares your current location as text — or with the photo attached, if you got there via Snap — without needing to finish saving the pin first."),
        HelpItem(Icons.Default.Mic, "Voice input", "Tap the microphone icon on any text field — tags, notes, titles, and more — to dictate instead of typing.")
    ),
    "Quick Pins" to listOf(
        HelpItem(Icons.Default.PushPin, "Quick Pin", "One-tap silent save at your current location — no camera, no dialog. Great for marking something fast while you're moving.", settingsCategoryId = "quickpin"),
        HelpItem(Icons.Default.NotificationsActive, "Quick Track", "Same one-tap save, but keeps a persistent notification reminding you to find your way back — tap \"Found\" once you're there, or navigate straight to it with the Navigate or Compass button.", settingsCategoryId = "quickpin"),
        HelpItem(Icons.Default.Edit, "Managing your pins", "Tap \"Manage\" inside the Quick-Pin sheet to add your own custom pins with any emoji, reorder them, or delete ones you don't use.", settingsCategoryId = "quickpin"),
        HelpItem(Icons.Default.CameraAlt, "Auto-Attach Photo", "Have Quick Pin and/or Quick Track open the camera first and save with the photo attached, instead of saving silently with no photo — set separately for each in Quick-Pin Behavior settings. Only applies inside the app; Quick Pin/Track launched from the home-screen widget always save silently with no camera, regardless of this setting.", settingsCategoryId = "quickpin")
    ),
    "The Vault" to listOf(
        HelpItem(Icons.Default.FilterList, "Filtering", "Near Me, Photos, and your most-used tags show as quick filter chips right above your spots — tap the filter icon next to them for the full tag cloud with search, or to switch to filtering by vehicle instead. Favorites works differently — see Favorites, below."),
        HelpItem(Icons.Default.GridView, "List vs. Grid view", "Switch between a scrollable list and a two-column photo grid from the toolbar above your spots — pick whichever you scan faster."),
        HelpItem(Icons.Default.Star, "Favorites", "Swipe or tap the star on any spot to favorite it — favorites are protected from Auto Delete. Tap the gold Star icon at the top right of the Vault to open your dedicated Favorites Hub: every favorite lives there, ready to pull up any time you need to quickly find a spot you visit routinely, plus a + Add Spot button to save somewhere you want to go — type an address, search for it in Maps, or share a place into the app (see below) — even if you've never actually been there to drop a pin. The form itself has its own + Add Photo button (asks Camera or Gallery) and voice-to-text on every field."),
        HelpItem(Icons.Default.Share, "Share a place from Google Maps", "Found a spot in Google Maps you want saved but don't know the exact address? Tap Share on it there and choose DropPin Vault — it opens the Add Spot form with the name, address, and location already filled in, so there's nothing left to search for. Save adds it straight to your Favorites. The Add Spot form's own \"Search in Maps\" button starts this same round trip for you — search there, then Share back to DropPin Vault to bring it home."),
        HelpItem(Icons.Default.Map, "Browse by Location", "Tap the map icon above your spots to see every saved pin plotted on a map, grouped by state/region and city — each entry has the same ⋮ menu (edit, share, add photo, edit tags, favorite) as the main Vault list."),
        HelpItem(Icons.Default.DateRange, "Browse by Calendar", "Tap the calendar icon to jump to any day you've saved spots on and see just that day's results — tap the month/year at the top to jump further back."),
        HelpItem(Icons.Default.Sell, "Tags", "Assign any number of tags to a spot from its ⋮ menu — type to reuse an existing tag or create a new one. Rename or delete tags globally from Settings → Vault → Manage Tags.", settingsCategoryId = "vault"),
        HelpItem(Icons.Default.Edit, "Editing a spot", "Tap Edit from a spot's ⋮ menu or its full-screen photo view to change its title, date, city, state, or notes."),
        HelpItem(Icons.Default.Delete, "Deleting a spot", "Swipe a spot left, or tap Delete at the bottom of its ⋮ menu. Either way it moves to Recently Deleted, not gone for good, and a \"Spot deleted\" banner with an UNDO button appears right away in case you didn't mean to. If \"Confirm Before Delete\" is on (Settings → Vault → Vault Behavior), a single swiped or menu-deleted spot asks first — deleting several at once from multi-select (below) always skips that prompt and relies on UNDO instead.", settingsCategoryId = "vault"),
        HelpItem(Icons.Default.History, "Recently Deleted", "Deleted spots land here first, not gone for good — restore them or clear them out permanently whenever you're ready."),
        HelpItem(Icons.Default.AutoDelete, "Auto Delete", "Turn on Auto Delete (in Vault settings, or the pill next to Recently Deleted) to automatically move spots older than a timeframe you pick into Recently Deleted. Favorites and archived spots are never touched.", settingsCategoryId = "vault"),
        HelpItem(Icons.Default.Archive, "Archive", "Hide a spot from the main Vault without deleting it — archived spots are kept forever, never shown in Auto Delete, and won't be touched by Clear All Vault Data. Swipe a spot right, or tap Archive in its ⋮ menu — either way a \"Spot archived\" banner with UNDO appears in case that was a mistake. View or restore archived spots any time from Settings → Vault → Archived Spots.", settingsCategoryId = "vault"),
        HelpItem(Icons.Default.SelectAll, "Multi-select & bulk actions", "Long-press any spot to start selecting more, then use the icons at the top: Share exports the selected spots as text, Select All grabs everything currently visible (only spots in expanded date sections — collapsed ones are left alone), and the trash icon deletes them all at once, immediately, with the same UNDO banner single-spot delete uses."),
        HelpItem(Icons.Default.Inventory2, "Vault Snapshot", "A quick stat card at the top of Vault settings showing your total saved spots and current active theme.", settingsCategoryId = "vault"),
        HelpItem(Icons.Default.Inventory2, "Prevent Duplicate Spots", "When you save at (or very near) a spot you already have a saved entry for within a set time window, DropPin Vault folds the new save into the existing entry instead of creating a duplicate — updating its timestamp and filling in any photo, notes, or title the original was missing, without moving its coordinates. On by default with a 24-hour merge window and a 25-meter (about 82 feet) merge distance — both adjustable with their own sliders in Vault settings, and the distance slider can display in meters or feet.", settingsCategoryId = "vault")
    ),
    "Vehicles & Automatic Parking" to listOf(
        HelpItem(Icons.Default.DirectionsCar, "Vehicles", "Add each of your vehicles with its own name, color, and icon, and link it to that car's Bluetooth so DropPin Vault knows which vehicle just disconnected.", settingsCategoryId = "vehicles"),
        HelpItem(Icons.Default.Bluetooth, "Automatic Parking", "When your phone disconnects from a linked vehicle's Bluetooth, DropPin Vault automatically saves a parking pin and starts active tracking — even if the app isn't open. Turn it on in Automatic Parking settings. Use \"Test Auto-Park Now\" there to simulate a disconnect and confirm it's working, with a clear pass/fail result.", settingsCategoryId = "autopark"),
        HelpItem(Icons.AutoMirrored.Filled.DirectionsWalk, "Motion & Fitness", "An extra layer on top of Automatic Parking — while connected to a linked car's Bluetooth, DropPin Vault watches for the moment you start walking away and caches a precise fix right then, for a more accurate pin than waiting for Bluetooth to fully disconnect.", settingsCategoryId = "autopark"),
        HelpItem(Icons.Default.LocationOn, "Quiet Zones", "Mark areas like home or work as Quiet Zones so Automatic Parking skips saving there entirely, or saves silently without a notification.", settingsCategoryId = "autopark"),
        HelpItem(Icons.Default.Bluetooth, "Home screen toggles", "Two small buttons near the top of the home screen let you flip Automatic Parking and Motion & Fitness on or off at a glance, with a link to their full settings.")
    ),
    "Security & Backup" to listOf(
        HelpItem(Icons.Default.Lock, "App Lock", "Require your fingerprint, face, or device PIN before DropPin Vault opens — turn it on under Security in Backup, Data & Privacy settings.", settingsCategoryId = "data"),
        HelpItem(Icons.Default.Storage, "Manual Backup", "Export your entire vault — every spot, photo, and note — to a single .zip file you control, and import it back on this device or a new one at any time.", settingsCategoryId = "data"),
        HelpItem(Icons.Default.Storage, "Scheduled Local Backup", "Point DropPin Vault at a folder and it'll keep a fresh backup there automatically on a schedule you choose, no manual exports needed.", settingsCategoryId = "data"),
        HelpItem(Icons.Default.CloudDone, "Cloud Sync (Google Drive)", "The frictionless, no-folder-to-pick option — connect once (during onboarding or from Settings → Data → Cloud Sync) and DropPin Vault automatically backs itself up weekly to a hidden folder in your own Google Drive, invisible to every other app. If this phone is ever lost, broken, or replaced, connecting the same account on a new one offers to restore it automatically. Entirely optional, and separate from the Manual Backup and Scheduled Local Backup options above — use any combination, or none.", settingsCategoryId = "data"),
        HelpItem(Icons.Default.FileUpload, "GPX Waypoints", "Import waypoints from a GPX file — exported from Garmin, Gaia, or another GPS tool — into your vault. Coordinates and notes only, no photos.", settingsCategoryId = "data"),
        HelpItem(Icons.Default.Delete, "Clear All Vault Data", "A permanent, type-to-confirm wipe of every saved spot and photo — available in both Vault settings and Backup, Data & Privacy. There's no undo, so export a backup first if you're not sure.", settingsCategoryId = "data")
    ),
    "Appearance & Themes" to listOf(
        HelpItem(Icons.Default.Palette, "Color Theme", "Pick from several preset color palettes, or build your own with the color wheel — everything in the app follows whatever you choose. Two presets are free; the rest, plus the custom color wheel, are Premium.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Palette, "Material You / Dynamic Color", "Premium — extracts real colors from your phone's current wallpaper (Android 12+) and applies them as your theme automatically, the same way your launcher and other Material You apps do.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Category, "Button Style", "Change the shape of buttons across the app — soft rounded corners, a full pill/capsule shape, cut corners, and more playful options. Premium beyond the default Classic shape. Button text color is chosen automatically for the best contrast against whatever theme or button color you pick.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Inventory2, "Vault Icon Style", "Pick which animated vault dial shows on your bottom navigation bar. Premium beyond the default Classic style.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Explore, "Compass Face Style", "Choose the look of the compass dial used on the Compass navigation screen — classic, arrow, dot, sci-fi, and more. Premium beyond the default Classic face.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Palette, "AMOLED True Black", "Premium — switches dark backgrounds to pure black, which can save real battery on OLED/AMOLED screens.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Palette, "Reduce Animations", "Premium — minimizes motion across the app for a calmer feel or better performance on older devices.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.FormatSize, "Text Size", "Scale text across the entire app up or down from Compact to Extra Large.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.FormatSize, "Font", "Switch the typeface used everywhere in the app, all built into Android so nothing needs downloading — every option is free.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Palette, "Background Patterns", "Premium (beyond the default) — the animated pattern behind every screen: gradient mesh, aurora bands, hex tessellation, light leaks, and more.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Widgets, "App Icon", "Premium (beyond the default) — pick from preset launcher icons. If a switch doesn't visibly update, DropPin Vault now tells you right away instead of leaving you guessing, and offers what to try next.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.FlashOn, "Splash Screen", "Premium (beyond the default) — signal forge, vault door, radar scan, triangulate, beacon wake, vault bloom, haunted gate, and winter gate intros using your chosen app icon.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.CheckCircle, "Found Splash Screen", "Premium (beyond the default) — fireworks, confetti, victory ripples, star burst, champagne pop, All Hallows' Found, and Yuletide Found celebrations when you mark a spot Found from the app or widgets.", settingsCategoryId = "appearance"),
        HelpItem(Icons.Default.Edit, "Vault Name", "Rename your vault anything you like — it shows on the home screen and splash screen.", settingsCategoryId = "appearance")
    ),
    "Sounds & Alerts" to listOf(
        HelpItem(Icons.Default.Notifications, "Alert Sound", "The tone played when an active-tracking timer expires — pick any ringtone on your device.", settingsCategoryId = "timer"),
        HelpItem(Icons.Default.Notifications, "Vibration & Lock Screen", "Control whether timer alerts vibrate, warn you 10 minutes early, and show full details on your lock screen.", settingsCategoryId = "timer"),
        HelpItem(Icons.Default.Notifications, "Haptic Feedback", "Premium (beyond the default pattern) — a physical buzz on taps throughout the app. Choose from several distinct feels, or turn it off entirely.", settingsCategoryId = "timer"),
        HelpItem(Icons.Default.Notifications, "Custom Sounds", "Real recorded sound effects (not synthesized) for button clicks and for opening the Vault — pick your favorite from several free options for each.", settingsCategoryId = "timer"),
        HelpItem(Icons.Default.CheckCircle, "Found sound", "The chime played whenever you mark a spot Found — from the app, a widget, or a Quick Settings tile — is the same sound everywhere, so it's instantly recognizable no matter how you triggered it.", settingsCategoryId = "timer")
    ),
    "Location & Precision" to listOf(
        HelpItem(Icons.Default.LocationOn, "High Accuracy GPS", "Uses more battery in exchange for the tightest possible location fix — on by default.", settingsCategoryId = "gps"),
        HelpItem(Icons.Default.Map, "Auto-Resolve Addresses", "Automatically turns your saved coordinates into a readable street address.", settingsCategoryId = "gps"),
        HelpItem(Icons.Default.LocationOn, "Distance Units", "Show saved spots' distance from you in miles or kilometers throughout the Vault.", settingsCategoryId = "gps"),
        HelpItem(Icons.Default.Share, "Sharing a Spot", "Share any saved spot's location — and optionally its photo — through your phone's normal share sheet to any app. The message includes a ready-to-tap link for both Google Maps (Android) and Apple Maps (iPhone), so it opens correctly no matter which kind of phone the other person has.")
    ),
    "Compass" to listOf(
        HelpItem(Icons.Default.Explore, "Navigating back to a spot", "Tap the Compass button on an active pin or any saved spot to open a full-screen compass pointing you straight back to it, with live distance and heading."),
        HelpItem(Icons.Default.Palette, "Compass Face Style", "Change the dial's look in Appearance & Themes — the style you pick there is exactly what shows up here.", settingsCategoryId = "appearance")
    ),
    "Widgets" to listOf(
        HelpItem(Icons.Default.Widgets, "Everyday Quick Actions widget", "Free — add it from your home screen's widget picker for one-tap Quick Pin, Quick Share, and Quick Track. After Quick Track, the widget switches to Found and Navigate until you clear tracking.", settingsCategoryId = "widgets"),
        HelpItem(Icons.Default.Widgets, "Vault Favorites widget", "Premium — a scrollable list of every starred spot. Tap any entry to open it in the Vault. Placing this widget without Premium shows an Unlock Premium card instead.", settingsCategoryId = "widgets"),
        HelpItem(Icons.Default.Sell, "Tag Filter widget", "Premium — pick one or more tags when you add or reconfigure it; the widget shows every saved spot carrying at least one of them, deduplicated, and stays in sync as you add, remove, or retag spots. Placing this widget without Premium shows an Unlock Premium card instead.", settingsCategoryId = "widgets"),
        HelpItem(Icons.Default.Star, "Premium Dashboard widget", "Premium — a larger widget with your most recent Vault entries (tap to open), Snap/Pin/Share Photo shortcuts, Bluetooth Auto and Motion toggles, and a tracking mode with photo plus Found, Navigate, and Compass. Tapping Bluetooth Auto or Motion when the permission they need hasn't been granted yet opens Automatic Parking settings instead of turning on — a widget can't show a permission prompt itself, so this gets you to the one place that can.", settingsCategoryId = "widgets"),
        HelpItem(Icons.Default.Palette, "Widget theming", "Widgets automatically match your in-app color theme and button shape when \"Sync Widget Colors With Theme\" is on in Widget settings.", settingsCategoryId = "widgets"),
        HelpItem(Icons.Default.FlashOn, "Quick Settings Tiles", "Add a Quick Pin tile (silently saves your current location in one tap) or a Quick Track tile (toggles active tracking, tap again to mark Found) to your phone's Quick Settings (swipe-down) panel — no need to open the app. See Settings → Widgets for how to add either one.", settingsCategoryId = "widgets")
    ),
    "Premium Unlocks" to listOf(
        HelpItem(Icons.Default.Star, "What's included", "Extra color themes, button styles, vault icons, compass faces, fonts, background patterns, app icons, splash screens, found splash screens, Material You, AMOLED True Black, Reduce Animations, and the full haptic pattern library.", settingsCategoryId = "premium"),
        HelpItem(Icons.Default.Star, "Unlocking everything", "One toggle in Premium Unlocks settings unlocks every premium option across the app at once.", settingsCategoryId = "premium")
    )
)

/** Which Help & Guide entry to land on and flash — [section] plus [question] together form the
 * same key HelpGuideSettingsContent uses to expand and scroll to a specific item, since question
 * text alone isn't unique across sections (e.g. "Compass Face Style" appears twice). */
data class HelpHighlightTarget(val section: String, val question: String)

/** One row in Settings search's fine-grained results — a single [HelpTopics] entry matched
 * against whatever the user typed. [categoryId] is where tapping it goes: a real Settings
 * category when the topic has one, otherwise "help" with [highlight] set so Help & Guide can
 * jump straight to it instead of just generically opening. */
data class SettingsSearchResult(
    val key: String,
    val label: String,
    val sectionLabel: String,
    val icon: ImageVector,
    val categoryId: String,
    val highlight: HelpHighlightTarget?
)

/** Full-text search over every individual setting/feature described in [HelpTopics] — not just
 * the dozen broad category keyword buckets in [SettingsCategories] — so typing any word that
 * appears in a topic's name, its explanation, or its section heading finds it. Label matches
 * (the topic's own name) rank above matches that only hit the body text, since a query landing
 * inside a long description is a weaker signal of relevance than landing in the title itself. */
fun buildSettingsSearchResults(query: String): List<SettingsSearchResult> {
    val q = query.trim()
    if (q.isBlank()) return emptyList()
    val labelMatches = mutableListOf<SettingsSearchResult>()
    val bodyMatches = mutableListOf<SettingsSearchResult>()
    HelpTopics.forEach { (section, items) ->
        items.forEach { item ->
            val labelHit = item.question.contains(q, ignoreCase = true)
            val bodyHit = item.answer.contains(q, ignoreCase = true) || section.contains(q, ignoreCase = true)
            if (labelHit || bodyHit) {
                val result = SettingsSearchResult(
                    key = "$section::${item.question}",
                    label = item.question,
                    sectionLabel = section,
                    icon = item.icon,
                    categoryId = item.settingsCategoryId ?: "help",
                    highlight = if (item.settingsCategoryId == null) HelpHighlightTarget(section, item.question) else null
                )
                if (labelHit) labelMatches += result else bodyMatches += result
            }
        }
    }
    return labelMatches + bodyMatches
}
