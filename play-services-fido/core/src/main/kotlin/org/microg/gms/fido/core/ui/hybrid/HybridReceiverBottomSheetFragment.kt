/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui.hybrid

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.microg.gms.fido.core.R

class HybridReceiverBottomSheetFragment : BottomSheetDialogFragment() {

    enum class State { INITIAL, CONNECTING, ERROR }

    private var currentState = State.INITIAL
    private var isRegistration = false
    private var rpId: String = ""

    private var onContinue: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var continueBtn: TextView
    private lateinit var cancelBtn: TextView

    companion object {
        const val TAG = "HybridReceiverBottomSheetFragment"
        private const val ARG_IS_REG = "arg_is_registration"
        private const val ARG_RP_ID = "arg_rp_id"
        fun newInstance(isRegistration: Boolean, rpId: String) = HybridReceiverBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_REG, isRegistration)
                putString(ARG_RP_ID, rpId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isRegistration = it.getBoolean(ARG_IS_REG, false)
            rpId = it.getString(ARG_RP_ID, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fido_hybrid_receiver_bottomsheet, container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleView = view.findViewById(R.id.tvTitle)
        bodyView = view.findViewById(R.id.tvBody)
        progressBar = view.findViewById(R.id.progressBar)
        continueBtn = view.findViewById(R.id.btnContinue)
        cancelBtn = view.findViewById(R.id.btnCancel)

        continueBtn.setOnClickListener { onContinue?.invoke() }
        cancelBtn.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }

        updateUI()
    }

    private fun updateUI() {
        when (currentState) {

            State.INITIAL -> {
                titleView.text = getString(R.string.fido_hybrid_receiver_initial_title)
                bodyView.text = getString(
                    if (isRegistration) R.string.fido_hybrid_receiver_initial_body_register
                    else R.string.fido_hybrid_receiver_initial_body_sign
                )
                progressBar.isVisible = false
                continueBtn.isVisible = true
                cancelBtn.isVisible = true
            }

            State.CONNECTING -> {
                titleView.text = getString(R.string.fido_hybrid_receiver_connecting_title)
                bodyView.text = getString(R.string.fido_hybrid_receiver_connecting_body)
                progressBar.isVisible = true
                continueBtn.isVisible = false
                cancelBtn.isVisible = true
            }

            State.ERROR -> {
                titleView.text = getString(R.string.fido_hybrid_receiver_error_title)
                bodyView.text = getString(R.string.fido_hybrid_receiver_error_body)
                progressBar.isVisible = false

                continueBtn.isVisible = false
                cancelBtn.apply {
                    isVisible = true
                    text = getString(android.R.string.ok)
                }
            }
        }
    }

    fun showConnecting() {
        currentState = State.CONNECTING
        if (isAdded) updateUI()
    }

    fun showError() {
        currentState = State.ERROR
        if (isAdded) updateUI()
    }

    fun setOnContinueListener(block: () -> Unit) {
        onContinue = block
    }

    fun setOnCancelListener(block: () -> Unit) {
        onCancel = block
    }
}