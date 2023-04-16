/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils;

import android.os.Build;
import android.os.WorkSource;
import android.util.Log;

import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;

public class WorkSourceUtil {
    private static final String TAG = "WorkSourceUtil";

    private static Method getMethod(String name, Class<?>... parameterTypes) throws Exception {
        Method method = WorkSource.class.getMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static <T> T invokeMethod(WorkSource workSource, Method method, Object... args) throws Exception {
        return (T) method.invoke(workSource, args);
    }

    private static <T> T invokeMethod(WorkSource workSource, String name, Object... args) throws Exception {
        return invokeMethod(workSource, getMethod(name), args);
    }

    public static void add(WorkSource workSource, int uid, String packageName) {
        try {
            invokeMethod(workSource, getMethod("add", Integer.TYPE, String.class), uid, packageName);
        } catch (Exception e) {
            try {
                invokeMethod(workSource, getMethod("add", Integer.TYPE), uid);
            } catch (Exception ex) {
                // Ignore
            }
        }
    }

    public static int size(WorkSource workSource) {
        try {
            return invokeMethod(workSource, "size");
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isEmpty(WorkSource workSource) {
        if (SDK_INT >= 28) {
            try {
                return invokeMethod(workSource, "isEmpty");
            } catch (Exception e) {
                // Ignore and fall-through to size()
            }
        }
        return size(workSource) == 0;
    }
}
