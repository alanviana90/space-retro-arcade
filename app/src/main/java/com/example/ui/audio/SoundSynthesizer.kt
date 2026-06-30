package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    var isMuted = false

    // Generates a quick sound and plays it on a background thread
    private fun playSound(generator: () -> ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                val buffer = generator()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(buffer.size * 2, minBufferSize)
                
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                // Release after it finishes playing
                val durationMs = (buffer.size.toFloat() / sampleRate * 1000).toLong()
                kotlinx.coroutines.delay(durationMs + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("SoundSynthesizer", "Error playing sound", e)
            }
        }
    }

    // Laser Sound: Slide from high frequency to low frequency
    fun playLaser() {
        playSound {
            val duration = 0.12f // seconds
            val numSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(numSamples)
            val startFreq = 1100.0
            val endFreq = 300.0
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * t
                val angle = 2.0 * Math.PI * currentFreq * (i.toDouble() / sampleRate)
                // Square wave for retro 8-bit sound
                val sampleValue = if (sin(angle) > 0) 3000 else -3000
                // Volume fade out
                val envelope = 1.0 - t
                buffer[i] = (sampleValue * envelope).toInt().toShort()
            }
            buffer
        }
    }

    // Explosion Sound: White noise combined with low frequency rumbling
    fun playExplosion() {
        playSound {
            val duration = 0.28f // seconds
            val numSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val noise = (Math.random() * 2.0 - 1.0) * 4500
                val rumbleFreq = 120.0 - (80.0 * t)
                val angle = 2.0 * Math.PI * rumbleFreq * (i.toDouble() / sampleRate)
                val rumble = sin(angle) * 3500
                
                // Fade out
                val envelope = 1.0 - t
                buffer[i] = ((noise + rumble) * envelope).toInt().toShort()
            }
            buffer
        }
    }

    // PowerUp Sound: Rapidly ascending arpeggio
    fun playPowerUp() {
        playSound {
            val duration = 0.35f // seconds
            val numSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(numSamples)
            val notes = doubleArrayOf(330.0, 440.0, 554.0, 659.0, 880.0) // A major arpeggio
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val noteIndex = ((t * notes.size).toInt()).coerceIn(0, notes.size - 1)
                val freq = notes[noteIndex]
                val angle = 2.0 * Math.PI * freq * (i.toDouble() / sampleRate)
                // Triangle/Sine blend wave
                val sampleValue = (sin(angle) * 4000).toInt().toShort()
                buffer[i] = sampleValue
            }
            buffer
        }
    }

    // Defeat / Game Over Sound: Descending sad sweep
    fun playDefeat() {
        playSound {
            val duration = 0.5f // seconds
            val numSamples = (sampleRate * duration).toInt()
            val buffer = ShortArray(numSamples)
            val startFreq = 440.0
            val endFreq = 110.0
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * t
                val angle = 2.0 * Math.PI * currentFreq * (i.toDouble() / sampleRate)
                val sampleValue = if (sin(angle) > 0) 2500 else -2500
                // Fade out
                val envelope = 1.0 - t
                buffer[i] = (sampleValue * envelope).toInt().toShort()
            }
            buffer
        }
    }
}
