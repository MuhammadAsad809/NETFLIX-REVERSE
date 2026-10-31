package com.netflix.moviebox.ui.player

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.netflix.moviebox.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private var tickSoundId: Int = 0
    private var popSoundId: Int = 0
    private var chimeSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load extracted 48kHz Vorbis OGG & iconic Ta-Dum sound
        try {
            tickSoundId = soundPool.load(context, R.raw.brf, 1)
            popSoundId = soundPool.load(context, R.raw.brg, 1)
            chimeSoundId = soundPool.load(context, R.raw.ta_dum, 1)
        } catch (e: Exception) {
            try {
                chimeSoundId = soundPool.load(context, R.raw.brh, 1)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private val appContext = context.applicationContext
    private var chimePlayer: android.media.MediaPlayer? = null

    fun playTick() {
        if (tickSoundId != 0) soundPool.play(tickSoundId, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    fun playPop() {
        if (popSoundId != 0) soundPool.play(popSoundId, 0.8f, 0.8f, 1, 0, 1.0f)
    }

    fun playChime() {
        try {
            chimePlayer?.release()
            chimePlayer = android.media.MediaPlayer.create(appContext, R.raw.ta_dum).apply {
                setOnCompletionListener { mp ->
                    mp.release()
                    if (chimePlayer == mp) chimePlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            if (chimeSoundId != 0) soundPool.play(chimeSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun release() {
        try {
            chimePlayer?.release()
            chimePlayer = null
        } catch (e: Exception) {
            // ignore
        }
        soundPool.release()
    }
}
