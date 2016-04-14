/*
 * Copyright 2013-2016 microG Project Team
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

package org.microg.gms.common;

import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;

public class MultiListenerProxy<T extends IInterface> implements InvocationHandler {
    private static final String TAG = "GmsMultiListener";

    public static <T extends IInterface> T get(Class<T> tClass, final Collection<T> listeners) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, new MultiListenerProxy<T>(listeners));
    }

    private final Collection<T> listeners;

    private MultiListenerProxy(Collection<T> listeners) {
        this.listeners = listeners;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        for (T listener : new HashSet<T>(listeners)) {
            try {
                method.invoke(listener, args);
            } catch (IllegalAccessException e) {
                Log.w(TAG, e);
                listeners.remove(listener);
            } catch (InvocationTargetException e) {
                Log.w(TAG, e.getTargetException());
                listeners.remove(listener);
            }
        }
        return null;
    }
}
