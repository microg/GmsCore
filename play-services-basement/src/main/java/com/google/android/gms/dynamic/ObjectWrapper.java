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

package com.google.android.gms.dynamic;

import android.os.IBinder;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;

public class ObjectWrapper<T> extends IObjectWrapper.Stub {
    private final T t;

    public ObjectWrapper(T t) {
        this.t = t;
    }

    @Nullable
    public static Object unwrap(IObjectWrapper obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof ObjectWrapper) {
            return ((ObjectWrapper) obj).t;
        }
        IBinder binder = obj.asBinder();
        Field[] fields = binder.getClass().getDeclaredFields();
        if (fields.length != 1) {
            throw new IllegalArgumentException();
        }
        Field field = fields[0];
        if (!field.isAccessible()) {
            field.setAccessible(true);
            try {
                return field.get(binder);
            } catch (NullPointerException localNullPointerException) {
                throw new IllegalArgumentException("Binder object is null.",
                        localNullPointerException);
            } catch (IllegalArgumentException localIllegalArgumentException) {
                throw new IllegalArgumentException("remoteBinder is the wrong class.",
                        localIllegalArgumentException);
            } catch (IllegalAccessException localIllegalAccessException) {
                throw new IllegalArgumentException("Could not access the field in remoteBinder.",
                        localIllegalAccessException);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Nullable
    public static <T> T unwrapTyped(IObjectWrapper obj, Class<T> clazz) {
        try {
            return clazz.cast(unwrap(obj));
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static <T> ObjectWrapper<T> wrap(T t) {
        return new ObjectWrapper<T>(t);
    }
}
