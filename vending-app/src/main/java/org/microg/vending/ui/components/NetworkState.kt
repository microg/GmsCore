package org.microg.vending.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.vending.R
import org.microg.vending.ui.NetworkState

@Composable
fun NetworkState(networkState: NetworkState, retry: () -> Unit, content: @Composable () -> Unit) {
    when (networkState) {
        NetworkState.ACTIVE -> {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        NetworkState.ERROR -> {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_network))
                    Button(retry, Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.error_retry))
                    }
                }
            }
        }

        NetworkState.PASSIVE -> {
            content()
        }
    }
}


@Preview
@Composable
fun NetworkStateActivePreview() {
    NetworkState(NetworkState.ACTIVE, { }) {}
}

@Preview
@Composable
fun NetworkStateErrorPreview() {
    NetworkState(NetworkState.ERROR, { }) {}
}

@Preview
@Composable
fun NetworkStatePassivePreview() {
    NetworkState(NetworkState.PASSIVE, {}) {
        Text("Network operation complete.", Modifier.padding(16.dp))
    }
}