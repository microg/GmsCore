/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui.widgets

import android.app.ActionBar
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import org.microg.vending.billing.ui.HtmlText
import org.microg.vending.billing.ui.getWindowWidth
import org.microg.vending.billing.ui.logic.PasswdInputViewState
import com.android.vending.R
import org.microg.vending.billing.ui.logic.ErrorMessageRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswdInputDialog(viewState: PasswdInputViewState) {
    if (viewState.visible) {
        Dialog(onDismissRequest = viewState.onDismissRequest) {
            val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window
            val lp = dialogWindow.attributes
            lp.width = getWindowWidth(LocalContext.current)
            lp.height = ActionBar.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            dialogWindow.attributes = lp
            Surface(
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .wrapContentHeight()
                        .wrapContentWidth()
                ) {
                    var inputText by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(top = 15.dp, start = 20.dp, end = 20.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = inputText,
                        label = { Text(text = viewState.label) },
                        placeholder = { Text(text = stringResource(R.string.tips_input_passwd)) },
                        onValueChange = {
                            inputText = it
                            viewState.hasError = false
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        isError = viewState.hasError,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            errorLabelColor = if (viewState.hasError) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            errorBorderColor = if (viewState.hasError) MaterialTheme.colorScheme.error else LocalContentColor.current
                        ),
                        supportingText = {
                            if (viewState.hasError) {
                                Text(
                                    text = when (viewState.errMsg) {
                                        ErrorMessageRef.PASSWORD_ERROR -> stringResource(R.string.error_passwd)
                                        ErrorMessageRef.NETWORK_ERROR -> stringResource(R.string.error_network)
                                        null -> ""
                                    },
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 15.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            checked = viewState.checked,
                            onCheckedChange = {
                                viewState.onCheckedChange(it)
                            }
                        )
                        Text(
                            text = stringResource(R.string.tips_remember_login_info),
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .align(Alignment.CenterVertically)
                                .clickable {
                                    viewState.onCheckedChange(!viewState.checked)
                                },
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HtmlText(
                            text = "<a href=\"https://accounts.google.com/RecoverAccount?fpOnly=1&amp;Email=${viewState.label}\">${stringResource(R.string.tips_forget_passwd)}</a>",
                            fontSize = 14.sp
                        )
                        HtmlText(
                            modifier = Modifier.padding(start = 5.dp),
                            text = "<a href=\"https://support.google.com/googleplay/answer/1626831\" target=\"_blank\">${stringResource(R.string.tips_more_details)}</a>",
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 20.dp, end = 20.dp),
                        enabled = inputText.isNotBlank(),
                        onClick = { viewState.onButtonClicked(inputText) },
                        shape = RoundedCornerShape(20),
                        contentPadding = PaddingValues(7.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.text_verify_button)
                        )
                    }
                }
            }
        }
    }
}