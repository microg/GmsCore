/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.graphics.Point
import com.google.mlkit.vision.barcode.internal.Address
import com.google.mlkit.vision.barcode.internal.Barcode
import com.google.mlkit.vision.barcode.internal.CalendarDateTime
import com.google.mlkit.vision.barcode.internal.CalendarEvent
import com.google.mlkit.vision.barcode.internal.ContactInfo
import com.google.mlkit.vision.barcode.internal.Email
import com.google.mlkit.vision.barcode.internal.GeoPoint
import com.google.mlkit.vision.barcode.internal.ImageMetadata
import com.google.mlkit.vision.barcode.internal.Phone
import com.google.mlkit.vision.barcode.internal.Sms
import com.google.mlkit.vision.barcode.internal.UrlBookmark
import com.google.mlkit.vision.barcode.internal.WiFi
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult
import java.util.Calendar
import java.util.Date
import kotlin.collections.mapIndexed
import kotlin.collections.orEmpty
import kotlin.text.split

fun Int.mlKitToZXingBarcodeFormats(): List<BarcodeFormat> {
    return listOfNotNull(
        BarcodeFormat.AZTEC.takeIf { (this and Barcode.AZTEC) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.CODABAR.takeIf { (this and Barcode.CODABAR) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.CODE_39.takeIf { (this and Barcode.CODE_39) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.CODE_93.takeIf { (this and Barcode.CODE_93) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.CODE_128.takeIf { (this and Barcode.CODE_128) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.DATA_MATRIX.takeIf { (this and Barcode.DATA_MATRIX) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.EAN_8.takeIf { (this and Barcode.EAN_8) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.EAN_13.takeIf { (this and Barcode.EAN_13) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.ITF.takeIf { (this and Barcode.ITF) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.PDF_417.takeIf { (this and Barcode.PDF417) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.QR_CODE.takeIf { (this and Barcode.QR_CODE) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.UPC_A.takeIf { (this and Barcode.UPC_A) > 0 || this == Barcode.ALL_FORMATS },
        BarcodeFormat.UPC_E.takeIf { (this and Barcode.UPC_E) > 0 || this == Barcode.ALL_FORMATS },
    )
}

fun BarcodeFormat.toMlKit(): Int = when (this) {
    BarcodeFormat.AZTEC -> Barcode.AZTEC
    BarcodeFormat.CODABAR -> Barcode.CODABAR
    BarcodeFormat.CODE_39 -> Barcode.CODE_39
    BarcodeFormat.CODE_93 -> Barcode.CODE_93
    BarcodeFormat.CODE_128 -> Barcode.CODE_128
    BarcodeFormat.DATA_MATRIX -> Barcode.DATA_MATRIX
    BarcodeFormat.EAN_8 -> Barcode.EAN_8
    BarcodeFormat.EAN_13 -> Barcode.EAN_13
    BarcodeFormat.ITF -> Barcode.ITF
    BarcodeFormat.PDF_417 -> Barcode.PDF417
    BarcodeFormat.QR_CODE -> Barcode.QR_CODE
    BarcodeFormat.UPC_A -> Barcode.UPC_A
    BarcodeFormat.UPC_E -> Barcode.UPC_E
    else -> Barcode.UNKNOWN_FORMAT
}

fun ParsedResultType.toMlKit(): Int = when (this) {
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
    else -> Barcode.UNKNOWN_TYPE
}

fun AddressBookParsedResult.toMlKit(): ContactInfo {
    val contactInfo = ContactInfo()
    // TODO: contactInfo.name
    contactInfo.organization = org
    contactInfo.title = title
    contactInfo.phones = phoneNumbers.orEmpty().mapIndexed { i, a ->
        Phone().apply {
            type = when (phoneTypes?.getOrNull(i)) {
                "WORK" -> Phone.WORK
                "HOME" -> Phone.HOME
                "FAX" -> Phone.FAX
                "MOBILE" -> Phone.MOBILE
                else -> Phone.UNKNOWN
            }
            number = a
        }
    }.toTypedArray()
    contactInfo.emails = emails.orEmpty().mapIndexed { i, a ->
        Email().apply {
            type = when (emailTypes?.getOrNull(i)) {
                "WORK" -> Email.WORK
                "HOME" -> Email.HOME
                else -> Email.UNKNOWN
            }
            address = a
        }
    }.toTypedArray()
    contactInfo.urls = urLs
    contactInfo.addresses = addresses.orEmpty().mapIndexed { i, a ->
        Address().apply {
            type = when (addressTypes?.getOrNull(i)) {
                "WORK" -> Address.WORK
                "HOME" -> Address.HOME
                else -> Address.UNKNOWN
            }
            addressLines = a.split("\n").toTypedArray()
        }
    }.toTypedArray()

    return contactInfo
}

fun CalendarParsedResult.toMlKit(): CalendarEvent {
    fun createDateTime(timestamp: Long, isAllDay: Boolean) = CalendarDateTime().apply {
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


    val event = CalendarEvent()
    event.summary = summary
    event.description = description
    event.location = location
    event.organizer = organizer
    event.start = createDateTime(startTimestamp, isStartAllDay)
    event.end = createDateTime(endTimestamp, isEndAllDay)
    return event
}

fun EmailAddressParsedResult.toMlKit(): Email {
    val email = Email()
    email.address = tos?.getOrNull(0)
    email.subject = subject
    email.body = body
    return email
}

fun GeoParsedResult.toMlKit(): GeoPoint {
    val geo = GeoPoint()
    geo.lat = latitude
    geo.lng = longitude
    return geo
}

fun TelParsedResult.toMlKit(): Phone {
    val phone = Phone()
    phone.number = number
    return phone
}

fun SMSParsedResult.toMlKit(): Sms {
    val sms = Sms()
    sms.message = body
    sms.phoneNumber = numbers?.getOrNull(0)
    return sms
}

fun WifiParsedResult.toMlKit(): WiFi {
    val wifi = WiFi()
    wifi.ssid = ssid
    wifi.password = password
    wifi.encryptionType = when (networkEncryption) {
        "OPEN" -> WiFi.OPEN
        "WEP" -> WiFi.WEP
        "WPA" -> WiFi.WPA
        "WPA2" -> WiFi.WPA
        else -> 0
    }
    return wifi
}

fun URIParsedResult.toMlKit(): UrlBookmark {
    val url = UrlBookmark()
    url.url = uri
    url.title = title
    return url
}

fun Result.toMlKit(metadata: ImageMetadata? = null): Barcode {
    val barcode = Barcode()
    barcode.format = barcodeFormat.toMlKit()
    barcode.rawBytes = rawBytes
    barcode.rawValue = text
    barcode.cornerPoints = resultPoints.map {
        when (metadata?.rotation ?: -1) {
            1 -> Point(metadata!!.height - it.y.toInt(), it.x.toInt())
            2 -> Point(metadata!!.width - it.x.toInt(), metadata.height - it.y.toInt())
            3 -> Point(it.y.toInt(), metadata!!.width - it.x.toInt())
            else -> Point(it.x.toInt(), it.y.toInt())
        }
    }.toTypedArray()

    val parsed = ResultParser.parseResult(this)

    barcode.displayValue = parsed.displayResult
    barcode.valueType = parsed.type.toMlKit()
    when (parsed) {
        is EmailAddressParsedResult -> barcode.email = parsed.toMlKit()
        is TelParsedResult -> barcode.phone = parsed.toMlKit()
        is SMSParsedResult -> barcode.sms = parsed.toMlKit()
        is WifiParsedResult -> barcode.wifi = parsed.toMlKit()
        is URIParsedResult -> barcode.urlBookmark = parsed.toMlKit()
        is GeoParsedResult -> barcode.geoPoint = parsed.toMlKit()
        is CalendarParsedResult -> barcode.calendarEvent = parsed.toMlKit()
        is AddressBookParsedResult -> barcode.contactInfo = parsed.toMlKit()
    }
    return barcode
}