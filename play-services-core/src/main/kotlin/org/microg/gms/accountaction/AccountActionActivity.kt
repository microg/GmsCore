package org.microg.gms.accountaction

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.R
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.login.LoginActivity

internal const val INTENT_KEY_USER_ACTION = "userAction"
internal const val INTENT_KEY_ACCOUNT_NAME = "accountName"

@RequiresApi(21)
class AccountActionActivity : ComponentActivity() {

    // mutableStateMapOf() returns an unordered map
    private val taskMap: MutableList<Pair<Requirement, Boolean>> = mutableStateListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")

        val actionExtra = intent.getSerializableExtra(INTENT_KEY_USER_ACTION)
        val accountName = intent.getStringExtra(INTENT_KEY_ACCOUNT_NAME)

        Log.d(TAG, "actionExtra: $actionExtra account: $accountName")

        if (actionExtra is UserSatisfyRequirements) {
            if (savedInstanceState == null) {
                val requirements = actionExtra.actions.toTypedArray()
                taskMap.addAll(requirements.map { it to false })
            }
            setContent {
                Content(accountName ?: "<?>", taskMap.toMap()) {
                    finish()
                }
            }
            return
        }

        if (actionExtra is Reauthenticate) {
            val account = AccountManager.get(this).accounts.find { it.name == accountName && it.type == AuthConstants.DEFAULT_ACCOUNT_TYPE } ?: return finish()
            val reAuth = actionExtra.reAuth
            if (reAuth) {
                val intent = Intent(this, LoginActivity::class.java).apply {
                    putExtra(LoginActivity.EXTRA_RE_AUTH_ACCOUNT, account)
                }
                startActivity(intent)
            } else {
                AccountManager.get(this).removeAccount(account, null, null)
            }
            cancelAccountNotificationChannel(account)
            finish()
            return
        }

    }

    override fun onResume() {
        super.onResume()

        for ((index, task) in taskMap.withIndex()) {
            taskMap[index] = task.component1() to checkRequirementSatisfied(task.component1())
        }
    }

    companion object {
        fun createIntent(context: Context, account: Account, action: Resolution) =
            Intent(context, AccountActionActivity::class.java).apply {
                putExtra(INTENT_KEY_USER_ACTION, action)
                putExtra(INTENT_KEY_ACCOUNT_NAME, account.name)
            }
    }
}

@Composable
fun Content(accountName: String, taskMap: Map<Requirement, Boolean>, finish: () -> Unit) {
    Column {
        Column {
            Text(
                text = stringResource(id = R.string.auth_action_activity_header),
                modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = stringResource(id = R.string.auth_action_activity_explanation, accountName),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()
        }
        Surface(Modifier.fillMaxHeight()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                UserInterventionComponents(userActions = taskMap)

                Button(
                    onClick = finish,
                    enabled = !taskMap.containsValue(false),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(
                            id = R.color.login_blue_theme_primary
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(16.dp)
                ) {
                    Text(text = stringResource(id = R.string.auth_action_activity_finish))
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    Content(
        "admin@example.com",
        mapOf(
            Requirement.ENABLE_CHECKIN to true,
            Requirement.ENABLE_GCM to true,
            Requirement.ALLOW_MICROG_GCM to false,
            Requirement.ENABLE_LOCKSCREEN to false
        )
    ) {}
}