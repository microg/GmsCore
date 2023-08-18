/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2014-2017 microG Project Team
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
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;

import static android.os.Build.VERSION.SDK_INT;

public class SwitchBar extends LinearLayout implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    public static interface OnSwitchChangeListener {
        /**
         * Called when the checked state of the Switch has changed.
         *
         * @param switchView The Switch view whose state has changed.
         * @param isChecked  The new checked state of switchView.
         */
        void onSwitchChanged(SwitchCompat switchView, boolean isChecked);
    }

    private final TextAppearanceSpan mSummarySpan;

    private ToggleSwitch mSwitch;
    private TextView mTextView;
    private String mLabel;
    private String mSummary;

    private ArrayList<OnSwitchChangeListener> mSwitchChangeListeners =
            new ArrayList<OnSwitchChangeListener>();

    public SwitchBar(Context context) {
        this(context, null);
    }

    public SwitchBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.switch_bar, this);

        mTextView = (TextView) findViewById(R.id.switch_text);
        if (SDK_INT > 16) {
            mTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        mLabel = getResources().getString(androidx.appcompat.R.string.abc_capital_off);
        mSummarySpan = new TextAppearanceSpan(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Widget_Switch);
        updateText();

        mSwitch = (ToggleSwitch) findViewById(R.id.switch_widget);
        // Prevent onSaveInstanceState() to be called as we are managing the state of the Switch
        // on our own
        mSwitch.setSaveEnabled(false);
        if (SDK_INT >= 16) {
            mSwitch.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        addOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(SwitchCompat switchView, boolean isChecked) {
                setTextViewLabel(isChecked);
            }
        });

        setOnClickListener(this);

        // Default is hide
        setVisibility(View.GONE);
    }

    public void setTextViewLabel(boolean isChecked) {
        mLabel = getResources()
                .getString(isChecked ? androidx.appcompat.R.string.abc_capital_on : androidx.appcompat.R.string.abc_capital_off);
        updateText();
    }

    public void setSummary(String summary) {
        mSummary = summary;
        updateText();
    }

    private void updateText() {
        if (TextUtils.isEmpty(mSummary)) {
            mTextView.setText(mLabel);
            return;
        }
        final SpannableStringBuilder ssb = new SpannableStringBuilder(mLabel).append('\n');
        final int start = ssb.length();
        ssb.append(mSummary);
        ssb.setSpan(mSummarySpan, start, ssb.length(), 0);
        mTextView.setText(ssb);
    }

    public void setChecked(boolean checked) {
        setTextViewLabel(checked);
        mSwitch.setChecked(checked);
    }

    public void setCheckedInternal(boolean checked) {
        setTextViewLabel(checked);
        mSwitch.setCheckedInternal(checked);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTextView.setEnabled(enabled);
        mSwitch.setEnabled(enabled);
    }

    public final ToggleSwitch getSwitch() {
        return mSwitch;
    }

    public void show() {
        if (!isShowing()) {
            setVisibility(View.VISIBLE);
            mSwitch.setOnCheckedChangeListener(this);
        }
    }

    public void hide() {
        if (isShowing()) {
            setVisibility(View.GONE);
            mSwitch.setOnCheckedChangeListener(null);
        }
    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        final boolean isChecked = !mSwitch.isChecked();
        setChecked(isChecked);
    }

    public void propagateChecked(boolean isChecked) {
        final int count = mSwitchChangeListeners.size();
        for (int n = 0; n < count; n++) {
            mSwitchChangeListeners.get(n).onSwitchChanged(mSwitch, isChecked);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        propagateChecked(isChecked);
    }

    public void addOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot add twice the same OnSwitchChangeListener");
        }
        mSwitchChangeListeners.add(listener);
    }

    public void removeOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (!mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot remove OnSwitchChangeListener");
        }
        mSwitchChangeListeners.remove(listener);
    }

    static class SavedState extends BaseSavedState {
        boolean checked;
        boolean visible;

        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(Boolean.class.getClassLoader());
            visible = (Boolean) in.readValue(Boolean.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
            out.writeValue(visible);
        }

        @Override
        public String toString() {
            return "SwitchBar.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + checked
                    + " visible=" + visible + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.checked = mSwitch.isChecked();
        ss.visible = isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        mSwitch.setCheckedInternal(ss.checked);
        setTextViewLabel(ss.checked);
        setVisibility(ss.visible ? View.VISIBLE : View.GONE);
        mSwitch.setOnCheckedChangeListener(ss.visible ? this : null);

        requestLayout();
    }
}
