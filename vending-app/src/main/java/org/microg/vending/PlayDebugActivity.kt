package org.microg.vending

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.vending.buildRequestHeaders
import com.google.android.finsky.AppInstallPolicy
import com.google.android.finsky.GoogleApiResponse
import kotlinx.coroutines.runBlocking
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.TAG
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_ENTERPRISE_CLIENT_POLICY
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.createDeviceEnvInfo


class PlayDebugActivity : ComponentActivity() {

    var apps: MutableList<Pair<String, AppInstallPolicy>> = mutableStateListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfileManager.ensureInitialized(this)

        val am = AccountManager.get(this)
        val account = am.getAccountsByType("com.google.work").first()!!
        Thread {
            runBlocking {
                val authData = AuthManager.getAuthData(this@PlayDebugActivity, account)
                val deviceInfo = createDeviceEnvInfo(this@PlayDebugActivity)
                if (deviceInfo == null || authData == null) {
                    Log.e(
                        TAG,
                        "Unable to open play store when deviceInfo = $deviceInfo and authData = $authData"
                    )
                    return@runBlocking
                }

                val headers = buildRequestHeaders(authData.authToken, authData.gsfId.toLong(16))
                    .plus("content-type" to "application/x-protobuf")
                val client = HttpClient(this@PlayDebugActivity)

                val apps = client.post(
                    url = URL_ENTERPRISE_CLIENT_POLICY,
                    headers = headers,
                    adapter = GoogleApiResponse.ADAPTER
                ).response?.enterpriseClientPolicyResult?.policy?.apps?.map { it.packageName!! to it.policy!! }
                this@PlayDebugActivity.apps.apply {
                    clear()
                    apps?.let { addAll(it) }
                }
            }
        }.start()

        @Composable
        fun AppRow(name: String) {
            Row(Modifier.padding(16.dp)) {
                Text(name)
            }
        }

        setContent {
            MaterialTheme {
                Column {
                    Text(account.name)
                    LazyColumn(Modifier.padding(16.dp)) {
                        item { Text("Required apps", style = MaterialTheme.typography.headlineSmall) }
                        items(apps.filter { it.second == AppInstallPolicy.MANDATORY }.map { it.first }) {
                            AppRow(it)
                        }
                        item { Text("Available apps", style = MaterialTheme.typography.headlineSmall) }
                        items(apps.filter { it.second == AppInstallPolicy.OPTIONAL }.map { it.first }) {
                            AppRow(it)
                        }
                    }
                }
            }
        }

    }
}