/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui.logic

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.volley.VolleyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.microg.gms.utils.toHexString
import org.microg.vending.billing.*
import org.microg.vending.billing.core.ui.ActionType
import org.microg.vending.billing.core.ui.BAction
import org.microg.vending.billing.core.ui.UIType

enum class NotificationEventId {
    FINISH,
    OPEN_PAYMENT_METHOD_ACTIVITY
}

enum class ErrorMessageRef {
    PASSWORD_ERROR,
    NETWORK_ERROR,
}

data class NotificationEvent(
    val id: NotificationEventId,
    val params: Bundle
)

@RequiresApi(21)
class InAppBillingViewModel : ViewModel() {
    private val _event = Channel<NotificationEvent>()
    val event = _event.receiveAsFlow()
    var startParams: Bundle? = null

    var loadingDialogVisible by mutableStateOf(value = false)
        private set
    var billingUiViewState by mutableStateOf(
        value = BillingUiViewState(
            onClickAction = ::handleClickAction
        )
    )
        private set
    var passwdInputViewState by mutableStateOf(
        value = PasswdInputViewState(
            onDismissRequest = ::handlePasswdInputViewDismiss,
            onButtonClicked = ::handlePasswdInput,
            onCheckedChange = ::handlePasswdCheckedChange,
            checked = !SettingsManager(ContextProvider.context).getAuthStatus()
        )
    )
    private lateinit var lastBuyFlowResult: BuyFlowResult

    private fun finishWithResult(result: Bundle) {
        viewModelScope.launch {
            _event.send(NotificationEvent(NotificationEventId.FINISH, result))
        }
    }

    private fun handlePasswdInputViewDismiss() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handlePasswdInputViewDismiss called")
        passwdInputViewState = passwdInputViewState.copy(visible = false)
    }

    private suspend fun submitBuyAction(authToken: String? = null) {
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM) ?: return finishWithResult(
            billingUiViewState.result
        )
        val buyFlowResult =
            InAppBillingServiceImpl.acquireRequest(ContextProvider.context, param, billingUiViewState.actionContextList, authToken)
        handleBuyFlowResult(buyFlowResult)
    }

    private suspend fun doAcquireRequest() {
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM) ?: return finishWithResult(
            billingUiViewState.result
        )
        val buyFlowResult = InAppBillingServiceImpl.acquireRequest(ContextProvider.context, param, billingUiViewState.actionContextList)
        handleBuyFlowResult(buyFlowResult)
    }

    private fun handlePasswdInput(password: String) {
        loadingDialogVisible = true
        passwdInputViewState = passwdInputViewState.copy(visible = false)
        billingUiViewState = billingUiViewState.copy(visible = false)
        viewModelScope.launch(Dispatchers.IO) {
            val param = startParams!!.getString(KEY_IAP_SHEET_UI_PARAM)
            val (statusCode, encodedRapt) = try {
                200 to InAppBillingServiceImpl.requestAuthProofToken(ContextProvider.context, param!!, password)
            } catch (e: VolleyError) {
                Log.w(TAG, e)
                e.networkResponse.statusCode to null
            } catch (e: Exception) {
                -1 to null
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "requestAuthProofToken statusCode=$statusCode, encodedRapt=$encodedRapt")
            if (encodedRapt.isNullOrEmpty()) {
                loadingDialogVisible = false
                val errMsg = when (statusCode) {
                    400 -> ErrorMessageRef.PASSWORD_ERROR
                    else -> ErrorMessageRef.NETWORK_ERROR
                }
                passwdInputViewState =
                    passwdInputViewState.copy(visible = true, hasError = true, errMsg = errMsg)
                billingUiViewState = billingUiViewState.copy(visible = true)
                return@launch
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handleBuyButtonClicked encodedRapt: $encodedRapt")
            SettingsManager(ContextProvider.context).setAuthStatus(!passwdInputViewState.checked)
            submitBuyAction(authToken = encodedRapt)
        }
    }

    private fun handlePasswdCheckedChange(checked: Boolean) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handlePasswdCheckedChange checked: $checked")
        passwdInputViewState = passwdInputViewState.copy(checked = checked)
    }

    private fun handleBuyButtonClicked(action: BAction) {
        val nextShowScreen = billingUiViewState.screenMap[action.screenId]
            ?: return finishWithResult(billingUiViewState.result)
        when (val uiType = nextShowScreen.uiInfo?.uiType) {
            UIType.LOADING_SPINNER -> {
                showLoading()
                billingUiViewState.actionContextList.addAll(action.actionContext)
                viewModelScope.launch(Dispatchers.IO) {
                    submitBuyAction()
                }
            }

            UIType.PURCHASE_AUTH_SCREEN -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handleBuyButtonClicked need auth")
                val account =
                    lastBuyFlowResult.account ?: return finishWithResult(billingUiViewState.result)
                // TODO: Magic constants? These are protobufs!
                billingUiViewState.actionContextList.add("ea010408011001b80301".decodeHex())
                billingUiViewState.actionContextList.add("0a020802b80301".decodeHex())
                passwdInputViewState = passwdInputViewState.copy(
                    visible = true,
                    label = account.name
                )
            }

            else -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handleBuyButtonClicked unknown next uiType: $uiType")
                finishWithResult(billingUiViewState.result)
            }
        }
    }

    private fun showLoading() {
        loadingDialogVisible = true
        passwdInputViewState = passwdInputViewState.copy(visible = false)
        billingUiViewState = billingUiViewState.copy(visible = false)
    }

    private fun handleClickAction(action: BAction?) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handleClickAction action: $action (contexts: ${action?.actionContext?.map { it.toHexString() }}")
        when (action?.type) {
            ActionType.SHOW -> {
                when (action.uiInfo?.uiType) {
                    UIType.PURCHASE_CART_BUY_BUTTON -> handleBuyButtonClicked(action)
                    UIType.PURCHASE_CHANGE_SUBSCRIPTION_CONTINUE_BUTTON,
                    UIType.PURCHASE_PAYMENT_DECLINED_CONTINUE_BUTTON,
                    UIType.PURCHASE_CART_PAYMENT_OPTIONS_LINK,
                    UIType.PURCHASE_CART_CONTINUE_BUTTON,
                    UIType.BILLING_PROFILE_SCREEN_ABANDON -> {
                        if (action.screenId?.isNotBlank() == true) {
                            if (showScreen(action.screenId!!)) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "showScreen ${action.screenId} success")
                                return
                            }
                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "showScreen ${action.screenId} false")
                        }
                        finishWithResult(billingUiViewState.result)
                    }

                    UIType.BILLING_PROFILE_OPTION_CREATE_INSTRUMENT,
                    UIType.BILLING_PROFILE_OPTION_ADD_PLAY_CREDIT,
                    UIType.BILLING_PROFILE_OPTION_REDEEM_CODE -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            showPaymentMethodPage("action")
                        }
                    }

                    else -> finishWithResult(billingUiViewState.result)
                }
            }

            ActionType.DELAY -> {
                viewModelScope.launch {
                    delay(action.delay!!.toLong())
                    finishWithResult(billingUiViewState.result)
                }
            }

            else -> {
                when (action?.uiInfo?.uiType) {
                    UIType.PURCHASE_CART_CONTINUE_BUTTON -> viewModelScope.launch(Dispatchers.IO) {
                        submitBuyAction()
                    }

                    UIType.PURCHASE_SUCCESS_SCREEN_WITH_AUTH_CHOICES -> {
                        viewModelScope.launch {
                            delay(3000)
                            finishWithResult(billingUiViewState.result)
                        }
                    }

                    UIType.BILLING_PROFILE_EXISTING_INSTRUMENT -> {
                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "switch payment method context: ${action.actionContext[0].toHexString()}")
                        showLoading()
                        billingUiViewState.actionContextList.addAll(action.actionContext)
                        viewModelScope.launch(Dispatchers.IO) {
                            doAcquireRequest()
                        }
                    }

                    UIType.PURCHASE_CART_PAYMENT_OPTIONS_LINK -> {
                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "open payment option page context: ${action.actionContext[0].toHexString()}")
                        showLoading()
                        billingUiViewState.actionContextList.addAll(action.actionContext)
                        viewModelScope.launch(Dispatchers.IO) {
                            doAcquireRequest()
                        }
                    }

                    else -> finishWithResult(billingUiViewState.result)
                }
            }
        }
    }

    private fun showScreen(screenId: String): Boolean {
        val showScreen = billingUiViewState.screenMap[screenId] ?: return false
        billingUiViewState = billingUiViewState.copy(
            showScreen = showScreen,
            visible = true
        )
        loadingDialogVisible = false
        return true
    }

    private suspend fun showPaymentMethodPage(src: String): Boolean {
        _event.send(
            NotificationEvent(
                NotificationEventId.OPEN_PAYMENT_METHOD_ACTIVITY,
                bundleOf("account" to lastBuyFlowResult.account, "src" to src)
            )
        )
        return true
    }

    private suspend fun handleBuyFlowResult(buyFlowResult: BuyFlowResult) {
        val failAction = suspend {
            _event.send(NotificationEvent(NotificationEventId.FINISH, buyFlowResult.result))
        }
        val action = buyFlowResult.acquireResult?.action ?: return failAction()
        val screenMap = buyFlowResult.acquireResult.screenMap
        val showScreen = screenMap[action.screenId] ?: return failAction()
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "handleAcquireResult, showScreen:$showScreen result:${buyFlowResult.acquireResult}")
        if (action.type != ActionType.SHOW) return failAction()
        lastBuyFlowResult = buyFlowResult
        billingUiViewState.screenMap.putAll(screenMap)
        billingUiViewState = billingUiViewState.copy(
            showScreen = showScreen,
            result = buyFlowResult.result,
            actionContextList = action.actionContext,
            visible = true
        )
        loadingDialogVisible = false
    }

    private suspend fun doLoadSheetUIAction(context: Context, param: String) {
        val buyFlowResult = InAppBillingServiceImpl.acquireRequest(context, param, firstRequest = true)
        handleBuyFlowResult(buyFlowResult)
    }

    suspend fun loadData(context: Context) {
        val param = startParams?.getString(KEY_IAP_SHEET_UI_PARAM)
            ?: throw RuntimeException("get action param failed")
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "loadData param:$param")
        showLoading()
        doLoadSheetUIAction(context, param)
    }

    fun close() {
        val result = if (billingUiViewState.result.containsKey("INAPP_PURCHASE_DATA"))
            billingUiViewState.result
        else
            resultBundle(BillingClient.BillingResponseCode.USER_CANCELED, "")
        finishWithResult(result)
    }
}