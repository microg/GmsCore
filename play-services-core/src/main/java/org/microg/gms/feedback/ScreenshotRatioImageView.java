package org.microg.gms.feedback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class ScreenshotRatioImageView extends ImageView {
    public ScreenshotRatioImageView(Context context) {
        super(context);
    }

    public ScreenshotRatioImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenshotRatioImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * ((float) getDrawable().getIntrinsicHeight() / getDrawable().getIntrinsicWidth()));

        setMeasuredDimension(width, height);
    }
}
