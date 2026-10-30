/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.vending.installer.uninstallPackage
import kotlinx.coroutines.runBlocking
import org.microg.gms.auth.AuthConstants
import org.microg.gms.profile.ProfileManager
import org.microg.vending.WorkAccountChangedReceiver
import org.microg.vending.billing.core.AuthData
import org.microg.vending.enterprise.EnterpriseApp
import org.microg.vending.enterprise.EnterpriseAppsRepository
import org.microg.vending.enterprise.AppState
import org.microg.vending.enterprise.NotInstalled
import org.microg.vending.enterprise.Pending
import com.android.vending.installer.InstallService
import org.microg.vending.ui.components.EnterpriseList
import org.microg.vending.ui.components.NetworkState
import java.io.IOException

@RequiresApi(21)
class WorkAppsActivity : ComponentActivity() {

    private val apps: MutableMap<EnterpriseApp, AppState> = mutableStateMapOf()
    private var networkState by mutableStateOf(NetworkState.ACTIVE)

    private var auth: AuthData? = null
        set(value) {
            field = value
            installService?.auth = value
        }

    private var installService: InstallService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            installService = (service as InstallService.LocalBinder).getService()
            installService?.auth = auth
            installService?.apps = apps
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            installService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            // Component should not be enabled; disable through receiver, and redirect to main activity
            WorkAccountChangedReceiver().onReceive(this, null)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        } else if (accounts.size > 1) {
            Log.w(TAG, "Multiple work accounts found. This is unexpected and could point " +
                    "towards misuse of the work account service API by the DPC.")
        }
        val account = accounts.first()

        load(account)

        setContent {
            VendingUi(account,
                install = { app: EnterpriseApp, isUpdate: Boolean ->
                    Intent(this@WorkAppsActivity, InstallService::class.java).let {
                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(it)
                        } else {
                            startService(it)
                        }
                    }
                    installService?.installAsync(app, isUpdate)
                },

                    //Thread { runBlocking { install(app, isUpdate) } }.start() },
                uninstall = { app -> Thread { runBlocking { uninstall(app) } }.start() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, InstallService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    private fun load(account: Account) {
        networkState = NetworkState.ACTIVE
        Thread {
            runBlocking {
                try {
                    val enterpriseApps = EnterpriseAppsRepository(this@WorkAppsActivity).fetchApps(account)
                    if (enterpriseApps == null) {
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    auth = enterpriseApps.auth
                    apps.apply {
                        clear()
                        putAll(enterpriseApps.states)
                    }
                    networkState = NetworkState.PASSIVE
                } catch (e: IOException) {
                    networkState = NetworkState.ERROR
                    Log.e(TAG, "Network error: ${e.message}")
                    e.printStackTrace()
                } catch (e: Exception) {
                    networkState = NetworkState.ERROR
                    Log.e(TAG, "Unexpected network response, cannot process")
                    e.printStackTrace()
                }
            }
        }.start()

    }

    private suspend fun uninstall(app: EnterpriseApp) {
        val previousState = apps[app]!!
        apps[app] = Pending
        runCatching { uninstallPackage(app.packageName) }.onSuccess {
            apps[app] = NotInstalled
        }.onFailure {
            apps[app] = previousState
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VendingUi(
        account: Account,
        install: (app: EnterpriseApp, isUpdate: Boolean) -> Unit,
        uninstall: (app: EnterpriseApp) -> Unit
    ) {
        MaterialTheme {
            Scaffold(
                topBar = {
                    WorkVendingTopAppBar()
                }
            ) { innerPadding ->
                Column(Modifier.padding(innerPadding)) {
                    NetworkState(networkState, { load(account) }) {
                        EnterpriseList(apps, install, uninstall)
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "GmsVendingWorkApp"
    }
}
