/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import org.microg.gms.smartdevice.d2d.R

/**
 * Source-device authentication bridge.
 *
 * The ordinary flow displays the existing copy-confirmation content before launching Android's
 * PIN/pattern/password UI. Account-picker flows use the same Fragment without a view. Android
 * owns the credential UI in both modes while this Fragment preserves the request/result lifecycle.
 */
class LockScreenFragment : Fragment() {
    interface Listener {
        fun onLockScreenSuccess(authenticationType: Int)
        fun onLockScreenCancelled()
        fun onLockScreenExplicitlyCancelled()
    }

    private var listener: Listener? = null
    private var launchPending = false
    private var requestInFlight = false

    private val credentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        requestInFlight = false
        launchPending = false
        if (result.resultCode == Activity.RESULT_OK) {
            listener?.onLockScreenSuccess(AUTH_CLASS_DEVICE_CREDENTIAL)
        } else {
            listener?.onLockScreenCancelled()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
            ?: error("Host activity must implement LockScreenFragment.Listener")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchPending = savedInstanceState?.getBoolean(STATE_LAUNCH_PENDING)
            ?: requireArguments().getBoolean(ARG_LAUNCH_ON_START, true)
        requestInFlight = savedInstanceState?.getBoolean(STATE_REQUEST_IN_FLIGHT) == true
    }

    override fun onResume() {
        super.onResume()
        launchIfReady()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = if (requireArguments().getBoolean(ARG_SHOW_CONFIRMATION, false)) {
        inflater.inflate(R.layout.d2d_source_confirmation, container, false)
    } else {
        null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = requireArguments()
        view.findViewById<TextView>(R.id.d2d_confirmation_title).text =
            arguments.getString(ARG_TITLE).orEmpty()
        view.findViewById<TextView>(R.id.d2d_confirmation_message).text =
            arguments.getString(ARG_MESSAGE).orEmpty()
        view.findViewById<Button>(R.id.d2d_confirmation_cancel).setOnClickListener {
            listener?.onLockScreenExplicitlyCancelled()
        }
        view.findViewById<Button>(R.id.d2d_confirmation_continue).apply {
            text = arguments.getString(ARG_CONTINUE_TEXT).orEmpty()
            setOnClickListener { requestAuthentication() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LAUNCH_PENDING, launchPending)
        outState.putBoolean(STATE_REQUEST_IN_FLIGHT, requestInFlight)
        super.onSaveInstanceState(outState)
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    fun requestAuthentication() {
        if (requestInFlight) return
        launchPending = true
        if (isResumed) launchIfReady()
    }

    private fun launchIfReady() {
        if (!launchPending || requestInFlight) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dispatchUnavailable()
            return
        }

        val keyguard = requireContext().getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val credentialIntent = keyguard?.createConfirmDeviceCredentialIntent(null, null)
        if (credentialIntent == null) {
            Log.w(TAG, "System credential confirmation is unavailable")
            dispatchUnavailable()
            return
        }

        launchPending = false
        requestInFlight = true
        runCatching {
            credentialLauncher.launch(credentialIntent)
        }.onFailure { error ->
            requestInFlight = false
            Log.w(TAG, "Unable to launch system credential confirmation", error)
            listener?.onLockScreenCancelled()
        }
    }

    private fun dispatchUnavailable() {
        launchPending = false
        requestInFlight = false
        listener?.onLockScreenCancelled()
    }

    companion object {
        private const val TAG = "D2DLockScreen"
        private const val ARG_LAUNCH_ON_START = "launchOnStart"
        private const val ARG_SHOW_CONFIRMATION = "showConfirmation"
        private const val ARG_TITLE = "confirmationTitle"
        private const val ARG_MESSAGE = "confirmationMessage"
        private const val ARG_CONTINUE_TEXT = "confirmationContinueText"
        private const val STATE_LAUNCH_PENDING = "launchPending"
        private const val STATE_REQUEST_IN_FLIGHT = "requestInFlight"
        private const val AUTH_CLASS_DEVICE_CREDENTIAL = 0

        fun newHeadlessInstance(): LockScreenFragment = LockScreenFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_LAUNCH_ON_START, true) }
        }

        fun newConfirmationInstance(
            title: String,
            message: String,
            continueText: String,
        ): LockScreenFragment = LockScreenFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_LAUNCH_ON_START, false)
                putBoolean(ARG_SHOW_CONFIRMATION, true)
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
                putString(ARG_CONTINUE_TEXT, continueText)
            }
        }
    }
}
