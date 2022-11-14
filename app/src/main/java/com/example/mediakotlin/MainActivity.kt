package com.example.mediakotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.MediaController
import androidx.core.app.ActivityCompat
import com.example.mediakotlin.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity(), ViewInit {

    private val TAG = "MainActivity"

    private val REQUEST_CODE_RECORD_AUDIO_PERMISSION = 200
    private val REQUEST_CODE_VIDEO_CAPTURE = 1

    var voiceFileName: String = ""
    var mediaRecorder: MediaRecorder? = null
    var mediaPlayer: MediaPlayer? = null
    var isRecordStart = false
    var isStartPlaying = false

    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        initView()

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_RECORD_AUDIO_PERMISSION)
        voiceFileName = "${externalCacheDir?.absolutePath}/voice_record.3gp"
    }

    override fun initView() {
        setContentView(binding.root)

        binding.voiceRecordingButton.setOnClickListener {
            if (isRecordStart) {
                stopRecording()
            } else {
                startRecording()
            }
            isRecordStart = !isRecordStart

            binding.voiceRecordingButton.text = when (isRecordStart) {
                true -> "음성 녹음 정지"
                false -> "음성 녹음 시작"
            }
        }

        binding.voicePlayButton.setOnClickListener {
            if (isStartPlaying) stopPlaying() else startPlaying()
            isStartPlaying = !isStartPlaying
            binding.voicePlayButton.text = when (isStartPlaying) {
                true -> "음성 재생 정지"
                false -> "음성 재생 시작"
            }
        }

        binding.videoRecordingButton.setOnClickListener {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(packageManager)?.also {
                    startActivityForResult(takeVideoIntent, REQUEST_CODE_VIDEO_CAPTURE)
                }
            }
        }
    }

    fun startPlaying() {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(voiceFileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "startPlaying: prepare() failed")
            }
        }
    }

    fun stopPlaying() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(voiceFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "startRecording: prepare() failed")
            }

            start()
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }

        mediaRecorder = null
    }

    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            val videoUri = data?.data
            binding.videoView.apply {
                setMediaController(MediaController(this@MainActivity))
                setVideoURI(videoUri)
                requestFocus()
                startPlaying()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionToRecordAccepted = if (requestCode == REQUEST_CODE_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (!permissionToRecordAccepted)
            finish()
    }

}