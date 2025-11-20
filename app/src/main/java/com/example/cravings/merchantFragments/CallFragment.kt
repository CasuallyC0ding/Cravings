package com.example.cravings.merchantFragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cravings.R
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class CallFragment : Fragment() {

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

    private val sampleRate = 16000
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO

    private var bufferSize = 0

    private var remoteIp = ""
    private var remotePort = 0
    private var localPort = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call, container, false)

        ipInput = view.findViewById(R.id.ipInput)
        portInput = view.findViewById(R.id.portInput)
        statusText = view.findViewById(R.id.statusText)
        callButton = view.findViewById(R.id.callButton)
        hangupButton = view.findViewById(R.id.hangupButton)

        hangupButton.isEnabled = false

        requestMicPermission()

        callButton.setOnClickListener { prepareCall() }
        hangupButton.setOnClickListener { endCall() }

        return view
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                199
            )
        }
    }

    private fun prepareCall() {
        remoteIp = ipInput.text.toString().trim()
        remotePort = portInput.text.toString().trim().toIntOrNull() ?: 0

        if (remoteIp.isEmpty() || remotePort == 0) {
            Toast.makeText(requireContext(), "Enter IP & Port", Toast.LENGTH_SHORT).show()
            return
        }

        localPort = remotePort
        startCall()
    }

    @SuppressLint("MissingPermission")
    private fun startCall() {
        ioScope.launch {
            try {
                updateStatus("Starting...")

                bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate, channelIn, audioFormat
                )
                if (bufferSize <= 0) bufferSize = 2048

                initAudio()
                initSocket()

                callActive = true

                withContext(Dispatchers.Main) {
                    callButton.isEnabled = false
                    hangupButton.isEnabled = true
                }

                updateStatus("Connected to $remoteIp:$remotePort")

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
                    buffer, read, InetAddress.getByName(remoteIp), remotePort
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

                withContext(Dispatchers.Main) {
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
        activity?.runOnUiThread { statusText.text = text }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callActive = false
        ioScope.cancel()
        endCall()
    }
}
