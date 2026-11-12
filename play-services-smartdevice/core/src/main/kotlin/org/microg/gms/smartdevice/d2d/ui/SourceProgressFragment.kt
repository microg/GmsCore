/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d.ui

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.microg.gms.smartdevice.d2d.R

/** Fragment displaying source-side transfer progress. */
class SourceProgressFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.d2d_source_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleText = arguments?.getString(ARG_TITLE)?.takeIf(String::isNotBlank)
        val messageText = arguments?.getString(ARG_MESSAGE)?.takeIf(String::isNotBlank)
        view.findViewById<TextView>(R.id.d2d_progress_title).apply {
            text = titleText
            visibility = if (titleText == null) View.GONE else View.VISIBLE
        }
        view.findViewById<TextView>(R.id.d2d_progress_message).apply {
            text = messageText
            visibility = if (messageText == null) View.GONE else View.VISIBLE
        }
        val progressBar = view.findViewById<ProgressBar>(R.id.d2d_progress_indicator)
        tintProgressBar(progressBar)
        // Do not retain a picker/confirmation title when the service did not supply
        // progress copy. The spinner remains the only visible progress affordance.
        activity?.title = titleText.orEmpty()
    }

    private fun tintProgressBar(target: ProgressBar) {
        if (Build.VERSION.SDK_INT >= 21) {
            val tint = ColorStateList.valueOf(resources.getColor(R.color.d2d_primary))
            target.indeterminateTintList = tint
            target.progressTintList = tint
        }
    }

    companion object {
        const val ARG_TITLE = "progressTitle"
        const val ARG_MESSAGE = "progressMessage"

        fun newInstance(
            title: String? = null,
            message: String? = null,
        ): SourceProgressFragment = SourceProgressFragment().apply {
            arguments = Bundle().apply {
                if (!title.isNullOrBlank()) putString(ARG_TITLE, title)
                if (!message.isNullOrBlank()) putString(ARG_MESSAGE, message)
            }
        }
    }
}
