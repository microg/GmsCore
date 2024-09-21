package org.microg.vending.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.vending.AppMeta
import com.android.vending.GetItemsRequest
import com.android.vending.GetItemsResponse
import com.android.vending.RequestApp
import com.android.vending.RequestItem
import com.android.vending.buildRequestHeaders
import com.android.volley.VolleyError
import com.google.android.finsky.GoogleApiResponse
import com.google.android.finsky.splitinstallservice.SplitInstallManager
import com.google.android.finsky.splitinstallservice.uninstallPackage
import kotlinx.coroutines.runBlocking
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.asProto
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.ui.TAG
import org.microg.vending.UploadDeviceConfigRequest
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_DELIVERY
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_FDFE
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ITEM_DETAILS
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.enterprise.App
import org.microg.vending.enterprise.EnterpriseApp
import org.microg.vending.ui.components.EnterpriseList
import org.microg.vending.ui.components.NetworkState
import java.io.IOException

@RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
class VendingActivity : ComponentActivity() {

    var apps: MutableList<EnterpriseApp> = mutableStateListOf()
    var networkState by mutableStateOf(NetworkState.ACTIVE)

    var auth: AuthData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType("com.google.work")
        if (accounts.isEmpty()) {
            TODO("App should only be visible if work accounts are added. Disable component and wonder why it was enabled in the first place")
        } else if (accounts.size > 1) {
            Log.w(TAG, "Multiple work accounts found. This is unexpected and could point " +
                    "towards misuse of the work account service API by the DPC.")
        }
        val account = accounts.first()

        load(account)

        val install: (app: EnterpriseApp, isUpdate: Boolean) -> Unit = { app, isUpdate ->
            Toast.makeText(this, "installing ${app.displayName} / ${app.packageName}", Toast.LENGTH_SHORT).show()
            Thread {
                runBlocking {
                    // Get download links for requested package
                    val res = HttpClient(this@VendingActivity).get(
                        url = URL_DELIVERY,
                        headers = buildRequestHeaders(auth!!.authToken, auth!!.gsfId.toLong(16)),
                        params = mapOf(
                            "ot" to "1",
                            "doc" to app.packageName,
                            "vc" to app.versionCode!!.toString()
                        ).plus(app.deliveryToken?.let { listOf("dtok" to it) } ?: emptyList()),
                        adapter = GoogleApiResponse.ADAPTER
                    )

                    Log.d(TAG, res.toString())
                    val triples = setOf(Triple("base", res.response!!.splitReqResult!!.pkgList!!.baseUrl!!, 0)) +
                            res.response!!.splitReqResult!!.pkgList!!.pkgDownLoadInfo!!.map {
                                Triple(it.splitPkgName!!, it.downloadUrl!!, 0)
                            }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        SplitInstallManager(this@VendingActivity).apply {
                            triples.forEach { updateSplitInstallRecord(app.packageName, it) }
                            notify(this@VendingActivity)
                            installSplitPackage(this@VendingActivity, app.packageName, triples, isUpdate)
                        }
                    } else {
                        TODO("implement installation on Lollipop devices")
                    }
                }
            }.start()
        }

        val uninstall: (app: EnterpriseApp) -> Unit = {
            uninstallPackage(it.packageName)
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
                    Log.d(TAG, "uploaddc: ${upload.response!!.uploadDeviceConfigResponse}")

                    // Fetch list of apps available to the scoped enterprise account
                    val apps = client.post(
                        url = URL_ENTERPRISE_CLIENT_POLICY,
                        headers = headers.plus("content-type" to "application/x-protobuf"),
                        adapter = GoogleApiResponse.ADAPTER
                    ).response?.enterpriseClientPolicyResult?.policy?.apps?.filter { it.packageName != null && it.policy != null }

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
                        adapter = GetItemsResponse.ADAPTER,
                        payload = GetItemsRequest(
                            apps.map {
                                RequestItem(RequestApp(AppMeta(it.packageName)))
                            }
                        )
                    ).items.map { it.response }.filterNotNull().map { item ->
                        val packageName = item.meta!!.packageName!!
                        val installedDetails = this@VendingActivity.packageManager.getInstalledPackages(0).find {
                            it.applicationInfo.packageName == packageName
                        }

                        val available = item.offer?.delivery != null

                        val versionCode = if (available) {
                            item.offer!!.version!!.versionCode!!
                        } else null

                        val state = if (!available && installedDetails == null) App.State.NOT_COMPATIBLE
                        else if (!available && installedDetails != null) App.State.INSTALLED
                        else if (available && installedDetails == null) App.State.NOT_INSTALLED
                        else if (available && installedDetails != null && installedDetails.versionCode > versionCode!!) App.State.UPDATE_AVAILABLE
                        else /* if (available && installedDetails != null) */ App.State.INSTALLED

                        EnterpriseApp(
                            packageName,
                            versionCode,
                            item.detail!!.name!!.displayName!!,
                            state,
                            item.detail.icon?.icon?.paint?.url,
                            item.offer?.delivery?.key,
                            apps.find { it.packageName!! == item.meta.packageName }!!.policy!!,
                        )
                    }.onEach {
                        Log.v(TAG, "${it.packageName} delivery token: ${it.deliveryToken ?: "none acquired"}")
                    }

                    this@VendingActivity.apps.apply {
                        clear()
                        addAll(details)
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