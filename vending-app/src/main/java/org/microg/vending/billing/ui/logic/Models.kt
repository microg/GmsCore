/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui.logic

import android.accounts.Account
import android.os.Bundle
import org.microg.vending.billing.core.ui.AcquireParsedResult
import org.microg.vending.billing.core.ui.BAction
import org.microg.vending.billing.core.ui.BScreen

data class BillingUiViewState(
    val visible: Boolean = false,
    val screenMap: MutableMap<String, BScreen> = mutableMapOf(),
    val showScreen: BScreen = BScreen(),
    val result: Bundle = Bundle.EMPTY,
    val actionContextList: MutableList<ByteArray> = mutableListOf(),
    val onClickAction: (BAction?) -> Unit
)

data class PasswdInputViewState(
    var visible: Boolean = false,
    var label: String = "",
    var hasError: Boolean = false,
    var errMsg: ErrorMessageRef? = null,
    var checked: Boolean = false,
    val onButtonClicked: (passwd: String) -> Unit,
    val onCheckedChange: (value: Boolean) -> Unit,
    val onDismissRequest: () -> Unit
)

data class BuyFlowResult(
    val acquireResult: AcquireParsedResult?,
    val account: Account?,
    val result: Bundle
)