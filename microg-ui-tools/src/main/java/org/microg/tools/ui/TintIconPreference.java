package org.microg.tools.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceViewHolder;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class TintIconPreference extends DimmableIconPreference {

    public TintIconPreference(Context context) {
        this(context, (AttributeSet) null);
    }

    public TintIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static int getThemeAccentColor(Context context) {
        int colorAttr;
        if (SDK_INT >= LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        Drawable icon = getIcon();
        if (icon != null) {
            DrawableCompat.setTint(icon, getThemeAccentColor(getContext()));
        }
    }
}
