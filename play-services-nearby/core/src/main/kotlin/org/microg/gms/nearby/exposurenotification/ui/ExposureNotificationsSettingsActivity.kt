/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.nearby.exposurenotification.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import org.microg.gms.nearby.core.R

class ExposureNotificationsSettingsActivity : AppCompatActivity() {
    private var appBarConfiguration: AppBarConfiguration? = null

    private val navController: NavController
        get() = (supportFragmentManager.findFragmentById(R.id.navhost) as NavHostFragment?)!!.navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exposure_notifications_settings_activity)
        appBarConfiguration = AppBarConfiguration.Builder(navController.graph).build()
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration!!)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration!!) || super.onSupportNavigateUp()
    }
}
