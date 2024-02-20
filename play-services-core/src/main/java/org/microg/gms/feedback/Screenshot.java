package org.microg.gms.feedback;


import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.feedback.ErrorReport;

import java.io.ByteArrayOutputStream;

public class Screenshot {
    private static final String TAG = Screenshot.class.getSimpleName();

    public int width;
    public int height;
    public String imgSrc;

    public static Screenshot encodeBitmapToScreenshot(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "Screenshot is either null or recycled");
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        return base64EncodeToScreenshot(byteArrayOutputStream.toByteArray(), bitmap.getWidth(), bitmap.getHeight());
    }

    public static Screenshot createScreenshot(String imgSrc, int width, int height) {
        Screenshot screenshot = new Screenshot();
        screenshot.width = width;
        screenshot.height = height;
        screenshot.imgSrc = imgSrc;
        return screenshot;
    }

    public static Screenshot base64EncodeToScreenshot(byte[] bArr, int i, int i2) {
        return createScreenshot(Base64.encodeToString(bArr, 0), i, i2);
    }

    public static void setScreenshotToErrorReport(ErrorReport errorReport, Screenshot screenshot) {
        errorReport.screenshotImgData = null;
        if (screenshot == null || TextUtils.isEmpty(screenshot.imgSrc)) {
            errorReport.screenshotHeight = 0;
            errorReport.screenshotWidth = 0;
            errorReport.screenshotImgSrc = null;
            return;
        }
        errorReport.screenshotHeight = screenshot.height;
        errorReport.screenshotWidth = screenshot.width;
        errorReport.screenshotImgSrc = screenshot.imgSrc;
    }

}