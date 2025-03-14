package org.microg.vending.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
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
import com.android.vending.buildRequestHeaders
import com.android.vending.installer.installPackagesFromNetwork
import com.android.vending.installer.uninstallPackage
import kotlinx.coroutines.runBlocking
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.asProto
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.ui.TAG
import org.microg.vending.UploadDeviceConfigRequest
import org.microg.vending.WorkAccountChangedReceiver
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_FDFE
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ITEM_DETAILS
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_PURCHASE
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.billing.proto.FailedResponse
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.enterprise.EnterpriseApp
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.enterprise.App
import org.microg.vending.enterprise.AppState
import org.microg.vending.enterprise.CommitingSession
import org.microg.vending.enterprise.Downloading
import org.microg.vending.enterprise.InstallError
import org.microg.vending.enterprise.Installed
import org.microg.vending.enterprise.NotCompatible
import org.microg.vending.enterprise.NotInstalled
import org.microg.vending.enterprise.Pending
import org.microg.vending.enterprise.UpdateAvailable
import org.microg.vending.enterprise.proto.AppInstallPolicy
import org.microg.vending.proto.AppMeta
import org.microg.vending.proto.GetItemsRequest
import org.microg.vending.proto.RequestApp
import org.microg.vending.proto.RequestItem
import org.microg.vending.ui.components.EnterpriseList
import org.microg.vending.ui.components.NetworkState
import java.io.IOException
import kotlin.random.Random

@RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
class WorkAppsActivity : ComponentActivity() {

    private var apps: MutableMap<EnterpriseApp, AppState> = mutableStateMapOf()
    private var networkState by mutableStateOf(NetworkState.ACTIVE)

    private var auth: AuthData? = null

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
        } else if (accounts.size > 1) {
            Log.w(TAG, "Multiple work accounts found. This is unexpected and could point " +
                    "towards misuse of the work account service API by the DPC.")
        }
        val account = accounts.first()

        load(account)

        setContent {
            VendingUi(account,
                install = { app, isUpdate -> Thread { runBlocking { install(app, isUpdate) } }.start() },
                uninstall = { app -> Thread { runBlocking { uninstall(app) } }.start() }
            )
        }
    }

    private fun load(account: Account) {
        networkState = NetworkState.ACTIVE
        Thread {
            runBlocking {
                try {
                    // Authenticate
                    auth = AuthManager.getAuthData(this@WorkAppsActivity, account)
                    val authData = auth
                    val deviceInfo = createDeviceEnvInfo(this@WorkAppsActivity)
                    if (deviceInfo == null || authData == null) {
                        Log.e(TAG, "Unable to open play store when deviceInfo = $deviceInfo and authData = $authData")
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    val headers = buildRequestHeaders(authData.authToken, authData.gsfId.toLong(16))
                    val client = HttpClient()

                    // Register device for server-side compatibility checking
                    val upload = client.post(
                        url = "$URL_FDFE/uploadDeviceConfig",
                        headers = headers.minus("X-PS-RH"),
                        payload = UploadDeviceConfigRequest(
                            DeviceConfiguration(this@WorkAppsActivity).asProto(),
                            manufacturer = Build.MANUFACTURER,
                            //gcmRegistrationId = TODO: looks like remote-triggered app downloads may be announced through GCM?
                        ),
                        adapter = GoogleApiResponse.ADAPTER
                    )
                    Log.d(TAG, "uploaddc: ${upload.payload!!.uploadDeviceConfigResponse}")

                    // Fetch list of apps available to the scoped enterprise account
                    val apps = client.post(
                        url = URL_ENTERPRISE_CLIENT_POLICY,
                        headers = headers.plus("content-type" to "application/x-protobuf"),
                        adapter = GoogleApiResponse.ADAPTER
                    ).payload?.enterpriseClientPolicyResponse?.policy?.apps?.filter { it.packageName != null }

                    if (apps == null) {
                        Log.e(TAG, "unexpected network response: missing expected fields")
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    Log.v(TAG, "app policy: ${apps.joinToString { "${it.packageName}: ${it.policy}" }}")

                    if (apps.isEmpty()) {
                        // Don't fetch details of empty app list (otherwise HTTP 400)
                        networkState = NetworkState.PASSIVE
                        this@WorkAppsActivity.apps.clear()
                        return@runBlocking
                    }

                    // Fetch details about all available apps
                    val details = client.post(
                        url = URL_ITEM_DETAILS,
                        // TODO: meaning unclear, but returns 400 without. constant? possibly has influence on which fields are returned?
                        headers = headers.plus("x-dfe-item-field-mask" to "GgWGHay3ByILPP/Avy+4A4YlCRM"),
                        payload = GetItemsRequest(
                            apps.map {
                                RequestItem(RequestApp(AppMeta(it.packageName)))
                            }
                        ),
                        adapter = GoogleApiResponse.ADAPTER
                    ).getItemsResponses.mapNotNull { it.response }.associate { item ->
                        val packageName = item.meta!!.packageName!!
                        val installedDetails = this@WorkAppsActivity.packageManager.getInstalledPackages(0).find {
                            it.applicationInfo.packageName == packageName
                        }

                        val available = item.offer?.delivery != null

                        val versionCode = if (available) {
                            item.offer!!.version!!.versionCode!!
                        } else null

                        val state = if (!available && installedDetails == null) NotCompatible
                        else if (!available && installedDetails != null) Installed
                        else if (available && installedDetails == null) NotInstalled
                        else if (available && installedDetails != null && installedDetails.versionCode < versionCode!!) UpdateAvailable
                        else /* if (available && installedDetails != null) */ Installed

                        EnterpriseApp(
                            packageName,
                            versionCode,
                            item.detail!!.name!!.displayName!!,
                            item.detail.icon?.icon?.paint?.url,
                            item.offer?.delivery?.key,
                            item.offer?.delivery?.dependencies?.map {
                                App(it.packageName!!, it.versionCode!!, it.packageName, null, emptyList(), null)
                            } ?: emptyList(),
                            apps.find { it.packageName!! == item.meta.packageName }!!.policy ?: AppInstallPolicy.OPTIONAL,
                        ) to state
                    }.onEach {
                        Log.v(TAG, "${it.key.packageName} (state: ${it.value}) delivery token: ${it.key.deliveryToken ?: "none acquired"}")
                    }

                    this@WorkAppsActivity.apps.apply {
                        clear()
                        putAll(details)
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

    private suspend fun install(app: EnterpriseApp, isUpdate: Boolean) {

        val previousState = apps[app]!!
        apps[app] = Pending

        val client = HttpClient()

        // Purchase app (only needs to be done once, in theory – behaviour seems flaky)
        // Ignore failures
        runCatching {
            if (app.policy != AppInstallPolicy.MANDATORY) {
                val parameters = mapOf(
                    "ot" to "1",
                    "doc" to app.packageName,
                    "vc" to app.versionCode.toString()
                )
                client.post(
                    url = URL_PURCHASE,
                    headers = buildRequestHeaders(
                        auth!!.authToken,
                        auth!!.gsfId.toLong(16)
                    ),
                    params = parameters,
                    adapter = GoogleApiResponse.ADAPTER
                )
            }
        }.onFailure { Log.i(TAG, "couldn't purchase ${app.packageName}: ${it.message}") }
            .onSuccess { Log.d(TAG, "purchased ${app.packageName} successfully") }

        // Install dependencies (different package name → needs to happen in a separate transaction)
        for (dependency in app.dependencies) {
            val downloadUrls = runCatching {

                client.requestDownloadUrls(
                    dependency.packageName,
                    dependency.versionCode!!.toLong(),
                    auth!!
                    // no delivery token available
                ) }

            if (downloadUrls.isFailure) {
                Log.w(TAG, "Failed to request download URLs for dependency ${dependency.packageName}: ${downloadUrls.exceptionOrNull()!!.message}")
                apps[app] = previousState
                return
            }

            runCatching {

                var lastNotification = 0L
                installPackagesFromNetwork(
                    packageName = dependency.packageName,
                    components = downloadUrls.getOrThrow(),
                    httpClient = client,
                    isUpdate = false // static libraries may never be installed as updates
                ) { session, progress ->

                    // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                    if (progress !is Downloading || lastNotification + 250 < System.currentTimeMillis()) {
                        notifyInstallProgress(app.displayName, session, progress, isDependency = true)
                        lastNotification = System.currentTimeMillis()
                    }
                }
            }.onFailure { exception ->
                Log.w(TAG, "Installation from network unsuccessful.", exception)
                notifyInstallProgress(app.displayName, sessionId = Random.nextInt(), progress = InstallError("unused"), false)
                apps[app] = previousState
                return
            }
        }

        // Get download links for requested package
        val downloadUrls = runCatching {

            client.requestDownloadUrls(
                app.packageName,
                app.versionCode!!.toLong(),
                auth!!,
                deliveryToken = app.deliveryToken
            ) }

        if (downloadUrls.isFailure) {
            Log.w(TAG, "Failed to request download URLs: ${downloadUrls.exceptionOrNull()!!.message}")
            apps[app] = previousState
            return
        }

        runCatching {

            var lastNotification = 0L
            installPackagesFromNetwork(
                packageName = app.packageName,
                components = downloadUrls.getOrThrow(),
                httpClient = client,
                isUpdate = isUpdate
            ) { session, progress ->


                // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                if (progress !is Downloading || lastNotification + 250 < System.currentTimeMillis()) {
                    notifyInstallProgress(app.displayName, session, progress)
                    lastNotification = System.currentTimeMillis()
                }

                if (progress is Downloading) apps[app] = progress
                else if (progress is CommitingSession) apps[app] = Pending
            }
        }.onSuccess {
            apps[app] = Installed
        }.onFailure { exception ->
            Log.w(TAG, "Installation from network unsuccessful.", exception)
            apps[app] = previousState
        }
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