/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * CpimMessageParser - CPIM (Common Presence and IM) message format parser
 * RFC 3862 compliant implementation
 */

package org.microg.gms.rcs.protocol

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CpimMessageParser {

    private const val CRLF = "\r\n"
    private const val HEADER_SEPARATOR = ": "
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(rawMessage: String): CpimMessage? {
        try {
            val parts = rawMessage.split("$CRLF$CRLF", limit = 3)
            if (parts.size < 2) return null
            
            val headers = parseHeaders(parts[0])
            
            val contentHeaders: Map<String, String>
            val body: String
            
            if (parts.size == 3) {
                contentHeaders = parseHeaders(parts[1])
                body = parts[2]
            } else {
                contentHeaders = emptyMap()
                body = parts[1]
            }
            
            return CpimMessage(
                from = headers["From"],
                to = headers["To"],
                dateTime = headers["DateTime"]?.let { parseDateTime(it) },
                subject = headers["Subject"],
                nsHeader = headers["NS"],
                requireHeader = headers["Require"],
                contentType = contentHeaders["Content-Type"] ?: "text/plain",
                contentDisposition = contentHeaders["Content-Disposition"],
                contentId = contentHeaders["Content-ID"],
                body = body,
                rawHeaders = headers,
                rawContentHeaders = contentHeaders
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun build(message: CpimMessage): String {
        val builder = StringBuilder()
        
        message.from?.let { builder.append("From: $it$CRLF") }
        message.to?.let { builder.append("To: $it$CRLF") }
        message.dateTime?.let { builder.append("DateTime: ${formatDateTime(it)}$CRLF") }
        message.subject?.let { builder.append("Subject: $it$CRLF") }
        message.nsHeader?.let { builder.append("NS: $it$CRLF") }
        message.requireHeader?.let { builder.append("Require: $it$CRLF") }
        
        builder.append(CRLF)
        
        builder.append("Content-Type: ${message.contentType}$CRLF")
        message.contentDisposition?.let { builder.append("Content-Disposition: $it$CRLF") }
        message.contentId?.let { builder.append("Content-ID: $it$CRLF") }
        
        builder.append(CRLF)
        
        builder.append(message.body)
        
        return builder.toString()
    }

    fun buildSimple(
        from: String,
        to: String,
        content: String,
        contentType: String = "text/plain"
    ): String {
        val message = CpimMessage(
            from = "<$from>",
            to = "<$to>",
            dateTime = Date(),
            contentType = contentType,
            body = content
        )
        return build(message)
    }

    private fun parseHeaders(headerBlock: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        headerBlock.lines().forEach { line ->
            val colonIndex = line.indexOf(HEADER_SEPARATOR)
            if (colonIndex > 0) {
                val name = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + HEADER_SEPARATOR.length).trim()
                headers[name] = value
            }
        }
        
        return headers
    }

    private fun parseDateTime(dateTimeString: String): Date? {
        return try {
            DATE_FORMAT.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDateTime(date: Date): String {
        return DATE_FORMAT.format(date)
    }
}

data class CpimMessage(
    val from: String? = null,
    val to: String? = null,
    val dateTime: Date? = null,
    val subject: String? = null,
    val nsHeader: String? = null,
    val requireHeader: String? = null,
    val contentType: String = "text/plain",
    val contentDisposition: String? = null,
    val contentId: String? = null,
    val body: String = "",
    val rawHeaders: Map<String, String> = emptyMap(),
    val rawContentHeaders: Map<String, String> = emptyMap()
) {
    fun extractPhoneNumber(uri: String?): String? {
        if (uri == null) return null
        
        val sipMatch = Regex("sip:([^@]+)@").find(uri)
        if (sipMatch != null) {
            return sipMatch.groupValues[1]
        }
        
        val telMatch = Regex("tel:([^>]+)").find(uri)
        if (telMatch != null) {
            return telMatch.groupValues[1]
        }
        
        return uri.replace(Regex("[<>]"), "")
    }
    
    fun getFromPhoneNumber(): String? = extractPhoneNumber(from)
    fun getToPhoneNumber(): String? = extractPhoneNumber(to)
}

object ImdnMessageParser {

    private const val IMDN_NAMESPACE = "urn:ietf:params:imdn"

    fun parseDispositionNotification(xml: String): ImdnNotification? {
        return try {
            val messageId = extractXmlValue(xml, "message-id")
            val status = when {
                xml.contains("<delivered/>") || xml.contains("<delivered />") -> ImdnStatus.DELIVERED
                xml.contains("<displayed/>") || xml.contains("<displayed />") -> ImdnStatus.DISPLAYED
                xml.contains("<failed/>") || xml.contains("<failed />") -> ImdnStatus.FAILED
                xml.contains("<forbidden/>") || xml.contains("<forbidden />") -> ImdnStatus.FORBIDDEN
                xml.contains("<error/>") || xml.contains("<error />") -> ImdnStatus.ERROR
                else -> ImdnStatus.UNKNOWN
            }
            val dateTime = extractXmlValue(xml, "datetime")
            
            ImdnNotification(
                messageId = messageId,
                status = status,
                dateTime = dateTime
            )
        } catch (e: Exception) {
            null
        }
    }

    fun buildDeliveredNotification(messageId: String, originalFrom: String): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <imdn xmlns="$IMDN_NAMESPACE">
                <message-id>$messageId</message-id>
                <datetime>${getCurrentDateTime()}</datetime>
                <recipient-uri>$originalFrom</recipient-uri>
                <delivery-notification>
                    <status>
                        <delivered/>
                    </status>
                </delivery-notification>
            </imdn>
        """.trimIndent()
    }

    fun buildDisplayedNotification(messageId: String, originalFrom: String): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <imdn xmlns="$IMDN_NAMESPACE">
                <message-id>$messageId</message-id>
                <datetime>${getCurrentDateTime()}</datetime>
                <recipient-uri>$originalFrom</recipient-uri>
                <display-notification>
                    <status>
                        <displayed/>
                    </status>
                </display-notification>
            </imdn>
        """.trimIndent()
    }

    private fun extractXmlValue(xml: String, tagName: String): String? {
        val pattern = Regex("<$tagName>([^<]+)</$tagName>")
        return pattern.find(xml)?.groupValues?.get(1)
    }

    private fun getCurrentDateTime(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}

data class ImdnNotification(
    val messageId: String?,
    val status: ImdnStatus,
    val dateTime: String?
)

enum class ImdnStatus {
    DELIVERED,
    DISPLAYED,
    FAILED,
    FORBIDDEN,
    ERROR,
    UNKNOWN
}
