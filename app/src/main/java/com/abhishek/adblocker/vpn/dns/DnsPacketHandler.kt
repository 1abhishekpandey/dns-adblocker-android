package com.abhishek.adblocker.vpn.dns

import android.net.VpnService
import com.abhishek.adblocker.data.blocklist.BlockedDomains
import com.abhishek.adblocker.util.Logger
import com.abhishek.adblocker.vpn.packet.IpPacketParser
import com.abhishek.adblocker.vpn.packet.PacketWriter
import com.abhishek.adblocker.vpn.packet.UdpPacketParser
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsPacketHandler(private val vpnService: VpnService) {
    private val upstreamDnsServer = InetAddress.getByName("8.8.8.8")

    fun processDnsPacket(ipPacketData: ByteArray): ByteArray? {
        val ipPacket = IpPacketParser.parse(ipPacketData) ?: return null

        if (!IpPacketParser.isUdp(ipPacket)) return null

        val udpPacket = UdpPacketParser.parse(ipPacket.payload) ?: return null

        if (!UdpPacketParser.isDnsRequest(udpPacket)) return null

        val dnsQuery = DnsRequestParser.parseDnsQuery(udpPacket.payload) ?: return null

        Logger.dnsQueryReceived(dnsQuery.hostname, dnsQuery.type)

        return if (BlockedDomains.isBlocked(dnsQuery.hostname)) {
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
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            vpnService.protect(socket)
            socket.soTimeout = 5000

            Logger.dnsForwarded(dnsQuery.hostname, upstreamDnsServer.hostAddress ?: "8.8.8.8")

            val sendPacket = DatagramPacket(
                udpPacket.payload,
                udpPacket.payload.size,
                upstreamDnsServer,
                53
            )
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)

            val dnsResponse = receiveBuffer.copyOf(receivePacket.length)
            Logger.d("DNS response received for ${dnsQuery.hostname}, size: ${dnsResponse.size}")

            PacketWriter.buildIpUdpPacket(
                sourceIp = ipPacket.destIp,
                destIp = ipPacket.sourceIp,
                sourcePort = udpPacket.destPort,
                destPort = udpPacket.sourcePort,
                dnsPayload = dnsResponse
            )
        } catch (e: Exception) {
            Logger.e("Error forwarding DNS request for ${dnsQuery.hostname}", e)
            null
        } finally {
            socket?.close()
        }
    }
}
