/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.internal.client.BarcodeDetectorOptions
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetector
import com.google.android.gms.vision.internal.FrameMetadataParcel
import com.google.zxing.*
import com.google.zxing.client.result.*
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

private const val TAG = "BarcodeDetector"

class BarcodeDetector(val context: Context, val options: BarcodeDetectorOptions) : INativeBarcodeDetector.Stub() {
    private val helper = BarcodeDecodeHelper(options.formats.gmsToZXingBarcodeFormats())
    private var loggedOnce = false

    override fun detectBitmap(wrappedBitmap: IObjectWrapper, metadata: FrameMetadataParcel): Array<Barcode> {
        if (!loggedOnce) Log.d(TAG, "detectBitmap(${ObjectWrapper.unwrap(wrappedBitmap)}, $metadata)").also { loggedOnce = true }
        val bitmap = wrappedBitmap.unwrap<Bitmap>() ?: return emptyArray()
        return helper.decodeFromBitmap(bitmap)
            .mapNotNull { runCatching { it.toGms(metadata) }.getOrNull() }.toTypedArray()
    }

    override fun detectBytes(wrappedByteBuffer: IObjectWrapper, metadata: FrameMetadataParcel): Array<Barcode> {
        if (!loggedOnce) Log.d(TAG, "detectBytes(${ObjectWrapper.unwrap(wrappedByteBuffer)}, $metadata)").also { loggedOnce = true }
        val bytes = wrappedByteBuffer.unwrap<ByteBuffer>() ?: return emptyArray()
        return helper.decodeFromLuminanceBytes(bytes, metadata.width, metadata.height, metadata.rotation)
            .mapNotNull { runCatching { it.toGms(metadata) }.getOrNull() }.toTypedArray()
    }

    override fun close() {
        Log.d(TAG, "close()")
    }
}

private fun Int.gmsToZXingBarcodeFormats(): List<BarcodeFormat> {
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


private fun BarcodeFormat.toGms(): Int = when (this) {
    BarcodeFormat.AZTEC -> Barcode.AZTEC
    BarcodeFormat.CODABAR -> Barcode.CODABAR
    BarcodeFormat.CODE_39 -> Barcode.CODE_39
    BarcodeFormat.CODE_93 -> Barcode.CODE_93
    BarcodeFormat.CODE_128 -> Barcode.CODE_128
    BarcodeFormat.DATA_MATRIX -> Barcode.DATA_MATRIX
    BarcodeFormat.EAN_13 -> Barcode.EAN_13
    BarcodeFormat.EAN_8 -> Barcode.EAN_8
    BarcodeFormat.ITF -> Barcode.ITF
    BarcodeFormat.PDF_417 -> Barcode.PDF417
    BarcodeFormat.QR_CODE -> Barcode.QR_CODE
    BarcodeFormat.UPC_A -> Barcode.UPC_A
    BarcodeFormat.UPC_E -> Barcode.UPC_E
    else -> throw UnsupportedOperationException()
}

private fun ParsedResultType.toGms(): Int = when (this) {
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

private fun AddressBookParsedResult.toGms(): Barcode.ContactInfo {
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

private fun CalendarParsedResult.toGms(): Barcode.CalendarEvent {
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

private fun EmailAddressParsedResult.toGms(): Barcode.Email {
    val email = Barcode.Email()
    email.address = tos?.getOrNull(0)
    email.subject = subject
    email.body = body
    return email
}

private fun GeoParsedResult.toGms(): Barcode.GeoPoint {
    val geo = Barcode.GeoPoint()
    geo.lat = latitude
    geo.lng = longitude
    return geo
}

private fun TelParsedResult.toGms(): Barcode.Phone {
    val phone = Barcode.Phone()
    phone.number = number
    return phone
}

private fun SMSParsedResult.toGms(): Barcode.Sms {
    val sms = Barcode.Sms()
    sms.message = body
    sms.phoneNumber = numbers?.getOrNull(0)
    return sms
}

private fun WifiParsedResult.toGms(): Barcode.WiFi {
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

private fun URIParsedResult.toGms(): Barcode.UrlBookmark {
    val url = Barcode.UrlBookmark()
    url.url = uri
    url.title = title
    return url
}

private fun Result.toGms(metadata: FrameMetadataParcel): Barcode {
    val barcode = Barcode()
    barcode.format = barcodeFormat.toGms()
    barcode.rawBytes = rawBytes
    barcode.rawValue = text
    barcode.cornerPoints = resultPoints.map {
        Point(it.x.toInt(), it.y.toInt())
    }.toTypedArray()

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