package org.microg.vending.enterprise

open class App(
    val packageName: String,
    val versionCode: Int?,
    val displayName: String,
    val state: State,
    val iconUrl: String?,
    val deliveryToken: String?
) {
    enum class State {
        /**
         * App cannot be installed on this user's device
         */
        NOT_COMPATIBLE,
        /**
        * App is available, but not installed on the user's device.
         */
        NOT_INSTALLED,
        /**
         * App is already installed on the device, but an update is available.
         */
        UPDATE_AVAILABLE,

        /**
         * App is installed on device and up to date.
         */
        INSTALLED
    }
}