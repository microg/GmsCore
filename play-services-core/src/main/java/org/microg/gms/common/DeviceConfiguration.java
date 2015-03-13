/*
 * Copyright 2013-2015 µg Project Team
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.opengl.GLES10;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class DeviceConfiguration {
    public List<String> availableFeatures;
    public int densityDpi;
    public int glEsVersion;
    public List<String> glExtensions;
    public boolean hasFiveWayNavigation;
    public boolean hasHardKeyboard;
    public int heightPixels;
    public int keyboardType;
    public List<String> locales;
    public List<String> nativePlatforms;
    public int navigation;
    public int screenLayout;
    public List<String> sharedLibraries;
    public int touchScreen;
    public int widthPixels;

    public DeviceConfiguration(Context context) {
        ConfigurationInfo configurationInfo = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
        touchScreen = configurationInfo.reqTouchScreen;
        keyboardType = configurationInfo.reqKeyboardType;
        navigation = configurationInfo.reqNavigation;
        Configuration configuration = context.getResources().getConfiguration();
        screenLayout = configuration.screenLayout;
        hasHardKeyboard = (configurationInfo.reqInputFeatures & ConfigurationInfo.INPUT_FEATURE_HARD_KEYBOARD) > 0;
        hasFiveWayNavigation = (configurationInfo.reqInputFeatures & ConfigurationInfo.INPUT_FEATURE_FIVE_WAY_NAV) > 0;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        densityDpi = displayMetrics.densityDpi;
        glEsVersion = configurationInfo.reqGlEsVersion;
        PackageManager packageManager = context.getPackageManager();
        sharedLibraries = Arrays.asList(packageManager.getSystemSharedLibraryNames());
        availableFeatures = new ArrayList<>();
        for (FeatureInfo featureInfo : packageManager.getSystemAvailableFeatures()) {
            if (featureInfo.name != null) availableFeatures.add(featureInfo.name);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nativePlatforms = Arrays.asList(Build.SUPPORTED_ABIS);
        } else {
            nativePlatforms = new ArrayList<>();
            nativePlatforms.add(Build.CPU_ABI);
            if (Build.CPU_ABI2 != null) nativePlatforms.add(Build.CPU_ABI2);
        }
        widthPixels = displayMetrics.widthPixels;
        heightPixels = displayMetrics.heightPixels;
        locales = Arrays.asList(context.getAssets().getLocales());
        glExtensions = new ArrayList<>();
        addEglExtensions(glExtensions);
    }

    private static void addEglExtensions(List<String> glExtensions) {
        EGL10 egl10 = (EGL10) EGLContext.getEGL();
        if (egl10 != null) {
            EGLDisplay display = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            egl10.eglInitialize(display, new int[2]);
            int cf[] = new int[1];
            if (egl10.eglGetConfigs(display, null, 0, cf)) {
                EGLConfig[] configs = new EGLConfig[cf[0]];
                if (egl10.eglGetConfigs(display, configs, cf[0], cf)) {
                    int[] a1 =
                            new int[]{EGL10.EGL_WIDTH, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_HEIGHT, EGL10.EGL_PBUFFER_BIT,
                                    EGL10.EGL_NONE};
                    int[] a2 = new int[]{12440, EGL10.EGL_PIXMAP_BIT, EGL10.EGL_NONE};
                    int[] a3 = new int[1];
                    for (int i = 0; i < cf[0]; i++) {
                        egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_CONFIG_CAVEAT, a3);
                        if (a3[0] != EGL10.EGL_SLOW_CONFIG) {
                            egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_SURFACE_TYPE, a3);
                            if ((1 & a3[0]) != 0) {
                                egl10.eglGetConfigAttrib(display, configs[i], EGL10.EGL_RENDERABLE_TYPE, a3);
                                if ((1 & a3[0]) != 0) {
                                    addExtensionsForConfig(egl10, display, configs[i], a1, null, glExtensions);
                                }
                                if ((4 & a3[0]) != 0) {
                                    addExtensionsForConfig(egl10, display, configs[i], a1, a2, glExtensions);
                                }
                            }
                        }
                    }
                }
            }
            egl10.eglTerminate(display);
        }
    }

    private static void addExtensionsForConfig(EGL10 egl10, EGLDisplay egldisplay, EGLConfig eglconfig, int ai[],
                                               int ai1[], List<String> set) {
        EGLContext eglcontext = egl10.eglCreateContext(egldisplay, eglconfig, EGL10.EGL_NO_CONTEXT, ai1);
        if (eglcontext != EGL10.EGL_NO_CONTEXT) {
            javax.microedition.khronos.egl.EGLSurface eglsurface =
                    egl10.eglCreatePbufferSurface(egldisplay, eglconfig, ai);
            if (eglsurface == EGL10.EGL_NO_SURFACE) {
                egl10.eglDestroyContext(egldisplay, eglcontext);
            } else {
                egl10.eglMakeCurrent(egldisplay, eglsurface, eglsurface, eglcontext);
                String s = GLES10.glGetString(7939);
                if (s != null && !s.isEmpty()) {
                    String as[] = s.split(" ");
                    int i = as.length;
                    for (int j = 0; j < i; j++) {
                        set.add(as[j]);
                    }

                }
                egl10.eglMakeCurrent(egldisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                egl10.eglDestroySurface(egldisplay, eglsurface);
                egl10.eglDestroyContext(egldisplay, eglcontext);
            }
        }
    }
}
