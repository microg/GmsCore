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

public abstract class AbstractBitmapDescriptor {

    private Bitmap bitmap;

    protected abstract Bitmap generateBitmap(Context context);

    public Bitmap loadBitmap(Context context) {
        if (bitmap == null) {
            synchronized (this) {
                if (bitmap == null)
                    bitmap = generateBitmap(context);
            }
        }
        return bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
