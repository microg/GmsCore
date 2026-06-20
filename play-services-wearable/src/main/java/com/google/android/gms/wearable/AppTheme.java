package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AppTheme extends AutoSafeParcelable {
    @SafeParceled(1)
    public int colorTheme;
    @SafeParceled(2)
    public int dynamicColor;
    @SafeParceled(3)
    public int screenAlignment;
    @SafeParceled(4)
    public int screenItemsSize;

    public AppTheme() {
    }

    public AppTheme(int colorTheme, int dynamicColor, int screenAlignment, int screenItemsSize) {
        this.colorTheme = colorTheme;
        this.dynamicColor = dynamicColor;
        this.screenAlignment = screenAlignment;
        this.screenItemsSize = screenItemsSize;
    }

    public int getColorTheme() {
        return colorTheme == 0 ? 1 : colorTheme;
    }

    public int getDynamicColor() {
        return dynamicColor == 0 ? 1 : dynamicColor;
    }

    public int getScreenAlignment() {
        return screenAlignment == 0 ? 1 : screenAlignment;
    }

    public int getScreenItemsSize() {
        return screenItemsSize == 0 ? 3 : screenItemsSize;
    }

    @Override
    public String toString() {
        return "AppTheme {dynamicColor =" + dynamicColor + ", colorTheme =" + colorTheme
                + ", screenAlignment =" + screenAlignment + ", screenItemsSize =" + screenItemsSize + "}";
    }

    public static final Creator<AppTheme> CREATOR = new AutoCreator<>(AppTheme.class);
}

