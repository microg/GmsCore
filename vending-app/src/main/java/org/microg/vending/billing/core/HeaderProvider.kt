package org.microg.vending.billing.core

object HeaderProvider {
    fun getBaseHeaders(authData: AuthData, deviceInfo: DeviceEnvInfo): MutableMap<String, String> {
        val headers: MutableMap<String, String> = HashMap()
        headers["Authorization"] = "Bearer " + authData.authToken
        headers["User-Agent"] = deviceInfo.userAgent
        return headers
    }

    fun getDefaultHeaders(authData: AuthData, deviceInfo: DeviceEnvInfo): MutableMap<String, String> {
        val headers: MutableMap<String, String> = HashMap()
        headers["Authorization"] = "Bearer " + authData.authToken
        headers["User-Agent"] = deviceInfo.userAgent
        headers["X-DFE-Device-Id"] = authData.gsfId
        headers["Accept-Language"] = "${deviceInfo.locale.language}-${deviceInfo.locale.country}"
        headers["X-Limit-Ad-Tracking-Enabled"] = "true"
        headers["X-DFE-Network-Type"] = "4"
        headers["X-DFE-Client-Id"] = "am-google"

        // TODO: Magic constants?
        headers["X-DFE-Phenotype"] =
            "H4sIAAAAAAAAAOOKcXb0DQ4oNzCoKNV1c0zMsywL9PVwqvBPcsr2TykJ8HUv9gx1La6I9Dcw9k7xTYtIMnasSopIq0g0SI8IdwxwDbfIygxw8U-PdPR1THML1DXNS_L0yffOinRxtLWVYgAAjtXkomAAAAA"
        headers["X-DFE-Encoded-Targets"] =
            "CAEaSuMFBdCPgQYJxAIED+cBfS+6AVYBIQojDSI3hAEODGxYvQGMAhRMWQEVWxniBQSSAjycAuESkgrgBeAfgCv4KI8VgxHqGNxrRbkI"

        headers["X-DFE-Request-Params"] = "timeoutMs=4000"
        headers["X-Ad-Id"] = "00000000-0000-0000-0000-000000000000"
        headers["Connection"] = "Keep-Alive"
        if (deviceInfo.androidId.isNotBlank())
            headers["x-public-android-id"] = deviceInfo.androidId
        if (authData.dfeCookie.isNotBlank())
            headers["x-dfe-cookie"] = authData.dfeCookie
        if (authData.deviceCheckInConsistencyToken.isNotBlank()) {
            headers["X-DFE-Device-Checkin-Consistency-Token"] =
                authData.deviceCheckInConsistencyToken
        }
        return headers
    }
}