/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.wifi.WifiDetails

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
    psc = psc,
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

internal fun JSONObject.toGeolocateResponse() = GeolocateResponse(
    location = optJSONObject("location")?.toResponseLocation(),
    accuracy = optDouble("accuracy"),
    fallback = optString("fallback").takeIf { it.isNotEmpty() },
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
    lat = getDouble("lat"),
    lng = getDouble("lng")
)

private fun JSONObject.toResponseError() = ResponseError(
    code = getInt("code"),
    message = getString("message")
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

private fun Fallback.toJson() = JSONObject().apply {
    if (lacf != null) put("lacf", lacf)
    if (ipf != null) put("ipf", ipf)
}