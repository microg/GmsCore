/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import org.microg.gms.checkin.DeviceConfig

fun DeviceConfiguration.asProto(): DeviceConfig = DeviceConfig(
    availableFeature = availableFeatures,
    densityDpi = densityDpi,
    glEsVersion = glEsVersion,
    glExtension = glExtensions,
    hasFiveWayNavigation = hasFiveWayNavigation,
    hasHardKeyboard = hasHardKeyboard,
    heightPixels = heightPixels,
    keyboardType = keyboardType,
    locale = locales,
    nativePlatform = nativePlatforms,
    navigation = navigation,
    screenLayout = screenLayout,
    sharedLibrary = sharedLibraries,
    touchScreen = touchScreen,
    widthPixels = widthPixels
)