package com.abhishek.adblocker.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.abhishek.adblocker.MainActivity
import com.abhishek.adblocker.R
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.dns.DnsPacketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AdBlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var dnsPacketHandler: DnsPacketHandler? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startVpnService()
                START_STICKY
            }
            ACTION_STOP -> {
                stopVpnService()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startVpnService() {
        if (vpnInterface != null) {
            Logger.w("VPN service already running")
            return
        }

        startForeground(NOTIFICATION_ID, createNotification())

        setupVpnInterface()
        dnsPacketHandler = DnsPacketHandler(this)
        startPacketProcessing()

        Logger.vpnStarted()
    }

    private fun setupVpnInterface() {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)
            .addRoute(DNS_SERVER, 32)  // Only route DNS traffic to 8.8.8.8
            .addDnsServer(DNS_SERVER)
            .setMtu(MTU)
            .setBlocking(false)

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            Logger.e("Failed to establish VPN interface")
            stopSelf()
            return
        }

        inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

        Logger.vpnInterfaceEstablished(VPN_ADDRESS)
    }

    private fun startPacketProcessing() {
        serviceScope.launch {
            processPackets()
        }
    }

    private suspend fun processPackets() {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val input = inputStream ?: return
        val output = outputStream ?: return

        Logger.packetProcessingStarted()

        while (serviceScope.isActive && vpnInterface != null) {
            try {
                buffer.clear()
                val length = input.read(buffer.array())

                if (length > 0) {
                    val packetData = buffer.array().copyOf(length)

                    serviceScope.launch {
                        processSinglePacket(packetData, output)
                    }
                }
            } catch (e: Exception) {
                if (serviceScope.isActive) {
                    Logger.e("Error reading packet from TUN", e)
                }
                break
            }
        }

        Logger.packetProcessingStopped()
    }

    private fun processSinglePacket(packetData: ByteArray, output: FileOutputStream) {
        try {
            val handler = dnsPacketHandler ?: return
            val responsePacket = handler.processDnsPacket(packetData)

            if (responsePacket != null) {
                writePacketToTun(responsePacket, output)
            }
        } catch (e: Exception) {
            Logger.e("Error processing packet", e)
        }
    }

    private fun writePacketToTun(packet: ByteArray, output: FileOutputStream) {
        try {
            synchronized(output) {
                output.write(packet)
                Logger.d("Wrote ${packet.size} bytes to TUN interface")
            }
        } catch (e: Exception) {
            Logger.e("Error writing packet to TUN", e)
        }
    }

    private fun stopVpnService() {
        Logger.i("Stopping VPN service...")

        serviceScope.cancel()

        dnsPacketHandler = null

        inputStream?.close()
        outputStream?.close()
        inputStream = null
        outputStream = null

        vpnInterface?.close()
        vpnInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Logger.vpnStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnService()
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Ad Blocker is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ad Blocker VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Ad Blocker VPN is active"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.abhishek.adblocker.START_VPN"
        const val ACTION_STOP = "com.abhishek.adblocker.STOP_VPN"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "adblocker_vpn_channel"

        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_PREFIX_LENGTH = 32
        private const val DNS_SERVER = "8.8.8.8"
        private const val MTU = 1500
        private const val BUFFER_SIZE = 32767
    }
}
