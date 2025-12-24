package com.abhishek.adblocker.vpn.packet

import java.nio.ByteBuffer

object PacketWriter {

    fun buildIpUdpPacket(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val udpLength = 8 + dnsPayload.size
        val totalLength = 20 + udpLength

        val buffer = ByteBuffer.allocate(totalLength)

        buffer.put((0x45).toByte())
        buffer.put(0)
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.put(64)
        buffer.put(17)
        buffer.putShort(0)
        buffer.put(sourceIp)
        buffer.put(destIp)

        val ipHeaderChecksum = calculateChecksum(buffer.array(), 0, 20)
        buffer.putShort(10, ipHeaderChecksum.toShort())

        buffer.putShort(sourcePort.toShort())
        buffer.putShort(destPort.toShort())
        buffer.putShort(udpLength.toShort())
        buffer.putShort(0)

        buffer.put(dnsPayload)

        return buffer.array()
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset

        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }

        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }

        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return sum.inv().toInt() and 0xFFFF
    }
}
