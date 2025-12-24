package com.abhishek.adblocker.vpn.dns

import org.xbill.DNS.Message
import java.io.IOException

data class DnsQuery(
    val id: Int,
    val hostname: String,
    val type: Int,
    val originalMessage: Message
)

object DnsRequestParser {

    fun parseDnsQuery(rawData: ByteArray): DnsQuery? {
        return try {
            val message = Message(rawData)
            val question = message.question ?: return null

            DnsQuery(
                id = message.header.id,
                hostname = question.name.toString(true),
                type = question.type,
                originalMessage = message
            )
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
