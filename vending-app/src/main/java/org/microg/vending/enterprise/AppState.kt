/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

internal sealed interface AppState

/**
 * App cannot be installed on this user's device
 */
internal data object NotCompatible : AppState

/**
 * App is available, but not installed on the user's device.
 */
internal data object NotInstalled : AppState

/**
 * App is already installed on the device, but an update is available.
 */
internal data object UpdateAvailable : AppState

/**
 * An unspecific app operation is currently outstanding
 */
internal data object Pending : AppState

/**
 * App is installed on device and up to date.
 */
internal data object Installed : AppState