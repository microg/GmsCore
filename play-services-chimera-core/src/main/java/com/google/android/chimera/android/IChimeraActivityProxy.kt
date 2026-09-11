/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.android

interface IChimeraActivityProxy : IActivityProxy {
    fun getChimeraActivity(): Activity
    fun clearFeatureRequest()
    fun hasFeatureRequest(): Boolean
    fun getSystemService(serviceName: String): Any?
}
