/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.graphics.Point
import com.google.android.gms.vision.barcode.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.client.result.*
import java.util.*

fun BarcodeFormat.toGms(): Int = when (this) {
    BarcodeFormat.AZTEC -> Barcode.AZTEC
    BarcodeFormat.CODABAR -> Barcode.CODABAR
    BarcodeFormat.CODE_128 -> Barcode.CODE_128
    BarcodeFormat.CODE_39 -> Barcode.CODE_39
    BarcodeFormat.CODE_93 -> Barcode.CODE_93
    BarcodeFormat.DATA_MATRIX -> Barcode.DATA_MATRIX
    BarcodeFormat.EAN_13 -> Barcode.EAN_13
    BarcodeFormat.EAN_8 -> Barcode.EAN_8
    BarcodeFormat.ITF -> Barcode.ITF
    BarcodeFormat.PDF_417 -> Barcode.PDF417
    BarcodeFormat.QR_CODE -> Barcode.QR_CODE
    BarcodeFormat.UPC_A -> Barcode.UPC_A
    BarcodeFormat.UPC_E -> Barcode.UPC_E
    else -> Barcode.ALL_FORMATS
}

fun ParsedResultType.toGms(): Int = when (this) {
    ParsedResultType.ADDRESSBOOK -> Barcode.CONTACT_INFO
    ParsedResultType.CALENDAR -> Barcode.CALENDAR_EVENT
    ParsedResultType.EMAIL_ADDRESS -> Barcode.EMAIL
    ParsedResultType.GEO -> Barcode.GEO
    ParsedResultType.ISBN -> Barcode.ISBN
    ParsedResultType.PRODUCT -> Barcode.PRODUCT
    ParsedResultType.SMS -> Barcode.SMS
    ParsedResultType.TEL -> Barcode.PHONE
    ParsedResultType.TEXT -> Barcode.TEXT
    ParsedResultType.URI -> Barcode.URL
    ParsedResultType.WIFI -> Barcode.WIFI
    else -> Barcode.TEXT
}

fun AddressBookParsedResult.toGms(): Barcode.ContactInfo {
    val contactInfo = Barcode.ContactInfo()
    // TODO: contactInfo.name
    contactInfo.organization = org
    contactInfo.title = title
    contactInfo.phones = phoneNumbers.orEmpty().mapIndexed { i, a ->
        Barcode.Phone().apply {
            type = when (phoneTypes?.getOrNull(i)) {
                "WORK" -> Barcode.Phone.WORK
                "HOME" -> Barcode.Phone.HOME
                "FAX" -> Barcode.Phone.FAX
                "MOBILE" -> Barcode.Phone.MOBILE
                else -> Barcode.Phone.UNKNOWN
            }
            number = a
        }
    }.toTypedArray()
    contactInfo.emails = emails.orEmpty().mapIndexed { i, a ->
        Barcode.Email().apply {
            type = when (emailTypes?.getOrNull(i)) {
                "WORK" -> Barcode.Email.WORK
                "HOME" -> Barcode.Email.HOME
                else -> Barcode.Email.UNKNOWN
            }
            address = a
        }
    }.toTypedArray()
    contactInfo.urls = urLs
    contactInfo.addresses = addresses.orEmpty().mapIndexed { i, a ->
        Barcode.Address().apply {
            type = when (addressTypes?.getOrNull(i)) {
                "WORK" -> Barcode.Address.WORK
                "HOME" -> Barcode.Address.HOME
                else -> Barcode.Address.UNKNOWN
            }
            addressLines = a.split("\n").toTypedArray()
        }
    }.toTypedArray()

    return contactInfo
}

fun CalendarParsedResult.toGms(): Barcode.CalendarEvent {
    fun createDateTime(timestamp: Long, isAllDay: Boolean) = Barcode.CalendarDateTime().apply {
        val calendar = Calendar.getInstance()
        calendar.time = Date(timestamp)
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH)
        day = calendar.get(Calendar.DAY_OF_MONTH)
        if (isAllDay) {
            hours = -1
            minutes = -1
            seconds = -1
        } else {
            hours = calendar.get(Calendar.HOUR_OF_DAY)
            minutes = calendar.get(Calendar.MINUTE)
            seconds = calendar.get(Calendar.SECOND)
        }
    }


    val event = Barcode.CalendarEvent()
    event.summary = summary
    event.description = description
    event.location = location
    event.organizer = organizer
    event.start = createDateTime(startTimestamp, isStartAllDay)
    event.end = createDateTime(endTimestamp, isEndAllDay)
    return event
}

fun EmailAddressParsedResult.toGms(): Barcode.Email {
    val email = Barcode.Email()
    email.address = tos?.getOrNull(0)
    email.subject = subject
    email.body = body
    return email
}

fun GeoParsedResult.toGms(): Barcode.GeoPoint {
    val geo = Barcode.GeoPoint()
    geo.lat = latitude
    geo.lng = longitude
    return geo
}

fun TelParsedResult.toGms(): Barcode.Phone {
    val phone = Barcode.Phone()
    phone.number = number
    return phone
}

fun SMSParsedResult.toGms(): Barcode.Sms {
    val sms = Barcode.Sms()
    sms.message = body
    sms.phoneNumber = numbers?.getOrNull(0)
    return sms
}

fun WifiParsedResult.toGms(): Barcode.WiFi {
    val wifi = Barcode.WiFi()
    wifi.ssid = ssid
    wifi.password = password
    wifi.encryptionType = when (networkEncryption) {
        "OPEN" -> Barcode.WiFi.OPEN
        "WEP" -> Barcode.WiFi.WEP
        "WPA" -> Barcode.WiFi.WPA
        "WPA2" -> Barcode.WiFi.WPA
        else -> 0
    }
    return wifi
}

fun URIParsedResult.toGms(): Barcode.UrlBookmark {
    val url = Barcode.UrlBookmark()
    url.url = uri
    url.title = title
    return url
}

fun ResultPoint.toPoint(): Point = Point(x.toInt(), y.toInt())
fun Result.toGms(): Barcode {
    val barcode = Barcode()
    barcode.format = barcodeFormat.toGms()
    barcode.rawBytes = rawBytes
    barcode.rawValue = text
    barcode.cornerPoints = resultPoints.map { it.toPoint() }.toTypedArray()

    val parsed = ResultParser.parseResult(this)

    barcode.displayValue = parsed.displayResult
    barcode.valueFormat = parsed.type.toGms()
    when (parsed) {
        is EmailAddressParsedResult ->
            barcode.email = parsed.toGms()
        is TelParsedResult ->
            barcode.phone = parsed.toGms()
        is SMSParsedResult ->
            barcode.sms = parsed.toGms()
        is WifiParsedResult ->
            barcode.wifi = parsed.toGms()
        is URIParsedResult ->
            barcode.url = parsed.toGms()
        is GeoParsedResult ->
            barcode.geoPoint = parsed.toGms()
        is CalendarParsedResult ->
            barcode.calendarEvent = parsed.toGms()
        is AddressBookParsedResult ->
            barcode.contactInfo = parsed.toGms()
    }

    return barcode
}
