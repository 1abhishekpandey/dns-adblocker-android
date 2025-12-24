package com.abhishek.adblocker.vpn.packet

import java.nio.ByteBuffer

data class UdpPacket(
    val sourcePort: Int,
    val destPort: Int,
    val length: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UdpPacket

        if (sourcePort != other.sourcePort) return false
        if (destPort != other.destPort) return false
        if (length != other.length) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourcePort
        result = 31 * result + destPort
        result = 31 * result + length
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object UdpPacketParser {
    const val DNS_PORT = 53

    fun parse(data: ByteArray): UdpPacket? {
        if (data.size < 8) return null

        val buffer = ByteBuffer.wrap(data)

        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF
        val length = buffer.short.toInt() and 0xFFFF

        buffer.short

        if (data.size < length) return null

        val payloadSize = length - 8
        val payload = ByteArray(payloadSize)
        buffer.get(payload)

        return UdpPacket(
            sourcePort = sourcePort,
            destPort = destPort,
            length = length,
            payload = payload
        )
    }

    fun isDnsRequest(packet: UdpPacket): Boolean = packet.destPort == DNS_PORT
    fun isDnsResponse(packet: UdpPacket): Boolean = packet.sourcePort == DNS_PORT
}
