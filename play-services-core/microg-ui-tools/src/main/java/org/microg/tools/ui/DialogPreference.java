/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.tools.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;

public class DialogPreference extends androidx.preference.DialogPreference implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private static final String DIALOG_FRAGMENT_TAG =
            "android.support.v7.preference.PreferenceFragment.DIALOG";

    public DialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogPreference(Context context) {
        super(context);
    }

    protected View onCreateDialogView() {
        return null;
    }

    /**
     * Called when the dialog is dismissed and should be used to save data to
     * the {@link SharedPreferences}.
     *
     * @param positiveResult Whether the positive button was clicked (true), or
     *                       the negative button was clicked or the dialog was canceled (false).
     */
    protected void onDialogClosed(boolean positiveResult) {
    }

    @Override
    public boolean onPreferenceDisplayDialog(PreferenceFragmentCompat caller, Preference pref) {
        DialogPreferenceCompatDialogFragment fragment = new DialogPreferenceCompatDialogFragment();
        fragment.setTargetFragment(caller, 0);
        fragment.show(caller.getFragmentManager(), DIALOG_FRAGMENT_TAG);
        return true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        ViewGroup.LayoutParams layoutParams = view.findViewById(R.id.icon_frame).getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            if (((LinearLayout.LayoutParams) layoutParams).leftMargin < 0) {
                ((LinearLayout.LayoutParams) layoutParams).leftMargin = 0;
            }
        }
    }

    public static class DialogPreferenceCompatDialogFragment extends PreferenceDialogFragmentCompat {

        @Override
        protected View onCreateDialogView(Context context) {
            if (getPreference() instanceof DialogPreference) {
                View view = ((DialogPreference) getPreference()).onCreateDialogView();
                if (view != null) return view;
            }
            return super.onCreateDialogView(context);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (getPreference() instanceof DialogPreference) {
                ((DialogPreference) getPreference()).onDialogClosed(positiveResult);
            }
        }

        public static DialogFragment newInstance(String key) {
            final DialogPreferenceCompatDialogFragment fragment = new DialogPreferenceCompatDialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }
    }
}
