/*
 * Copyright 2013-2015 microG Project Team
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

package org.microg.gms.maps;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;
import android.view.ViewDebug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A hacked Context that allows access to gms resources but feels like the remote context for everything else.
 */
public class ContextContainer extends Context {
    private Context original;

    public ContextContainer(Context original) {
        this.original = original;
    }

    @Override
    public AssetManager getAssets() {
        return ResourcesContainer.get().getAssets();
    }

    @Override
    public Resources getResources() {
        return ResourcesContainer.get();
    }

    @Override
    public PackageManager getPackageManager() {
        return original.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver() {
        return original.getContentResolver();
    }

    @Override
    public Looper getMainLooper() {
        return original.getMainLooper();
    }

    @Override
    public Context getApplicationContext() {
        return original.getApplicationContext();
    }

    @Override
    public void setTheme(int i) {
        original.setTheme(i);
    }

    @Override
    @ViewDebug.ExportedProperty(
            deepExport = true
    )
    public Resources.Theme getTheme() {
        return original.getTheme();
    }

    @Override
    public ClassLoader getClassLoader() {
        return original.getClassLoader();
    }

    @Override
    public String getPackageName() {
        return original.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return original.getApplicationInfo();
    }

    @Override
    public String getPackageResourcePath() {
        return original.getPackageResourcePath();
    }

    @Override
    public String getPackageCodePath() {
        return original.getPackageCodePath();
    }

    @Override
    public SharedPreferences getSharedPreferences(String s, int i) {
        return original.getSharedPreferences(s, i);
    }

    @Override
    public FileInputStream openFileInput(String s) throws FileNotFoundException {
        return original.openFileInput(s);
    }

    @Override
    public FileOutputStream openFileOutput(String s, int i) throws FileNotFoundException {
        return original.openFileOutput(s, i);
    }

    @Override
    public boolean deleteFile(String s) {
        return original.deleteFile(s);
    }

    @Override
    public File getFileStreamPath(String s) {
        return original.getFileStreamPath(s);
    }

    @Override
    public File getFilesDir() {
        return original.getFilesDir();
    }

    @Override
    @TargetApi(21)
    public File getNoBackupFilesDir() {
        return original.getNoBackupFilesDir();
    }

    @Override
    public File getExternalFilesDir(String s) {
        return original.getExternalFilesDir(s);
    }

    @Override
    @TargetApi(19)
    public File[] getExternalFilesDirs(String s) {
        return original.getExternalFilesDirs(s);
    }

    @Override
    public File getObbDir() {
        return original.getObbDir();
    }

    @Override
    @TargetApi(19)
    public File[] getObbDirs() {
        return original.getObbDirs();
    }

    @Override
    public File getCacheDir() {
        return original.getCacheDir();
    }

    @Override
    @TargetApi(21)
    public File getCodeCacheDir() {
        return original.getCodeCacheDir();
    }

    @Override
    public File getExternalCacheDir() {
        return original.getExternalCacheDir();
    }

    @Override
    @TargetApi(19)
    public File[] getExternalCacheDirs() {
        return original.getExternalCacheDirs();
    }

    @Override
    @TargetApi(21)
    public File[] getExternalMediaDirs() {
        return original.getExternalMediaDirs();
    }

    @Override
    public String[] fileList() {
        return original.fileList();
    }

    @Override
    public File getDir(String s, int i) {
        return original.getDir(s, i);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String s, int i,
                                               SQLiteDatabase.CursorFactory cursorFactory) {
        return original.openOrCreateDatabase(s, i, cursorFactory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String s, int i,
                                               SQLiteDatabase.CursorFactory cursorFactory,
                                               DatabaseErrorHandler databaseErrorHandler) {
        return original.openOrCreateDatabase(s, i, cursorFactory, databaseErrorHandler);
    }

    @Override
    public boolean deleteDatabase(String s) {
        return original.deleteDatabase(s);
    }

    @Override
    public File getDatabasePath(String s) {
        return original.getDatabasePath(s);
    }

    @Override
    public String[] databaseList() {
        return original.databaseList();
    }

    @Override
    @Deprecated
    public Drawable getWallpaper() {
        return original.getWallpaper();
    }

    @Override
    @Deprecated
    public Drawable peekWallpaper() {
        return original.peekWallpaper();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumWidth() {
        return original.getWallpaperDesiredMinimumWidth();
    }

    @Override
    @Deprecated
    public int getWallpaperDesiredMinimumHeight() {
        return original.getWallpaperDesiredMinimumHeight();
    }

    @Override
    @Deprecated
    public void setWallpaper(Bitmap bitmap) throws IOException {
        original.setWallpaper(bitmap);
    }

    @Override
    @Deprecated
    public void setWallpaper(InputStream inputStream) throws IOException {
        original.setWallpaper(inputStream);
    }

    @Override
    @Deprecated
    public void clearWallpaper() throws IOException {
        original.clearWallpaper();
    }

    @Override
    public void startActivity(Intent intent) {
        original.startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivity(Intent intent, Bundle bundle) {
        original.startActivity(intent, bundle);
    }

    @Override
    public void startActivities(Intent[] intents) {
        original.startActivities(intents);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startActivities(Intent[] intents, Bundle bundle) {
        original.startActivities(intents, bundle);
    }

    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i1, int i2)
            throws IntentSender.SendIntentException {
        original.startIntentSender(intentSender, intent, i, i1, i2);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void startIntentSender(IntentSender intentSender, Intent intent, int i, int i1, int i2,
                                  Bundle bundle) throws IntentSender.SendIntentException {
        original.startIntentSender(intentSender, intent, i, i1, i2, bundle);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        original.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String s) {
        original.sendBroadcast(intent, s);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String s) {
        original.sendOrderedBroadcast(intent, s);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String s,
                                     BroadcastReceiver broadcastReceiver, Handler handler, int i, String s1,
                                     Bundle bundle) {
        original.sendOrderedBroadcast(intent, s, broadcastReceiver, handler, i, s1, bundle);
    }

    @Override
    @TargetApi(17)
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        original.sendBroadcastAsUser(intent, userHandle);
    }

    @Override
    @TargetApi(17)
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, String s) {
        original.sendBroadcastAsUser(intent, userHandle, s);
    }

    @Override
    @TargetApi(17)
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, String s,
                                           BroadcastReceiver broadcastReceiver, Handler handler, int i, String s1,
                                           Bundle bundle) {
        original.sendOrderedBroadcastAsUser(intent, userHandle, s, broadcastReceiver, handler, i,
                s1,
                bundle);
    }

    @Override
    @Deprecated
    public void sendStickyBroadcast(Intent intent) {
        original.sendStickyBroadcast(intent);
    }

    @Override
    @Deprecated
    public void sendStickyOrderedBroadcast(Intent intent,
                                           BroadcastReceiver broadcastReceiver, Handler handler, int i, String s,
                                           Bundle bundle) {
        original.sendStickyOrderedBroadcast(intent, broadcastReceiver, handler, i, s, bundle);
    }

    @Override
    @Deprecated
    public void removeStickyBroadcast(Intent intent) {
        original.removeStickyBroadcast(intent);
    }

    @Override
    @Deprecated
    @TargetApi(17)
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        original.sendStickyBroadcastAsUser(intent, userHandle);
    }

    @Override
    @Deprecated
    @TargetApi(17)
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle userHandle,
                                                 BroadcastReceiver broadcastReceiver, Handler handler, int i, String s,
                                                 Bundle bundle) {
        original.sendStickyOrderedBroadcastAsUser(intent, userHandle, broadcastReceiver, handler, i,
                s,
                bundle);
    }

    @Override
    @Deprecated
    @TargetApi(17)
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {
        original.removeStickyBroadcastAsUser(intent, userHandle);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver,
                                   IntentFilter intentFilter) {
        return original.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver broadcastReceiver,
                                   IntentFilter intentFilter, String s, Handler handler) {
        return original.registerReceiver(broadcastReceiver, intentFilter, s, handler);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        original.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public ComponentName startService(Intent intent) {
        return original.startService(intent);
    }

    @Override
    public boolean stopService(Intent intent) {
        return original.stopService(intent);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return original.bindService(intent, serviceConnection, i);
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        original.unbindService(serviceConnection);
    }

    @Override
    public boolean startInstrumentation(ComponentName componentName, String s, Bundle bundle) {
        return original.startInstrumentation(componentName, s, bundle);
    }

    @Override
    public Object getSystemService(String s) {
        return original.getSystemService(s);
    }

    @TargetApi(23)
    @Override
    public String getSystemServiceName(Class<?> serviceClass) {
        return original.getSystemServiceName(serviceClass);
    }

    @Override
    public int checkPermission(String s, int i, int i1) {
        return original.checkPermission(s, i, i1);
    }

    @Override
    public int checkCallingPermission(String s) {
        return original.checkCallingPermission(s);
    }

    @Override
    public int checkCallingOrSelfPermission(String s) {
        return original.checkCallingOrSelfPermission(s);
    }

    @TargetApi(23)
    @Override
    public int checkSelfPermission(String permission) {
        return original.checkSelfPermission(permission);
    }

    @Override
    public void enforcePermission(String s, int i, int i1, String s1) {
        original.enforcePermission(s, i, i1, s1);
    }

    @Override
    public void enforceCallingPermission(String s, String s1) {
        original.enforceCallingPermission(s, s1);
    }

    @Override
    public void enforceCallingOrSelfPermission(String s, String s1) {
        original.enforceCallingOrSelfPermission(s, s1);
    }

    @Override
    public void grantUriPermission(String s, Uri uri, int i) {
        original.grantUriPermission(s, uri, i);
    }

    @Override
    public void revokeUriPermission(Uri uri, int i) {
        original.revokeUriPermission(uri, i);
    }

    @Override
    public int checkUriPermission(Uri uri, int i, int i1, int i2) {
        return original.checkUriPermission(uri, i, i1, i2);
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int i) {
        return original.checkCallingUriPermission(uri, i);
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int i) {
        return original.checkCallingOrSelfUriPermission(uri, i);
    }

    @Override
    public int checkUriPermission(Uri uri, String s, String s1, int i, int i1, int i2) {
        return original.checkUriPermission(uri, s, s1, i, i1, i2);
    }

    @Override
    public void enforceUriPermission(Uri uri, int i, int i1, int i2, String s) {
        original.enforceUriPermission(uri, i, i1, i2, s);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int i, String s) {
        original.enforceCallingUriPermission(uri, i, s);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int i, String s) {
        original.enforceCallingOrSelfUriPermission(uri, i, s);
    }

    @Override
    public void enforceUriPermission(Uri uri, String s, String s1, int i, int i1, int i2,
                                     String s2) {
        original.enforceUriPermission(uri, s, s1, i, i1, i2, s2);
    }

    @Override
    public Context createPackageContext(String s, int i)
            throws PackageManager.NameNotFoundException {
        return original.createPackageContext(s, i);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public Context createConfigurationContext(Configuration configuration) {
        return original.createConfigurationContext(configuration);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public Context createDisplayContext(Display display) {
        return original.createDisplayContext(display);
    }

    /* HIDDEN */

    public String getBasePackageName() {
        return (String) safeInvoke("getBasePackageName");
    }

    public String getOpPackageName() {
        return (String) safeInvoke("getOpPackageName");
    }

    public File getSharedPrefsFile(String name) {
        return (File) safeInvoke("getBasePackageName", String.class, name);
    }

    public void startActivityAsUser(Intent intent, UserHandle user) {
        safeInvoke("startActivityAsUser", Intent.class, UserHandle.class, intent, user);
    }

    public void startActivityAsUser(Intent intent, Bundle options, UserHandle userId) {
        safeInvoke("startActivityAsUser", Intent.class, Bundle.class, UserHandle.class, intent, options, userId);
    }

    public void startActivityForResult(String who, Intent intent, int requestCode, Bundle options) {
        safeInvoke("startActivityForResult", String.class, Intent.class, int.class, Bundle.class, who, intent, requestCode, options);
    }

    public boolean canStartActivityForResult() {
        return (Boolean) safeInvoke("canStartActivityForResult");
    }

    public void startActivitiesAsUser(Intent[] intents, Bundle options, UserHandle userHandle) {
        safeInvoke("startActivitiesAsUser", new Class[]{Intent[].class, Bundle.class, UserHandle.class}, intents, options, userHandle);
    }

    public void sendBroadcastMultiplePermissions(Intent intent, String[] receiverPermissions) {
        safeInvoke("sendBroadcastMultiplePermissions", Intent.class, String[].class, intent, receiverPermissions);
    }

    public void sendBroadcast(Intent intent, String receiverPermission, int appOp) {
        safeInvoke("sendBroadcast", Intent.class, String.class, int.class, intent, receiverPermission, appOp);
    }


    private Object safeInvoke(String name) {
        return safeInvoke(name, new Class[0]);
    }

    private <T1> Object safeInvoke(String name, Class<T1> t1Class, T1 t1Value) {
        return safeInvoke(name, new Class[]{t1Class}, t1Value);
    }

    private <T1, T2> Object safeInvoke(String name, Class<T1> t1Class, Class<T2> t2Class, T1 t1Value, T2 t2Value) {
        return safeInvoke(name, new Class[]{t1Class, t2Class}, t1Value, t2Value);
    }

    private <T1, T2, T3> Object safeInvoke(String name, Class<T1> t1Class, Class<T2> t2Class, Class<T3> t3Class, T1 t1Value, T2 t2Value, T3 t3Value) {
        return safeInvoke(name, new Class[]{t1Class, t2Class, t3Class}, t1Value, t2Value, t3Value);
    }

    private <T1, T2, T3, T4> Object safeInvoke(String name, Class<T1> t1Class, Class<T2> t2Class, Class<T3> t3Class, Class<T4> t4Class, T1 t1Value, T2 t2Value, T3 t3Value, T4 t4Value) {
        return safeInvoke(name, new Class[]{t1Class, t2Class, t3Class, t4Class}, t1Value, t2Value, t3Value, t4Value);
    }

    private Object safeInvoke(String name, Class[] classes, Object... values) {
        try {
            Method method = Context.class.getDeclaredMethod(name, classes);
            return method.invoke(original, values);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            } else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
