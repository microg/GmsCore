package org.microg.gms.accountaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.R

@RequiresApi(21)
class AccountActionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            org.microg.gms.accountaction.Preview()
        }
    }
}

@Composable
fun Content(userTasks: UserIntervention, finish: () -> Unit) {
    Column {
        Column {
            Text(
                text = stringResource(id = R.string.auth_action_activity_header),
                modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = stringResource(id = R.string.auth_action_activity_explanation, "admin@fynngodau.de"),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()
        }
        Surface(Modifier.fillMaxHeight()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                userTasks.UiComponents()
                Button(
                    onClick = finish,
                    enabled = false,
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
    Content(UserIntervention(setOf(UserAction.ENABLE_CHECKIN, UserAction.ENABLE_GCM, UserAction.ALLOW_MICROG_GCM, UserAction.ENABLE_LOCKSCREEN))) {}
}