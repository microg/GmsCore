package org.microg.vending.billing.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDialog(visible: Boolean) {
    if (visible) {
        BackHandler(enabled = true) {

        }
        Box(
            modifier = Modifier
                .height(LocalConfiguration.current.screenHeightDp.dp.times(0.3f))
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}