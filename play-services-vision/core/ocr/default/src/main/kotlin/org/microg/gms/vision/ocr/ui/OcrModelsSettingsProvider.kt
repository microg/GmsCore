/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.ocr.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.navigation.NavController
import org.microg.gms.ui.settings.SettingsProvider
import org.microg.gms.ui.settings.SettingsProvider.Companion.Entry
import org.microg.gms.ui.settings.SettingsProvider.Companion.Group.OTHER
import org.microg.gms.vision.ocr.R

@Keep
object OcrModelsSettingsProvider : SettingsProvider {
    override fun getEntriesStatic(context: Context): List<Entry> {
        if (android.os.Build.VERSION.SDK_INT < 26) return emptyList()
        return listOf(Entry("pref_ocr_models", OTHER, R.id.ocrModelsActivity, context.getString(R.string.ocr_models_title)))
    }

    override fun preProcessSettingsIntent(intent: Intent) = Unit

    override fun extendNavigation(navController: NavController) {
        navController.graph.addAll(navController.navInflater.inflate(R.navigation.nav_ocr_models))
    }
}
