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
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.microg.gms.smartdevice.d2d.R

/** Fragment displaying the source-side "Copy to your new device?" confirmation panel. */
class SourceConfirmationFragment : Fragment() {

    interface Listener {
        /** The user tapped the primary (continue / OK) button. */
        fun onConfirmed()
        /** The user tapped the cancel button. The host should abort the transfer. */
        fun onCancelled()
    }

    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (parentFragment as? Listener) ?: (context as? Listener)
            ?: throw IllegalStateException(
                "Containing activity must implement SourceConfirmationFragment.Listener"
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.d2d_source_confirmation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleText = arguments?.getCharSequence(ARG_TITLE)?.toString()?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_title)
        val messageText = arguments?.getCharSequence(ARG_MESSAGE)?.toString()?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_message)
        val continueText = arguments?.getString(ARG_CONTINUE_TEXT)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_action)
        view.findViewById<TextView>(R.id.d2d_confirmation_title).text = titleText
        view.findViewById<TextView>(R.id.d2d_confirmation_message).text = messageText

        val cancelButton = view.findViewById<Button>(R.id.d2d_confirmation_cancel)
        val continueButton = view.findViewById<Button>(R.id.d2d_confirmation_continue)
        cancelButton.setOnClickListener {
            listener?.onCancelled()
        }
        continueButton.text = continueText
        continueButton.setOnClickListener {
            listener?.onConfirmed()
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    companion object {
        const val ARG_TITLE = "confirmationTitle"
        const val ARG_MESSAGE = "confirmationMessage"
        const val ARG_CONTINUE_TEXT = "confirmationContinueText"

        /**
         * Standard "Copy to your new device?" copy-confirmation panel.
         */
        fun newCopyConfirmation(
            title: String? = null,
            message: String? = null,
            continueText: String? = null,
        ): SourceConfirmationFragment = SourceConfirmationFragment().apply {
            arguments = Bundle().apply {
                if (!title.isNullOrBlank()) putString(ARG_TITLE, title)
                if (!message.isNullOrBlank()) putString(ARG_MESSAGE, message)
                if (!continueText.isNullOrBlank()) putString(ARG_CONTINUE_TEXT, continueText)
            }
        }
    }
}
