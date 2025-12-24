package com.abhishek.adblocker.vpn.packet

import java.nio.ByteBuffer

data class IpPacket(
    val version: Int,
    val protocol: Int,
    val sourceIp: ByteArray,
    val destIp: ByteArray,
    val headerLength: Int,
    val totalLength: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IpPacket

        if (version != other.version) return false
        if (protocol != other.protocol) return false
        if (!sourceIp.contentEquals(other.sourceIp)) return false
        if (!destIp.contentEquals(other.destIp)) return false
        if (headerLength != other.headerLength) return false
        if (totalLength != other.totalLength) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + protocol
        result = 31 * result + sourceIp.contentHashCode()
        result = 31 * result + destIp.contentHashCode()
        result = 31 * result + headerLength
        result = 31 * result + totalLength
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object IpPacketParser {
    private const val IP_PROTOCOL_UDP = 17
    private const val IP_PROTOCOL_TCP = 6

    fun parse(data: ByteArray): IpPacket? {
        if (data.size < 20) return null

        val buffer = ByteBuffer.wrap(data)

        val versionAndIhl = buffer.get().toInt() and 0xFF
        val version = (versionAndIhl shr 4) and 0x0F
        val ihl = versionAndIhl and 0x0F
        val headerLength = ihl * 4

        if (version != 4) return null
        if (data.size < headerLength) return null

        buffer.get()

        val totalLength = buffer.short.toInt() and 0xFFFF
        if (data.size < totalLength) return null

        buffer.position(9)
        val protocol = buffer.get().toInt() and 0xFF

        buffer.position(12)
        val sourceIp = ByteArray(4)
        buffer.get(sourceIp)

        val destIp = ByteArray(4)
        buffer.get(destIp)

        buffer.position(headerLength)
        val payloadSize = totalLength - headerLength
        val payload = ByteArray(payloadSize)
        buffer.get(payload)

        return IpPacket(
            version = version,
            protocol = protocol,
            sourceIp = sourceIp,
            destIp = destIp,
            headerLength = headerLength,
            totalLength = totalLength,
            payload = payload
        )
    }

    fun isUdp(packet: IpPacket): Boolean = packet.protocol == IP_PROTOCOL_UDP
    fun isTcp(packet: IpPacket): Boolean = packet.protocol == IP_PROTOCOL_TCP
}
