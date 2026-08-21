package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap

/** Every button-click and vault sound is a real CC0 recording (sourced from freesound.org)
 * bundled as a raw resource — see each enum's [rawRes]. Clicks use [SoundPool] since they fire
 * frequently and need to feel instant; the vault sound (one press at a time) uses [MediaPlayer],
 * which is simpler but has more per-play setup cost than a pooled/preloaded SoundPool sample. */
object AppSounds {

    @Volatile
    private var vaultMediaPlayer: MediaPlayer? = null

    /** [rawRes] is null only for NONE. */
    enum class ClickSound(val id: String, val label: String, val rawRes: Int?) {
        NONE("none", "None", null),
        MOUSE_CLICK("mouse_click", "Mouse Click", R.raw.mouse_click),
        SHARP_CLICK("sharp_click", "Sharp Click", R.raw.sharp_click),
        DIGITAL_BLIP("digital_blip", "8-Bit Blip", R.raw.digital_blip),
        WOOD_KNOCK("wood_knock", "Wood Knock", R.raw.wood_knock),
        GLASS_BREAK("glass_break", "Glass Break", R.raw.glass_break),
        BUBBLE_POP("bubble_pop", "Bubble Pop", R.raw.bubble_pop),
        SCI_FI("sci_fi", "Sci-Fi", R.raw.sci_fi);

        companion object {
            fun fromId(id: String?): ClickSound = entries.firstOrNull { it.id == id } ?: NONE
        }
    }

    enum class VaultSound(val id: String, val label: String, val description: String, val rawRes: Int?) {
        NONE("none", "None", "", null),
        LOCK_CLICK("lock_click", "Lock Click", "A crisp deadbolt click", R.raw.lock_click),
        AIRLOCK_HISS("airlock_hiss", "Airlock Hiss", "A pressurized release", R.raw.airlock_hiss),
        SAFE_DOOR("safe_door", "Safe Door", "A heavy safe swinging shut", R.raw.safe_door),
        VAULT_DOOR_CREAK("vault_door_creak", "Vault Door Creak", "A slow, groaning swing", R.raw.vault_door_creak);

        companion object {
            fun fromId(id: String?): VaultSound = entries.firstOrNull { it.id == id } ?: NONE
        }
    }

    private var soundPool: SoundPool? = null
    private val loadedSoundIds = ConcurrentHashMap<Int, Int>()
    // SoundPool.load() is asynchronous — a sample isn't actually decoded and playable the moment
    // load() returns. Playing it immediately after loading (the old behavior) silently did
    // nothing the very first time a given click sound was used, since the pooled play() call ran
    // before the async decode finished; only the second tap (by then loaded) actually played.
    // Sample ids queued here get played by the pool-wide load-complete listener the instant each
    // one finishes decoding, so even a brand-new sound plays on the first tap.
    private val pendingAutoplay = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    private fun pool(context: Context): SoundPool {
        soundPool?.let { return it }
        val created = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        created.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0 && pendingAutoplay.remove(sampleId)) {
                pool.play(sampleId, 1f, 1f, 1, 0, 1f)
            }
        }
        soundPool = created
        return created
    }

    fun playClickSound(context: Context, prefs: SharedPreferences) {
        val sound = ClickSound.fromId(prefs.getString("click_sound", ClickSound.NONE.id))
        playPooled(context, sound.rawRes)
    }

    fun playVaultSound(context: Context, prefs: SharedPreferences) {
        val sound = VaultSound.fromId(prefs.getString("vault_sound", VaultSound.NONE.id))
        playRawSound(context, sound.rawRes)
    }

    /** Bypasses the saved preference — used by the Settings picker so tapping a row previews
     * that exact sound. */
    fun preview(context: Context, soundId: String) {
        playPooled(context, ClickSound.fromId(soundId).rawRes)
    }

    fun previewVaultSound(context: Context, soundId: String) {
        playRawSound(context, VaultSound.fromId(soundId).rawRes)
    }

    private fun playPooled(context: Context, rawRes: Int?) {
        if (rawRes == null) return
        try {
            val appContext = context.applicationContext
            val pool = pool(appContext)
            val cachedId = loadedSoundIds[rawRes]
            if (cachedId != null) {
                pool.play(cachedId, 1f, 1f, 1, 0, 1f)
            } else {
                val soundId = pool.load(appContext, rawRes, 1)
                loadedSoundIds[rawRes] = soundId
                pendingAutoplay.add(soundId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playRawSound(context: Context, rawRes: Int?) {
        if (rawRes == null) return
        try {
            // Single-flight — rapid Settings previews used to stack MediaPlayers until each
            // completion listener ran, stressing low-RAM devices.
            runCatching {
                vaultMediaPlayer?.stop()
                vaultMediaPlayer?.release()
            }
            vaultMediaPlayer = null
            val player = MediaPlayer.create(context.applicationContext, rawRes) ?: return
            vaultMediaPlayer = player
            player.setOnCompletionListener { mp ->
                mp.release()
                if (vaultMediaPlayer === mp) vaultMediaPlayer = null
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (vaultMediaPlayer === mp) vaultMediaPlayer = null
                true
            }
            player.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
