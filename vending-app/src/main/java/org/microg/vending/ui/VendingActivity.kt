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
import com.android.vending.installer.installPackages
import com.android.volley.VolleyError
import com.android.vending.installer.uninstallPackage
import kotlinx.coroutines.runBlocking
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
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.delivery.downloadPackageComponents
import org.microg.vending.enterprise.EnterpriseApp
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.enterprise.AppState
import org.microg.vending.proto.AppMeta
import org.microg.vending.proto.GetItemsRequest
import org.microg.vending.proto.RequestApp
import org.microg.vending.proto.RequestItem
import org.microg.vending.ui.components.EnterpriseList
import org.microg.vending.ui.components.NetworkState
import java.io.IOException

@RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
class VendingActivity : ComponentActivity() {

    var apps: MutableMap<EnterpriseApp, AppState> = mutableStateMapOf()
    var networkState by mutableStateOf(NetworkState.ACTIVE)

    var auth: AuthData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType("com.google.work")
        if (accounts.isEmpty()) {
            // Component should not be enabled; disable through receiver, and redirect to main activity
            WorkAccountChangedReceiver().onReceive(this, null)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            TODO("App should only be visible if work accounts are added. Disable component and wonder why it was enabled in the first place")
        } else if (accounts.size > 1) {
            Log.w(TAG, "Multiple work accounts found. This is unexpected and could point " +
                    "towards misuse of the work account service API by the DPC.")
        }
        val account = accounts.first()

        load(account)

        val install: (app: EnterpriseApp, isUpdate: Boolean) -> Unit = { app, isUpdate ->
            Thread {
                runBlocking {

                    val previousState = apps[app]!!
                    apps[app] = AppState.PENDING

                    val client = HttpClient(this@VendingActivity)

                    // Get download links for requested package
                    val downloadUrls = runCatching { client.requestDownloadUrls(
                        app.packageName,
                        app.versionCode!!.toLong(),
                        auth!!,
                        deliveryToken = app.deliveryToken
                    ) }

                    if (downloadUrls.isFailure) {
                        Log.w(TAG, "Failed to request download URLs: ${downloadUrls.exceptionOrNull()!!.message}")
                        apps[app] = previousState
                        return@runBlocking
                    }

                    val packageFiles = client.downloadPackageComponents(this@VendingActivity, downloadUrls.getOrThrow(), Unit)
                    if (packageFiles.values.any { it == null }) {
                        Log.w(TAG, "Cannot proceed to installation as not all files were downloaded")
                        apps[app] = previousState
                        return@runBlocking
                    }

                    runCatching {
                        installPackages(
                            app.packageName,
                            packageFiles.values.filterNotNull(),
                            isUpdate
                        )
                    }.onSuccess {
                        apps[app] = AppState.INSTALLED
                    }.onFailure {
                        apps[app] = previousState
                    }
                }
            }.start()
        }

        val uninstall: (app: EnterpriseApp) -> Unit = { app ->
            Thread {
                runBlocking {

                    val previousState = apps[app]!!
                    apps[app] = AppState.PENDING
                    runCatching { uninstallPackage(app.packageName) }.onSuccess {
                        apps[app] = AppState.NOT_INSTALLED
                    }.onFailure {
                        apps[app] = previousState
                    }

                }
            }.start()
        }

        setContent {
            VendingUi(account, install, uninstall)
        }
    }

    private fun load(account: Account) {
        networkState = NetworkState.ACTIVE
        Thread {
            runBlocking {
                try {
                    // Authenticate
                    auth = AuthManager.getAuthData(this@VendingActivity, account)
                    val authData = auth
                    val deviceInfo = createDeviceEnvInfo(this@VendingActivity)
                    if (deviceInfo == null || authData == null) {
                        Log.e(TAG, "Unable to open play store when deviceInfo = $deviceInfo and authData = $authData")
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    val headers = buildRequestHeaders(authData.authToken, authData.gsfId.toLong(16))
                    val client = HttpClient(this@VendingActivity)

                    // Register device for server-side compatibility checking
                    val upload = client.post(
                        url = "$URL_FDFE/uploadDeviceConfig",
                        headers = headers.minus("X-PS-RH"),
                        payload = UploadDeviceConfigRequest(
                            DeviceConfiguration(this@VendingActivity).asProto(),
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
                    ).payload?.enterpriseClientPolicyResponse?.policy?.apps?.filter { it.packageName != null && it.policy != null }

                    if (apps == null) {
                        Log.e(TAG, "unexpected network response: missing expected fields")
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    Log.v(TAG, "app policy: ${apps.joinToString { "${it.packageName}: ${it.policy}" }}")

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
                        val installedDetails = this@VendingActivity.packageManager.getInstalledPackages(0).find {
                            it.applicationInfo.packageName == packageName
                        }

                        val available = item.offer?.delivery != null

                        val versionCode = if (available) {
                            item.offer!!.version!!.versionCode!!
                        } else null

                        val state = if (!available && installedDetails == null) AppState.NOT_COMPATIBLE
                        else if (!available && installedDetails != null) AppState.INSTALLED
                        else if (available && installedDetails == null) AppState.NOT_INSTALLED
                        else if (available && installedDetails != null && installedDetails.versionCode < versionCode!!) AppState.UPDATE_AVAILABLE
                        else /* if (available && installedDetails != null) */ AppState.INSTALLED

                        EnterpriseApp(
                            packageName,
                            versionCode,
                            item.detail!!.name!!.displayName!!,
                            item.detail.icon?.icon?.paint?.url,
                            item.offer?.delivery?.key,
                            apps.find { it.packageName!! == item.meta.packageName }!!.policy!!,
                        ) to state
                    }.onEach {
                        Log.v(TAG, "${it.key.packageName} (state: ${it.value}) delivery token: ${it.key.deliveryToken ?: "none acquired"}")
                    }

                    this@VendingActivity.apps.apply {
                        clear()
                        putAll(details)
                    }
                    networkState = NetworkState.PASSIVE
                } catch (e: IOException) {
                    networkState = NetworkState.ERROR
                    Log.e(TAG, "Network error: ${e.message}")
                    e.printStackTrace()
                } catch (e: VolleyError) {
                    networkState = NetworkState.ERROR
                    Log.e(TAG, "Network error: ${e.message}")
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    networkState = NetworkState.ERROR
                    Log.e(TAG, "Unexpected network response, cannot process")
                    e.printStackTrace()
                }
            }
        }.start()

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
}