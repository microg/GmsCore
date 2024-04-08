package org.microg.gms.accountaction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.R
import org.microg.gms.accountaction.UserAction.*

@Composable
fun UserInterventionComponents(userActions: Map<UserAction, Boolean>) {
    for ((index, action) in userActions.entries.withIndex()) {
        when (action.component1()) {
            ENABLE_CHECKIN -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_checkin),
                description = stringResource(id = R.string.auth_action_step_enable_checkin_description),
                sequenceNumber = index + 1,
                completed = action.component2()
            )
            ENABLE_GCM -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_gcm),
                description = stringResource(id = R.string.auth_action_step_enable_gcm_description),
                sequenceNumber = index + 1,
                completed = action.component2()
            )
            ALLOW_MICROG_GCM -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_allow_microg_gcm),
                description = stringResource(id = R.string.auth_action_step_allow_microg_gcm_description),
                sequenceNumber = index + 1,
                completed = action.component2()
            )
            ENABLE_LOCKSCREEN -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_lockscreen),
                description = stringResource(id = R.string.auth_action_step_enable_lockscreen_description),
                sequenceNumber = index + 1,
                completed = action.component2()
            )
            REAUTHENTICATE -> TODO()
        }
    }
}

@Composable
fun UserInterventionCommonComponent(title: String, description: String, sequenceNumber: Int?, completed: Boolean) {
    Surface(onClick = { /*TODO*/ }) {

        val color = if (completed) {
            colorResource(id = R.color.material_success)
        } else {
            colorResource(id = R.color.login_blue_theme_primary)
        }

        Column(
            Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {

            Row {
                Box(Modifier.size(32.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = color
                        )
                    }

                    if (completed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.auth_action_step_completed_content_description),
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.White
                        )
                    } else {
                        sequenceNumber?.let {
                            Text(
                                text = it.toString(),
                                modifier = Modifier.align(Alignment.Center),
                                style = LocalTextStyle.current.copy(color = Color.White)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(16.dp))
                if (!completed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(id = R.string.auth_action_step_perform_content_description),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            if (!completed) {
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewInterventionComponent() {
    Column {
        UserInterventionComponents(userActions =
           mapOf(ENABLE_CHECKIN to true, ENABLE_GCM to true, ALLOW_MICROG_GCM to false, ENABLE_LOCKSCREEN to false)
        )
    }
}