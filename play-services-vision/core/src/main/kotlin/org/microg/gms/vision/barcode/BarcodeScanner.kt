/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.media.Image
import android.os.Build.VERSION.SDK_INT
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.barcode.aidls.IBarcodeScanner
import com.google.mlkit.vision.barcode.internal.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.*
import org.microg.gms.utils.warnOnTransactionIssues
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "BarcodeScanner"

class BarcodeScanner(val context: Context, val options: BarcodeScannerOptions) : IBarcodeScanner.Stub() {
    private val helper =
        BarcodeDecodeHelper(if (options.allPotentialBarcodesEnabled) BarcodeFormat.values().toList() else options.supportedFormats.mlKitToZXingBarcodeFormats())
    private var loggedOnce = false

    override fun init() {
        Log.d(TAG, "init()")
    }

    override fun close() {
        Log.d(TAG, "close()")
    }

    override fun detect(wrappedImage: IObjectWrapper, metadata: ImageMetadata): List<Barcode> {
        if (!loggedOnce) Log.d(TAG, "detect(${ObjectWrapper.unwrap(wrappedImage)}, $metadata)").also { loggedOnce = true }
        return when (metadata.format) {
            ImageFormat.NV21 -> wrappedImage.unwrap<ByteBuffer>()?.let { helper.decodeFromLuminanceBytes(it, metadata.width, metadata.height) }
            ImageFormat.YUV_420_888 -> if (SDK_INT >= 19) wrappedImage.unwrap<Image>()?.let { image -> helper.decodeFromImage(image) } else null

            else -> null
        }?.map { it.toMlKit(metadata) } ?: emptyList()
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}

private fun Int.mlKitToZXingBarcodeFormats(): List<BarcodeFormat> {
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

private fun BarcodeFormat.toMlKit(): Int = when (this) {
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

private fun ParsedResultType.toMlKit(): Int = when (this) {
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

private fun AddressBookParsedResult.toMlKit(): ContactInfo {
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

private fun CalendarParsedResult.toMlKit(): CalendarEvent {
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

private fun EmailAddressParsedResult.toMlKit(): Email {
    val email = Email()
    email.address = tos?.getOrNull(0)
    email.subject = subject
    email.body = body
    return email
}

private fun GeoParsedResult.toMlKit(): GeoPoint {
    val geo = GeoPoint()
    geo.lat = latitude
    geo.lng = longitude
    return geo
}

private fun TelParsedResult.toMlKit(): Phone {
    val phone = Phone()
    phone.number = number
    return phone
}

private fun SMSParsedResult.toMlKit(): Sms {
    val sms = Sms()
    sms.message = body
    sms.phoneNumber = numbers?.getOrNull(0)
    return sms
}

private fun WifiParsedResult.toMlKit(): WiFi {
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


private fun URIParsedResult.toMlKit(): UrlBookmark {
    val url = UrlBookmark()
    url.url = uri
    url.title = title
    return url
}

private fun Result.toMlKit(metadata: ImageMetadata): Barcode {
    val barcode = Barcode()
    barcode.format = barcodeFormat.toMlKit()
    barcode.rawBytes = rawBytes
    barcode.rawValue = text
    barcode.cornerPoints = resultPoints.map {
        when (metadata.rotation) {
            1 -> Point(metadata.height - it.y.toInt(), it.x.toInt())
            2 -> Point(metadata.width - it.x.toInt(), metadata.height - it.y.toInt())
            3 -> Point(it.y.toInt(), metadata.width - it.x.toInt())
            else -> Point(it.x.toInt(), it.y.toInt())
        }
    }.toTypedArray()

    val parsed = ResultParser.parseResult(this)

    barcode.displayValue = parsed.displayResult
    barcode.valueType = parsed.type.toMlKit()
    when (parsed) {
        is EmailAddressParsedResult ->
            barcode.email = parsed.toMlKit()

        is TelParsedResult ->
            barcode.phone = parsed.toMlKit()

        is SMSParsedResult ->
            barcode.sms = parsed.toMlKit()

        is WifiParsedResult ->
            barcode.wifi = parsed.toMlKit()

        is URIParsedResult ->
            barcode.urlBookmark = parsed.toMlKit()

        is GeoParsedResult ->
            barcode.geoPoint = parsed.toMlKit()

        is CalendarParsedResult ->
            barcode.calendarEvent = parsed.toMlKit()

        is AddressBookParsedResult ->
            barcode.contactInfo = parsed.toMlKit()
    }

    return barcode
}