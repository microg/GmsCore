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

package org.microg.gms.common;

import android.os.IInterface;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class MultiListenerProxy<T extends IInterface> implements InvocationHandler {
    private static final String TAG = "GmsMultiListener";

    public static <T extends IInterface> T get(Class<T> tClass, final Collection<T> listeners) {
        return get(tClass, new CollectionListenerPool<>(listeners));
    }

    public static <T extends IInterface> T get(Class<T> tClass, final ListenerPool<T> listenerPool) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, new MultiListenerProxy<>(listenerPool));
    }

    private final ListenerPool<T> listeners;

    private MultiListenerProxy(ListenerPool<T> listeners) {
        this.listeners = listeners;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        for (T listener : new HashSet<>(listeners)) {
            try {
                method.invoke(listener, args);
            } catch (Exception e) {
                Log.w(TAG, e);
                listeners.remove(listener);
            }
        }
        return null;
    }

    public static abstract class ListenerPool<T> implements Collection<T> {
        @Override
        public boolean addAll(Collection<? extends T> collection) {
            return false;
        }

        @Override
        public boolean add(T object) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object o : collection) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean x = true;
            for (Object o : collection) {
                if (!remove(o)) x = false;
            }
            return x;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return false;
        }

        @NonNull
        @Override
        public Object[] toArray() {
            throw new IllegalArgumentException();
        }

        @NonNull
        @Override
        public <T1> T1[] toArray(T1[] array) {
            throw new IllegalArgumentException();
        }
    }

    private static class CollectionListenerPool<T> extends ListenerPool<T> {

        private final Collection<T> listeners;

        public CollectionListenerPool(Collection<T> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void clear() {
            listeners.clear();
        }

        @Override
        public boolean contains(Object object) {
            return listeners.contains(object);
        }

        @Override
        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        @NonNull
        @Override
        public Iterator<T> iterator() {
            return listeners.iterator();
        }

        @Override
        public boolean remove(Object object) {
            return listeners.remove(object);
        }

        @Override
        public int size() {
            return listeners.size();
        }
    }

    public static class MultiCollectionListenerPool<T> extends ListenerPool<T> {
        private final Collection<? extends Collection<T>> multiCol;

        public MultiCollectionListenerPool(Collection<? extends Collection<T>> multiCol) {
            this.multiCol = multiCol;
        }

        @Override
        public void clear() {
            for (Collection<T> ts : multiCol) {
                ts.clear();
            }
        }

        @Override
        public boolean contains(Object object) {
            for (Collection<T> ts : multiCol) {
                if (ts.contains(object)) return true;
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            for (Collection<T> ts : multiCol) {
                if (!ts.isEmpty()) return false;
            }
            return true;
        }

        @NonNull
        @Override
        public Iterator<T> iterator() {
            final Iterator<? extends Collection<T>> interMed = multiCol.iterator();
            return new Iterator<T>() {
                private Iterator<T> med;

                @Override
                public boolean hasNext() {
                    while ((med == null || !med.hasNext()) && interMed.hasNext()) {
                        med = interMed.next().iterator();
                    }
                    return med != null && med.hasNext();
                }

                @Override
                public T next() {
                    while (med == null || !med.hasNext()) {
                        med = interMed.next().iterator();
                    }
                    return med.next();
                }

                @Override
                public void remove() {
                    med.remove();
                }
            };
        }

        @Override
        public boolean remove(Object object) {
            for (Collection<T> ts : multiCol) {
                if (ts.remove(object)) return true;
            }
            return false;
        }

        @Override
        public int size() {
            int sum = 0;
            for (Collection<T> ts : multiCol) {
                sum += ts.size();
            }
            return sum;
        }
    }
}
