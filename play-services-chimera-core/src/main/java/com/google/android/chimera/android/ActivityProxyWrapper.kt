/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.android

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
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import android.net.Uri
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
import com.google.android.chimera.context.ContextThemeWrapper
import java.lang.Deprecated

open class ActivityProxyWrapper: ContextThemeWrapper {
    private lateinit var activityProxy: IActivityProxy

    protected constructor(context: Context?) : super(context)

    constructor(base: Context?, themeResId: Int) : super(base, themeResId)

    constructor(base: Context?, theme: Resources.Theme?) : super(base, theme)

    open fun addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        activityProxy.platform_addContentView(view, params)
    }

    open fun clearOverrideActivityTransition(v: Int) {
        activityProxy.platform_clearOverrideActivityTransition(v)
    }

    open fun closeContextMenu() {
        activityProxy.platform_closeContextMenu()
    }

    open fun closeOptionsMenu() {
        activityProxy.platform_closeOptionsMenu()
    }

    open fun convertFromTranslucent() {
        activityProxy.platform_convertFromTranslucent()
    }

    open fun convertToTranslucent(
        listener: Any?,
        activityOptions: ActivityOptions?
    ): Boolean {
        return activityProxy.platform_convertToTranslucent(listener, activityOptions)
    }

    open fun createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent {
        return activityProxy.platform_createPendingResult(requestCode, data, flags)
    }

    open fun dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_dispatchGenericMotionEvent(motionEvent)
    }

    open fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_dispatchKeyEvent(keyEvent)
    }

    open fun dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_dispatchKeyShortcutEvent(keyEvent)
    }

    open fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return activityProxy.platform_dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }

    open fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_dispatchTouchEvent(motionEvent)
    }

    open fun dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_dispatchTrackballEvent(motionEvent)
    }

    open fun <T : View?> findViewById(id: Int): T? {
        return this.activityProxy.platform_findViewById(id)
    }

    fun <T : View?> public_findViewById(id: Int): T? {
        return this.findViewById(id)
    }

    open fun finish() {
        activityProxy.platform_finish()
    }

    open fun finishActivity(v: Int) {
        activityProxy.platform_finishActivity(v)
    }

    @Deprecated
    open fun finishActivityFromChild(activity: Activity, requestCode: Int) {
        activityProxy.platform_finishActivityFromChild(activity, requestCode)
    }

    open fun finishAffinity() {
        activityProxy.platform_finishAffinity()
    }

    open fun finishAfterTransition() {
        activityProxy.platform_finishAfterTransition()
    }

    open fun finishAndRemoveTask() {
        activityProxy.platform_finishAndRemoveTask()
    }

    @Deprecated
    open fun finishFromChild(activity: Activity?) {
        activityProxy.platform_finishFromChild(activity)
    }

    open fun getActionBar(): ActionBar? {
        return activityProxy.platform_getActionBar()
    }

    open fun getApplication(): Application? {
        return this.activityProxy.platform_getApplication()
    }

    open fun getCaller(): ComponentCaller? {
        return this.activityProxy.platform_getCaller()
    }

    open fun getCallingActivity(): ComponentName? {
        return this.activityProxy.platform_getCallingActivity()
    }

    open fun getCallingPackage(): String? {
        return this.activityProxy.platform_getCallingPackage()
    }

    open fun getChangingConfigurations(): Int {
        return this.activityProxy.platform_getChangingConfigurations()
    }

    open fun getComponentName(): ComponentName? {
        return this.activityProxy.platform_getComponentName()
    }

    open fun getContentScene(): Scene? {
        return this.activityProxy.platform_getContentScene()
    }

    open fun getContentTransitionManager(): TransitionManager? {
        return this.activityProxy.platform_getContentTransitionManager()
    }

    open fun getCurrentCaller(): ComponentCaller {
        return this.activityProxy.platform_getCurrentCaller()
    }

    open fun getCurrentFocus(): View? {
        return this.activityProxy.platform_getCurrentFocus()
    }

    @Deprecated
    open fun getFragmentManager(): FragmentManager? {
        return this.activityProxy.platform_getFragmentManager()
    }

    open fun getInitialCaller(): ComponentCaller {
        return this.activityProxy.platform_getInitialCaller()
    }

    open fun getIntent(): Intent? {
        return this.activityProxy.platform_getIntent()
    }

    open fun getLastNonConfigurationInstance(): Any? {
        return activityProxy.platform_getLastNonConfigurationInstance()
    }

    open fun getLaunchedFromPackage(): String? {
        return activityProxy.platform_getLaunchedFromPackage()
    }

    open fun getLaunchedFromUid(): Int {
        return activityProxy.platform_getLaunchedFromUid()
    }

    open fun getLayoutInflater(): LayoutInflater {
        return activityProxy.platform_getLayoutInflater()
    }

    @Deprecated
    open fun getLoaderManager(): LoaderManager? {
        return activityProxy.platform_getLoaderManager()
    }

    open fun getLocalClassName(): String {
        return activityProxy.platform_getLocalClassName()
    }

    open fun getMaxNumPictureInPictureActions(): Int {
        return activityProxy.platform_getMaxNumPictureInPictureActions()
    }

    open fun getMediaController(): MediaController? {
        return activityProxy.platform_getMediaController()
    }

    open fun setMediaController(mediaController: MediaController?) {
        activityProxy.platform_setMediaController(mediaController)
    }

    open fun getMenuInflater(): MenuInflater {
        return activityProxy.platform_getMenuInflater()
    }

    open fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return activityProxy.platform_getOnBackInvokedDispatcher()
    }

    @Deprecated
    open fun getParent(): Activity {
        return activityProxy.platform_getParent()
    }

    open fun getParentActivityIntent(): Intent? {
        return activityProxy.platform_getParentActivityIntent()
    }

    open fun getPreferences(v: Int): SharedPreferences? {
        return activityProxy.platform_getPreferences(v)
    }
    open fun getReferrer(): Uri? {
        return activityProxy.platform_getReferrer()
    }

    open fun getRequestedOrientation(): Int {
        return activityProxy.platform_getRequestedOrientation()
    }

    open fun setRequestedOrientation(v: Int) {
        activityProxy.platform_setRequestedOrientation(v)
    }

    open fun getSearchEvent(): SearchEvent? {
        return activityProxy.platform_getSearchEvent()
    }

    open fun getSplashScreen(): SplashScreen {
        return activityProxy.platform_getSplashScreen()
    }

    open fun getTaskId(): Int {
        return activityProxy.platform_getTaskId()
    }

    open fun getTitle(): CharSequence? {
        return activityProxy.platform_getTitle()
    }

    @Deprecated
    open fun getTitleColor(): Int {
        return activityProxy.platform_getTitleColor()
    }

    open fun setTitleColor(v: Int) {
        activityProxy.platform_setTitleColor(v)
    }

    open fun getVoiceInteractor(): VoiceInteractor? {
        return activityProxy.platform_getVoiceInteractor()
    }

    open fun getVolumeControlStream(): Int {
        return activityProxy.platform_getVolumeControlStream()
    }

    open fun setVolumeControlStream(v: Int) {
        activityProxy.platform_setVolumeControlStream(v)
    }

    open fun getWindow(): Window? {
        return activityProxy.platform_getWindow()
    }

    open fun getWindowManager(): WindowManager? {
        return activityProxy.platform_getWindowManager()
    }

    open fun hasWindowFocus(): Boolean {
        return activityProxy.platform_hasWindowFocus()
    }

    open fun invalidateOptionsMenu() {
        activityProxy.platform_invalidateOptionsMenu()
    }
    open fun isActivityTransitionRunning(): Boolean {
        return activityProxy.platform_isActivityTransitionRunning()
    }

    @Deprecated
    open fun isBackgroundVisibleBehind(): Boolean {
        return activityProxy.platform_isBackgroundVisibleBehind()
    }

    open fun isChangingConfigurations(): Boolean {
        return activityProxy.platform_isChangingConfigurations()
    }
    @Deprecated
    open fun isChild(): Boolean {
        return activityProxy.platform_isChild()
    }

    open fun isDestroyed(): Boolean {
        return activityProxy.platform_isDestroyed()
    }

    open fun isFinishing(): Boolean {
        return activityProxy.platform_isFinishing()
    }

    open fun isImmersive(): Boolean {
        return activityProxy.platform_isImmersive()
    }

    open fun setImmersive(z: Boolean) {
        activityProxy.platform_setImmersive(z)
    }

    open fun isInMultiWindowMode(): Boolean {
        return activityProxy.platform_isInMultiWindowMode()
    }

    open fun isInPictureInPictureMode(): Boolean {
        return activityProxy.platform_isInPictureInPictureMode()
    }

    open fun isLaunchedFromBubble(): Boolean {
        return activityProxy.platform_isLaunchedFromBubble()
    }

    open fun isLocalVoiceInteractionSupported(): Boolean {
        return activityProxy.platform_isLocalVoiceInteractionSupported()
    }

    open fun isTaskRoot(): Boolean {
        return activityProxy.platform_isTaskRoot()
    }

    open fun isVoiceInteraction(): Boolean {
        return activityProxy.platform_isVoiceInteraction()
    }

    open fun isVoiceInteractionRoot(): Boolean {
        return activityProxy.platform_isVoiceInteractionRoot()
    }

    @Deprecated
    open fun managedQuery(
        uri: Uri?,
        arr_s: Array<String?>?,
        s: String?,
        arr_s1: Array<String?>?,
        s1: String?
    ): Cursor? {
        return activityProxy.platform_managedQuery(uri, arr_s, s, arr_s1, s1)
    }

    open fun moveTaskToBack(z: Boolean): Boolean {
        return activityProxy.platform_moveTaskToBack(z)
    }

    open fun navigateUpTo(intent: Intent?): Boolean {
        return activityProxy.platform_navigateUpTo(intent)
    }

    @Deprecated
    open fun navigateUpToFromChild(activity: Activity?, intent: Intent?): Boolean {
        return activityProxy.platform_navigateUpToFromChild(activity, intent)
    }

    open fun onActionModeFinished(actionMode: ActionMode?) {
        activityProxy.platform_onActionModeFinished(actionMode)
    }

    open fun onActionModeStarted(actionMode: ActionMode?) {
        activityProxy.platform_onActionModeStarted(actionMode)
    }

    open fun onActivityReenter(v: Int, intent: Intent?) {
        activityProxy.platform_onActivityReenter(v, intent)
    }

    protected open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityProxy.platform_onActivityResult(requestCode, resultCode, data)
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        activityProxy.platform_onActivityResult(requestCode, resultCode, data, caller)
    }

    @Deprecated
    open fun onAttachFragment(fragment: Fragment?) {
        activityProxy.platform_onAttachFragment(fragment)
    }

    open fun onAttachedToWindow() {
        activityProxy.platform_onAttachedToWindow()
    }

    @Deprecated
    open fun onBackPressed() {
        activityProxy.platform_onBackPressed()
    }

    @Deprecated
    open fun onBackgroundVisibleBehindChanged(z: Boolean) {
        activityProxy.platform_onBackgroundVisibleBehindChanged(z)
    }

    protected open fun onChildTitleChanged(
        activity: Activity?,
        charSequence: CharSequence?
    ) {
        activityProxy.platform_onChildTitleChanged(activity, charSequence)
    }

    open fun onConfigurationChanged(configuration: Configuration) {
        activityProxy.platform_onConfigurationChanged(configuration)
    }

    open fun onContentChanged() {
        activityProxy.platform_onContentChanged()
    }

    open fun onContextItemSelected(menuItem: MenuItem): Boolean {
        return activityProxy.platform_onContextItemSelected(menuItem)
    }

    open fun onContextMenuClosed(menu: Menu) {
        activityProxy.platform_onContextMenuClosed(menu)
    }

    protected open fun onCreate(bundle: Bundle?) {
        activityProxy.platform_onCreate(bundle)
    }

    open fun onCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        activityProxy.platform_onCreate(bundle, persistableBundle)
    }

    open fun onCreateContextMenu(
        contextMenu: ContextMenu?,
        view: View?,
        contextMenuInfo: ContextMenu.ContextMenuInfo?
    ) {
        activityProxy.platform_onCreateContextMenu(
            contextMenu,
            view,
            contextMenuInfo
        )
    }

    open fun onCreateDescription(): CharSequence? {
        return activityProxy.platform_onCreateDescription()
    }

    open fun onCreateNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        activityProxy.platform_onCreateNavigateUpTaskStack(taskStackBuilder)
    }

    open fun onCreateOptionsMenu(menu: Menu): Boolean {
        return activityProxy.platform_onCreateOptionsMenu(menu)
    }

    open fun onCreatePanelMenu(v: Int, menu: Menu): Boolean {
        return activityProxy.platform_onCreatePanelMenu(v, menu)
    }

    open fun onCreatePanelView(v: Int): View? {
        return activityProxy.platform_onCreatePanelView(v)
    }

    @Deprecated
    open fun onCreateThumbnail(bitmap: Bitmap?, canvas: Canvas?): Boolean {
        return activityProxy.platform_onCreateThumbnail(bitmap, canvas)
    }

    open fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        return activityProxy.platform_onCreateView(parent, name, context, attrs)
    }

    open fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return activityProxy.platform_onCreateView(name, context, attrs)
    }

    protected open fun onDestroy() {
        activityProxy.platform_onDestroy()
    }

    open fun onDetachedFromWindow() {
        activityProxy.platform_onDetachedFromWindow()
    }

    open fun onGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_onGenericMotionEvent(motionEvent)
    }

    open fun onKeyDown(v: Int, keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_onKeyDown(v, keyEvent)
    }

    open fun onKeyLongPress(v: Int, keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_onKeyLongPress(v, keyEvent)
    }

    open fun onKeyMultiple(v: Int, v1: Int, keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_onKeyMultiple(v, v1, keyEvent)
    }

    open fun onKeyShortcut(v: Int, keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_onKeyShortcut(v, keyEvent)
    }

    open fun onKeyUp(v: Int, keyEvent: KeyEvent?): Boolean {
        return activityProxy.platform_onKeyUp(v, keyEvent)
    }

    open fun onLocalVoiceInteractionStarted() {
        activityProxy.platform_onLocalVoiceInteractionStarted()
    }

    open fun onLocalVoiceInteractionStopped() {
        activityProxy.platform_onLocalVoiceInteractionStopped()
    }

    open fun onMenuItemSelected(featureId: Int, menuItem: MenuItem): Boolean {
        return activityProxy.platform_onMenuItemSelected(featureId, menuItem)
    }

    open fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return activityProxy.platform_onMenuOpened(featureId, menu)
    }

    open fun onNavigateUp(): Boolean {
        return activityProxy.platform_onNavigateUp()
    }

    @Deprecated
    open fun onNavigateUpFromChild(activity: Activity?): Boolean {
        return activityProxy.platform_onNavigateUpFromChild(activity)
    }

    protected open fun onNewIntent(intent: Intent?) {
        activityProxy.platform_onNewIntent(intent)
    }

    open fun onNewIntent(intent: Intent, componentCaller: ComponentCaller) {
        activityProxy.platform_onNewIntent(intent, componentCaller)
    }

    open fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return activityProxy.platform_onOptionsItemSelected(menuItem)
    }

    open fun onOptionsMenuClosed(menu: Menu) {
        activityProxy.platform_onOptionsMenuClosed(menu)
    }

    open fun onPanelClosed(featureId: Int, menu: Menu) {
        activityProxy.platform_onPanelClosed(featureId, menu)
    }

    protected open fun onPause() {
        activityProxy.platform_onPause()
    }

    private fun onPointerCaptureChanged(hasCapture: Boolean) {
        activityProxy.platform_onPointerCaptureChanged(hasCapture)
    }

    protected open fun onPostCreate(bundle: Bundle?) {
        activityProxy.platform_onPostCreate(bundle)
    }

    open fun onPostCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        activityProxy.platform_onPostCreate(bundle, persistableBundle)
    }

    protected open fun onPostResume() {
        activityProxy.platform_onPostResume()
    }

    open fun onPrepareNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        activityProxy.platform_onPrepareNavigateUpTaskStack(taskStackBuilder)
    }

    open fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return activityProxy.platform_onPrepareOptionsMenu(menu)
    }

    open fun onPreparePanel(v: Int, view: View?, menu: Menu): Boolean {
        return activityProxy.platform_onPreparePanel(v, view, menu)
    }

    open fun onProvideReferrer(): Uri? {
        return activityProxy.platform_onProvideReferrer()
    }

    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        activityProxy.platform_onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, extra: Int) {
        activityProxy.platform_onRequestPermissionsResult(requestCode, permissions, grantResults, extra)
    }

    protected open fun onRestart() {
        activityProxy.platform_onRestart()
    }

    protected open fun onRestoreInstanceState(bundle: Bundle) {
        activityProxy.platform_onRestoreInstanceState(bundle)
    }

    open fun onRestoreInstanceState(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        activityProxy.platform_onRestoreInstanceState(bundle, persistableBundle)
    }

    protected open fun onResume() {
        activityProxy.platform_onResume()
    }

    protected open fun onSaveInstanceState(bundle: Bundle) {
        activityProxy.platform_onSaveInstanceState(bundle)
    }

    open fun onSaveInstanceState(bundle: Bundle, persistableBundle: PersistableBundle) {
        activityProxy.platform_onSaveInstanceState(bundle, persistableBundle)
    }

    open fun onSearchRequested(): Boolean {
        return activityProxy.platform_onSearchRequested()
    }

    open fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return activityProxy.platform_onSearchRequested(searchEvent)
    }

    protected open fun onStart() {
        activityProxy.platform_onStart()
    }

    @Deprecated
    open fun onStateNotSaved() {
        activityProxy.platform_onStateNotSaved()
    }

    protected open fun onStop() {
        activityProxy.platform_onStop()
    }

    protected open fun onTitleChanged(charSequence: CharSequence?, v: Int) {
        activityProxy.platform_onTitleChanged(charSequence, v)
    }

    open fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_onTouchEvent(motionEvent)
    }

    open fun onTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return activityProxy.platform_onTrackballEvent(motionEvent)
    }

    open fun onUserInteraction() {
        activityProxy.platform_onUserInteraction()
    }

    protected open fun onUserLeaveHint() {
        activityProxy.platform_onUserLeaveHint()
    }

    @Deprecated
    open fun onVisibleBehindCanceled() {
        activityProxy.platform_onVisibleBehindCanceled()
    }

    open fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        activityProxy.platform_onWindowAttributesChanged(params)
    }

    open fun onWindowFocusChanged(z: Boolean) {
        activityProxy.platform_onWindowFocusChanged(z)
    }

    open fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return activityProxy.platform_onWindowStartingActionMode(callback)
    }

    open fun onWindowStartingActionMode(callback: ActionMode.Callback?, v: Int): ActionMode? {
        return activityProxy.platform_onWindowStartingActionMode(callback, v)
    }

    open fun openContextMenu(view: View?) {
        activityProxy.platform_openContextMenu(view)
    }

    open fun openOptionsMenu() {
        activityProxy.platform_openOptionsMenu()
    }

    open fun overrideActivityTransition(v: Int, v1: Int, v2: Int) {
        activityProxy.platform_overrideActivityTransition(v, v1, v2)
    }

    open fun overrideActivityTransition(v: Int, v1: Int, v2: Int, v3: Int) {
        activityProxy.platform_overrideActivityTransition(v, v1, v2, v3)
    }

    @Deprecated
    open fun overridePendingTransition(v: Int, v1: Int) {
        activityProxy.platform_overridePendingTransition(v, v1)
    }

    @Deprecated
    open fun overridePendingTransition(v: Int, v1: Int, v2: Int) {
        activityProxy.platform_overridePendingTransition(v, v1, v2)
    }

    open fun postponeEnterTransition() {
        activityProxy.platform_postponeEnterTransition()
    }

    open fun public_addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        this.addContentView(view, params)
    }

    open fun public_clearOverrideActivityTransition(v: Int) {
        this.clearOverrideActivityTransition(v)
    }

    open fun public_closeContextMenu() {
        this.closeContextMenu()
    }

    open fun public_closeOptionsMenu() {
        this.closeOptionsMenu()
    }

    open fun public_convertFromTranslucent() {
        this.convertFromTranslucent()
    }

    open fun public_convertToTranslucent(listener: Any?, activityOptions: ActivityOptions?): Boolean {
        return this.convertToTranslucent(listener, activityOptions)
    }

    open fun public_createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent {
        return this.createPendingResult(requestCode, data, flags)
    }

    open fun public_dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return this.dispatchGenericMotionEvent(motionEvent)
    }

    open fun public_dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return this.dispatchKeyEvent(keyEvent)
    }

    open fun public_dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return this.dispatchKeyShortcutEvent(keyEvent)
    }

    open fun public_dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return this.dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }

    open fun public_dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        return this.dispatchTouchEvent(motionEvent)
    }

    open fun public_dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return this.dispatchTrackballEvent(motionEvent)
    }

    open fun public_finish() {
        this.finish()
    }

    open fun public_finishActivity(v: Int) {
        this.finishActivity(v)
    }

    @Deprecated
    open fun public_finishActivityFromChild(activity: Activity, requestCode: Int) {
        this.finishActivityFromChild(activity, requestCode)
    }

    open fun public_finishAffinity() {
        this.finishAffinity()
    }

    open fun public_finishAfterTransition() {
        this.finishAfterTransition()
    }

    open fun public_finishAndRemoveTask() {
        this.finishAndRemoveTask()
    }

    @Deprecated
    open fun public_finishFromChild(activity: Activity?) {
        this.finishFromChild(activity)
    }

    open fun public_getActionBar(): ActionBar? {
        return getActionBar()
    }

    open fun public_getApplication(): Application? {
        return getApplication()
    }

    open fun public_getCaller(): ComponentCaller? {
        return getCaller()
    }

    open fun public_getCallingActivity(): ComponentName? {
        return getCallingActivity()
    }

    open fun public_getCallingPackage(): String? {
        return getCallingPackage()
    }

    open fun public_getChangingConfigurations(): Int {
        return getChangingConfigurations()
    }

    open fun public_getComponentName(): ComponentName? {
        return getComponentName()
    }

    open fun public_getContentScene(): Scene? {
        return getContentScene()
    }

    open fun public_getContentTransitionManager(): TransitionManager? {
        return getContentTransitionManager()
    }

    open fun public_getCurrentCaller(): ComponentCaller {
        return getCurrentCaller()
    }

    open fun public_getCurrentFocus(): View? {
        return getCurrentFocus()
    }

    @Deprecated
    open fun public_getFragmentManager(): FragmentManager? {
        return getFragmentManager()
    }

    open fun public_getInitialCaller(): ComponentCaller {
        return getInitialCaller()
    }

    open fun public_getIntent(): Intent? {
        return getIntent()
    }

    open fun public_getLastNonConfigurationInstance(): Any? {
        return getLastNonConfigurationInstance()
    }
    open fun public_getLaunchedFromPackage(): String? {
        return getLaunchedFromPackage()
    }

    open fun public_getLaunchedFromUid(): Int {
        return getLaunchedFromUid()
    }

    open fun public_getLayoutInflater(): LayoutInflater {
        return getLayoutInflater()
    }

    @Deprecated
    open fun public_getLoaderManager(): LoaderManager? {
        return getLoaderManager()
    }

    open fun public_getLocalClassName(): String {
        return getLocalClassName()
    }

    open fun public_getMaxNumPictureInPictureActions(): Int {
        return getMaxNumPictureInPictureActions()
    }

    open fun public_getMediaController(): MediaController? {
        return getMediaController()
    }

    open fun public_getMenuInflater(): MenuInflater? {
        return getMenuInflater()
    }

    open fun public_getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return getOnBackInvokedDispatcher()
    }

    @Deprecated
    open fun public_getParent(): Activity? {
        return getParent()
    }

    open fun public_getParentActivityIntent(): Intent? {
        return getParentActivityIntent()
    }

    open fun public_getPreferences(v: Int): SharedPreferences? {
        return getPreferences(v)
    }

    open fun public_getReferrer(): Uri? {
        return getReferrer()
    }

    open fun public_getRequestedOrientation(): Int {
        return getRequestedOrientation()
    }

    open fun public_getSearchEvent(): SearchEvent? {
        return getSearchEvent()
    }

    open fun public_getSplashScreen(): SplashScreen? {
        return getSplashScreen()
    }

    open fun public_getTaskId(): Int {
        return getTaskId()
    }

    open fun public_getTitle(): CharSequence? {
        return getTitle()
    }

    @Deprecated
    open fun public_getTitleColor(): Int {
        return getTitleColor()
    }

    open fun public_getVoiceInteractor(): VoiceInteractor? {
        return getVoiceInteractor()
    }

    open fun public_getVolumeControlStream(): Int {
        return getVolumeControlStream()
    }

    open fun public_getWindow(): Window? {
        return getWindow()
    }

    open fun public_getWindowManager(): WindowManager? {
        return getWindowManager()
    }

    open fun public_hasWindowFocus(): Boolean {
        return this.hasWindowFocus()
    }

    open fun public_invalidateOptionsMenu() {
        this.invalidateOptionsMenu()
    }

    open fun public_isActivityTransitionRunning(): Boolean {
        return isActivityTransitionRunning()
    }

    @Deprecated
    open fun public_isBackgroundVisibleBehind(): Boolean {
        return isBackgroundVisibleBehind()
    }

    open fun public_isChangingConfigurations(): Boolean {
        return this.isChangingConfigurations()
    }
    @Deprecated
    open fun public_isChild(): Boolean {
        return isChild()
    }

    open fun public_isDestroyed(): Boolean {
        return isDestroyed()
    }

    open fun public_isFinishing(): Boolean {
        return isFinishing()
    }

    open fun public_isImmersive(): Boolean {
        return isImmersive()
    }

    open fun public_isInMultiWindowMode(): Boolean {
        return isInMultiWindowMode()
    }

    open fun public_isInPictureInPictureMode(): Boolean {
        return isInPictureInPictureMode()
    }

    open fun public_isLaunchedFromBubble(): Boolean {
        return isLaunchedFromBubble()
    }

    open fun public_isLocalVoiceInteractionSupported(): Boolean {
        return isLocalVoiceInteractionSupported()
    }

    open fun public_isTaskRoot(): Boolean {
        return isTaskRoot()
    }

    open fun public_isVoiceInteraction(): Boolean {
        return isVoiceInteraction()
    }

    open fun public_isVoiceInteractionRoot(): Boolean {
        return isVoiceInteractionRoot()
    }

    @Deprecated
    open fun public_managedQuery(
        uri: Uri?,
        arr_s: Array<String?>?,
        s: String?,
        arr_s1: Array<String?>?,
        s1: String?
    ): Cursor? {
        return this.managedQuery(uri, arr_s, s, arr_s1, s1)
    }

    open fun public_moveTaskToBack(z: Boolean): Boolean {
        return this.moveTaskToBack(z)
    }

    open fun public_navigateUpTo(intent: Intent?): Boolean {
        return this.navigateUpTo(intent)
    }

    @Deprecated
    open fun public_navigateUpToFromChild(activity: Activity?, intent: Intent?): Boolean {
        return this.navigateUpToFromChild(activity, intent)
    }

    open fun public_onActionModeFinished(actionMode: ActionMode?) {
        this.onActionModeFinished(actionMode)
    }

    open fun public_onActionModeStarted(actionMode: ActionMode?) {
        this.onActionModeStarted(actionMode)
    }

    open fun public_onActivityReenter(v: Int, intent: Intent?) {
        this.onActivityReenter(v, intent)
    }

    open fun public_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        this.onActivityResult(requestCode, resultCode, data)
    }

    open fun public_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        this.onActivityResult(requestCode, resultCode, data, caller)
    }

    @Deprecated
    open fun public_onAttachFragment(fragment: Fragment?) {
        this.onAttachFragment(fragment)
    }

    open fun public_onAttachedToWindow() {
        this.onAttachedToWindow()
    }

    @Deprecated
    open fun public_onBackPressed() {
        this.onBackPressed()
    }

    @Deprecated
    open fun public_onBackgroundVisibleBehindChanged(z: Boolean) {
        this.onBackgroundVisibleBehindChanged(z)
    }

    open fun public_onChildTitleChanged(activity: Activity?, charSequence: CharSequence?) {
        this.onChildTitleChanged(activity, charSequence)
    }

    open fun public_onConfigurationChanged(configuration: Configuration) {
        this.onConfigurationChanged(configuration)
    }

    open fun public_onContentChanged() {
        this.onContentChanged()
    }

    open fun public_onContextItemSelected(menuItem: MenuItem): Boolean {
        return this.onContextItemSelected(menuItem)
    }

    open fun public_onContextMenuClosed(menu: Menu) {
        this.onContextMenuClosed(menu)
    }

    open fun public_onCreate(bundle: Bundle?) {
        this.onCreate(bundle)
    }

    open fun public_onCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        this.onCreate(bundle, persistableBundle)
    }

    open fun public_onCreateContextMenu(
        contextMenu: ContextMenu?,
        view: View?,
        contextMenuInfo: ContextMenu.ContextMenuInfo?
    ) {
        this.onCreateContextMenu(contextMenu, view, contextMenuInfo)
    }

    open fun public_onCreateDescription(): CharSequence? {
        return this.onCreateDescription()
    }

    open fun public_onCreateNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        this.onCreateNavigateUpTaskStack(taskStackBuilder)
    }

    open fun public_onCreateOptionsMenu(menu: Menu): Boolean {
        return this.onCreateOptionsMenu(menu)
    }

    open fun public_onCreatePanelMenu(v: Int, menu: Menu): Boolean {
        return this.onCreatePanelMenu(v, menu)
    }

    open fun public_onCreatePanelView(v: Int): View? {
        return this.onCreatePanelView(v)
    }

    @Deprecated
    open fun public_onCreateThumbnail(bitmap: Bitmap?, canvas: Canvas?): Boolean {
        return this.onCreateThumbnail(bitmap, canvas)
    }

    open fun public_onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        return this.onCreateView(parent, name, context, attrs)
    }

    open fun public_onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return this.onCreateView(name, context, attrs)
    }

    open fun public_onDestroy() {
        this.onDestroy()
    }

    open fun public_onDetachedFromWindow() {
        this.onDetachedFromWindow()
    }

    open fun public_onGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return this.onGenericMotionEvent(motionEvent)
    }

    open fun public_onKeyDown(v: Int, keyEvent: KeyEvent?): Boolean {
        return this.onKeyDown(v, keyEvent)
    }

    open fun public_onKeyLongPress(v: Int, keyEvent: KeyEvent?): Boolean {
        return this.onKeyLongPress(v, keyEvent)
    }

    open fun public_onKeyMultiple(v: Int, v1: Int, keyEvent: KeyEvent?): Boolean {
        return this.onKeyMultiple(v, v1, keyEvent)
    }

    open fun public_onKeyShortcut(v: Int, keyEvent: KeyEvent?): Boolean {
        return this.onKeyShortcut(v, keyEvent)
    }

    open fun public_onKeyUp(v: Int, keyEvent: KeyEvent?): Boolean {
        return this.onKeyUp(v, keyEvent)
    }

    open fun public_onLocalVoiceInteractionStarted() {
        this.onLocalVoiceInteractionStarted()
    }

    open fun public_onLocalVoiceInteractionStopped() {
        this.onLocalVoiceInteractionStopped()
    }

    open fun public_onMenuItemSelected(v: Int, menuItem: MenuItem): Boolean {
        return this.onMenuItemSelected(v, menuItem)
    }

    open fun public_onMenuOpened(v: Int, menu: Menu): Boolean {
        return this.onMenuOpened(v, menu)
    }

    open fun public_onNavigateUp(): Boolean {
        return this.onNavigateUp()
    }

    @Deprecated
    open fun public_onNavigateUpFromChild(activity: Activity?): Boolean {
        return this.onNavigateUpFromChild(activity)
    }

    open fun public_onNewIntent(intent: Intent?) {
        this.onNewIntent(intent)
    }

    open fun public_onNewIntent(intent: Intent, componentCaller: ComponentCaller) {
        this.onNewIntent(intent, componentCaller)
    }

    open fun public_onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return this.onOptionsItemSelected(menuItem)
    }

    open fun public_onOptionsMenuClosed(menu: Menu) {
        this.onOptionsMenuClosed(menu)
    }

    open fun public_onPanelClosed(v: Int, menu: Menu) {
        this.onPanelClosed(v, menu)
    }

    open fun public_onPause() {
        this.onPause()
    }

    open fun public_onPointerCaptureChanged(z: Boolean) {
        this.onPointerCaptureChanged(z)
    }

    open fun public_onPostCreate(bundle: Bundle?) {
        this.onPostCreate(bundle)
    }

    open fun public_onPostCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        this.onPostCreate(bundle, persistableBundle)
    }

    open fun public_onPostResume() {
        this.onPostResume()
    }

    open fun public_onPrepareNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        this.onPrepareNavigateUpTaskStack(taskStackBuilder)
    }

    open fun public_onPrepareOptionsMenu(menu: Menu): Boolean {
        return this.onPrepareOptionsMenu(menu)
    }

    open fun public_onPreparePanel(v: Int, view: View?, menu: Menu): Boolean {
        return this.onPreparePanel(v, view, menu)
    }

    open fun public_onProvideReferrer(): Uri? {
        return this.onProvideReferrer()
    }

    open fun public_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        this.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    open fun public_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, extra: Int) {
        this.onRequestPermissionsResult(requestCode, permissions, grantResults, extra)
    }

    open fun public_onRestart() {
        this.onRestart()
    }

    open fun public_onRestoreInstanceState(bundle: Bundle) {
        this.onRestoreInstanceState(bundle)
    }

    open fun public_onRestoreInstanceState(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        this.onRestoreInstanceState(bundle, persistableBundle)
    }

    open fun public_onResume() {
        this.onResume()
    }

    open fun public_onSaveInstanceState(bundle: Bundle) {
        this.onSaveInstanceState(bundle)
    }

    open fun public_onSaveInstanceState(bundle: Bundle, persistableBundle: PersistableBundle) {
        this.onSaveInstanceState(bundle, persistableBundle)
    }

    open fun public_onSearchRequested(): Boolean {
        return this.onSearchRequested()
    }

    open fun public_onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return this.onSearchRequested(searchEvent)
    }

    open fun public_onStart() {
        this.onStart()
    }

    @Deprecated
    open fun public_onStateNotSaved() {
        this.onStateNotSaved()
    }

    open fun public_onStop() {
        this.onStop()
    }

    open fun public_onTitleChanged(charSequence: CharSequence?, v: Int) {
        this.onTitleChanged(charSequence, v)
    }

    open fun public_onTouchEvent(motionEvent: MotionEvent?): Boolean {
        return this.onTouchEvent(motionEvent)
    }

    open fun public_onTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return this.onTrackballEvent(motionEvent)
    }

    open fun public_onUserInteraction() {
        this.onUserInteraction()
    }

    open fun public_onUserLeaveHint() {
        this.onUserLeaveHint()
    }

    @Deprecated
    open fun public_onVisibleBehindCanceled() {
        this.onVisibleBehindCanceled()
    }

    open fun public_onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        this.onWindowAttributesChanged(params)
    }

    open fun public_onWindowFocusChanged(z: Boolean) {
        this.onWindowFocusChanged(z)
    }

    open fun public_onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return this.onWindowStartingActionMode(callback)
    }

    open fun public_onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        v: Int
    ): ActionMode? {
        return this.onWindowStartingActionMode(callback, v)
    }

    open fun public_openContextMenu(view: View?) {
        this.openContextMenu(view)
    }

    open fun public_openOptionsMenu() {
        this.openOptionsMenu()
    }

    open fun public_overrideActivityTransition(v: Int, v1: Int, v2: Int) {
        this.overrideActivityTransition(v, v1, v2)
    }

    open fun public_overrideActivityTransition(v: Int, v1: Int, v2: Int, v3: Int) {
        this.overrideActivityTransition(v, v1, v2, v3)
    }

    @Deprecated
    open fun public_overridePendingTransition(v: Int, v1: Int) {
        this.overridePendingTransition(v, v1)
    }

    @Deprecated
    open fun public_overridePendingTransition(v: Int, v1: Int, v2: Int) {
        this.overridePendingTransition(v, v1, v2)
    }

    open fun public_postponeEnterTransition() {
        this.postponeEnterTransition()
    }

    open fun public_recreate() {
        this.recreate()
    }

    open fun public_registerActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks) {
        this.registerActivityLifecycleCallbacks(callback)
    }

    open fun public_registerForContextMenu(view: View?) {
        this.registerForContextMenu(view)
    }

    open fun public_releaseInstance(): Boolean {
        return this.releaseInstance()
    }

    open fun public_reportFullyDrawn() {
        this.reportFullyDrawn()
    }

    open fun public_requestDragAndDropPermissions(dragEvent: DragEvent?): DragAndDropPermissions? {
        return this.requestDragAndDropPermissions(dragEvent)
    }

    open fun public_requestFullscreenMode(request: Int, outcomeReceiver: OutcomeReceiver<Void, Throwable>?) {
        this.requestFullscreenMode(request, outcomeReceiver)
    }

    open fun public_requestPermissions(permissions: Array<String?>, requestCode: Int) {
        this.requestPermissions(permissions, requestCode)
    }

    open fun public_requestPermissions(permissions: Array<String?>, requestCode: Int, userId: Int) {
        this.requestPermissions(permissions, requestCode, userId)
    }

    @Deprecated
    open fun public_requestVisibleBehind(z: Boolean): Boolean {
        return this.requestVisibleBehind(z)
    }

    open fun public_requestWindowFeature(v: Int): Boolean {
        return this.requestWindowFeature(v)
    }

    open fun public_requireViewById(v: Int): View? {
        return this.requireViewById(v)
    }

    open fun public_runOnUiThread(runnable: Runnable?) {
        this.runOnUiThread(runnable)
    }

    open fun public_setActionBar(toolbar: Toolbar?) {
        this.setActionBar(toolbar)
    }

    open fun public_setAllowCrossUidActivitySwitchFromBelow(z: Boolean) {
        this.setAllowCrossUidActivitySwitchFromBelow(z)
    }

    open fun public_setContentTransitionManager(transitionManager: TransitionManager?) {
        this.setContentTransitionManager(transitionManager)
    }

    open fun public_setContentView(v: Int) {
        this.setContentView(v)
    }

    open fun public_setContentView(view: View?) {
        this.setContentView(view)
    }

    open fun public_setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        this.setContentView(view, params)
    }

    open fun public_setDefaultKeyMode(v: Int) {
        this.setDefaultKeyMode(v)
    }

    open fun public_setEnterSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        this.setEnterSharedElementCallback(sharedElementCallback)
    }

    open fun public_setExitSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        this.setExitSharedElementCallback(sharedElementCallback)
    }

    open fun public_setFeatureDrawable(v: Int, drawable: Drawable?) {
        this.setFeatureDrawable(v, drawable)
    }

    open fun public_setFeatureDrawableAlpha(v: Int, v1: Int) {
        this.setFeatureDrawableAlpha(v, v1)
    }

    open fun public_setFeatureDrawableResource(v: Int, v1: Int) {
        this.setFeatureDrawableResource(v, v1)
    }

    open fun public_setFeatureDrawableUri(v: Int, uri: Uri?) {
        this.setFeatureDrawableUri(v, uri)
    }

    open fun public_setFinishOnTouchOutside(z: Boolean) {
        this.setFinishOnTouchOutside(z)
    }

    open fun public_setImmersive(z: Boolean) {
        setImmersive(z)
    }

    open fun public_setInheritShowWhenLocked(z: Boolean) {
        this.setInheritShowWhenLocked(z)
    }

    open fun public_setIntent(intent: Intent?) {
        this.setIntent(intent)
    }

    open fun public_setIntent(intent: Intent?, componentCaller: ComponentCaller?) {
        this.setIntent(intent, componentCaller)
    }

    open fun public_setLocusContext(locusId: LocusId?, bundle: Bundle?) {
        this.setLocusContext(locusId, bundle)
    }

    open fun public_setMediaController(mediaController: MediaController?) {
        setMediaController(mediaController)
    }

    open fun public_setPictureInPictureParams(pictureInPictureParams: PictureInPictureParams) {
        this.setPictureInPictureParams(pictureInPictureParams)
    }

    @Deprecated
    open fun public_setProgress(v: Int) {
        this.setProgress(v)
    }

    @Deprecated
    open fun public_setProgressBarIndeterminate(z: Boolean) {
        this.setProgressBarIndeterminate(z)
    }

    @Deprecated
    open fun public_setProgressBarIndeterminateVisibility(z: Boolean) {
        this.setProgressBarIndeterminateVisibility(z)
    }

    @Deprecated
    open fun public_setProgressBarVisibility(z: Boolean) {
        this.setProgressBarVisibility(z)
    }

    open fun public_setRecentsScreenshotEnabled(z: Boolean) {
        this.setRecentsScreenshotEnabled(z)
    }

    open fun public_setRequestedOrientation(v: Int) {
        setRequestedOrientation(v)
    }

    open fun public_setResult(v: Int) {
        this.setResult(v)
    }

    open fun public_setResult(v: Int, intent: Intent?) {
        this.setResult(v, intent)
    }

    @Deprecated
    open fun public_setSecondaryProgress(v: Int) {
        this.setSecondaryProgress(v)
    }

    open fun public_setShouldDockBigOverlays(z: Boolean) {
        this.setShouldDockBigOverlays(z)
    }

    open fun public_setShowWhenLocked(z: Boolean) {
        this.setShowWhenLocked(z)
    }

    open fun public_setTaskDescription(taskDescription: ActivityManager.TaskDescription?) {
        this.setTaskDescription(taskDescription)
    }

    open fun public_setTitle(v: Int) {
        this.setTitle(v)
    }

    open fun public_setTitle(charSequence: CharSequence?) {
        this.setTitle(charSequence)
    }

    @Deprecated
    open fun public_setTitleColor(v: Int) {
        setTitleColor(v)
    }

    open fun public_setTranslucent(z: Boolean): Boolean {
        return this.setTranslucent(z)
    }

    open fun public_setTurnScreenOn(z: Boolean) {
        this.setTurnScreenOn(z)
    }

    open fun public_setVisible(z: Boolean) {
        this.setVisible(z)
    }

    open fun public_setVolumeControlStream(v: Int) {
        setVolumeControlStream(v)
    }

    open fun public_setVrModeEnabled(enabled: Boolean, componentName: ComponentName) {
        this.setVrModeEnabled(enabled, componentName)
    }

    open fun public_shouldDockBigOverlays(): Boolean {
        return this.shouldDockBigOverlays()
    }

    open fun public_shouldShowRequestPermissionRationale(permission: String): Boolean {
        return this.shouldShowRequestPermissionRationale(permission)
    }

    open fun public_shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean {
        return this.shouldShowRequestPermissionRationale(permission, userId)
    }

    open fun public_shouldUpRecreateTask(intent: Intent?): Boolean {
        return this.shouldUpRecreateTask(intent)
    }

    open fun public_showAssist(bundle: Bundle?): Boolean {
        return this.showAssist(bundle)
    }

    open fun public_showLockTaskEscapeMessage() {
        this.showLockTaskEscapeMessage()
    }

    open fun public_startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return this.startActionMode(callback)
    }

    open fun public_startActionMode(callback: ActionMode.Callback?, v: Int): ActionMode? {
        return this.startActionMode(callback, v)
    }

    open fun public_startActivities(arr_intent: Array<Intent?>) {
        this.startActivities(arr_intent)
    }

    open fun public_startActivities(arr_intent: Array<Intent?>, bundle: Bundle?) {
        this.startActivities(arr_intent, bundle)
    }

    open fun public_startActivity(intent: Intent?) {
        this.startActivity(intent)
    }

    open fun public_startActivity(intent: Intent?, bundle: Bundle?) {
        this.startActivity(intent, bundle)
    }

    open fun public_startActivityForResult(intent: Intent?, v: Int) {
        this.startActivityForResult(intent, v)
    }

    open fun public_startActivityForResult(intent: Intent?, v: Int, bundle: Bundle?) {
        this.startActivityForResult(intent, v, bundle)
    }

    open fun public_startActivityForResultAsUser(
        intent: Intent?,
        v: Int,
        bundle: Bundle?,
        userHandle: UserHandle?
    ) {
        this.startActivityForResultAsUser(intent, v, bundle, userHandle)
    }

    open fun public_startActivityForResultAsUser(intent: Intent?, v: Int, userHandle: UserHandle?) {
        this.startActivityForResultAsUser(intent, v, userHandle)
    }

    open fun public_startActivityForResultAsUser(
        intent: Intent?,
        s: String?,
        v: Int,
        bundle: Bundle?,
        userHandle: UserHandle?
    ) {
        this.startActivityForResultAsUser(intent, s, v, bundle, userHandle)
    }

    @Deprecated
    open fun public_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int) {
        this.startActivityFromChild(child, intent, requestCode)
    }

    @Deprecated
    open fun public_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int, options: Bundle?) {
        this.startActivityFromChild(child, intent, requestCode, options)
    }

    @Deprecated
    open fun public_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int) {
        this.startActivityFromFragment(fragment, intent, requestCode)
    }

    @Deprecated
    open fun public_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int, options: Bundle?) {
        this.startActivityFromFragment(fragment, intent, requestCode, options)
    }

    open fun public_startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return this.startActivityIfNeeded(intent, requestCode)
    }

    open fun public_startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean {
        return this.startActivityIfNeeded(intent, requestCode, options)
    }

    open fun public_startIntentSender(
        intentSender: IntentSender,
        intent: Intent?,
        v: Int,
        v1: Int,
        v2: Int
    ) {
        this.startIntentSender(intentSender, intent, v, v1, v2)
    }

    open fun public_startIntentSender(
        intentSender: IntentSender,
        intent: Intent?,
        v: Int,
        v1: Int,
        v2: Int,
        bundle: Bundle?
    ) {
        this.startIntentSender(intentSender, intent, v, v1, v2, bundle)
    }

    open fun public_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        this.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    open fun public_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        this.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @Deprecated
    open fun public_startIntentSenderFromChild(
        activity: Activity?,
        intentSender: IntentSender?,
        v: Int,
        intent: Intent?,
        v1: Int,
        v2: Int,
        v3: Int
    ) {
        this.startIntentSenderFromChild(activity, intentSender, v, intent, v1, v2, v3)
    }

    @Deprecated
    open fun public_startIntentSenderFromChild(
        activity: Activity?,
        intentSender: IntentSender?,
        v: Int,
        intent: Intent?,
        v1: Int,
        v2: Int,
        v3: Int,
        bundle: Bundle?
    ) {
        this.startIntentSenderFromChild(activity, intentSender, v, intent, v1, v2, v3, bundle)
    }

    open fun public_startLocalVoiceInteraction(bundle: Bundle?) {
        this.startLocalVoiceInteraction(bundle)
    }

    open fun public_startLockTask() {
        this.startLockTask()
    }

    @Deprecated
    open fun public_startManagingCursor(cursor: Cursor?) {
        this.startManagingCursor(cursor)
    }

    open fun public_startNextMatchingActivity(intent: Intent): Boolean {
        return this.startNextMatchingActivity(intent)
    }

    open fun public_startNextMatchingActivity(intent: Intent, bundle: Bundle?): Boolean {
        return this.startNextMatchingActivity(intent, bundle)
    }

    open fun public_startPostponedEnterTransition() {
        this.startPostponedEnterTransition()
    }

    open fun public_startSearch(s: String?, z: Boolean, bundle: Bundle?, z1: Boolean) {
        this.startSearch(s, z, bundle, z1)
    }

    open fun public_stopLocalVoiceInteraction() {
        this.stopLocalVoiceInteraction()
    }

    open fun public_stopLockTask() {
        this.stopLockTask()
    }

    @Deprecated
    open fun public_stopManagingCursor(cursor: Cursor?) {
        this.stopManagingCursor(cursor)
    }

    open fun public_takeKeyEvents(z: Boolean) {
        this.takeKeyEvents(z)
    }

    open fun public_triggerSearch(s: String?, bundle: Bundle?) {
        this.triggerSearch(s, bundle)
    }

    open fun public_unregisterActivityLifecycleCallbacks(callbacks: Application.ActivityLifecycleCallbacks) {
        this.unregisterActivityLifecycleCallbacks(callbacks)
    }

    open fun public_unregisterForContextMenu(view: View?) {
        this.unregisterForContextMenu(view)
    }

    open fun recreate() {
        activityProxy.platform_recreate()
    }

    open fun registerActivityLifecycleCallbacks(callbacks: Application.ActivityLifecycleCallbacks) {
        activityProxy.platform_registerActivityLifecycleCallbacks(callbacks)
    }

    open fun registerForContextMenu(view: View?) {
        activityProxy.platform_registerForContextMenu(view)
    }

    open fun releaseInstance(): Boolean {
        return activityProxy.platform_releaseInstance()
    }

    open fun reportFullyDrawn() {
        activityProxy.platform_reportFullyDrawn()
    }

    open fun requestDragAndDropPermissions(dragEvent: DragEvent?): DragAndDropPermissions? {
        return activityProxy.platform_requestDragAndDropPermissions(dragEvent)
    }

    open fun requestFullscreenMode(request: Int, approvalCallback: OutcomeReceiver<Void, Throwable>?) {
        activityProxy.platform_requestFullscreenMode(request, approvalCallback)
    }

    open fun requestPermissions(permissions: Array<String?>, requestCode: Int) {
        activityProxy.platform_requestPermissions(permissions, requestCode)
    }

    open fun requestPermissions(permissions: Array<String?>, requestCode: Int, userId: Int) {
        activityProxy.platform_requestPermissions(permissions, requestCode, userId)
    }

    @Deprecated
    open fun requestVisibleBehind(z: Boolean): Boolean {
        return activityProxy.platform_requestVisibleBehind(z)
    }

    open fun requestWindowFeature(v: Int): Boolean {
        return activityProxy.platform_requestWindowFeature(v)
    }

    open fun requireViewById(v: Int): View? {
        return activityProxy.platform_requireViewById(v)
    }

    open fun runOnUiThread(runnable: Runnable?) {
        activityProxy.platform_runOnUiThread(runnable)
    }

    open fun setActionBar(toolbar: Toolbar?) {
        activityProxy.platform_setActionBar(toolbar)
    }

    open fun setAllowCrossUidActivitySwitchFromBelow(z: Boolean) {
        activityProxy.platform_setAllowCrossUidActivitySwitchFromBelow(z)
    }

    open fun setContentTransitionManager(transitionManager: TransitionManager?) {
        this.activityProxy.platform_setContentTransitionManager(transitionManager)
    }

    open fun setContentView(v: Int) {
        activityProxy.platform_setContentView(v)
    }

    open fun setContentView(view: View?) {
        activityProxy.platform_setContentView(view)
    }

    open fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        activityProxy.platform_setContentView(view, params)
    }

    open fun setDefaultKeyMode(v: Int) {
        activityProxy.platform_setDefaultKeyMode(v)
    }

    open fun setEnterSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        activityProxy.platform_setEnterSharedElementCallback(sharedElementCallback)
    }

    open fun setExitSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        activityProxy.platform_setExitSharedElementCallback(sharedElementCallback)
    }

    open fun setFeatureDrawable(v: Int, drawable: Drawable?) {
        activityProxy.platform_setFeatureDrawable(v, drawable)
    }

    open fun setFeatureDrawableAlpha(v: Int, v1: Int) {
        activityProxy.platform_setFeatureDrawableAlpha(v, v1)
    }

    open fun setFeatureDrawableResource(v: Int, v1: Int) {
        activityProxy.platform_setFeatureDrawableResource(v, v1)
    }

    open fun setFeatureDrawableUri(v: Int, uri: Uri?) {
        activityProxy.platform_setFeatureDrawableUri(v, uri)
    }

    open fun setFinishOnTouchOutside(z: Boolean) {
        activityProxy.platform_setFinishOnTouchOutside(z)
    }

    open fun setInheritShowWhenLocked(z: Boolean) {
        activityProxy.platform_setInheritShowWhenLocked(z)
    }

    open fun setIntent(intent: Intent?) {
        this.activityProxy.platform_setIntent(intent)
    }

    open fun setIntent(intent: Intent?, componentCaller: ComponentCaller?) {
        activityProxy.platform_setIntent(intent, componentCaller)
    }

    open fun setLocusContext(locusId: LocusId?, bundle: Bundle?) {
        activityProxy.platform_setLocusContext(locusId, bundle)
    }

    open fun setPictureInPictureParams(pictureInPictureParams: PictureInPictureParams) {
        activityProxy.platform_setPictureInPictureParams(pictureInPictureParams)
    }

    @Deprecated
    open fun setProgress(v: Int) {
        activityProxy.platform_setProgress(v)
    }

    @Deprecated
    open fun setProgressBarIndeterminate(z: Boolean) {
        activityProxy.platform_setProgressBarIndeterminate(z)
    }

    @Deprecated
    open fun setProgressBarIndeterminateVisibility(z: Boolean) {
        activityProxy.platform_setProgressBarIndeterminateVisibility(z)
    }

    @Deprecated
    open fun setProgressBarVisibility(z: Boolean) {
        activityProxy.platform_setProgressBarVisibility(z)
    }

    open fun setProxyCallbacks(activityProxy: IActivityProxy, context: Context?) {
        this.activityProxy = activityProxy
    }

    open fun setRecentsScreenshotEnabled(z: Boolean) {
        activityProxy.platform_setRecentsScreenshotEnabled(z)
    }

    open fun setResult(v: Int) {
        activityProxy.platform_setResult(v)
    }

    open fun setResult(v: Int, intent: Intent?) {
        activityProxy.platform_setResult(v, intent)
    }

    @Deprecated
    open fun setSecondaryProgress(v: Int) {
        activityProxy.platform_setSecondaryProgress(v)
    }

    open fun setShouldDockBigOverlays(z: Boolean) {
        activityProxy.platform_setShouldDockBigOverlays(z)
    }

    open fun setShowWhenLocked(z: Boolean) {
        activityProxy.platform_setShowWhenLocked(z)
    }

    open fun setTaskDescription(taskDescription: ActivityManager.TaskDescription?) {
        activityProxy.platform_setTaskDescription(taskDescription)
    }

    open fun setTitle(v: Int) {
        activityProxy.platform_setTitle(v)
    }

    open fun setTitle(charSequence: CharSequence?) {
        activityProxy.platform_setTitle(charSequence)
    }

    open fun setTranslucent(z: Boolean): Boolean {
        return activityProxy.platform_setTranslucent(z)
    }

    open fun setTurnScreenOn(z: Boolean) {
        activityProxy.platform_setTurnScreenOn(z)
    }

    open fun setVisible(z: Boolean) {
        activityProxy.platform_setVisible(z)
    }

    open fun setVrModeEnabled(enabled: Boolean, requestedComponent: ComponentName) {
        activityProxy.platform_setVrModeEnabled(enabled, requestedComponent)
    }

    open fun shouldDockBigOverlays(): Boolean {
        return activityProxy.platform_shouldDockBigOverlays()
    }

    open fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return activityProxy.platform_shouldShowRequestPermissionRationale(permission)
    }

    open fun shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean {
        return activityProxy.platform_shouldShowRequestPermissionRationale(permission, userId)
    }

    open fun shouldUpRecreateTask(intent: Intent?): Boolean {
        return activityProxy.platform_shouldUpRecreateTask(intent)
    }

    open fun showAssist(bundle: Bundle?): Boolean {
        return activityProxy.platform_showAssist(bundle)
    }

    open fun showLockTaskEscapeMessage() {
        activityProxy.platform_showLockTaskEscapeMessage()
    }

    open fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return activityProxy.platform_startActionMode(callback)
    }

    open fun startActionMode(callback: ActionMode.Callback?, v: Int): ActionMode? {
        return activityProxy.platform_startActionMode(callback, v)
    }
    override open fun startActivities(intents: Array<Intent?>) {
        activityProxy.platform_startActivities(intents)
    }
    override open fun startActivities(intents: Array<Intent?>, options: Bundle?) {
        activityProxy.platform_startActivities(intents, options)
    }
    override open fun startActivity(intent: Intent?) {
        activityProxy.platform_startActivity(intent)
    }
    override open fun startActivity(intent: Intent?, bundle: Bundle?) {
        activityProxy.platform_startActivity(intent, bundle)
    }

    open fun startActivityForResult(intent: Intent?, v: Int) {
        activityProxy.platform_startActivityForResult(intent, v)
    }

    open fun startActivityForResult(intent: Intent?, v: Int, bundle: Bundle?) {
        activityProxy.platform_startActivityForResult(intent, v, bundle)
    }

    open fun startActivityForResultAsUser(
        intent: Intent?,
        v: Int,
        bundle: Bundle?,
        userHandle: UserHandle?
    ) {
        activityProxy.platform_startActivityForResultAsUser(intent, v, bundle, userHandle)
    }

    open fun startActivityForResultAsUser(intent: Intent?, v: Int, userHandle: UserHandle?) {
        activityProxy.platform_startActivityForResultAsUser(intent, v, userHandle)
    }

    open fun startActivityForResultAsUser(
        intent: Intent?,
        s: String?,
        v: Int,
        bundle: Bundle?,
        userHandle: UserHandle?
    ) {
        activityProxy.platform_startActivityForResultAsUser(intent, s, v, bundle, userHandle)
    }

    @Deprecated
    open fun startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int) {
        activityProxy.platform_startActivityFromChild(child, intent, requestCode)
    }

    @Deprecated
    open fun startActivityFromChild(activity: Activity, intent: Intent?, v: Int, bundle: Bundle?) {
        activityProxy.platform_startActivityFromChild(activity, intent, v, bundle)
    }

    @Deprecated
    open fun startActivityFromFragment(fragment: Fragment, intent: Intent?, v: Int) {
        activityProxy.platform_startActivityFromFragment(fragment, intent, v)
    }

    @Deprecated
    open fun startActivityFromFragment(fragment: Fragment, intent: Intent?, v: Int, bundle: Bundle?) {
        activityProxy.platform_startActivityFromFragment(fragment, intent, v, bundle)
    }

    open fun startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return activityProxy.platform_startActivityIfNeeded(intent, requestCode)
    }

    open fun startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean {
        return activityProxy.platform_startActivityIfNeeded(intent, requestCode, options)
    }
    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        activityProxy.platform_startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        activityProxy.platform_startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    open fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        activityProxy.platform_startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    open fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        activityProxy.platform_startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @Deprecated
    open fun startIntentSenderFromChild(
        activity: Activity?,
        intentSender: IntentSender?,
        v: Int,
        intent: Intent?,
        v1: Int,
        v2: Int,
        v3: Int
    ) {
        activityProxy.platform_startIntentSenderFromChild(
            activity,
            intentSender,
            v,
            intent,
            v1,
            v2,
            v3
        )
    }

    @Deprecated
    open fun startIntentSenderFromChild(
        activity: Activity?,
        intentSender: IntentSender?,
        v: Int,
        intent: Intent?,
        v1: Int,
        v2: Int,
        v3: Int,
        bundle: Bundle?
    ) {
        activityProxy.platform_startIntentSenderFromChild(
            activity,
            intentSender,
            v,
            intent,
            v1,
            v2,
            v3,
            bundle
        )
    }

    open fun startLocalVoiceInteraction(bundle: Bundle?) {
        activityProxy.platform_startLocalVoiceInteraction(bundle)
    }

    open fun startLockTask() {
        activityProxy.platform_startLockTask()
    }

    @Deprecated
    open fun startManagingCursor(cursor: Cursor?) {
        activityProxy.platform_startManagingCursor(cursor)
    }

    open fun startNextMatchingActivity(intent: Intent): Boolean {
        return activityProxy.platform_startNextMatchingActivity(intent)
    }

    open fun startNextMatchingActivity(intent: Intent, bundle: Bundle?): Boolean {
        return activityProxy.platform_startNextMatchingActivity(intent, bundle)
    }

    open fun startPostponedEnterTransition() {
        activityProxy.platform_startPostponedEnterTransition()
    }

    open fun startSearch(s: String?, z: Boolean, bundle: Bundle?, z1: Boolean) {
        activityProxy.platform_startSearch(s, z, bundle, z1)
    }

    open fun stopLocalVoiceInteraction() {
        activityProxy.platform_stopLocalVoiceInteraction()
    }

    open fun stopLockTask() {
        activityProxy.platform_stopLockTask()
    }

    @Deprecated
    open fun stopManagingCursor(cursor: Cursor?) {
        activityProxy.platform_stopManagingCursor(cursor)
    }

    open fun takeKeyEvents(z: Boolean) {
        activityProxy.platform_takeKeyEvents(z)
    }

    open fun triggerSearch(s: String?, bundle: Bundle?) {
        activityProxy.platform_triggerSearch(s, bundle)
    }

    open fun unregisterActivityLifecycleCallbacks(callbacks: Application.ActivityLifecycleCallbacks) {
        activityProxy.platform_unregisterActivityLifecycleCallbacks(callbacks)
    }

    open fun unregisterForContextMenu(view: View?) {
        activityProxy.platform_unregisterForContextMenu(view)
    }
}
