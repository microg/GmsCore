/*
 * SPDX-FileCopyrightText: 2026 HtheB
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.android.vending.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.ContextProvider
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.InAppBillingServiceImpl
import org.microg.vending.billing.PurchaseManager
import org.microg.vending.billing.core.GetPurchaseHistoryParams
import org.microg.vending.billing.core.PurchaseItem
import java.text.DateFormat
import java.util.Date

private const val BILLING_PERMISSION = "com.android.vending.BILLING"
private const val API_VERSION = 24
private const val MAX_HISTORY_PAGES = 20
private const val PREFS_NAME = "purchase_recovery"
private const val PREF_LAST_TOKEN = "last_restored_token"
private const val PREF_LAST_ACCOUNT = "last_restored_account"
private const val PREF_LAST_PACKAGE = "last_restored_package"

@RequiresApi(21)
class PurchaseRecoveryActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var accountSpinner: Spinner
    private lateinit var appSpinner: Spinner
    private lateinit var scanButton: Button
    private lateinit var undoButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var results: LinearLayout
    private var accounts: List<Account> = emptyList()
    private var apps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfileManager.ensureInitialized(this)
        ContextProvider.init(application)
        title = getString(R.string.purchase_recovery_title)
        buildUi()
        loadChoices()
    }

    private fun buildUi() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.purchase_recovery_title)
            textSize = 26f
        }, matchWrap().apply { bottomMargin = dp(12) })
        content.addView(TextView(this).apply {
            text = getString(R.string.purchase_recovery_intro)
            textSize = 15f
            movementMethod = LinkMovementMethod.getInstance()
        }, matchWrap().apply { bottomMargin = dp(24) })

        content.addView(label(R.string.purchase_recovery_account))
        accountSpinner = Spinner(this)
        content.addView(accountSpinner, matchWrap().apply { bottomMargin = dp(16) })

        content.addView(label(R.string.purchase_recovery_app))
        appSpinner = Spinner(this)
        content.addView(appSpinner, matchWrap().apply { bottomMargin = dp(20) })

        scanButton = Button(this).apply {
            text = getString(R.string.purchase_recovery_scan)
            setOnClickListener { scanSelectedApp() }
        }
        content.addView(scanButton, matchWrap())

        progress = ProgressBar(this).apply {
            visibility = View.GONE
        }
        content.addView(progress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(16)
            bottomMargin = dp(8)
        })

        undoButton = Button(this).apply {
            text = getString(R.string.purchase_recovery_undo)
            setOnClickListener { confirmUndo() }
        }
        content.addView(undoButton, matchWrap().apply { topMargin = dp(8) })

        results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(results, matchWrap().apply { topMargin = dp(16) })

        setContentView(ScrollView(this).apply { addView(content) })
        updateUndoButton()
    }

    private fun loadChoices() {
        accounts = AccountManager.get(this).getAccountsByType(DEFAULT_ACCOUNT_TYPE).toList()
        apps = packageManager.getInstalledPackages(android.content.pm.PackageManager.GET_PERMISSIONS)
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { it.requestedPermissions?.contains(BILLING_PERMISSION) == true }
            .mapNotNull { packageInfo ->
                runCatching {
                    AppEntry(
                        packageInfo.applicationInfo?.loadLabel(packageManager)?.toString()
                            ?: packageInfo.packageName,
                        packageInfo.packageName
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()

        accountSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            accounts.map { it.name }
        )
        appSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            apps.map { "${it.label} (${it.packageName})" }
        )

        scanButton.isEnabled = accounts.isNotEmpty() && apps.isNotEmpty()
        if (accounts.isEmpty()) addResultMessage(getString(R.string.purchase_recovery_no_accounts))
        if (apps.isEmpty()) addResultMessage(getString(R.string.purchase_recovery_no_apps))
    }

    private fun scanSelectedApp() {
        val account = accounts.getOrNull(accountSpinner.selectedItemPosition) ?: return
        val app = apps.getOrNull(appSpinner.selectedItemPosition) ?: return
        setWorking(true)
        results.removeAllViews()
        addResultMessage(getString(R.string.purchase_recovery_scanning))
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val inApp = loadHistory(account, app.packageName, "inapp")
                        .distinctBy { it.purchaseToken.ifBlank { it.jsonData } }
                    val subscriptions = loadHistory(account, app.packageName, "subs")
                        .distinctBy { it.purchaseToken.ifBlank { it.jsonData } }
                    val cachedTokens = PurchaseManager
                        .queryPurchases(account, app.packageName, "inapp")
                        .mapTo(mutableSetOf()) { it.purchaseToken }
                    val missing = inApp.filterNot { it.purchaseToken in cachedTokens }
                    ScanResult(
                        inApp = missing,
                        cachedCount = inApp.size - missing.size,
                        subscriptionCount = subscriptions.size
                    )
                }
            }.onSuccess { result ->
                showScanResult(account, app, result)
            }.onFailure { error ->
                results.removeAllViews()
                addResultMessage(getString(
                    R.string.purchase_recovery_error,
                    error.message ?: error.javaClass.simpleName
                ))
            }
            setWorking(false)
        }
    }

    private suspend fun loadHistory(
        account: Account,
        targetPackage: String,
        type: String
    ): List<RecoveryReceipt> {
        val core = InAppBillingServiceImpl.createIAPCore(this, account, targetPackage)
        val receipts = mutableListOf<RecoveryReceipt>()
        val continuations = mutableSetOf<String>()
        var continuation: String? = null
        repeat(MAX_HISTORY_PAGES) {
            val response = core.getPurchaseHistory(GetPurchaseHistoryParams(
                apiVersion = API_VERSION,
                type = type,
                continuationToken = continuation,
                extraParams = mapOf(
                    "playBillingLibraryVersion" to "8.0.0",
                    "enablePendingPurchases" to true
                )
            ))
            if (response.getCode() != 0) {
                throw IllegalStateException(response.getMessage().ifBlank {
                    "Google Play Billing response ${response.getCode()}"
                })
            }
            response.purchaseHistoryList.orEmpty().forEach { item ->
                receipts += RecoveryReceipt(type, item.sku, item.jsonData, item.signature)
            }
            continuation = response.continuationToken
            if (continuation.isNullOrEmpty() || !continuations.add(continuation!!)) return receipts
        }
        return receipts
    }

    private fun showScanResult(account: Account, app: AppEntry, result: ScanResult) {
        results.removeAllViews()
        if (result.inApp.isEmpty() && result.cachedCount == 0 && result.subscriptionCount == 0) {
            addResultMessage(getString(R.string.purchase_recovery_none))
            return
        }
        val summary = when {
            result.inApp.isNotEmpty() -> getString(
                R.string.purchase_recovery_found,
                result.subscriptionCount,
                result.cachedCount
            )
            result.cachedCount > 0 -> getString(
                R.string.purchase_recovery_all_cached,
                result.cachedCount,
                result.subscriptionCount
            )
            else -> getString(
                R.string.purchase_recovery_subscriptions_only,
                result.subscriptionCount
            )
        }
        addResultMessage(summary)
        result.inApp.forEach { receipt ->
            val parsed = runCatching { JSONObject(receipt.jsonData) }.getOrNull()
            val purchaseTime = parsed?.optLong("purchaseTime", 0L) ?: 0L
            val date = if (purchaseTime > 0L) {
                DateFormat.getDateInstance().format(Date(purchaseTime))
            } else {
                "Unknown date"
            }
            results.addView(Button(this).apply {
                text = "${receipt.sku}\n$date"
                isAllCaps = false
                setOnClickListener { confirmRestore(account, app, receipt) }
            }, matchWrap())
        }
    }

    private fun confirmRestore(account: Account, app: AppEntry, receipt: RecoveryReceipt) {
        AlertDialog.Builder(this)
            .setTitle(R.string.purchase_recovery_confirm_title)
            .setMessage(getString(R.string.purchase_recovery_confirm_body, receipt.sku))
            .setNegativeButton(R.string.purchase_recovery_cancel, null)
            .setPositiveButton(R.string.purchase_recovery_restore) { _, _ ->
                restoreReceipt(account, app, receipt)
            }
            .show()
    }

    private fun restoreReceipt(account: Account, app: AppEntry, receipt: RecoveryReceipt) {
        runCatching {
            check(receipt.type == "inapp") { "Only one-time purchases are supported" }
            val json = JSONObject(receipt.jsonData)
            val sku = json.optString("productId")
            val token = json.optString("purchaseToken", json.optString("token"))
            val purchaseTime = json.optLong("purchaseTime", 0L)
            check(sku == receipt.sku) { "Receipt product ID does not match" }
            check(token.isNotBlank()) { "Receipt has no purchase token" }
            check(receipt.signature.isNotBlank()) { "Receipt has no developer signature" }
            check(purchaseTime > 0L) { "Receipt has no purchase time" }
            check(PurchaseManager.queryPurchases(account, app.packageName, "inapp")
                .none { it.purchaseToken == token }) {
                "This receipt is already cached; nothing was changed"
            }

            PurchaseManager.addPurchase(
                account,
                app.packageName,
                PurchaseItem(
                    type = "inapp",
                    sku = sku,
                    pkgName = app.packageName,
                    purchaseToken = token,
                    purchaseState = 0,
                    jsonData = receipt.jsonData,
                    signature = receipt.signature,
                    startAt = purchaseTime,
                    expireAt = Long.MAX_VALUE
                )
            )
            check(PurchaseManager.queryPurchases(account, app.packageName, "inapp")
                .any { it.purchaseToken == token && it.sku == sku }) {
                "FakeStore could not verify the restored receipt"
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_TOKEN, token)
                .putString(PREF_LAST_ACCOUNT, account.name)
                .putString(PREF_LAST_PACKAGE, app.packageName)
                .commit()
            updateUndoButton()
            Toast.makeText(
                this,
                getString(R.string.purchase_recovery_success, app.label),
                Toast.LENGTH_LONG
            ).show()
        }.onFailure { error ->
            Toast.makeText(
                this,
                getString(R.string.purchase_recovery_error, error.message ?: error.javaClass.simpleName),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun confirmUndo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.purchase_recovery_undo_title)
            .setMessage(R.string.purchase_recovery_undo_body)
            .setNegativeButton(R.string.purchase_recovery_cancel, null)
            .setPositiveButton(R.string.purchase_recovery_undo) { _, _ -> undoLastRestore() }
            .show()
    }

    private fun undoLastRestore() {
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val token = preferences.getString(PREF_LAST_TOKEN, null) ?: return
        val accountName = preferences.getString(PREF_LAST_ACCOUNT, null)
        val packageName = preferences.getString(PREF_LAST_PACKAGE, null)
        if (accountName != null && packageName != null) {
            val account = accounts.firstOrNull { it.name == accountName }
                ?: Account(accountName, DEFAULT_ACCOUNT_TYPE)
            val isSameReceipt = PurchaseManager
                .queryPurchases(account, packageName, "inapp")
                .any { it.purchaseToken == token }
            if (isSameReceipt) PurchaseManager.removePurchase(token)
        }
        preferences.edit()
            .remove(PREF_LAST_TOKEN)
            .remove(PREF_LAST_ACCOUNT)
            .remove(PREF_LAST_PACKAGE)
            .apply()
        updateUndoButton()
        Toast.makeText(this, R.string.purchase_recovery_undone, Toast.LENGTH_LONG).show()
    }

    private fun setWorking(working: Boolean) {
        progress.visibility = if (working) View.VISIBLE else View.GONE
        scanButton.isEnabled = !working && accounts.isNotEmpty() && apps.isNotEmpty()
        accountSpinner.isEnabled = !working
        appSpinner.isEnabled = !working
    }

    private fun updateUndoButton() {
        undoButton.isEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .let {
                it.contains(PREF_LAST_TOKEN) &&
                    it.contains(PREF_LAST_ACCOUNT) &&
                    it.contains(PREF_LAST_PACKAGE)
            }
    }

    private fun addResultMessage(message: String) {
        results.addView(TextView(this).apply {
            text = message
            textSize = 15f
            setPadding(0, 12, 0, 12)
        }, matchWrap())
    }

    private fun label(resource: Int) = TextView(this).apply {
        setText(resource)
        textSize = 14f
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private data class AppEntry(val label: String, val packageName: String)

private data class RecoveryReceipt(
    val type: String,
    val sku: String,
    val jsonData: String,
    val signature: String
) {
    val purchaseToken: String
        get() = runCatching {
            val json = JSONObject(jsonData)
            json.optString("purchaseToken", json.optString("token"))
        }.getOrDefault("")
}

private data class ScanResult(
    val inApp: List<RecoveryReceipt>,
    val cachedCount: Int,
    val subscriptionCount: Int
)
