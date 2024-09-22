package org.microg.vending.enterprise

enum class AppState {
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
     * An app operation is currently outstanding
     */
    PENDING,
    /**
     * App is installed on device and up to date.
     */
    INSTALLED
}