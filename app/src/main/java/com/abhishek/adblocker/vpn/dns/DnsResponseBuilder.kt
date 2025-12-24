package com.abhishek.adblocker.vpn.dns

import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Rcode
import org.xbill.DNS.Section

object DnsResponseBuilder {

    fun createBlockedResponse(request: Message): ByteArray {
        val response = Message(request.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.rcode = Rcode.NXDOMAIN

        if (request.question != null) {
            response.addRecord(request.question, Section.QUESTION)
        }

        return response.toWire()
    }

    fun createEmptyResponse(request: Message): ByteArray {
        val response = Message(request.header.id)
        response.header.setFlag(Flags.QR.toInt())
        response.header.rcode = Rcode.NOERROR

        if (request.question != null) {
            response.addRecord(request.question, Section.QUESTION)
        }

        return response.toWire()
    }
}
