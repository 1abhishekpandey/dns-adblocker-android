package com.abhishek.adblocker.vpn.dns

import android.net.VpnService
import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.packet.IpPacketParser
import com.abhishek.adblocker.vpn.packet.PacketWriter
import com.abhishek.adblocker.vpn.packet.UdpPacketParser
import kotlinx.coroutines.CoroutineScope
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsPacketHandler(
    private val vpnService: VpnService,
    private val coroutineScope: CoroutineScope,
    dnsServerAddress: String = "8.8.8.8"
) {
    private val upstreamDnsServer = InetAddress.getByName(dnsServerAddress)

    // Reusable socket for DNS forwarding to reduce battery drain
    private val dnsSocketLazy = lazy {
        DatagramSocket().apply {
            vpnService.protect(this)
            soTimeout = 5000
        }
    }
    private val dnsSocket: DatagramSocket by dnsSocketLazy

    fun processDnsPacket(ipPacketData: ByteArray): ByteArray? {
        val ipPacket = IpPacketParser.parse(ipPacketData) ?: return null

        if (!IpPacketParser.isUdp(ipPacket)) return null

        val udpPacket = UdpPacketParser.parse(ipPacket.payload) ?: return null

        if (!UdpPacketParser.isDnsRequest(udpPacket)) return null

        val dnsQuery = DnsRequestParser.parseDnsQuery(udpPacket.payload) ?: return null

        Logger.dnsQueryReceived(dnsQuery.hostname, dnsQuery.type)

        val isBlocked = BlockedDomains.isBlocked(dnsQuery.hostname)
        val isUserBlocked = BlockedDomains.isBlockedByUser(dnsQuery.hostname)
        DomainObserver.addDomain(dnsQuery.hostname, isBlocked, isUserBlocked)

        return if (isBlocked) {
            Logger.domainBlocked(dnsQuery.hostname)
            val blockedResponse = DnsResponseBuilder.createBlockedResponse(dnsQuery.originalMessage)
            PacketWriter.buildIpUdpPacket(
                sourceIp = ipPacket.destIp,
                destIp = ipPacket.sourceIp,
                sourcePort = udpPacket.destPort,
                destPort = udpPacket.sourcePort,
                dnsPayload = blockedResponse
            )
        } else {
            Logger.domainAllowed(dnsQuery.hostname)
            forwardDnsRequest(ipPacket, udpPacket, dnsQuery)
        }
    }

    private fun forwardDnsRequest(
        ipPacket: com.abhishek.adblocker.vpn.packet.IpPacket,
        udpPacket: com.abhishek.adblocker.vpn.packet.UdpPacket,
        dnsQuery: DnsQuery
    ): ByteArray? {
        return try {
            Logger.dnsForwarded(dnsQuery.hostname, upstreamDnsServer.hostAddress ?: "8.8.8.8")

            val sendPacket = DatagramPacket(
                udpPacket.payload,
                udpPacket.payload.size,
                upstreamDnsServer,
                53
            )

            // Synchronize socket access to prevent concurrent access issues
            synchronized(dnsSocket) {
                dnsSocket.send(sendPacket)

                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                dnsSocket.receive(receivePacket)

                val dnsResponse = receiveBuffer.copyOf(receivePacket.length)
                Logger.d("DNS response received for ${dnsQuery.hostname}, size: ${dnsResponse.size}")

                PacketWriter.buildIpUdpPacket(
                    sourceIp = ipPacket.destIp,
                    destIp = ipPacket.sourceIp,
                    sourcePort = udpPacket.destPort,
                    destPort = udpPacket.sourcePort,
                    dnsPayload = dnsResponse
                )
            }
        } catch (e: Exception) {
            Logger.e("Error forwarding DNS request for ${dnsQuery.hostname}", e)
            null
        }
    }

    fun cleanup() {
        try {
            if (dnsSocketLazy.isInitialized()) {
                dnsSocket.close()
            }
        } catch (e: Exception) {
            Logger.e("Error closing DNS socket", e)
        }
    }
}
