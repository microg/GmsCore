/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.udc

private enum class Controls(val id: Int) {
    APP_USAGE_TIME(1),
    SCREEN_TIME_LIMIT(2),
    APP_INSTALL(3),
    IN_APP_PURCHASE(4),
    LOCATION_SHARING(5),
    CALL_LOG(6),
    CONTACT(9),
    CALENDAR(10),
    CAMERA(15),
    MICROPHONE(17),
    STORAGE(18),
    NOTIFICATION(44),
    ACCESSIBILITY_SERVICE(45),
    DEVICE_ADMIN(46),
    SYSTEM_WINDOW_OVERLAY(48),
    SYSTEM_ALERT_WINDOW(61),
    FLOATING_WINDOW(80),
    PICTURE_IN_PICTURE(94),
    SYSTEM_UI_VISIBILITY(95),
    BACKGROUND_APP_RESTRICTION(101),
    BATTERY_OPTIMIZATION(102)
}

private val ALLOW_CONTROLS_PACKAGES = mapOf(
    Pair("com.google.android.googlequicksearchbox", setOf(Controls.STORAGE, Controls.MICROPHONE, Controls.SYSTEM_WINDOW_OVERLAY)),
)

fun getAllowControlsByPackage(packageName: String) : ByteArray? {
    val controls = ALLOW_CONTROLS_PACKAGES[packageName] ?: return null
    return ActivityControlsSettings.build {
        items(controls.map { controls -> ActivityControlItem(controls.id, 1, 1, 1) })
        global(GlobalSettings(1, 1))
    }.encode()
}
