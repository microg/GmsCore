/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.smartdevice.d2d.BootstrapAccount
import org.microg.gms.smartdevice.d2d.R
import java.util.ArrayList

/** Source-side account picker that shows every local account before confirmation. */
class SourceAccountPickerFragment : Fragment() {

    interface Listener {
        /** The user confirmed a non-empty selection. */
        fun onAccountsSelected(selected: ArrayList<BootstrapAccount>)
        /** The user chose to continue without transferring accounts. */
        fun onSkipped()
        /** The user tapped the cancel button. The host should abort the transfer. */
        fun onCancelled()
    }

    private var listener: Listener? = null
    private var selectable: BooleanArray = BooleanArray(0)
    private var accounts: ArrayList<BootstrapAccount> = ArrayList()
    private var skipButtonVisible = true
    private var kidsOnboardingEnforced = false
    private var selectedAccountIndex = -1
    private var actionInFlight = false
    private var pickerState: SourceAccountPickerState? = null
    private var accountContainer: LinearLayout? = null
    private var primaryButton: Button? = null
    private var secondaryButton: Button? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (parentFragment as? Listener) ?: (context as? Listener)
            ?: throw IllegalStateException(
                "Containing activity must implement SourceAccountPickerFragment.Listener"
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        accounts = (savedInstanceState?.getParcelableArrayList<BootstrapAccount>(STATE_ACCOUNTS)
            ?: arguments?.getParcelableArrayList<BootstrapAccount>(ARG_ACCOUNTS)
            ?: ArrayList<BootstrapAccount>())
        selectable = (savedInstanceState?.getBooleanArray(STATE_SELECTABLE)
            ?: arguments?.getBooleanArray(ARG_SELECTABLE)
            ?: BooleanArray(0))
        require(selectable.size == accounts.size) { "Account selectability must match accounts" }
        skipButtonVisible = savedInstanceState?.getBoolean(STATE_SKIP_BUTTON_VISIBLE)
            ?: arguments?.getBoolean(ARG_SKIP_BUTTON_VISIBLE, true)
            ?: true
        kidsOnboardingEnforced = savedInstanceState?.getBoolean(STATE_KIDS_ONBOARDING_ENFORCED)
            ?: arguments?.getBoolean(ARG_KIDS_ONBOARDING_ENFORCED, false)
            ?: false
        selectedAccountIndex = savedInstanceState?.getInt(STATE_SELECTED_ACCOUNT_INDEX) ?: -1
        actionInFlight = savedInstanceState?.getBoolean(STATE_ACTION_IN_FLIGHT) == true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.d2d_source_account_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pickerTitle = arguments?.getString(ARG_TITLE)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_accounts_title)
        val pickerSubtitle = arguments?.getString(ARG_SUBTITLE)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_accounts_subtitle)
        view.findViewById<TextView>(R.id.d2d_account_picker_title).text = pickerTitle
        view.findViewById<TextView>(R.id.d2d_account_picker_subtitle).text = pickerSubtitle
        activity?.title = pickerTitle

        pickerState = SourceAccountPickerState(selectable, selectedAccountIndex)
        selectedAccountIndex = pickerState?.selectedIndex ?: -1

        accountContainer = view.findViewById(R.id.d2d_account_container)
        primaryButton = view.findViewById(R.id.d2d_continue_button)
        secondaryButton = view.findViewById(R.id.d2d_skip_button)
        renderState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_ACCOUNTS, accounts)
        outState.putBooleanArray(STATE_SELECTABLE, selectable)
        outState.putBoolean(STATE_SKIP_BUTTON_VISIBLE, skipButtonVisible)
        outState.putBoolean(STATE_KIDS_ONBOARDING_ENFORCED, kidsOnboardingEnforced)
        outState.putInt(STATE_SELECTED_ACCOUNT_INDEX, pickerState?.selectedIndex ?: selectedAccountIndex)
        outState.putBoolean(STATE_ACTION_IN_FLIGHT, actionInFlight)
    }

    /** Re-enables the picker after the system credential screen is dismissed. */
    fun restoreAfterCredentialCancellation(): Boolean {
        if (view == null) return false
        actionInFlight = false
        renderState()
        return true
    }

    /** Snapshot used by the V2 host to rebuild this state after a configuration change. */
    internal fun accountSnapshot(): ArrayList<BootstrapAccount> = ArrayList(accounts)

    override fun onDestroyView() {
        accountContainer = null
        primaryButton = null
        secondaryButton = null
        pickerState = null
        super.onDestroyView()
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    private fun renderState() {
        val container = accountContainer ?: return
        val state = pickerState ?: return
        container.removeAllViews()
        state.visibleAccountIndices.forEach { index ->
            addAccountRow(
                container = container,
                index = index,
                clickable = selectable[index],
                selected = index == state.selectedIndex,
            )
        }
        primaryButton?.apply {
            visibility = if (state.canContinue) View.VISIBLE else View.GONE
            text = getString(R.string.d2d_continue)
            isEnabled = !actionInFlight && state.canContinue
            setOnClickListener { submitAccount(state.selectedIndex) }
        }
        configureSecondaryAction()
    }

    private fun addAccountRow(
        container: LinearLayout,
        index: Int,
        clickable: Boolean,
        selected: Boolean,
    ) {
        val account = accounts[index]
        val row = layoutInflater.inflate(R.layout.d2d_source_account_row, container, false)
        row.findViewById<TextView>(R.id.d2d_account_name).text = account.displayLabel()
        val interactive = clickable && selectable[index] && !actionInFlight
        row.isSelected = selected
        row.isEnabled = if (clickable) interactive else true
        row.isClickable = interactive
        row.isFocusable = interactive
        row.alpha = if (selectable[index]) 1f else 0.55f
        if (interactive) {
            row.setOnClickListener {
                if (pickerState?.select(index) == true) {
                    selectedAccountIndex = index
                    renderState()
                }
            }
        } else {
            row.setOnClickListener(null)
        }
        val density = resources.displayMetrics.density
        container.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = (density * 10 + 0.5f).toInt() },
        )
    }

    private fun configureSecondaryAction() {
        secondaryButton?.apply {
            visibility = View.VISIBLE
            isEnabled = !actionInFlight
            when {
                skipButtonVisible && kidsOnboardingEnforced -> {
                    text = getString(R.string.d2d_cancel)
                    setOnClickListener { listener?.onCancelled() }
                }
                skipButtonVisible -> {
                    text = getString(R.string.d2d_skip)
                    setOnClickListener {
                        actionInFlight = true
                        renderState()
                        listener?.onSkipped()
                    }
                }
                else -> visibility = View.GONE
            }
        }
    }

    private fun submitAccount(index: Int) {
        if (index !in accounts.indices || !selectable[index] || actionInFlight) return
        actionInFlight = true
        renderState()
        listener?.onAccountsSelected(arrayListOf(accounts[index]))
    }

    private fun BootstrapAccount.displayLabel(): String =
        displayName?.takeIf(String::isNotBlank)
            ?: name?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_unknown_account)

    companion object {
        const val ARG_ACCOUNTS = "accounts"
        const val ARG_SELECTABLE = "selectable"
        const val ARG_TITLE = "pickerTitle"
        const val ARG_SUBTITLE = "pickerSubtitle"
        const val ARG_SKIP_BUTTON_VISIBLE = "skipButtonVisible"
        const val ARG_KIDS_ONBOARDING_ENFORCED = "kidsOnboardingEnforced"

        private const val STATE_ACCOUNTS = "accounts"
        private const val STATE_SELECTABLE = "selectable"
        private const val STATE_SKIP_BUTTON_VISIBLE = "skipButtonVisible"
        private const val STATE_KIDS_ONBOARDING_ENFORCED = "kidsOnboardingEnforced"
        private const val STATE_SELECTED_ACCOUNT_INDEX = "selectedAccountIndex"
        private const val STATE_ACTION_IN_FLIGHT = "actionInFlight"

        fun newInstance(
            accounts: ArrayList<BootstrapAccount>,
            selectable: BooleanArray,
            title: String? = null,
            subtitle: String? = null,
            skipButtonVisible: Boolean = true,
            kidsOnboardingEnforced: Boolean = false,
        ): SourceAccountPickerFragment {
            require(selectable.size == accounts.size) { "Account selectability must match accounts" }
            return SourceAccountPickerFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_ACCOUNTS, accounts)
                    putBooleanArray(ARG_SELECTABLE, selectable.copyOf())
                    putBoolean(ARG_SKIP_BUTTON_VISIBLE, skipButtonVisible)
                    putBoolean(ARG_KIDS_ONBOARDING_ENFORCED, kidsOnboardingEnforced)
                    if (!title.isNullOrBlank()) putString(ARG_TITLE, title)
                    if (!subtitle.isNullOrBlank()) putString(ARG_SUBTITLE, subtitle)
                }
            }
        }
    }
}

private class SourceAccountPickerState(
    selectable: BooleanArray,
    initialSelection: Int,
) {
    private val selectable = selectable.copyOf()

    val visibleAccountIndices: List<Int> = selectable.indices.toList()

    var selectedIndex: Int = initialSelection
        private set

    val canContinue: Boolean
        get() = selectedIndex in selectable.indices && selectable[selectedIndex]

    init {
        if (!canContinue) selectedIndex = selectable.indexOfFirst { it }
    }

    fun select(index: Int): Boolean {
        if (index !in selectable.indices || !selectable[index]) return false
        selectedIndex = index
        return true
    }
}
