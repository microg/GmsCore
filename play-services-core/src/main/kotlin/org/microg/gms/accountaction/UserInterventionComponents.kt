package org.microg.gms.accountaction

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.R
import org.microg.gms.accountaction.Requirement.*
import org.microg.gms.common.Constants
import org.microg.gms.ui.AskPushPermission

const val ACTION_CHECKIN = "org.microg.gms.settings.CHECKIN_SETTINGS"
const val ACTION_GCM = "org.microg.gms.settings.GCM_SETTINGS"

@Composable
fun UserInterventionComponents(userActions: Map<Requirement, Boolean>) {
    for ((index, action) in userActions.entries.withIndex()) {
        val context = LocalContext.current as Activity
        val displayIndex = if (userActions.size > 1) index + 1 else null
        when (action.component1()) {
            ENABLE_CHECKIN -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_checkin),
                description = stringResource(id = R.string.auth_action_step_enable_checkin_description),
                sequenceNumber = displayIndex,
                completed = action.component2()
            ) {
                Intent(ACTION_CHECKIN).let { context.startActivityForResult(it, 0) }
            }
            ENABLE_GCM -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_gcm),
                description = stringResource(id = R.string.auth_action_step_enable_gcm_description),
                sequenceNumber = displayIndex,
                completed = action.component2()
            ) {
                Intent(ACTION_GCM).let { context.startActivityForResult(it, 1) }
            }
            ALLOW_MICROG_GCM -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_allow_microg_gcm),
                description = stringResource(id = R.string.auth_action_step_allow_microg_gcm_description),
                sequenceNumber = displayIndex,
                completed = action.component2()
            ) {
                Intent(context, AskPushPermission::class.java).apply {
                    putExtra(AskPushPermission.EXTRA_REQUESTED_PACKAGE, Constants.GMS_PACKAGE_NAME)
                    putExtra(AskPushPermission.EXTRA_FORCE_ASK, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                }.let { context.startActivity(it) }
            }
            ENABLE_LOCKSCREEN -> UserInterventionCommonComponent(
                title = stringResource(id = R.string.auth_action_step_enable_lockscreen),
                description = stringResource(id = R.string.auth_action_step_enable_lockscreen_description),
                sequenceNumber = displayIndex,
                completed = action.component2()
            ) {
                runCatching {
                    Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).let { context.startActivity(it) }
                }.onFailure {
                    Intent(android.provider.Settings.ACTION_SETTINGS).let { context.startActivity(it) }
                }

            }
        }
    }
}

@Composable
fun UserInterventionCommonComponent(title: String, description: String, sequenceNumber: Int?, completed: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = !completed) {

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
                        if (sequenceNumber == null) {
                            Canvas(modifier = Modifier.size(12.dp).align(Alignment.Center)) {
                                drawCircle(
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = sequenceNumber.toString(),
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
        UserInterventionComponents(
            userActions = mapOf(
                ENABLE_CHECKIN to true,
                ENABLE_GCM to true,
                ALLOW_MICROG_GCM to false,
                ENABLE_LOCKSCREEN to false
            )
        )
        HorizontalDivider()
        UserInterventionComponents(
            userActions = mapOf(ENABLE_LOCKSCREEN to false)
        )
    }
}