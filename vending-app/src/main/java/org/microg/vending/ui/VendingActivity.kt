package org.microg.vending.ui

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.vending.buildRequestHeaders
import com.android.volley.VolleyError
import com.google.android.finsky.GoogleApiResponse
import kotlinx.coroutines.runBlocking
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.TAG
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo
import org.microg.vending.enterprise.App
import org.microg.vending.enterprise.EnterpriseApp


class VendingActivity : ComponentActivity() {

    var apps: MutableList<EnterpriseApp> = mutableStateListOf()
    var networkState by mutableStateOf(NetworkState.ACTIVE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val am = AccountManager.get(this)
        val account = am.getAccountsByType("com.google.work").first()!!
        Thread {
            runBlocking {
                val authData = AuthManager.getAuthData(this@VendingActivity, account)
                val deviceInfo = createDeviceEnvInfo(this@VendingActivity)
                if (deviceInfo == null || authData == null) {
                    Log.e(TAG, "Unable to open play store when deviceInfo = $deviceInfo and authData = $authData")
                    return@runBlocking
                }

                val headers = buildRequestHeaders(authData.authToken, authData.gsfId.toLong(16))
                    .plus("content-type" to "application/x-protobuf")
                val client = HttpClient(this@VendingActivity)

                try {
                    val apps = client.post(
                        url = URL_ENTERPRISE_CLIENT_POLICY,
                        headers = headers,
                        adapter = GoogleApiResponse.ADAPTER
                    ).response?.enterpriseClientPolicyResult?.policy?.apps?.filter { it.packageName != null && it.policy != null }
                        ?.map {
                            EnterpriseApp(
                                it.packageName!!,
                                "Display name placeholder",
                                App.State.NOT_INSTALLED,
                                "https://i.ytimg.com/vi/IWZFLZ1mvc4/hqdefault.jpg",
                                it.policy!!
                            )
                        }
                    this@VendingActivity.apps.apply {
                        clear()
                        apps?.let { addAll(it) }
                    }
                    networkState = NetworkState.PASSIVE
                } catch (e: VolleyError) {
                    networkState = NetworkState.ERROR
                }
            }
        }.start()



        setContent {
            MaterialTheme {
                Column {
                    Text(account.name)
                    NetworkStateComponent(networkState, { TODO("reload") }) {
                        EnterpriseListComponent(apps)
                    }
                }
            }
        }

    }
}