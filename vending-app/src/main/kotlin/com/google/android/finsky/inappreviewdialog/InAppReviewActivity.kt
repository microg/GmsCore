/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.inappreviewdialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class InAppReviewActivity: AppCompatActivity() {
    companion object {
        const val CALLING_PACKAGE = "calling_package"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK)
        finish()
    }
}