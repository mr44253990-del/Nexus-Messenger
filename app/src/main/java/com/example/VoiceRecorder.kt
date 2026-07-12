package com.example

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording() {
        audioFile = File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.mp3")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile!!.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            audioFile?.delete()
            audioFile = null
        }
        mediaRecorder = null
        return audioFile
    }

    fun cancelRecording() {
        stopRecording()
        audioFile?.delete()
        audioFile = null
    }
}
