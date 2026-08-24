/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.FAMILY_PAGE_CONTENT_NEGATIVE_BUTTON_INDEX
import com.google.android.gms.family.v2.manage.FAMILY_PAGE_CONTENT_POSITIVE_BUTTON_INDEX
import com.google.android.gms.family.v2.manage.FAMILY_PAGE_CONTENT_TEXT_INDEX
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.model.PasswordCheckState
import com.google.android.gms.family.v2.model.HelpData
import org.microg.gms.family.FamilyRole

@Composable
fun FamilyDeleteFragmentScreen(
    viewModel: FamilyViewModel,
    displayName: String,
    leaveFamily: Boolean,
    onHelpClick: (HelpData) -> Unit,
    onCancelDelete: () -> Unit,
    onValidatePassword: (String, MemberDataModel) -> Unit,
    onCheckPasswordSuccess: (MemberDataModel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val passwordState by viewModel.passwordCheckState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(passwordState) {
        if (passwordState is PasswordCheckState.Success) {
            onCheckPasswordSuccess((passwordState as PasswordCheckState.Success).member)
            viewModel.resetPasswordState()
            showDialog = false
            password = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            uiState.pageData?.let { pageData ->
                HtmlTextWithHelpLinks(
                    htmlContent = pageData.sectionMap[FAMILY_PAGE_CONTENT_TEXT_INDEX] ?: "",
                    helpMap = pageData.helpMap ?: emptyMap(),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    onHelpClick = onHelpClick
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancelDelete) {
                    Text(text = uiState.pageData?.sectionMap?.get(FAMILY_PAGE_CONTENT_NEGATIVE_BUTTON_INDEX) ?: "", fontSize = 13.sp)
                }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { showDialog = true }) {
                    Text(text = uiState.pageData?.sectionMap?.get(FAMILY_PAGE_CONTENT_POSITIVE_BUTTON_INDEX) ?: "", fontSize = 13.sp)
                }
            }
        }

        if (showDialog) {
            PasswordDialog(
                currentModel = uiState.currentMember,
                displayName = displayName,
                leaveFamily = leaveFamily,
                password = password,
                isChecking = passwordState is PasswordCheckState.Checking,
                errorMessage = if (passwordState is PasswordCheckState.Error) (passwordState as PasswordCheckState.Error).message else null,
                onPasswordChange = { password = it },
                onConfirm = {
                    if (password.isNotEmpty()) {
                        onValidatePassword(password, uiState.currentMember)
                    }
                },
                onCancel = {
                    viewModel.resetPasswordState()
                    password = ""
                    showDialog = false
                })
        }
    }
}

@Composable
fun HtmlTextWithHelpLinks(
    htmlContent: String, helpMap: Map<String, HelpData>, textColor: Color, onHelpClick: (HelpData) -> Unit
) {
    val annotatedString = remember(htmlContent, helpMap) {
        buildAnnotatedString {
            val spanned = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)

            val plainText = spanned.toString()
            append(plainText)

            val regex = Regex("<help-(.*?)>(.*?)</help-\\1>", RegexOption.IGNORE_CASE)
            regex.findAll(htmlContent).forEach { match ->
                val helpKey = match.groupValues[1]
                val displayText = match.groupValues[2]
                val startIndex = plainText.indexOf(displayText)
                if (startIndex >= 0) {
                    val endIndex = startIndex + displayText.length
                    addStyle(
                        style = SpanStyle(
                            color = Color.Blue, textDecoration = TextDecoration.None
                        ), start = startIndex, end = endIndex
                    )
                    addStringAnnotation(
                        tag = "HELP_TAG", annotation = helpKey, start = startIndex, end = endIndex
                    )
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = LocalTextStyle.current.copy(
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations("HELP_TAG", offset, offset).firstOrNull()?.let { annotation ->
                helpMap[annotation.item]?.let { helpData ->
                    onHelpClick(helpData)
                }
            }
        })
}

@Composable
private fun PasswordDialog(
    currentModel: MemberDataModel,
    displayName: String,
    password: String,
    leaveFamily: Boolean,
    isChecking: Boolean,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            val title = if (leaveFamily) {
                stringResource(id = R.string.family_management_remove_member_password_title, displayName)
            } else if (currentModel.role == FamilyRole.HEAD_OF_HOUSEHOLD.value) {
                stringResource(id = R.string.family_management_delete_family_password_title)
            } else {
                stringResource(id = R.string.family_management_leave_family_password_title)
            }
            Text(text = title)
        },
        text = {
            Column {
                Text(currentModel.email)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = { Text(stringResource(R.string.family_management_input_pwd)) },
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = Color.Red) }
                    })
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm, enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(id = R.string.family_management_delete_group_confirm), fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(id = R.string.family_management_delete_group_cancel), fontSize = 13.sp)
            }
        })
}