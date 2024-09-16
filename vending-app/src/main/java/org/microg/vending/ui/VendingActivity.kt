package org.microg.vending.ui

import AppMeta
import GetItemsRequest
import GetItemsResponse
import RequestApp
import RequestItem
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.vending.R
import com.android.vending.buildRequestHeaders
import com.android.volley.VolleyError
import com.google.android.finsky.GoogleApiResponse
import kotlinx.coroutines.runBlocking
import org.microg.gms.profile.ProfileManager
import org.microg.gms.ui.TAG
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ITEM_DETAILS
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.billing.proto.ResponseWrapper
import org.microg.vending.enterprise.App
import org.microg.vending.enterprise.EnterpriseApp
import java.io.IOException


class VendingActivity : ComponentActivity() {

    var apps: MutableList<EnterpriseApp> = mutableStateListOf()
    var networkState by mutableStateOf(NetworkState.ACTIVE)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val am = AccountManager.get(this)
        val account = am.getAccountsByType("com.google.work").first()!!
        Thread {
            runBlocking {
                try {
                    val authData = AuthManager.getAuthData(this@VendingActivity, account)
                    val deviceInfo = createDeviceEnvInfo(this@VendingActivity)
                    if (deviceInfo == null || authData == null) {
                        Log.e(TAG, "Unable to open play store when deviceInfo = $deviceInfo and authData = $authData")
                        networkState = NetworkState.ERROR
                        return@runBlocking
                    }

                    val headers = buildRequestHeaders(authData.authToken, authData.gsfId.toLong(16))
                    val client = HttpClient(this@VendingActivity)

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

                    val details = client.post(
                        url = URL_ITEM_DETAILS,
                        // TODO: meaning unclear, but returns 400 without. constant? possibly has influence on which fields are returned?
                        headers = headers.plus("x-dfe-item-field-mask" to "GgJGCCIKBgIAXASAAAAAAQ"),
                        adapter = GetItemsResponse.ADAPTER,
                        payload = GetItemsRequest(
                            apps.map {
                                RequestItem(RequestApp(AppMeta(it.packageName!!)))
                            }
                        )
                    ).items.map { it.response }.map { item ->
                        EnterpriseApp(
                            item!!.meta!!.packageName!!,
                            item.detail!!.name!!.displayName!!,
                            App.State.NOT_INSTALLED,
                            item.detail.icon?.icon?.paint?.url,
                            apps.find { it.packageName!! == item.meta!!.packageName }!!.policy!!,
                        )
                    }

                    this@VendingActivity.apps.apply {
                        clear()
                        addAll(details)
                    }
                    networkState = NetworkState.PASSIVE
                } catch (e: IOException) {
                    networkState = NetworkState.ERROR
                } catch (e: VolleyError) {
                    networkState = NetworkState.ERROR
                } catch (e: NullPointerException) {
                    networkState = NetworkState.ERROR
                }
            }
        }.start()

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row {
                                    Icon(
                                        painterResource(R.drawable.ic_work),
                                        contentDescription = null,
                                        Modifier.align(Alignment.CenterVertically),
                                        tint = LocalContentColor.current
                                    )
                                    Text(stringResource(R.string.vending_activity_name),
                                        Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 8.dp)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        NetworkStateComponent(networkState, { TODO("reload") }) {
                            EnterpriseListComponent(apps)
                        }
                    }
                }
            }
        }

    }
}