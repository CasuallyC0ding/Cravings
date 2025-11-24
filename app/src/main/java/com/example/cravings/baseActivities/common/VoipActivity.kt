package com.example.cravings.baseActivities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cravings.R
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class VoipActivity : AppCompatActivity() {

    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var statusText: TextView
    private lateinit var callButton: Button
    private lateinit var hangupButton: Button

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var udpSocket: DatagramSocket? = null

    private var callActive = false
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 16000         // More stable
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO

    private var bufferSize = 0

    private var remoteIp = ""
    private var remotePort = 0
    private var localPort = 0               // Same for both phones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voip)

        initViews()
        requestMicPermission()
    }

    private fun initViews() {
        ipInput = findViewById(R.id.ipInput)
        portInput = findViewById(R.id.portInput)
        statusText = findViewById(R.id.statusText)
        callButton = findViewById(R.id.callButton)
        hangupButton = findViewById(R.id.hangupButton)

        hangupButton.isEnabled = false

        callButton.setOnClickListener { prepareCall() }
        hangupButton.setOnClickListener { endCall() }
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                199
            )
        }
    }

    private fun prepareCall() {
        remoteIp = ipInput.text.toString().trim()
        remotePort = portInput.text.toString().trim().toIntOrNull() ?: 0

        if (remoteIp.isEmpty() || remotePort == 0) {
            Toast.makeText(this, "Enter IP & Port", Toast.LENGTH_SHORT).show()
            return
        }

        // SAME port for both phones
        localPort = remotePort

        startCall()
    }

    @SuppressLint("MissingPermission")
    private fun startCall() {
        ioScope.launch {
            try {
                updateStatus("Starting...")

                bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    channelIn,
                    audioFormat
                )

                if (bufferSize <= 0) bufferSize = 2048

                initAudio()
                initSocket()

                callActive = true

                runOnUiThread {
                    callButton.isEnabled = false
                    hangupButton.isEnabled = true
                }

                updateStatus("Connected: $remoteIp:$remotePort\nListening on port $localPort")

                launch { sendAudio() }
                launch { receiveAudio() }

            } catch (e: Exception) {
                updateStatus("Error: ${e.message}")
            }
        }
    }

    private fun initSocket() {
        udpSocket = DatagramSocket(localPort)
        udpSocket?.reuseAddress = true
    }

    @SuppressLint("MissingPermission")
    private fun initAudio() {

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            channelIn,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED)
            throw Exception("AudioRecord failed")

        audioRecord?.startRecording()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormatObj = AudioFormat.Builder()
            .setEncoding(audioFormat)
            .setSampleRate(sampleRate)
            .setChannelMask(channelOut)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormatObj,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        if (audioTrack?.state != AudioTrack.STATE_INITIALIZED)
            throw Exception("AudioTrack failed")

        audioTrack?.play()
    }

    private suspend fun sendAudio() {
        val buffer = ByteArray(bufferSize)

        while (callActive) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                val packet = DatagramPacket(
                    buffer,
                    read,
                    InetAddress.getByName(remoteIp),
                    remotePort
                )
                udpSocket?.send(packet)
            }
        }
    }

    private suspend fun receiveAudio() {
        val buffer = ByteArray(bufferSize)

        while (callActive) {
            val packet = DatagramPacket(buffer, buffer.size)
            udpSocket?.receive(packet)
            audioTrack?.write(packet.data, 0, packet.length)
        }
    }

    private fun endCall() {
        callActive = false

        ioScope.launch {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null

                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null

                udpSocket?.close()
                udpSocket = null

                runOnUiThread {
                    callButton.isEnabled = true
                    hangupButton.isEnabled = false
                    updateStatus("Call ended")
                }

            } catch (e: Exception) {
                updateStatus("End error: ${e.message}")
            }
        }
    }

    private fun updateStatus(text: String) {
        runOnUiThread { statusText.text = text }
    }

    override fun onDestroy() {
        super.onDestroy()
        callActive = false
        ioScope.cancel()
        endCall()
    }
}
