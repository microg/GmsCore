/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core.ui

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class ContainedEditTextPreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val editText = holder.itemView.findViewById<EditText>(android.R.id.edit)
        (editText as? TextWatcher)?.let { editText.removeTextChangedListener(it) }
        editText.addTextChangedListener { textChangedListener(it?.toString() ?: "") }
        editText.tag = this
        editText.hint = hint
        editText.text.replace(0, editText.text.length, text)
        editText.isEnabled = editable
        if (requestFocus) {
            editText.requestFocus()
            (editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            requestFocus = false
        }
    }

    private var requestFocus: Boolean = false
    fun editRequestFocus() {
        requestFocus = true
        notifyChanged()
    }

    var textChangedListener: (String) -> Unit = {}

    var editable: Boolean = true
        set(value) {
            field = value
            notifyChanged()
        }

    var text: String = ""
        set(value) {
            field = value
            notifyChanged()
        }

    var hint: String = ""
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        layoutResource = R.layout.preference_material_with_widget_below
        widgetLayoutResource = R.layout.preference_edit_widget
    }
}
