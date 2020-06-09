/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.tools.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * A preference item that can dim the icon when it's disabled, either directly or because its parent
 * is disabled.
 */
public class DimmableIconPreference extends Preference {
    private static final int ICON_ALPHA_ENABLED = 255;
    private static final int ICON_ALPHA_DISABLED = 102;

    private final CharSequence mContentDescription;

    public DimmableIconPreference(Context context) {
        this(context, (AttributeSet) null);
    }

    public DimmableIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentDescription = null;
    }

    public DimmableIconPreference(Context context, CharSequence contentDescription) {
        super(context);
        mContentDescription = contentDescription;
    }

    protected boolean shouldDimIcon() {
        return !isEnabled();
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(dimmed ? ICON_ALPHA_DISABLED : ICON_ALPHA_ENABLED);
            setIcon(icon);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (!TextUtils.isEmpty(mContentDescription)) {
            final TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setContentDescription(mContentDescription);
        }
        ViewGroup.LayoutParams layoutParams = view.findViewById(R.id.icon_frame).getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            if (((LinearLayout.LayoutParams) layoutParams).leftMargin < 0) {
                ((LinearLayout.LayoutParams) layoutParams).leftMargin = 0;
            }
        }
        dimIcon(shouldDimIcon());
    }
}
