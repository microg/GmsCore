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

package org.microg.gms.maps.vtm.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.maps.vtm.ResourcesContainer;
import org.microg.gms.maps.vtm.R;

public class DefaultBitmapDescriptor extends AbstractBitmapDescriptor {
    public static final DefaultBitmapDescriptor DEFAULT_DESCRIPTOR = new DefaultBitmapDescriptor(0);
    public static final BitmapDescriptorImpl DEFAULT_DESCRIPTOR_IMPL = new BitmapDescriptorImpl(ObjectWrapper.wrap(DEFAULT_DESCRIPTOR));
    public static final int DEGREES = 360;

    private final float hue;

    public DefaultBitmapDescriptor(float hue) {
        this.hue = hue > 180 ? -DEGREES + hue : hue;
    }

    @Override
    public Bitmap generateBitmap(Context context) {
        Bitmap source;
        if (this == DEFAULT_DESCRIPTOR) {
            source = BitmapFactory.decodeResource(ResourcesContainer.get(), R.drawable.maps_default_marker);
        } else {
            source = DEFAULT_DESCRIPTOR.loadBitmap(context);
        }
        if (hue % DEGREES == 0) return source;
        Paint paint = new Paint();
        paint.setColorFilter(adjustHue(hue));
        Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(source, 0, 0, paint);
        return bitmap;
    }

    /**
     * Creates a HUE ajustment ColorFilter
     * <p/>
     * see http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
     * see http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
     *
     * @param value degrees to shift the hue.
     */
    public static ColorFilter adjustHue(float value) {
        ColorMatrix cm = new ColorMatrix();
        adjustHue(cm, value);
        return new ColorMatrixColorFilter(cm);
    }

    /**
     * see http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
     * see http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
     */
    public static void adjustHue(ColorMatrix cm, float value) {
        value = cleanValue(value, 180f) / 180f * (float) Math.PI;
        if (value == 0) {
            return;
        }
        float cosVal = (float) Math.cos(value);
        float sinVal = (float) Math.sin(value);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        float[] mat = new float[]{lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                lumG + cosVal * (-lumG) + sinVal * (-lumG),
                lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                lumR + cosVal * (-lumR) + sinVal * (0.143f),
                lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
                lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                lumG + cosVal * (-lumG) + sinVal * (lumG),
                lumB + cosVal * (1 - lumB) + sinVal * (lumB),
                0, 0, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f};
        cm.postConcat(new ColorMatrix(mat));
    }

    protected static float cleanValue(float p_val, float p_limit) {
        return Math.min(p_limit, Math.max(-p_limit, p_val));
    }
}
