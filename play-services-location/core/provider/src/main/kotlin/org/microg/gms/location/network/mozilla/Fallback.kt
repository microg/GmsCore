/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.mozilla

/**
 * By default, both a GeoIP based position fallback and a fallback based on cell location areas (lac’s) are enabled. Omit the fallbacks section if you want to use the defaults. Change the values to false if you want to disable either of the fallbacks.
 */
data class Fallback(
    /**
     * If no exact cell match can be found, fall back from exact cell position estimates to more coarse grained cell location area estimates rather than going directly to an even worse GeoIP based estimate.
     */
    val lacf: Boolean? = null,
    /**
     * If no position can be estimated based on any of the provided data points, fall back to an estimate based on a GeoIP database based on the senders IP address at the time of the query.
     */
    val ipf: Boolean? = null
)