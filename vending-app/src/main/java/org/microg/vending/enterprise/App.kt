package org.microg.vending.enterprise

open class App(
    val packageName: String,
    val displayName: String,
    val state: State,
    val iconUrl: String?
) {
    enum class State {
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