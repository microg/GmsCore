/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.microg.gms.recaptcha.core.R

private val FilledButtonColor = Color(0xFF1A73E8)
private val LinkButtonColor = Color(0xFF1967D2)

internal enum class VerificationStatus {
    CONFIRM,
    LOADING,
    FAILED,
    VERIFIED,
}

private fun VerificationStatus.titleRes(): Int = when (this) {
    VerificationStatus.CONFIRM -> R.string.recaptcha_qr_confirm_title
    VerificationStatus.LOADING -> R.string.recaptcha_qr_progress_title
    VerificationStatus.FAILED -> R.string.recaptcha_qr_failed_title
    VerificationStatus.VERIFIED -> R.string.recaptcha_qr_success_title
}

private fun VerificationStatus.descriptionRes(): Int = when (this) {
    VerificationStatus.CONFIRM -> R.string.recaptcha_qr_confirm_description
    VerificationStatus.LOADING -> R.string.recaptcha_qr_progress_description
    VerificationStatus.FAILED -> R.string.recaptcha_qr_failed_description
    VerificationStatus.VERIFIED -> R.string.recaptcha_qr_success_description
}

private fun VerificationStatus.primaryButtonRes(): Int = when (this) {
    VerificationStatus.CONFIRM -> R.string.recaptcha_qr_confirm_button
    VerificationStatus.LOADING -> R.string.recaptcha_qr_progress_button
    VerificationStatus.FAILED -> R.string.recaptcha_qr_failed_button
    VerificationStatus.VERIFIED -> R.string.recaptcha_qr_success_button
}

private fun VerificationStatus.illustrationRes(): Int = when (this) {
    VerificationStatus.CONFIRM -> R.drawable.ic_recaptcha_qr_confirm
    VerificationStatus.LOADING -> R.drawable.ic_recaptcha_qr_progress
    VerificationStatus.FAILED -> R.drawable.ic_recaptcha_qr_failed
    VerificationStatus.VERIFIED -> R.drawable.ic_recaptcha_qr_success
}

@Composable
internal fun RecaptchaDeepLinkScreen(
    status: VerificationStatus,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(status.illustrationRes()),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(status.titleRes()),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(status.descriptionRes()),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            PrimaryButton(status, onPrimaryButtonClick)
            if (status == VerificationStatus.CONFIRM) {
                TextButton(onClick = onSecondaryButtonClick) {
                    Text(
                        text = stringResource(R.string.recaptcha_qr_confirm_cancel_button),
                        color = LinkButtonColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(status: VerificationStatus, onClick: () -> Unit) {
    val label = stringResource(status.primaryButtonRes())
    when (status) {
        VerificationStatus.CONFIRM,
        VerificationStatus.VERIFIED -> Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FilledButtonColor),
        ) {
            Text(label, color = Color.White)
        }

        VerificationStatus.FAILED -> OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label, color = LinkButtonColor)
        }

        VerificationStatus.LOADING -> TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LinkButtonColor,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.size(8.dp))
            Text(label, color = LinkButtonColor)
        }
    }
}
