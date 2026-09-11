/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.app.ActionBar
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.Application
import android.app.ComponentCaller
import android.app.Fragment
import android.app.FragmentManager
import android.app.LoaderManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.SharedElementCallback
import android.app.TaskStackBuilder
import android.app.VoiceInteractor
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.LocusId
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.PersistableBundle
import android.os.UserHandle
import android.transition.Scene
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.ActionMode
import android.view.ContextMenu
import android.view.DragAndDropPermissions
import android.view.DragEvent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toolbar
import android.window.OnBackInvokedDispatcher
import android.window.SplashScreen
import androidx.annotation.RequiresApi
import com.google.android.chimera.android.ActivityProxyWrapper
import com.google.android.chimera.android.IActivityProxy
import java.lang.Deprecated

open class BaseActivityProxy: Activity(), IActivityProxy {
    open var activityProxyWrapper: ActivityProxyWrapper? = null

    override fun addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        activityProxyWrapper?.public_addContentView(view, params) ?: super.addContentView(view, params)
    }

    override fun platform_addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.addContentView(view, params)
    }

    override fun clearOverrideActivityTransition(transitionType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activityProxyWrapper?.public_clearOverrideActivityTransition(transitionType) ?: super.clearOverrideActivityTransition(transitionType)
        }
    }

    override fun platform_clearOverrideActivityTransition(transitionType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            super.clearOverrideActivityTransition(transitionType)
        }
    }

    override fun closeContextMenu() {
        activityProxyWrapper?.public_closeContextMenu() ?: super.closeContextMenu()
    }

    override fun platform_closeContextMenu() {
        super.closeContextMenu()
    }

    override fun closeOptionsMenu() {
        activityProxyWrapper?.public_closeOptionsMenu() ?: super.closeOptionsMenu()
    }

    override fun platform_closeOptionsMenu() {
        super.closeOptionsMenu()
    }

    fun convertFromTranslucent() {
        activityProxyWrapper?.public_convertFromTranslucent() ?: throw UnsupportedOperationException("convertFromTranslucent is SystemApi")
    }

    override fun platform_convertFromTranslucent() {
        throw UnsupportedOperationException("convertFromTranslucent is SystemApi")
    }

    fun convertToTranslucent(listener: Any?, options: ActivityOptions?): Boolean {
        return activityProxyWrapper?.public_convertToTranslucent(listener, options) ?: throw UnsupportedOperationException("convertFromTranslucent is SystemApi")
    }

    override fun platform_convertToTranslucent(listener: Any?, options: ActivityOptions?): Boolean {
        throw UnsupportedOperationException("convertFromTranslucent is SystemApi")
    }

    override fun createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent {
        return activityProxyWrapper?.public_createPendingResult(requestCode, data, flags) ?: super.createPendingResult(requestCode, data, flags)
    }

    override fun platform_createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent {
        return super.createPendingResult(requestCode, data, flags)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchGenericMotionEvent(event) ?: super.dispatchGenericMotionEvent(event)
    }

    override fun platform_dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchKeyEvent(event) ?: super.dispatchKeyEvent(event)
    }

    override fun platform_dispatchKeyEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchKeyShortcutEvent(event) ?: super.dispatchKeyShortcutEvent(event)
    }

    override fun platform_dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchPopulateAccessibilityEvent(event) ?: super.dispatchPopulateAccessibilityEvent(event)
    }

    override fun platform_dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return super.dispatchPopulateAccessibilityEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchTouchEvent(event) ?: super.dispatchTouchEvent(event)
    }

    override fun platform_dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_dispatchTrackballEvent(event) ?: super.dispatchTrackballEvent(event)
    }
    override fun platform_dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return super.dispatchTrackballEvent(event)
    }

    override fun <T : View?> findViewById(id: Int): T? {
        return activityProxyWrapper?.public_findViewById(id) ?: super.findViewById(id)
    }

    override fun <T : View?> platform_findViewById(id: Int): T? {
        return super.findViewById(id)
    }

    override fun finish() {
        activityProxyWrapper?.public_finish() ?: super.finish()
    }

    override fun platform_finish() {
        super.finish()
    }

    override fun finishActivity(requestCode: Int) {
        activityProxyWrapper?.public_finishActivity(requestCode) ?: super.finishActivity(requestCode)
    }

    override fun platform_finishActivity(requestCode: Int) {
        super.finishActivity(requestCode)
    }

    override fun finishActivityFromChild(child: Activity, requestCode: Int) {
        activityProxyWrapper?.public_finishActivityFromChild(child, requestCode) ?: super.finishActivityFromChild(child, requestCode)
    }

    override fun platform_finishActivityFromChild(child: Activity, requestCode: Int) {
        super.finishActivityFromChild(child, requestCode)
    }

    override fun finishAffinity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            activityProxyWrapper?.public_finishAffinity() ?: super.finishAffinity()
        }
    }

    override fun platform_finishAffinity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.finishAffinity()
        }
    }

    override fun finishAfterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityProxyWrapper?.public_finishAfterTransition() ?: super.finishAfterTransition()
        }
    }

    override fun platform_finishAfterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAfterTransition()
        }
    }

    override fun finishAndRemoveTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityProxyWrapper?.public_finishAndRemoveTask() ?: super.finishAndRemoveTask()
        }
    }

    override fun platform_finishAndRemoveTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask()
        }
    }

    override fun finishFromChild(child: Activity?) {
        activityProxyWrapper?.public_finishFromChild(child) ?: super.finishFromChild(child)
    }

    @Deprecated
    override fun platform_finishFromChild(child: Activity?) {
        super.finishFromChild(child)
    }

    override fun getActionBar(): ActionBar? {
        return activityProxyWrapper?.public_getActionBar() ?: super.getActionBar()
    }

    override fun platform_getActionBar(): ActionBar? {
        return super.getActionBar()
    }

    override fun platform_getApplication(): Application? {
        return super.getApplication()
    }

    override fun getCaller(): ComponentCaller? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activityProxyWrapper?.public_getCaller()
        } else {
            super.getCaller()
        }
    }

    override fun platform_getCaller(): ComponentCaller? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            super.getCaller()
        } else {
            null
        }
    }

    override fun getCallingActivity(): ComponentName? {
        return activityProxyWrapper?.public_getCallingActivity() ?: super.getCallingActivity()
    }

    override fun platform_getCallingActivity(): ComponentName? {
        return super.getCallingActivity()
    }

    override fun getCallingPackage(): String? {
        return activityProxyWrapper?.public_getCallingPackage() ?: super.getCallingPackage()
    }

    override fun platform_getCallingPackage(): String? {
        return super.getCallingPackage()
    }

    override fun getChangingConfigurations(): Int {
        return activityProxyWrapper?.public_getChangingConfigurations() ?: super.getChangingConfigurations()
    }

    override fun platform_getChangingConfigurations(): Int {
        return super.getChangingConfigurations()
    }

    override fun getComponentName(): ComponentName? {
        return activityProxyWrapper?.public_getComponentName() ?: super.getComponentName()
    }

    override fun platform_getComponentName(): ComponentName? {
        return super.getComponentName()
    }

    override fun getContentScene(): Scene? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityProxyWrapper?.public_getContentScene() ?: super.getContentScene()
        } else {
            null
        }
    }

    override fun platform_getContentScene(): Scene? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.getContentScene()
        } else {
            null
        }
    }

    override fun getContentTransitionManager(): TransitionManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityProxyWrapper?.public_getContentTransitionManager() ?: super.getContentTransitionManager()
        } else {
            null
        }
    }

    override fun platform_getContentTransitionManager(): TransitionManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.getContentTransitionManager()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun getCurrentCaller(): ComponentCaller {
        return activityProxyWrapper?.public_getCurrentCaller()!!
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_getCurrentCaller(): ComponentCaller {
        return super.getCurrentCaller()
    }

    override fun getCurrentFocus(): View? {
        return activityProxyWrapper?.public_getCurrentFocus() ?: super.getCurrentFocus()
    }

    override fun platform_getCurrentFocus(): View? {
        return super.getCurrentFocus()
    }

    override fun getFragmentManager(): FragmentManager? {
        return activityProxyWrapper?.public_getFragmentManager() ?: super.getFragmentManager()
    }

    override fun platform_getFragmentManager(): FragmentManager? {
        return super.getFragmentManager()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun getInitialCaller(): ComponentCaller {
        return activityProxyWrapper?.public_getInitialCaller()!!
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_getInitialCaller(): ComponentCaller {
        return super.getInitialCaller()
    }

    override fun getIntent(): Intent? {
        return activityProxyWrapper?.public_getIntent() ?: super.getIntent()
    }
    override fun platform_getIntent(): Intent? {
        return super.getIntent()
    }

    override fun getLastNonConfigurationInstance(): Any? {
        return activityProxyWrapper?.public_getLastNonConfigurationInstance() ?: super.getLastNonConfigurationInstance()
    }

    override fun platform_getLastNonConfigurationInstance(): Any? {
        return super.getLastNonConfigurationInstance()
    }

    override fun getLaunchedFromPackage(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activityProxyWrapper?.public_getLaunchedFromPackage() ?: super.getLaunchedFromPackage()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_getLaunchedFromPackage(): String? {
        return super.getLaunchedFromPackage()
    }

    override fun getLaunchedFromUid(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activityProxyWrapper?.public_getLaunchedFromUid() ?: super.getLaunchedFromUid()
        } else {
            0
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_getLaunchedFromUid(): Int {
        return super.getLaunchedFromUid()
    }

    override fun getLayoutInflater(): LayoutInflater {
        return activityProxyWrapper?.public_getLayoutInflater() ?: super.getLayoutInflater()
    }

    override fun platform_getLayoutInflater(): LayoutInflater {
        return super.getLayoutInflater()
    }

    @Deprecated
    override fun getLoaderManager(): LoaderManager? {
        return activityProxyWrapper?.public_getLoaderManager() ?: super.getLoaderManager()
    }

    @Deprecated
    override fun platform_getLoaderManager(): LoaderManager? {
        return activityProxyWrapper?.public_getLoaderManager() ?: super.getLoaderManager()
    }

    override fun getLocalClassName(): String {
        return activityProxyWrapper?.public_getLocalClassName() ?: super.getLocalClassName()
    }

    override fun platform_getLocalClassName(): String {
        return super.getLocalClassName()
    }

    override fun getMaxNumPictureInPictureActions(): Int {
        return activityProxyWrapper?.public_getMaxNumPictureInPictureActions() ?: super.getMaxNumPictureInPictureActions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun platform_getMaxNumPictureInPictureActions(): Int {
        return super.getMaxNumPictureInPictureActions()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_getMediaController(): MediaController {
        return super.getMediaController()
    }

    override fun getMenuInflater(): MenuInflater {
        return activityProxyWrapper?.public_getMenuInflater() ?: super.getMenuInflater()
    }

    override fun platform_getMenuInflater(): MenuInflater {
        return super.getMenuInflater()
    }

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return activityProxyWrapper?.public_getOnBackInvokedDispatcher() ?: super.getOnBackInvokedDispatcher()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun platform_getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return super.getOnBackInvokedDispatcher()
    }

    @Deprecated
    override fun platform_getParent(): Activity {
        return super.getParent()
    }

    override fun getParentActivityIntent(): Intent? {
        return activityProxyWrapper?.public_getParentActivityIntent() ?: super.getParentActivityIntent()
    }

    override fun platform_getParentActivityIntent(): Intent? {
        return super.getParentActivityIntent()
    }

    override fun getPreferences(mode: Int): SharedPreferences? {
        return activityProxyWrapper?.public_getPreferences(mode) ?: super.getPreferences(mode)
    }

    override fun platform_getPreferences(mode: Int): SharedPreferences? {
        return super.getPreferences(mode)
    }

    override fun getReferrer(): Uri? {
        return activityProxyWrapper?.public_getReferrer() ?: super.getReferrer()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun platform_getReferrer(): Uri? {
        return super.getReferrer()
    }

    override fun getRequestedOrientation(): Int {
        return activityProxyWrapper?.public_getRequestedOrientation() ?: super.getRequestedOrientation()
    }

    override fun platform_getRequestedOrientation(): Int {
        return super.getRequestedOrientation()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_getSearchEvent(): SearchEvent? {
        return super.getSearchEvent()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun platform_getSplashScreen(): SplashScreen {
        return super.getSplashScreen()
    }

    override fun getTaskId(): Int {
        return activityProxyWrapper?.public_getTaskId() ?: super.getTaskId()
    }

    override fun platform_getTaskId(): Int {
        return super.getTaskId()
    }

    override fun platform_getTitle(): CharSequence? {
        return super.getTitle()
    }

    override fun platform_getTitleColor(): Int {
        return super.getTitleColor()
    }

    override fun getVoiceInteractor(): VoiceInteractor? {
        return activityProxyWrapper?.public_getVoiceInteractor() ?: super.getVoiceInteractor()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_getVoiceInteractor(): VoiceInteractor? {
        return super.getVoiceInteractor()
    }

    override fun platform_getVolumeControlStream(): Int {
        return super.getVolumeControlStream()
    }

    override fun getWindow(): Window? {
        return activityProxyWrapper?.public_getWindow() ?: super.getWindow()
    }

    override fun platform_getWindow(): Window? {
        return super.getWindow()
    }

    override fun getWindowManager(): WindowManager? {
        return activityProxyWrapper?.public_getWindowManager() ?: super.getWindowManager()
    }

    override fun platform_getWindowManager(): WindowManager? {
        return super.getWindowManager()
    }

    override fun hasWindowFocus(): Boolean {
        return activityProxyWrapper?.public_hasWindowFocus() ?: super.hasWindowFocus()
    }

    override fun platform_hasWindowFocus(): Boolean {
        return super.hasWindowFocus()
    }

    override fun invalidateOptionsMenu() {
        activityProxyWrapper?.public_invalidateOptionsMenu() ?: super.invalidateOptionsMenu()
    }

    override fun platform_invalidateOptionsMenu() {
        super.invalidateOptionsMenu()
    }

    override fun isActivityTransitionRunning(): Boolean {
        return activityProxyWrapper?.public_isActivityTransitionRunning() ?: super.isActivityTransitionRunning()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun platform_isActivityTransitionRunning(): Boolean {
        return super.isActivityTransitionRunning()
    }

    @Deprecated
    fun isBackgroundVisibleBehind(): Boolean {
        throw UnsupportedOperationException("isBackgroundVisibleBehind is SystemApi")
    }

    override fun platform_isBackgroundVisibleBehind(): Boolean {
        throw UnsupportedOperationException("isBackgroundVisibleBehind is SystemApi")
    }

    override fun isChangingConfigurations(): Boolean {
        return activityProxyWrapper?.public_isChangingConfigurations() ?: super.isChangingConfigurations()
    }

    override fun platform_isChangingConfigurations(): Boolean {
        return super.isChangingConfigurations()
    }

    override fun platform_isChild(): Boolean {
        return super.isChild()
    }

    override fun platform_isDestroyed(): Boolean {
        return super.isDestroyed()
    }

    override fun isFinishing(): Boolean {
        return activityProxyWrapper?.public_isFinishing() ?: super.isFinishing()
    }

    override fun platform_isFinishing(): Boolean {
        return super.isFinishing()
    }

    override fun isImmersive(): Boolean {
        return activityProxyWrapper?.public_isImmersive() ?: super.isImmersive()
    }

    override fun platform_isImmersive(): Boolean {
        return super.isImmersive()
    }

    override fun isInMultiWindowMode(): Boolean {
        return activityProxyWrapper?.public_isInMultiWindowMode() ?: super.isInMultiWindowMode()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_isInMultiWindowMode(): Boolean {
        return super.isInMultiWindowMode()
    }

    override fun isInPictureInPictureMode(): Boolean {
        return activityProxyWrapper?.public_isInPictureInPictureMode() ?: super.isInPictureInPictureMode()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_isInPictureInPictureMode(): Boolean {
        return super.isInPictureInPictureMode()
    }

    override fun isLaunchedFromBubble(): Boolean {
        return activityProxyWrapper?.public_isLaunchedFromBubble() ?: super.isLaunchedFromBubble()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun platform_isLaunchedFromBubble(): Boolean {
        return super.isLaunchedFromBubble()
    }

    override fun isLocalVoiceInteractionSupported(): Boolean {
        return activityProxyWrapper?.public_isLocalVoiceInteractionSupported() ?: super.isLocalVoiceInteractionSupported()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_isLocalVoiceInteractionSupported(): Boolean {
        return super.isLocalVoiceInteractionSupported()
    }

    override fun isTaskRoot(): Boolean {
        return activityProxyWrapper?.public_isTaskRoot() ?: super.isTaskRoot()
    }
    override fun platform_isTaskRoot(): Boolean {
        return super.isTaskRoot()
    }

    override fun isVoiceInteraction(): Boolean {
        return activityProxyWrapper?.public_isVoiceInteraction() ?: super.isVoiceInteraction()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_isVoiceInteraction(): Boolean {
        return super.isVoiceInteraction()
    }
    override fun isVoiceInteractionRoot(): Boolean {
        return activityProxyWrapper?.public_isVoiceInteractionRoot() ?: super.isVoiceInteractionRoot()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_isVoiceInteractionRoot(): Boolean {
        return super.isVoiceInteractionRoot()
    }

    @Deprecated
    override fun platform_managedQuery(uri: Uri?, projection: Array<String?>?, selection: String?, selectionArgs: Array<String?>?, sortOrder: String?): Cursor? {
        return super.managedQuery(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun moveTaskToBack(nonRoot: Boolean): Boolean {
        return activityProxyWrapper?.public_moveTaskToBack(nonRoot) ?: super.moveTaskToBack(nonRoot)
    }

    override fun platform_moveTaskToBack(nonRoot: Boolean): Boolean {
        return super.moveTaskToBack(nonRoot)
    }

    override fun navigateUpTo(intent: Intent?): Boolean {
        return activityProxyWrapper?.public_navigateUpTo(intent) ?: super.navigateUpTo(intent)
    }

    override fun platform_navigateUpTo(intent: Intent?): Boolean {
        return super.navigateUpTo(intent)
    }

    override fun navigateUpToFromChild(child: Activity?, intent: Intent?): Boolean {
        return activityProxyWrapper?.public_navigateUpToFromChild(child, intent) ?: super.navigateUpToFromChild(child, intent)
    }

    override fun platform_navigateUpToFromChild(child: Activity?, intent: Intent?): Boolean {
        return super.navigateUpToFromChild(child, intent)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        activityProxyWrapper?.public_onActionModeFinished(mode) ?: super.onActionModeFinished(mode)
    }

    override fun platform_onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        activityProxyWrapper?.public_onActionModeStarted(mode) ?: super.onActionModeStarted(mode)
    }

    override fun platform_onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        activityProxyWrapper?.public_onActivityReenter(resultCode, data) ?: super.onActivityReenter(resultCode, data)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityProxyWrapper?.public_onActivityResult(requestCode, resultCode, data) ?: super.onActivityResult(requestCode, resultCode, data)
    }

    override fun platform_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            activityProxyWrapper?.public_onActivityResult(requestCode, resultCode, data, caller) ?: super.onActivityResult(requestCode, resultCode, data, caller)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)
    }

    @Deprecated
    override fun onAttachFragment(fragment: Fragment?) {
        activityProxyWrapper?.public_onAttachFragment(fragment) ?: super.onAttachFragment(fragment)
    }

    @Deprecated
    override fun platform_onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
    }

    override fun onAttachedToWindow() {
        activityProxyWrapper?.public_onAttachedToWindow() ?: super.onAttachedToWindow()
    }

    override fun platform_onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    @Deprecated
    override fun onBackPressed() {
        activityProxyWrapper?.public_onBackPressed() ?: super.onBackPressed()
    }

    override fun platform_onBackPressed() {
        super.onBackPressed()
    }

    @Deprecated
    fun onBackgroundVisibleBehindChanged(visible: Boolean) {
        throw UnsupportedOperationException("not supported")
    }

    @Deprecated
    override fun platform_onBackgroundVisibleBehindChanged(visible: Boolean) {
        throw UnsupportedOperationException("not supported")
    }

    override fun onChildTitleChanged(child: Activity?, title: CharSequence?) {
        activityProxyWrapper?.public_onChildTitleChanged(child, title) ?: super.onChildTitleChanged(child, title)
    }

    override fun platform_onChildTitleChanged(child: Activity?, title: CharSequence?) {
        super.onChildTitleChanged(child, title)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        activityProxyWrapper?.public_onConfigurationChanged(newConfig) ?: super.onConfigurationChanged(newConfig)
    }

    override fun platform_onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onContentChanged() {
        activityProxyWrapper?.public_onContentChanged() ?: super.onContentChanged()
    }

    override fun platform_onContentChanged() {
        super.onContentChanged()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return activityProxyWrapper?.public_onContextItemSelected(item) ?: super.onContextItemSelected(item)
    }

    override fun platform_onContextItemSelected(item: MenuItem): Boolean {
        return super.onContextItemSelected(item)
    }

    override fun onContextMenuClosed(menu: Menu) {
        activityProxyWrapper?.public_onContextMenuClosed(menu) ?: super.onContextMenuClosed(menu)
    }

    override fun platform_onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activityProxyWrapper?.public_onCreate(savedInstanceState) ?: super.onCreate(savedInstanceState)
    }

    override fun platform_onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        activityProxyWrapper?.public_onCreate(savedInstanceState, persistentState) ?: super.onCreate(savedInstanceState, persistentState)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        activityProxyWrapper?.public_onCreateContextMenu(menu, view, menuInfo)
            ?: super.onCreateContextMenu(menu, view, menuInfo)
    }

    override fun platform_onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)
    }

    override fun onCreateDescription(): CharSequence? {
        return activityProxyWrapper?.public_onCreateDescription() ?: super.onCreateDescription()
    }

    override fun platform_onCreateDescription(): CharSequence? {
        return super.onCreateDescription()
    }

    override fun onCreateNavigateUpTaskStack(builder: TaskStackBuilder?) {
        activityProxyWrapper?.public_onCreateNavigateUpTaskStack(builder) ?: super.onCreateNavigateUpTaskStack(builder)
    }

    override fun platform_onCreateNavigateUpTaskStack(builder: TaskStackBuilder?) {
        super.onCreateNavigateUpTaskStack(builder)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return activityProxyWrapper?.public_onCreateOptionsMenu(menu) ?: super.onCreateOptionsMenu(menu)
    }

    override fun platform_onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return activityProxyWrapper?.public_onCreatePanelMenu(featureId, menu) ?: super.onCreatePanelMenu(featureId, menu)
    }

    override fun platform_onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return super.onCreatePanelMenu(featureId, menu)
    }

    override fun onCreatePanelView(featureId: Int): View? {
        return activityProxyWrapper?.public_onCreatePanelView(featureId) ?: super.onCreatePanelView(featureId)
    }

    override fun platform_onCreatePanelView(featureId: Int): View? {
        return super.onCreatePanelView(featureId)
    }

    @Deprecated
    override fun onCreateThumbnail(outBitmap: Bitmap?, canvas: Canvas?): Boolean {
        return activityProxyWrapper?.public_onCreateThumbnail(outBitmap, canvas) ?: super.onCreateThumbnail(outBitmap, canvas)
    }

    @Deprecated
    override fun platform_onCreateThumbnail(outBitmap: Bitmap?, canvas: Canvas?): Boolean {
        return super.onCreateThumbnail(outBitmap, canvas)
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        return activityProxyWrapper?.public_onCreateView(parent, name, context, attrs) ?: super.onCreateView(parent, name, context, attrs)
    }

    override fun platform_onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return activityProxyWrapper?.public_onCreateView(name, context, attrs) ?: super.onCreateView(name, context, attrs)
    }

    override fun platform_onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
    }

    override fun onDestroy() {
        activityProxyWrapper?.public_onDestroy() ?: super.onDestroy()
    }

    override fun platform_onDestroy() {
        super.onDestroy()
    }

    override fun onDetachedFromWindow() {
        activityProxyWrapper?.public_onDetachedFromWindow() ?: super.onDetachedFromWindow()
    }

    override fun platform_onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_onGenericMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    override fun platform_onGenericMotionEvent(event: MotionEvent?): Boolean {
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_onKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    override fun platform_onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_onKeyLongPress(keyCode, event) ?: super.onKeyLongPress(keyCode, event)
    }

    override fun platform_onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_onKeyMultiple(keyCode, repeatCount, event) ?: super.onKeyMultiple(keyCode, repeatCount, event)
    }

    override fun platform_onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_onKeyShortcut(keyCode, event) ?: super.onKeyShortcut(keyCode, event)
    }

    override fun platform_onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyShortcut(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return activityProxyWrapper?.public_onKeyUp(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }

    override fun platform_onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onLocalVoiceInteractionStarted() {
        activityProxyWrapper?.public_onLocalVoiceInteractionStarted() ?: super.onLocalVoiceInteractionStarted()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()
    }

    override fun onLocalVoiceInteractionStopped() {
        activityProxyWrapper?.public_onLocalVoiceInteractionStopped() ?: super.onLocalVoiceInteractionStopped()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_onLocalVoiceInteractionStopped() {
        super.onLocalVoiceInteractionStopped()
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return activityProxyWrapper?.public_onMenuItemSelected(featureId, item) ?: super.onMenuItemSelected(featureId, item)
    }

    override fun platform_onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return super.onMenuItemSelected(featureId, item)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return activityProxyWrapper?.public_onMenuOpened(featureId, menu) ?: super.onMenuOpened(featureId, menu)
    }

    override fun platform_onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return super.onMenuOpened(featureId, menu)
    }

    override fun onNavigateUp(): Boolean {
        return activityProxyWrapper?.public_onNavigateUp() ?: super.onNavigateUp()
    }

    override fun platform_onNavigateUp(): Boolean {
        return super.onNavigateUp()
    }

    @Deprecated
    override fun onNavigateUpFromChild(child: Activity?): Boolean {
        return activityProxyWrapper?.public_onNavigateUpFromChild(child) ?: super.onNavigateUpFromChild(child)
    }

    @Deprecated
    override fun platform_onNavigateUpFromChild(child: Activity?): Boolean {
        return super.onNavigateUpFromChild(child)
    }

    override fun onNewIntent(intent: Intent?) {
        activityProxyWrapper?.public_onNewIntent(intent) ?: super.onNewIntent(intent)
    }

    override fun platform_onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        activityProxyWrapper?.public_onNewIntent(intent, caller) ?: super.onNewIntent(intent, caller)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return activityProxyWrapper?.public_onOptionsItemSelected(item) ?: super.onOptionsItemSelected(item)
    }

    override fun platform_onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    override fun onOptionsMenuClosed(menu: Menu) {
        activityProxyWrapper?.public_onOptionsMenuClosed(menu) ?: super.onOptionsMenuClosed(menu)
    }

    override fun platform_onOptionsMenuClosed(menu: Menu) {
        return super.onOptionsMenuClosed(menu)
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        activityProxyWrapper?.public_onPanelClosed(featureId, menu) ?: super.onPanelClosed(featureId, menu)
    }

    override fun platform_onPanelClosed(featureId: Int, menu: Menu) {
        return super.onPanelClosed(featureId, menu)
    }

    override fun onPause() {
        activityProxyWrapper?.public_onPause() ?: super.onPause()
    }

    override fun platform_onPause() {
        return super.onPause()
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        activityProxyWrapper?.public_onPointerCaptureChanged(hasCapture) ?: super.onPointerCaptureChanged(hasCapture)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun platform_onPointerCaptureChanged(hasCapture: Boolean) {
        super.onPointerCaptureChanged(hasCapture)
    }
    override fun onPostCreate(savedInstanceState: Bundle?) {
        activityProxyWrapper?.public_onPostCreate(savedInstanceState) ?: super.onPostCreate(savedInstanceState)
    }

    override fun platform_onPostCreate(savedInstanceState: Bundle?) {
        return super.onPostCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        activityProxyWrapper?.public_onPostCreate(savedInstanceState, persistentState) ?: super.onPostCreate(savedInstanceState, persistentState)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        return super.onPostCreate(savedInstanceState, persistentState)
    }

    override fun onPostResume() {
        activityProxyWrapper?.public_onPostResume() ?: super.onPostResume()
    }

    override fun platform_onPostResume() {
        return super.onPostResume()
    }

    override fun onPrepareNavigateUpTaskStack(builder: TaskStackBuilder?) {
        activityProxyWrapper?.public_onPrepareNavigateUpTaskStack(builder) ?: super.onPrepareNavigateUpTaskStack(builder)
    }

    override fun platform_onPrepareNavigateUpTaskStack(builder: TaskStackBuilder?) {
        super.onPrepareNavigateUpTaskStack(builder)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return activityProxyWrapper?.public_onPrepareOptionsMenu(menu) ?: super.onPrepareOptionsMenu(menu)
    }

    override fun platform_onPrepareOptionsMenu(menu: Menu): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return activityProxyWrapper?.public_onPreparePanel(featureId, view, menu) ?: super.onPreparePanel(featureId, view, menu)
    }

    override fun platform_onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return super.onPreparePanel(featureId, view, menu)
    }

    override fun onProvideReferrer(): Uri? {
        return activityProxyWrapper?.public_onProvideReferrer() ?: super.onProvideReferrer()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_onProvideReferrer(): Uri? {
        return super.onProvideReferrer()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        activityProxyWrapper?.public_onRequestPermissionsResult(requestCode, permissions, grantResults) ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, userId: Int) {
        activityProxyWrapper?.public_onRequestPermissionsResult(requestCode, permissions, grantResults, userId) ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults, userId)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, userId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, userId)
    }

    override fun onRestart() {
        activityProxyWrapper?.public_onRestart() ?: super.onRestart()
    }

    override fun platform_onRestart() {
        super.onRestart()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        activityProxyWrapper?.public_onRestoreInstanceState(savedInstanceState) ?: super.onRestoreInstanceState(savedInstanceState)
    }

    override fun platform_onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        activityProxyWrapper?.public_onRestoreInstanceState(savedInstanceState, persistentState) ?: super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_onRestoreInstanceState(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override fun onResume() {
        activityProxyWrapper?.public_onResume() ?: super.onResume()
    }

    override fun platform_onResume() {
        super.onResume()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        activityProxyWrapper?.public_onSaveInstanceState(outState) ?: super.onSaveInstanceState(outState)
    }

    override fun platform_onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        activityProxyWrapper?.public_onSaveInstanceState(outState, outPersistentState) ?: super.onSaveInstanceState(outState, outPersistentState)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }
    override fun onSearchRequested(): Boolean {
        return activityProxyWrapper?.public_onSearchRequested() ?: super.onSearchRequested()
    }

    override fun platform_onSearchRequested(): Boolean {
        return super.onSearchRequested()
    }

    override fun onSearchRequested(event: SearchEvent?): Boolean {
        return activityProxyWrapper?.public_onSearchRequested(event) ?: super.onSearchRequested(event)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_onSearchRequested(event: SearchEvent?): Boolean {
        return super.onSearchRequested(event)
    }

    override fun onStart() {
        activityProxyWrapper?.public_onStart() ?: super.onStart()
    }

    override fun platform_onStart() {
        super.onStart()
    }

    @Deprecated
    override fun onStateNotSaved() {
        activityProxyWrapper?.public_onStateNotSaved() ?: super.onStateNotSaved()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Deprecated
    override fun platform_onStateNotSaved() {
        super.onStateNotSaved()
    }

    override fun onStop() {
        activityProxyWrapper?.public_onStop() ?: super.onStop()
    }

    override fun platform_onStop() {
        super.onStop()
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        activityProxyWrapper?.public_onTitleChanged(title, color) ?: super.onTitleChanged(title, color)
    }

    override fun platform_onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun platform_onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        return activityProxyWrapper?.public_onTrackballEvent(event) ?: super.onTrackballEvent(event)
    }

    override fun platform_onTrackballEvent(event: MotionEvent?): Boolean {
        return super.onTrackballEvent(event)
    }


    override fun onUserInteraction() {
        activityProxyWrapper?.public_onUserInteraction() ?: super.onUserInteraction()
    }

    override fun platform_onUserInteraction() {
        super.onUserInteraction()
    }

    override fun onUserLeaveHint() {
        activityProxyWrapper?.public_onUserLeaveHint() ?: super.onUserLeaveHint()
    }

    override fun platform_onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    @Deprecated
    override fun onVisibleBehindCanceled() {
        activityProxyWrapper?.public_onVisibleBehindCanceled() ?: super.onVisibleBehindCanceled()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Deprecated
    override fun platform_onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled()
    }

    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        activityProxyWrapper?.public_onWindowAttributesChanged(params) ?: super.onWindowAttributesChanged(params)
    }

    override fun platform_onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        super.onWindowAttributesChanged(params)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        activityProxyWrapper?.public_onWindowFocusChanged(hasFocus) ?: super.onWindowFocusChanged(hasFocus)
    }

    override fun platform_onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return activityProxyWrapper?.public_onWindowStartingActionMode(callback) ?: super.onWindowStartingActionMode(callback)
    }

    override fun platform_onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return super.onWindowStartingActionMode(callback)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return activityProxyWrapper?.public_onWindowStartingActionMode(callback, type) ?: super.onWindowStartingActionMode(callback, type)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return super.onWindowStartingActionMode(callback, type)
    }

    override fun openContextMenu(view: View?) {
        activityProxyWrapper?.public_openContextMenu(view) ?: super.openContextMenu(view)
    }

    override fun platform_openContextMenu(view: View?) {
        super.openContextMenu(view)
    }

    override fun openOptionsMenu() {
        activityProxyWrapper?.public_openOptionsMenu() ?: super.openOptionsMenu()
    }

    override fun platform_openOptionsMenu() {
        super.openOptionsMenu()
    }

    override fun overrideActivityTransition(transitionType: Int, enterAnim: Int, exitAnim: Int) {
        activityProxyWrapper?.public_overrideActivityTransition(transitionType, enterAnim, exitAnim) ?: super.overrideActivityTransition(transitionType, enterAnim, exitAnim)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_overrideActivityTransition(transitionType: Int, enterAnim: Int, exitAnim: Int) {
        super.overrideActivityTransition(transitionType, enterAnim, exitAnim)
    }

    override fun overrideActivityTransition(transitionType: Int, enterAnim: Int, exitAnim: Int, backgroundColor: Int) {
        activityProxyWrapper?.public_overrideActivityTransition(transitionType, enterAnim, exitAnim, backgroundColor) ?: super.overrideActivityTransition(transitionType, enterAnim, exitAnim, backgroundColor)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_overrideActivityTransition(transitionType: Int, enterAnim: Int, exitAnim: Int, backgroundColor: Int) {
        super.overrideActivityTransition(transitionType, enterAnim, exitAnim, backgroundColor)
    }

    @Deprecated
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        activityProxyWrapper?.public_overridePendingTransition(enterAnim, exitAnim) ?: super.overridePendingTransition(enterAnim, exitAnim)
    }

    @Deprecated
    override fun platform_overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        super.overridePendingTransition(enterAnim, exitAnim)
    }

    @Deprecated
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int, backgroundColor: Int) {
        activityProxyWrapper?.public_overridePendingTransition(enterAnim, exitAnim, backgroundColor) ?: super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Deprecated
    override fun platform_overridePendingTransition(enterAnim: Int, exitAnim: Int, backgroundColor: Int) {
        super.overridePendingTransition(enterAnim, exitAnim, backgroundColor)
    }

    override fun postponeEnterTransition() {
        activityProxyWrapper?.public_postponeEnterTransition() ?: super.postponeEnterTransition()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_postponeEnterTransition() {
        super.postponeEnterTransition()
    }

    override fun recreate() {
        activityProxyWrapper?.public_recreate() ?: super.recreate()
    }

    override fun platform_recreate() {
        super.recreate()
    }

    override fun registerActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
        activityProxyWrapper?.public_registerActivityLifecycleCallbacks(callback) ?: super.registerActivityLifecycleCallbacks(callback)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun platform_registerActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
        super.registerActivityLifecycleCallbacks(callback)
    }

    override fun registerForContextMenu(view: View?) {
        activityProxyWrapper?.public_registerForContextMenu(view) ?: super.registerForContextMenu(view)
    }

    override fun platform_registerForContextMenu(view: View?) {
        super.registerForContextMenu(view)
    }

    override fun releaseInstance(): Boolean {
        return activityProxyWrapper?.public_releaseInstance() ?: super.releaseInstance()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_releaseInstance(): Boolean {
        return super.releaseInstance()
    }

    override fun reportFullyDrawn() {
        activityProxyWrapper?.public_reportFullyDrawn() ?: super.reportFullyDrawn()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_reportFullyDrawn() {
        super.reportFullyDrawn()
    }

    override fun requestDragAndDropPermissions(event: DragEvent?): DragAndDropPermissions? {
        return activityProxyWrapper?.public_requestDragAndDropPermissions(event) ?: super.requestDragAndDropPermissions(event)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_requestDragAndDropPermissions(event: DragEvent?): DragAndDropPermissions? {
        return super.requestDragAndDropPermissions(event)
    }

    override fun requestFullscreenMode(request: Int, approvalCallback: OutcomeReceiver<Void, Throwable>?) {
        activityProxyWrapper?.public_requestFullscreenMode(request, approvalCallback) ?: super.requestFullscreenMode(request, approvalCallback)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_requestFullscreenMode(request: Int, approvalCallback: OutcomeReceiver<Void, Throwable>?) {
        super.requestFullscreenMode(request, approvalCallback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_requestPermissions(permissions: Array<String?>, requestCode: Int) {
        super.requestPermissions(permissions, requestCode)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_requestPermissions(permissions: Array<String?>, requestCode: Int, userId: Int) {
        super.requestPermissions(permissions, requestCode, userId)
    }

    @Deprecated
    override fun requestVisibleBehind(visible: Boolean): Boolean {
        return activityProxyWrapper?.public_requestVisibleBehind(visible) ?: super.requestVisibleBehind(visible)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Deprecated
    override fun platform_requestVisibleBehind(visible: Boolean): Boolean {
        return super.requestVisibleBehind(visible)
    }

    override fun platform_requestWindowFeature(featureId: Int): Boolean {
        return super.requestWindowFeature(featureId)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun platform_requireViewById(id: Int): View? {
        return super.requireViewById(id)
    }

    override fun platform_runOnUiThread(action: Runnable?) {
        super.runOnUiThread(action)
    }

    override fun setActionBar(toolbar: Toolbar?) {
        activityProxyWrapper?.public_setActionBar(toolbar) ?: super.setActionBar(toolbar)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setActionBar(toolbar: Toolbar?) {
        super.setActionBar(toolbar)
    }

    override fun setAllowCrossUidActivitySwitchFromBelow(allow: Boolean) {
        activityProxyWrapper?.public_setAllowCrossUidActivitySwitchFromBelow(allow) ?: super.setAllowCrossUidActivitySwitchFromBelow(allow)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_setAllowCrossUidActivitySwitchFromBelow(allow: Boolean) {
        super.setAllowCrossUidActivitySwitchFromBelow(allow)
    }

    override fun setContentTransitionManager(transitionManager: TransitionManager?) {
        activityProxyWrapper?.public_setContentTransitionManager(transitionManager) ?: super.setContentTransitionManager(transitionManager)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setContentTransitionManager(transitionManager: TransitionManager?) {
        super.setContentTransitionManager(transitionManager)
    }

    override fun setContentView(layoutResID: Int) {
        activityProxyWrapper?.public_setContentView(layoutResID) ?: super.setContentView(layoutResID)
    }

    override fun platform_setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
    }

    override fun setContentView(view: View?) {
        activityProxyWrapper?.public_setContentView(view) ?: super.setContentView(view)
    }

    override fun platform_setContentView(view: View?) {
        super.setContentView(view)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        activityProxyWrapper?.public_setContentView(view, params) ?: super.setContentView(view, params)
    }

    override fun platform_setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
    }

    override fun platform_setDefaultKeyMode(mode: Int) {
        super.setDefaultKeyMode(mode)
    }
    override fun setEnterSharedElementCallback(callback: SharedElementCallback?) {
        activityProxyWrapper?.public_setEnterSharedElementCallback(callback) ?: super.setEnterSharedElementCallback(callback)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setEnterSharedElementCallback(callback: SharedElementCallback?) {
        super.setEnterSharedElementCallback(callback)
    }

    override fun setExitSharedElementCallback(callback: SharedElementCallback?) {
        activityProxyWrapper?.public_setExitSharedElementCallback(callback) ?: super.setExitSharedElementCallback(callback)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setExitSharedElementCallback(callback: SharedElementCallback?) {
        super.setExitSharedElementCallback(callback)
    }

    override fun platform_setFeatureDrawable(featureId: Int, drawable: Drawable?) {
        super.setFeatureDrawable(featureId, drawable)
    }

    override fun platform_setFeatureDrawableAlpha(featureId: Int, alpha: Int) {
        super.setFeatureDrawableAlpha(featureId, alpha)
    }

    override fun platform_setFeatureDrawableResource(featureId: Int, resId: Int) {
        super.setFeatureDrawableResource(featureId, resId)
    }

    override fun platform_setFeatureDrawableUri(featureId: Int, uri: Uri?) {
        super.setFeatureDrawableUri(featureId, uri)
    }

    override fun setFinishOnTouchOutside(finish: Boolean) {
        activityProxyWrapper?.public_setFinishOnTouchOutside(finish) ?: super.setFinishOnTouchOutside(finish)
    }

    override fun platform_setFinishOnTouchOutside(finish: Boolean) {
        super.setFinishOnTouchOutside(finish)
    }

    override fun setImmersive(immersive: Boolean) {
        activityProxyWrapper?.public_setImmersive(immersive) ?: super.setImmersive(immersive)
    }

    override fun platform_setImmersive(immersive: Boolean) {
        super.setImmersive(immersive)
    }

    override fun setInheritShowWhenLocked(showWhenLocked: Boolean) {
        activityProxyWrapper?.public_setInheritShowWhenLocked(showWhenLocked) ?: super.setInheritShowWhenLocked(showWhenLocked)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun platform_setInheritShowWhenLocked(showWhenLocked: Boolean) {
        super.setInheritShowWhenLocked(showWhenLocked)
    }

    override fun setIntent(intent: Intent?) {
        activityProxyWrapper?.public_setIntent(intent) ?: super.setIntent(intent)
    }

    override fun platform_setIntent(intent: Intent?) {
        super.setIntent(intent)
    }

    override fun setIntent(intent: Intent?, caller: ComponentCaller?) {
        activityProxyWrapper?.public_setIntent(intent, caller) ?: super.setIntent(intent, caller)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_setIntent(intent: Intent?, caller: ComponentCaller?) {
        super.setIntent(intent, caller)
    }

    override fun setLocusContext(locusId: LocusId?, bundle: Bundle?) {
        activityProxyWrapper?.public_setLocusContext(locusId, bundle) ?: super.setLocusContext(locusId, bundle)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun platform_setLocusContext(locusId: LocusId?, bundle: Bundle?) {
        super.setLocusContext(locusId, bundle)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setMediaController(controller: MediaController?) {
        super.setMediaController(controller)
    }

    override fun setPictureInPictureParams(params: PictureInPictureParams) {
        activityProxyWrapper?.public_setPictureInPictureParams(params) ?: super.setPictureInPictureParams(params)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun platform_setPictureInPictureParams(params: PictureInPictureParams) {
        super.setPictureInPictureParams(params)
    }

    @Deprecated
    override fun platform_setProgress(progress: Int) {
        super.setProgress(progress)
    }

    @Deprecated
    override fun platform_setProgressBarIndeterminate(indeterminate: Boolean) {
        super.setProgressBarIndeterminate(indeterminate)
    }

    @Deprecated
    override fun platform_setProgressBarIndeterminateVisibility(visible: Boolean) {
        super.setProgressBarIndeterminateVisibility(visible)
    }

    override fun platform_setProgressBarVisibility(visible: Boolean) {
        super.setProgressBarVisibility(visible)
    }

    override fun setRecentsScreenshotEnabled(enabled: Boolean) {
        activityProxyWrapper?.public_setRecentsScreenshotEnabled(enabled) ?: super.setRecentsScreenshotEnabled(enabled)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun platform_setRecentsScreenshotEnabled(enabled: Boolean) {
        super.setRecentsScreenshotEnabled(enabled)
    }

    override fun setRequestedOrientation(orientation: Int) {
        activityProxyWrapper?.public_setRequestedOrientation(orientation) ?: super.setRequestedOrientation(orientation)
    }

    override fun platform_setRequestedOrientation(orientation: Int) {
        super.setRequestedOrientation(orientation)
    }

    override fun platform_setResult(resultCode: Int) {
        super.setResult(resultCode)
    }

    override fun platform_setResult(resultCode: Int, data: Intent?) {
        super.setResult(resultCode, data)
    }

    @Deprecated
    override fun platform_setSecondaryProgress(secondaryProgress: Int) {
        super.setSecondaryProgress(secondaryProgress)
    }

    override fun setShouldDockBigOverlays(shouldDock: Boolean) {
        activityProxyWrapper?.public_setShouldDockBigOverlays(shouldDock) ?: super.setShouldDockBigOverlays(shouldDock)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun platform_setShouldDockBigOverlays(shouldDock: Boolean) {
        super.setShouldDockBigOverlays(shouldDock)
    }

    override fun setShowWhenLocked(showWhenLocked: Boolean) {
        activityProxyWrapper?.public_setShowWhenLocked(showWhenLocked) ?: super.setShowWhenLocked(showWhenLocked)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun platform_setShowWhenLocked(showWhenLocked: Boolean) {
        super.setShowWhenLocked(showWhenLocked)
    }

    override fun setTaskDescription(description: ActivityManager.TaskDescription?) {
        activityProxyWrapper?.public_setTaskDescription(description) ?: super.setTaskDescription(description)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_setTaskDescription(description: ActivityManager.TaskDescription?) {
        super.setTaskDescription(description)
    }

    override fun setTitle(titleId: Int) {
        activityProxyWrapper?.public_setTitle(titleId) ?: super.setTitle(titleId)
    }

    override fun platform_setTitle(titleId: Int) {
        super.setTitle(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        activityProxyWrapper?.public_setTitle(title) ?: super.setTitle(title)
    }

    override fun platform_setTitle(title: CharSequence?) {
        super.setTitle(title)
    }

    @Deprecated
    override fun setTitleColor(textColor: Int) {
        activityProxyWrapper?.public_setTitleColor(textColor) ?: super.setTitleColor(textColor)
    }

    @Deprecated
    override fun platform_setTitleColor(textColor: Int) {
        super.setTitleColor(textColor)
    }

    override fun setTranslucent(translucent: Boolean): Boolean {
        return activityProxyWrapper?.public_setTranslucent(translucent) ?: super.setTranslucent(translucent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun platform_setTranslucent(translucent: Boolean): Boolean {
        return super.setTranslucent(translucent)
    }

    override fun setTurnScreenOn(turnScreenOn: Boolean) {
        activityProxyWrapper?.public_setTurnScreenOn(turnScreenOn) ?: super.setTurnScreenOn(turnScreenOn)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun platform_setTurnScreenOn(turnScreenOn: Boolean) {
        super.setTurnScreenOn(turnScreenOn)
    }
    override fun setVisible(visible: Boolean) {
        activityProxyWrapper?.public_setVisible(visible) ?: super.setVisible(visible)
    }

    override fun platform_setVisible(visible: Boolean) {
        super.setVisible(visible)
    }

    override fun platform_setVolumeControlStream(streamType: Int) {
        super.setVolumeControlStream(streamType)
    }

    override fun setVrModeEnabled(enabled: Boolean, requestedComponent: ComponentName) {
        activityProxyWrapper?.public_setVrModeEnabled(enabled, requestedComponent) ?: super.setVrModeEnabled(enabled, requestedComponent)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_setVrModeEnabled(enabled: Boolean, requestedComponent: ComponentName) {
        super.setVrModeEnabled(enabled, requestedComponent)
    }

    override fun shouldDockBigOverlays(): Boolean {
        return activityProxyWrapper?.public_shouldDockBigOverlays() ?: super.shouldDockBigOverlays()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun platform_shouldDockBigOverlays(): Boolean {
        return super.shouldDockBigOverlays()
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return activityProxyWrapper?.public_shouldShowRequestPermissionRationale(permission) ?: super.shouldShowRequestPermissionRationale(permission)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_shouldShowRequestPermissionRationale(permission: String): Boolean {
        return super.shouldShowRequestPermissionRationale(permission)
    }

    override fun shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean {
        return activityProxyWrapper?.public_shouldShowRequestPermissionRationale(permission, userId) ?: super.shouldShowRequestPermissionRationale(permission, userId)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun platform_shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean {
        return super.shouldShowRequestPermissionRationale(permission, userId)
    }

    override fun shouldUpRecreateTask(targetIntent: Intent?): Boolean {
        return activityProxyWrapper?.public_shouldUpRecreateTask(targetIntent) ?: super.shouldUpRecreateTask(targetIntent)
    }

    override fun platform_shouldUpRecreateTask(targetIntent: Intent?): Boolean {
        return super.shouldUpRecreateTask(targetIntent)
    }

    override fun showAssist(args: Bundle?): Boolean {
        return activityProxyWrapper?.public_showAssist(args) ?: super.showAssist(args)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_showAssist(args: Bundle?): Boolean {
        return super.showAssist(args)
    }

    override fun showLockTaskEscapeMessage() {
        activityProxyWrapper?.public_showLockTaskEscapeMessage() ?: super.showLockTaskEscapeMessage()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_showLockTaskEscapeMessage() {
        super.showLockTaskEscapeMessage()
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return activityProxyWrapper?.public_startActionMode(callback) ?: super.startActionMode(callback)
    }

    override fun platform_startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return super.startActionMode(callback)
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return activityProxyWrapper?.public_startActionMode(callback, type) ?: super.startActionMode(callback, type)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun platform_startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return super.startActionMode(callback, type)
    }

    override fun startActivities(intents: Array<Intent?>) {
        activityProxyWrapper?.public_startActivities(intents) ?: super.startActivities(intents)
    }

    override fun platform_startActivities(intents: Array<Intent?>) {
        super.startActivities(intents)
    }

    override fun startActivities(intents: Array<Intent?>, options: Bundle?) {
        activityProxyWrapper?.public_startActivities(intents, options) ?: super.startActivities(intents, options)
    }

    override fun platform_startActivities(intents: Array<Intent?>, options: Bundle?) {
        super.startActivities(intents, options)
    }

    override fun startActivity(intent: Intent?) {
        activityProxyWrapper?.public_startActivity(intent) ?: super.startActivity(intent)
    }

    override fun platform_startActivity(intent: Intent?) {
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        activityProxyWrapper?.public_startActivity(intent, options) ?: super.startActivity(intent, options)
    }

    override fun platform_startActivity(intent: Intent?, options: Bundle?) {
        super.startActivity(intent, options)
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        activityProxyWrapper?.public_startActivityForResult(intent, requestCode) ?: super.startActivityForResult(intent, requestCode)
    }

    override fun platform_startActivityForResult(intent: Intent?, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        activityProxyWrapper?.public_startActivityForResult(intent, requestCode, options) ?: super.startActivityForResult(intent, requestCode, options)
    }

    override fun platform_startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
    }

    fun startActivityForResultAsUser(intent: Intent?, requestCode: Int, options: Bundle?, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_startActivityForResultAsUser(intent: Intent?, requestCode: Int, options: Bundle?, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    fun startActivityForResultAsUser(intent: Intent?, requestCode: Int, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun platform_startActivityForResultAsUser(intent: Intent?, requestCode: Int, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    fun startActivityForResultAsUser(intent: Intent?, permission: String?, requestCode: Int, options: Bundle?, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun platform_startActivityForResultAsUser(intent: Intent?, permission: String?, requestCode: Int, options: Bundle?, user: UserHandle?) {
        throw UnsupportedOperationException("not supported @SystemApi")
    }

    @Deprecated
    override fun startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int) {
        activityProxyWrapper?.public_startActivityFromChild(child, intent, requestCode) ?: super.startActivityFromChild(child, intent, requestCode)
    }

    override fun platform_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int) {
        super.startActivityFromChild(child, intent, requestCode)
    }

    @Deprecated
    override fun startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int, options: Bundle?) {
        activityProxyWrapper?.public_startActivityFromChild(child, intent, requestCode, options) ?: super.startActivityFromChild(child, intent, requestCode, options)
    }

    @Deprecated
    override fun platform_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityFromChild(child, intent, requestCode, options)
    }

    @Deprecated
    override fun startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int) {
        activityProxyWrapper?.public_startActivityFromFragment(fragment, intent, requestCode) ?: super.startActivityFromFragment(fragment, intent, requestCode)
    }

    @Deprecated
    override fun platform_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int) {
        super.startActivityFromFragment(fragment, intent, requestCode)
    }

    @Deprecated
    override fun startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int, options: Bundle?) {
        activityProxyWrapper?.public_startActivityFromFragment(fragment, intent, requestCode, options) ?: super.startActivityFromFragment(fragment, intent, requestCode, options)
    }

    @Deprecated
    override fun platform_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int, options: Bundle?) {
        super.startActivityFromFragment(fragment, intent, requestCode, options)
    }

    override fun startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return activityProxyWrapper?.public_startActivityIfNeeded(intent, requestCode) ?: super.startActivityIfNeeded(intent, requestCode)
    }

    override fun platform_startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return super.startActivityIfNeeded(intent, requestCode)
    }

    override fun startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean {
        return activityProxyWrapper?.public_startActivityIfNeeded(intent, requestCode, options) ?: super.startActivityIfNeeded(intent, requestCode, options)
    }

    override fun platform_startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean {
        return super.startActivityIfNeeded(intent, requestCode, options)
    }

    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        activityProxyWrapper?.public_startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags) ?: super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun platform_startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        activityProxyWrapper?.public_startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags, options) ?: super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun platform_startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        activityProxyWrapper?.public_startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags) ?: super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun platform_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        activityProxyWrapper?.public_startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options) ?: super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun platform_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @Deprecated
    override fun startIntentSenderFromChild(child: Activity?, intentSender: IntentSender?, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        activityProxyWrapper?.public_startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags) ?: super.startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    @Deprecated
    override fun platform_startIntentSenderFromChild(child: Activity?, intentSender: IntentSender?, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        super.startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    @Deprecated
    override fun startIntentSenderFromChild(child: Activity?, intentSender: IntentSender?, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        activityProxyWrapper?.public_startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options) ?: super.startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @Deprecated
    override fun platform_startIntentSenderFromChild(child: Activity?, intentSender: IntentSender?, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        super.startIntentSenderFromChild(child, intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun startLocalVoiceInteraction(privateOptions: Bundle?) {
        activityProxyWrapper?.public_startLocalVoiceInteraction(privateOptions) ?: super.startLocalVoiceInteraction(privateOptions)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_startLocalVoiceInteraction(privateOptions: Bundle?) {
        super.startLocalVoiceInteraction(privateOptions)
    }

    override fun startLockTask() {
        activityProxyWrapper?.public_startLockTask() ?: super.startLockTask()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_startLockTask() {
        super.startLockTask()
    }

    @Deprecated
    override fun startManagingCursor(cursor: Cursor?) {
        activityProxyWrapper?.public_startManagingCursor(cursor) ?: super.startManagingCursor(cursor)
    }

    @Deprecated
    override fun platform_startManagingCursor(cursor: Cursor?) {
        super.startManagingCursor(cursor)
    }

    override fun startNextMatchingActivity(intent: Intent): Boolean {
        return activityProxyWrapper?.public_startNextMatchingActivity(intent) ?: super.startNextMatchingActivity(intent)
    }

    override fun platform_startNextMatchingActivity(intent: Intent): Boolean {
        return super.startNextMatchingActivity(intent)
    }

    override fun startNextMatchingActivity(intent: Intent, options: Bundle?): Boolean {
        return activityProxyWrapper?.public_startNextMatchingActivity(intent, options) ?: super.startNextMatchingActivity(intent, options)
    }

    override fun platform_startNextMatchingActivity(intent: Intent, options: Bundle?): Boolean {
        return super.startNextMatchingActivity(intent, options)
    }

    override fun startPostponedEnterTransition() {
        activityProxyWrapper?.public_startPostponedEnterTransition() ?: super.startPostponedEnterTransition()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
    }

    override fun startSearch(initialQuery: String?, selectInitialQuery: Boolean, appSearchData: Bundle?, globalSearch: Boolean) {
        activityProxyWrapper?.public_startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch) ?: super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch)
    }

    override fun platform_startSearch(initialQuery: String?, selectInitialQuery: Boolean, appSearchData: Bundle?, globalSearch: Boolean) {
        super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch)
    }

    override fun stopLocalVoiceInteraction() {
        activityProxyWrapper?.public_stopLocalVoiceInteraction() ?: super.stopLocalVoiceInteraction()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun platform_stopLocalVoiceInteraction() {
        super.stopLocalVoiceInteraction()
    }

    override fun stopLockTask() {
        activityProxyWrapper?.public_stopLockTask() ?: super.stopLockTask()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun platform_stopLockTask() {
        super.stopLockTask()
    }

    @Deprecated
    override fun stopManagingCursor(cursor: Cursor?) {
        activityProxyWrapper?.public_stopManagingCursor(cursor) ?: super.stopManagingCursor(cursor)
    }

    @Deprecated
    override fun platform_stopManagingCursor(cursor: Cursor?) {
        super.stopManagingCursor(cursor)
    }

    override fun takeKeyEvents(get: Boolean) {
        activityProxyWrapper?.public_takeKeyEvents(get) ?: super.takeKeyEvents(get)
    }

    override fun platform_takeKeyEvents(get: Boolean) {
        super.takeKeyEvents(get)
    }

    override fun triggerSearch(query: String?, appSearchData: Bundle?) {
        activityProxyWrapper?.public_triggerSearch(query, appSearchData) ?: super.triggerSearch(query, appSearchData)
    }

    override fun platform_triggerSearch(query: String?, appSearchData: Bundle?) {
        super.triggerSearch(query, appSearchData)
    }

    override fun unregisterActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
        activityProxyWrapper?.public_unregisterActivityLifecycleCallbacks(callback) ?: super.unregisterActivityLifecycleCallbacks(callback)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun platform_unregisterActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
        super.unregisterActivityLifecycleCallbacks(callback)
    }

    override fun unregisterForContextMenu(view: View?) {
        activityProxyWrapper?.public_unregisterForContextMenu(view) ?: super.unregisterForContextMenu(view)
    }

    override fun platform_unregisterForContextMenu(view: View?) {
        super.unregisterForContextMenu(view)
    }

}
