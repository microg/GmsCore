/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class PayActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Not yet supported:\n${intent?.action}"
            gravity = Gravity.CENTER
        })
    }
}