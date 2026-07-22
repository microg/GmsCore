/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.wifi.WifiDetails

private fun JSONObject.getDouble(vararg names: String): Double {
    for (name in names) {
        if (has(name)) return getDouble(name)
    }
    throw JSONException("Values are all null: ${names.joinToString(", ")}")
}

private fun JSONObject.getInt(vararg names: String): Int {
    for (name in names) {
        if (has(name)) return getInt(name)
    }
    throw JSONException("Values are all null: ${names.joinToString(", ")}")
}

private fun JSONObject.getString(vararg names: String): String {
    for (name in names) {
        if (has(name)) return getString(name)
    }
    throw JSONException("Values are all null: ${names.joinToString(", ")}")
}

private fun JSONObject.optDouble(vararg names: String): Double? {
    for (name in names) {
        if (has(name)) optDouble(name).takeIf { it.isFinite() }?.let { return it }
    }
    return null
}

private fun JSONObject.optInt(vararg names: String): Int? {
    for (name in names) {
        runCatching { if (has(name)) return getInt(name) }
    }
    return null
}

private fun JSONObject.optLong(vararg names: String): Long? {
    for (name in names) {
        runCatching { if (has(name)) return getLong(name) }
    }
    return null
}

private fun JSONObject.optString(vararg names: String): String? {
    for (name in names) {
        if (has(name)) return optString(name)
    }
    return null
}

internal fun CellDetails.toCellTower() = CellTower(
    radioType = when (type) {
        CellDetails.Companion.Type.GSM -> RadioType.GSM
        CellDetails.Companion.Type.WCDMA -> RadioType.WCDMA
        CellDetails.Companion.Type.LTE -> RadioType.LTE
        else -> throw IllegalArgumentException("Unsupported radio type")
    },
    mobileCountryCode = mcc,
    mobileNetworkCode = mnc,
    locationAreaCode = lac ?: tac,
    cellId = cid?.toInt(),
    age = timestamp?.let { System.currentTimeMillis() - it },
    psc = pscOrPci,
    signalStrength = signalStrength
)

internal fun CellTower.toCellDetails() = CellDetails(
    type = when(radioType) {
        RadioType.GSM -> CellDetails.Companion.Type.GSM
        RadioType.WCDMA -> CellDetails.Companion.Type.WCDMA
        RadioType.LTE -> CellDetails.Companion.Type.LTE
        else -> throw IllegalArgumentException("Unsupported radio type")
    },
    mcc = mobileNetworkCode,
    mnc = mobileNetworkCode,
    lac = locationAreaCode,
    tac = locationAreaCode,
    cid = cellId?.toLong(),
    pscOrPci = psc,
    signalStrength = signalStrength
)

internal fun WifiDetails.toWifiAccessPoint() = WifiAccessPoint(
    macAddress = macAddress,
    age = timestamp?.let { System.currentTimeMillis() - it },
    channel = channel,
    frequency = frequency,
    signalStrength = signalStrength,
    ssid = ssid
)

internal fun WifiAccessPoint.toWifiDetails() = WifiDetails(
    macAddress = macAddress,
    channel = channel,
    frequency = frequency,
    signalStrength = signalStrength,
    ssid = ssid,
)

internal fun JSONObject.toGeolocateResponse() = GeolocateResponse(
    location = optJSONObject("location")?.toResponseLocation(),
    horizontalAccuracy = optDouble("accuracy", "acc", "horizontalAccuracy"),
    verticalAccuracy = optDouble("altAccuracy", "altitudeAccuracy", "verticalAccuracy"),
    fallback = optString("fallback").takeIf { it.isNotEmpty() },
    raw = optJSONArray("raw")?.toRawGeolocateEntries().orEmpty(),
    error = optJSONObject("error")?.toResponseError()
)

internal fun GeolocateRequest.toJson() = JSONObject().apply {
    if (carrier != null) put("carrier", carrier)
    if (considerIp != null) put("considerIp", considerIp)
    if (homeMobileCountryCode != null) put("homeMobileCountryCode", homeMobileCountryCode)
    if (homeMobileNetworkCode != null) put("homeMobileNetworkCode", homeMobileNetworkCode)
    if (radioType != null) put("radioType", radioType.toString())
    if (!bluetoothBeacons.isNullOrEmpty()) put("bluetoothBeacons", JSONArray(bluetoothBeacons.map(BluetoothBeacon::toJson)))
    if (!cellTowers.isNullOrEmpty()) put("cellTowers", JSONArray(cellTowers.map(CellTower::toJson)))
    if (!wifiAccessPoints.isNullOrEmpty()) put("wifiAccessPoints", JSONArray(wifiAccessPoints.map(WifiAccessPoint::toJson)))
    if (fallbacks != null) put("fallbacks", fallbacks.toJson())
}

internal fun GeosubmitRequest.toJson() = JSONObject().apply {
    if (items != null) put("items", JSONArray(items.map(GeosubmitItem::toJson)))
}

private fun GeosubmitItem.toJson() = JSONObject().apply {
    if (timestamp != null) put("timestamp", timestamp)
    if (position != null) put("position", position.toJson())
    if (!bluetoothBeacons.isNullOrEmpty()) put("bluetoothBeacons", JSONArray(bluetoothBeacons.map(BluetoothBeacon::toJson)))
    if (!cellTowers.isNullOrEmpty()) put("cellTowers", JSONArray(cellTowers.map(CellTower::toJson)))
    if (!wifiAccessPoints.isNullOrEmpty()) put("wifiAccessPoints", JSONArray(wifiAccessPoints.map(WifiAccessPoint::toJson)))
}

private fun GeosubmitPosition.toJson() = JSONObject().apply {
    if (latitude != null) put("latitude", latitude)
    if (longitude != null) put("longitude", longitude)
    if (accuracy != null) put("accuracy", accuracy)
    if (altitude != null) put("altitude", altitude)
    if (altitudeAccuracy != null) put("altitudeAccuracy", altitudeAccuracy)
    if (age != null) put("age", age)
    if (heading != null) put("heading", heading)
    if (pressure != null) put("pressure", pressure)
    if (speed != null) put("speed", speed)
    if (source != null) put("source", source.toString())
}

private fun JSONObject.toResponseLocation() = ResponseLocation(
    latitude = getDouble("lat", "latitude"),
    longitude = getDouble("lng", "longitude"),
    altitude = optDouble("alt", "altitude"),
    horizontalAccuracy = optDouble("acc", "accuracy", "horizontalAccuracy"),
    verticalAccuracy = optDouble("altAccuracy", "altitudeAccuracy", "verticalAccuracy"),
)

private fun JSONObject.toResponseError() = ResponseError(
    code = optInt("code", "errorCode", "statusCode"),
    message = optString("message", "msg", "statusText")
)

private fun JSONArray.toRawGeolocateEntries(): List<RawGeolocateEntry> =
    (0 until length()).mapNotNull { optJSONObject(it)?.toRawGeolocateEntry() }

private fun JSONObject.toRawGeolocateEntry() = RawGeolocateEntry(
    timestamp = optLong("time", "timestamp"),
    bluetoothBeacon = optJSONObject("bluetoothBeacon")?.toBluetoothBeacon(),
    cellTower = optJSONObject("cellTower")?.toCellTower(),
    wifiAccessPoint = optJSONObject("wifiAccessPoint")?.toWifiAccessPoint(),
    location = optJSONObject("location")?.toResponseLocation(),
    horizontalAccuracy = optDouble("acc", "accuracy", "horizontalAccuracy"),
    verticalAccuracy = optDouble("altAccuracy", "altitudeAccuracy", "verticalAccuracy"),
    omit = optBoolean("omit")
)

private fun JSONObject.toBluetoothBeacon() = BluetoothBeacon(
    macAddress = getString("macAddress", "mac", "address"),
    name = optString("name"),
)

private fun JSONObject.toCellTower() = CellTower(
    radioType = optString("radioType", "radio", "type")?.let { runCatching { RadioType.valueOf(it.uppercase()) }.getOrNull() },
    mobileCountryCode = optInt("mobileCountryCode", "mcc"),
    mobileNetworkCode = optInt("mobileNetworkCode", "mnc"),
    locationAreaCode = optInt("locationAreaCode", "lac", "trackingAreaCode", "tac"),
    cellId = optInt("cellId", "cellIdentity", "cid"),
    psc = optInt("psc", "primaryScramblingCode", "physicalCellId", "pci"),
)

private fun JSONObject.toWifiAccessPoint() = WifiAccessPoint(
    macAddress = getString("macAddress", "mac", "bssid", "address"),
    channel = optInt("channel", "chan"),
    frequency = optInt("frequency", "freq"),
    ssid = optString("ssid"),
)

private fun BluetoothBeacon.toJson() = JSONObject().apply {
    if (macAddress != null) put("macAddress", macAddress)
    if (name != null) put("name", name)
    if (age != null) put("age", age)
    if (signalStrength != null) put("signalStrength", signalStrength)
}

private fun CellTower.toJson() = JSONObject().apply {
    if (radioType != null) put("radioType", radioType.toString())
    if (mobileCountryCode != null) put("mobileCountryCode", mobileCountryCode)
    if (mobileNetworkCode != null) put("mobileNetworkCode", mobileNetworkCode)
    if (locationAreaCode != null) put("locationAreaCode", locationAreaCode)
    if (cellId != null) put("cellId", cellId)
    if (age != null) put("age", age)
    if (psc != null) put("psc", psc)
    if (signalStrength != null) put("signalStrength", signalStrength)
    if (timingAdvance != null) put("timingAdvance", timingAdvance)
}

private fun WifiAccessPoint.toJson() = JSONObject().apply {
    put("macAddress", macAddress)
    if (age != null) put("age", age)
    if (channel != null) put("channel", channel)
    if (frequency != null) put("frequency", frequency)
    if (signalStrength != null) put("signalStrength", signalStrength)
    if (signalToNoiseRatio != null) put("signalToNoiseRatio", signalToNoiseRatio)
    if (ssid != null) put("ssid", ssid)
}

val WifiAccessPoint.macClean: String
    get() = macAddress.lowercase().replace(":", "")

val WifiAccessPoint.macBytes: ByteArray
    get() {
        val mac = macClean
        return byteArrayOf(
            mac.substring(0, 2).toInt(16).toByte(),
            mac.substring(2, 4).toInt(16).toByte(),
            mac.substring(4, 6).toInt(16).toByte(),
            mac.substring(6, 8).toInt(16).toByte(),
            mac.substring(8, 10).toInt(16).toByte(),
            mac.substring(10, 12).toInt(16).toByte()
        )
    }

val GeolocateRequest.isWifiOnly: Boolean
    get() = bluetoothBeacons.isNullOrEmpty() && cellTowers.isNullOrEmpty() && wifiAccessPoints?.isNotEmpty() == true

val GeolocateRequest.isCellOnly: Boolean
    get() = bluetoothBeacons.isNullOrEmpty() && wifiAccessPoints.isNullOrEmpty() && cellTowers?.isNotEmpty() == true

private fun Fallback.toJson() = JSONObject().apply {
    if (lacf != null) put("lacf", lacf)
    if (ipf != null) put("ipf", ipf)
}