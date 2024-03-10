/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import java.util.Random

// TODO: Use existing code
object DeviceIdentifier {
    var wifiMac = randomMacAddress()
    var meid = randomMeid()
    var serial = randomSerial("008741A0B2C4D6E8")
    var esn: String = ""


    private fun randomMacAddress(): String {
        var mac = "b407f9"
        val rand = Random()
        for (i in 0..5) {
            mac += rand.nextInt(16).toString(16)
        }
        return mac
    }

    private fun randomSerial(
        template: String,
        prefixLength: Int = (template.length / 2).coerceAtMost(6)
    ): String {
        val serial = StringBuilder()
        template.forEachIndexed { index, c ->
            serial.append(
                when {
                    index < prefixLength -> c
                    c.isDigit() -> '0' + kotlin.random.Random.nextInt(10)
                    c.isLowerCase() && c <= 'f' -> 'a' + kotlin.random.Random.nextInt(6)
                    c.isLowerCase() -> 'a' + kotlin.random.Random.nextInt(26)
                    c.isUpperCase() && c <= 'F' -> 'A' + kotlin.random.Random.nextInt(6)
                    c.isUpperCase() -> 'A' + kotlin.random.Random.nextInt(26)
                    else -> c
                }
            )
        }
        return serial.toString()
    }

    private fun randomMeid(): String {
        // http://en.wikipedia.org/wiki/International_Mobile_Equipment_Identity
        // We start with a known base, and generate random MEID
        var meid = "35503104"
        val rand = Random()
        for (i in 0..5) {
            meid += rand.nextInt(10).toString()
        }

        // Luhn algorithm (check digit)
        var sum = 0
        for (i in meid.indices) {
            var c = meid[i].toString().toInt()
            if ((meid.length - i - 1) % 2 == 0) {
                c *= 2
                c = c % 10 + c / 10
            }
            sum += c
        }
        val check = (100 - sum) % 10
        meid += check.toString()
        return meid
    }
}