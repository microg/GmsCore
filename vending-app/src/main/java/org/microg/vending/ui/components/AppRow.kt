package org.microg.vending.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.vending.R
import org.microg.vending.enterprise.App

@Composable
fun AppRow(app: App) {
    Row(
        Modifier.padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSpace = Modifier.size(48.dp)
        if (app.iconUrl != null) {
            AsyncImage(
                model = app.iconUrl,
                modifier = iconSpace,
                contentDescription = null,
            )
        } else {
            Spacer(iconSpace)
        }
        Text(app.displayName)
        Spacer(Modifier.weight(1f))
        if (app.state != App.State.NOT_INSTALLED) {
            IconButton({}) {
                Icon(Icons.Default.Delete, stringResource(R.string.vending_overview_row_action_uninstall), tint = MaterialTheme.colorScheme.secondary)
            }
        }
        if (app.state == App.State.UPDATE_AVAILABLE) {
            FilledIconButton({}, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Icon(painterResource(R.drawable.ic_update), stringResource(R.string.vending_overview_row_action_update), tint = MaterialTheme.colorScheme.secondary)
            }
        }
        if (app.state == App.State.NOT_INSTALLED)
        FilledIconButton({}, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Icon(painterResource(R.drawable.ic_download), stringResource(R.string.vending_overview_row_action_install), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview
@Composable
fun AppRowNotInstalledPreview() {
    AppRow(App("org.mozilla.firefox", "Firefox", App.State.NOT_INSTALLED, null))
}

@Preview
@Composable
fun AppRowUpdateablePreview() {
    AppRow(App("org.mozilla.firefox", "Firefox", App.State.UPDATE_AVAILABLE, null))
}

@Preview
@Composable
fun AppRowInstalledPreview() {
    AppRow(App("org.mozilla.firefox", "Firefox", App.State.INSTALLED, null))
}

