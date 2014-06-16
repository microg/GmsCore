/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import com.google.android.gms.R;
import com.google.android.gms.maps.internal.ResourcesContainer;

public class DefaultBitmapDescriptor extends AbstractBitmapDescriptor {
	private float hue;

	public DefaultBitmapDescriptor(float hue) {
		this.hue = hue;
	}

	@Override
	public Bitmap generateBitmap(Context context) {
		Bitmap source = BitmapFactory.decodeResource(ResourcesContainer.get(), R.drawable.maps_default_marker);
		Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
		float[] hsv = new float[3];
		for (int x = 0; x < bitmap.getWidth(); x++) {
			for (int y = 0; y < bitmap.getHeight(); y++) {
				int pixel = source.getPixel(x, y);
				Color.colorToHSV(pixel, hsv);
				hsv[0] = (hsv[0] + hue) % 360;
				bitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hsv));
			}
		}
		return bitmap;
	}
}
