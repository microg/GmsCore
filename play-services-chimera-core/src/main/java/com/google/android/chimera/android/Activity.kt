/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.android

import android.app.ActionBar
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
import androidx.annotation.Keep
import com.google.android.chimera.component.ChimeraProxyCallback
import com.google.android.chimera.InstanceProvider
import com.google.android.chimera.annotation.ChimeraApiVersion

const val DEFAULT_KEYS_DIALER = 1
const val DEFAULT_KEYS_DISABLE = 0
const val DEFAULT_KEYS_SEARCH_GLOBAL = 4
const val DEFAULT_KEYS_SEARCH_LOCAL = 3
const val DEFAULT_KEYS_SHORTCUT = 2
const val RESULT_CANCELED = 0
const val RESULT_FIRST_USER = 1
const val RESULT_OK = -1

@Keep
abstract class Activity protected constructor(): ActivityProxyWrapper(null, null), Window.Callback, ChimeraProxyCallback, InstanceProvider {
    companion object {
        @JvmStatic
        fun fromProxy(activity: android.app.Activity): Activity {
            return (activity as IChimeraActivityProxy).getChimeraActivity()
        }
    }

    private var chimeraActivityProxy: IChimeraActivityProxy? = null

    @ChimeraApiVersion(added = 0L)
    override fun addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.addContentView(view, params)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun clearOverrideActivityTransition(v: Int) {
        super.clearOverrideActivityTransition(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun closeContextMenu() {
        super.closeContextMenu()
    }

    @ChimeraApiVersion(added = 0L)
    override fun closeOptionsMenu() {
        super.closeOptionsMenu()
    }

    @ChimeraApiVersion(added = 0L)
    override fun convertFromTranslucent() {
        super.convertFromTranslucent()
    }

    @ChimeraApiVersion(added = 0L)
    override fun convertToTranslucent(listener: Any?, activityOptions: ActivityOptions?): Boolean {
        return super.convertToTranslucent(listener, activityOptions)
    }

    @ChimeraApiVersion(added = 0L)
    override fun createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent {
        return super.createPendingResult(requestCode, data, flags)
    }

    @ChimeraApiVersion(added = 0L)
    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return super.dispatchGenericMotionEvent(motionEvent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(keyEvent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return super.dispatchKeyShortcutEvent(keyEvent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return super.dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(motionEvent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return super.dispatchTrackballEvent(motionEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun <T : View?> findViewById(id: Int): T? {
        return super.findViewById(id)
    }

    @ChimeraApiVersion(added = 0L)
    override fun finish() {
        super.finish()
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishActivity(v: Int) {
        super.finishActivity(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishActivityFromChild(activity: android.app.Activity, requestCode: Int) {
        super.finishActivityFromChild(activity, requestCode)
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishAffinity() {
        super.finishAffinity()
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishAfterTransition() {
        super.finishAfterTransition()
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishAndRemoveTask() {
        super.finishAndRemoveTask()
    }

    @ChimeraApiVersion(added = 0L)
    override fun finishFromChild(activity: android.app.Activity?) {
        super.finishFromChild(activity)
    }

    @ChimeraApiVersion(added = 0x77L)
    override fun getActionBar(): ActionBar? {
        return super.getActionBar()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getApplication(): Application? {
        return super.getApplication()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun getCaller(): ComponentCaller? {
        return super.getCaller()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getCallingActivity(): ComponentName? {
        return super.getCallingActivity()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getCallingPackage(): String? {
        return super.getCallingPackage()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getChangingConfigurations(): Int {
        return super.getChangingConfigurations()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getChimeraImpl(): Any {
        return this
    }

    @ChimeraApiVersion(added = 0L)
    override fun getComponentName(): ComponentName? {
        return super.getComponentName()
    }

    @ChimeraApiVersion(added = 0L)
    fun getContainerActivity(): android.app.Activity {
        return this.chimeraActivityProxy as android.app.Activity
    }

    @ChimeraApiVersion(added = 0L)
    override fun getContentScene(): Scene? {
        return super.getContentScene()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getContentTransitionManager(): TransitionManager? {
        return super.getContentTransitionManager()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun getCurrentCaller(): ComponentCaller {
        return super.getCurrentCaller()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getCurrentFocus(): View? {
        return super.getCurrentFocus()
    }
    override fun getFragmentManager(): FragmentManager? {
        return super.getFragmentManager()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun getInitialCaller(): ComponentCaller {
        return super.getInitialCaller()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getIntent(): Intent? {
        return super.getIntent()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getLastNonConfigurationInstance(): Any? {
        return super.getLastNonConfigurationInstance()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun getLaunchedFromPackage(): String? {
        return super.getLaunchedFromPackage()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun getLaunchedFromUid(): Int {
        return super.getLaunchedFromUid()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getLayoutInflater(): LayoutInflater {
        return super.getLayoutInflater()
    }
    @ChimeraApiVersion(added = 0L)
    override fun getLoaderManager(): LoaderManager? {
        return super.getLoaderManager()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getLocalClassName(): String {
        return super.getLocalClassName()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun getMaxNumPictureInPictureActions(): Int {
        return super.getMaxNumPictureInPictureActions()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getMediaController(): MediaController? {
        return super.getMediaController()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getMenuInflater(): MenuInflater {
        return super.getMenuInflater()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        return super.getOnBackInvokedDispatcher()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getParent(): android.app.Activity {
        return super.getParent()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getParentActivityIntent(): Intent? {
        return super.getParentActivityIntent()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getPreferences(v: Int): SharedPreferences? {
        return super.getPreferences(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun getReferrer(): Uri? {
        return super.getReferrer()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getRequestedOrientation(): Int {
        return super.getRequestedOrientation()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getSearchEvent(): SearchEvent? {
        return super.getSearchEvent()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun getSplashScreen(): SplashScreen {
        return super.getSplashScreen()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getSystemService(name: String): Any? {
        chimeraActivityProxy?.let { proxy ->
            return when (name) {
                "layout_inflater", "print" -> proxy.getSystemService(name)
                else -> super.getSystemService(name)
            }
        }
        return super.getSystemService(name)
    }

    @ChimeraApiVersion(added = 0L)
    override fun getTaskId(): Int {
        return super.getTaskId()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getTitle(): CharSequence? {
        return super.getTitle()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getTitleColor(): Int {
        return super.getTitleColor()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getVoiceInteractor(): VoiceInteractor? {
        return super.getVoiceInteractor()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getVolumeControlStream(): Int {
        return super.getVolumeControlStream()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getWindow(): Window? {
        return super.getWindow()
    }

    @ChimeraApiVersion(added = 0L)
    override fun getWindowManager(): WindowManager? {
        return super.getWindowManager()
    }
    @ChimeraApiVersion(added = 0L)
    override fun hasWindowFocus(): Boolean {
        return super.hasWindowFocus()
    }

    @ChimeraApiVersion(added = 0L)
    override fun invalidateOptionsMenu() {
        super.invalidateOptionsMenu()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun isActivityTransitionRunning(): Boolean {
        return super.isActivityTransitionRunning()
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun isBackgroundVisibleBehind(): Boolean {
        return super.isBackgroundVisibleBehind()
    }
    @ChimeraApiVersion(added = 0L)
    override fun isChangingConfigurations(): Boolean {
        return super.isChangingConfigurations()
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun isChild(): Boolean {
        return super.isChild()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isDestroyed(): Boolean {
        return super.isDestroyed()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isFinishing(): Boolean {
        return super.isFinishing()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isImmersive(): Boolean {
        return super.isImmersive()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun isInMultiWindowMode(): Boolean {
        return super.isInMultiWindowMode()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun isInPictureInPictureMode(): Boolean {
        return super.isInPictureInPictureMode()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun isLaunchedFromBubble(): Boolean {
        return super.isLaunchedFromBubble()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun isLocalVoiceInteractionSupported(): Boolean {
        return super.isLocalVoiceInteractionSupported()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isTaskRoot(): Boolean {
        return super.isTaskRoot()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isVoiceInteraction(): Boolean {
        return super.isVoiceInteraction()
    }

    @ChimeraApiVersion(added = 0L)
    override fun isVoiceInteractionRoot(): Boolean {
        return super.isVoiceInteractionRoot()
    }
    @ChimeraApiVersion(added = 0L)
    override fun managedQuery(uri: Uri?, arr_s: Array<String?>?, s: String?, arr_s1: Array<String?>?, s1: String?): Cursor? {
        return super.managedQuery(uri, arr_s, s, arr_s1, s1)
    }

    @ChimeraApiVersion(added = 0L)
    override fun moveTaskToBack(z: Boolean): Boolean {
        return super.moveTaskToBack(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun navigateUpTo(intent: Intent?): Boolean {
        return super.navigateUpTo(intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun navigateUpToFromChild(activity: android.app.Activity?, intent: Intent?): Boolean {
        return super.navigateUpToFromChild(activity, intent)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onActionModeFinished(actionMode: ActionMode?) {
        super.onActionModeFinished(actionMode)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onActionModeStarted(actionMode: ActionMode?) {
        super.onActionModeStarted(actionMode)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onActivityReenter(v: Int, intent: Intent?) {
        super.onActivityReenter(v, intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller) {
        super.onActivityResult(requestCode, resultCode, data, caller)
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onBackPressed() {
        super.onBackPressed()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onBackgroundVisibleBehindChanged(z: Boolean) {
        super.onBackgroundVisibleBehindChanged(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onChildTitleChanged(activity: android.app.Activity?, charSequence: CharSequence?) {
        super.onChildTitleChanged(activity, charSequence)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onContentChanged() {
        super.onContentChanged()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onContextItemSelected(menuItem: MenuItem): Boolean {
        return super.onContextItemSelected(menuItem)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        super.onCreate(bundle, persistableBundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateContextMenu(contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateDescription(): CharSequence? {
        return super.onCreateDescription()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        super.onCreateNavigateUpTaskStack(taskStackBuilder)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onCreatePanelMenu(v: Int, menu: Menu): Boolean {
        return super.onCreatePanelMenu(v, menu)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onCreatePanelView(v: Int): View? {
        return super.onCreatePanelView(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateThumbnail(bitmap: Bitmap?, canvas: Canvas?): Boolean {
        return super.onCreateThumbnail(bitmap, canvas)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(parent, name, context, attrs)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onDestroy() {
        super.onDestroy()
    }
    @ChimeraApiVersion(added = 0L)
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return super.onGenericMotionEvent(motionEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onKeyDown(v: Int, keyEvent: KeyEvent?): Boolean {
        return super.onKeyDown(v, keyEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onKeyLongPress(v: Int, keyEvent: KeyEvent?): Boolean {
        return super.onKeyLongPress(v, keyEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onKeyMultiple(v: Int, v1: Int, keyEvent: KeyEvent?): Boolean {
        return super.onKeyMultiple(v, v1, keyEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onKeyShortcut(v: Int, keyEvent: KeyEvent?): Boolean {
        return super.onKeyShortcut(v, keyEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onKeyUp(v: Int, keyEvent: KeyEvent?): Boolean {
        return super.onKeyUp(v, keyEvent)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun onLocalVoiceInteractionStopped() {
        super.onLocalVoiceInteractionStopped()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onMenuItemSelected(featureId: Int, menuItem: MenuItem): Boolean {
        return super.onMenuItemSelected(featureId, menuItem)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return super.onMenuOpened(featureId, menu)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onNavigateUp(): Boolean {
        return super.onNavigateUp()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onNavigateUpFromChild(activity: android.app.Activity?): Boolean {
        return super.onNavigateUpFromChild(activity)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun onNewIntent(intent: Intent, componentCaller: ComponentCaller) {
        super.onNewIntent(intent, componentCaller)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return super.onOptionsItemSelected(menuItem)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onOptionsMenuClosed(menu: Menu) {
        super.onOptionsMenuClosed(menu)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onPanelClosed(featureId: Int, menu: Menu) {
        super.onPanelClosed(featureId, menu)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPause() {
        super.onPause()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPostCreate(bundle: Bundle?) {
        super.onPostCreate(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPostCreate(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        super.onPostCreate(bundle, persistableBundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPostResume() {
        super.onPostResume()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPrepareNavigateUpTaskStack(taskStackBuilder: TaskStackBuilder?) {
        super.onPrepareNavigateUpTaskStack(taskStackBuilder)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onPreparePanel(v: Int, view: View?, menu: Menu): Boolean {
        return super.onPreparePanel(v, view, menu)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onProvideReferrer(): Uri? {
        return super.onProvideReferrer()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, extra: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, extra)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onRestart() {
        super.onRestart()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onRestoreInstanceState(bundle: Bundle?, persistableBundle: PersistableBundle?) {
        super.onRestoreInstanceState(bundle, persistableBundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onResume() {
        super.onResume()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onSaveInstanceState(bundle: Bundle, persistableBundle: PersistableBundle) {
        super.onSaveInstanceState(bundle, persistableBundle)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onSearchRequested(): Boolean {
        return super.onSearchRequested()
    }
    @ChimeraApiVersion(added = 0L)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return super.onSearchRequested(searchEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onStart() {
        super.onStart()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onStateNotSaved() {
        super.onStateNotSaved()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onStop() {
        super.onStop()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onTitleChanged(charSequence: CharSequence?, v: Int) {
        super.onTitleChanged(charSequence, v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        return super.onTouchEvent(motionEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return super.onTrackballEvent(motionEvent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun onUserInteraction() {
        super.onUserInteraction()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    @ChimeraApiVersion(added = 0L)
    override fun onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled()
    }
    @ChimeraApiVersion(added = 0L)
    override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
        super.onWindowAttributesChanged(params)
    }
    @ChimeraApiVersion(added = 0L)
    override fun onWindowFocusChanged(z: Boolean) {
        super.onWindowFocusChanged(z)
    }
    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return super.onWindowStartingActionMode(callback)
    }
    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, v: Int): ActionMode? {
        return super.onWindowStartingActionMode(callback, v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun openContextMenu(view: View?) {
        super.openContextMenu(view)
    }

    @ChimeraApiVersion(added = 0L)
    override fun openOptionsMenu() {
        super.openOptionsMenu()
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun overrideActivityTransition(v: Int, v1: Int, v2: Int) {
        super.overrideActivityTransition(v, v1, v2)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun overrideActivityTransition(v: Int, v1: Int, v2: Int, v3: Int) {
        super.overrideActivityTransition(v, v1, v2, v3)
    }

    @ChimeraApiVersion(added = 0L)
    override fun overridePendingTransition(v: Int, v1: Int) {
        super.overridePendingTransition(v, v1)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun overridePendingTransition(v: Int, v1: Int, v2: Int) {
        super.overridePendingTransition(v, v1, v2)
    }

    @ChimeraApiVersion(added = 0L)
    override fun postponeEnterTransition() {
        super.postponeEnterTransition()
    }

    @ChimeraApiVersion(added = 0L)
    override fun recreate() {
        super.recreate()
    }

    override fun registerActivityLifecycleCallbacks(callbacks: Application.ActivityLifecycleCallbacks) {
        super.registerActivityLifecycleCallbacks(callbacks)
    }

    @ChimeraApiVersion(added = 0L)
    override fun registerForContextMenu(view: View?) {
        super.registerForContextMenu(view)
    }

    @ChimeraApiVersion(added = 0L)
    override fun releaseInstance(): Boolean {
        return super.releaseInstance()
    }

    @ChimeraApiVersion(added = 0L)
    override fun reportFullyDrawn() {
        super.reportFullyDrawn()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun requestDragAndDropPermissions(dragEvent: DragEvent?): DragAndDropPermissions? {
        return super.requestDragAndDropPermissions(dragEvent)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun requestFullscreenMode(request: Int, approvalCallback: OutcomeReceiver<Void, Throwable>?) {
        super.requestFullscreenMode(request, approvalCallback)
    }

    @ChimeraApiVersion(added = 0L)
    override fun requestPermissions(permissions: Array<String?>, requestCode: Int) {
        super.requestPermissions(permissions, requestCode)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun requestPermissions(permissions: Array<String?>, requestCode: Int, userId: Int) {
        super.requestPermissions(permissions, requestCode, userId)
    }

    @ChimeraApiVersion(added = 0L)
    override fun requestVisibleBehind(z: Boolean): Boolean {
        return super.requestVisibleBehind(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun requestWindowFeature(v: Int): Boolean {
        return super.requestWindowFeature(v)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun requireViewById(v: Int): View? {
        return super.requireViewById(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun runOnUiThread(runnable: Runnable?) {
        super.runOnUiThread(runnable)
    }

    @ChimeraApiVersion(added = 0x77L)
    override fun setActionBar(toolbar: Toolbar?) {
        super.setActionBar(toolbar)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun setAllowCrossUidActivitySwitchFromBelow(z: Boolean) {
        super.setAllowCrossUidActivitySwitchFromBelow(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setContentTransitionManager(transitionManager: TransitionManager?) {
        super.setContentTransitionManager(transitionManager)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setContentView(v: Int) {
        super.setContentView(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setContentView(view: View?) {
        super.setContentView(view)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setDefaultKeyMode(v: Int) {
        super.setDefaultKeyMode(v)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setEnterSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        super.setEnterSharedElementCallback(sharedElementCallback)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setExitSharedElementCallback(sharedElementCallback: SharedElementCallback?) {
        super.setExitSharedElementCallback(sharedElementCallback)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setFeatureDrawable(v: Int, drawable: Drawable?) {
        super.setFeatureDrawable(v, drawable)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setFeatureDrawableAlpha(v: Int, v1: Int) {
        super.setFeatureDrawableAlpha(v, v1)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setFeatureDrawableResource(v: Int, v1: Int) {
        super.setFeatureDrawableResource(v, v1)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setFeatureDrawableUri(v: Int, uri: Uri?) {
        super.setFeatureDrawableUri(v, uri)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setFinishOnTouchOutside(z: Boolean) {
        super.setFinishOnTouchOutside(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setImmersive(z: Boolean) {
        super.setImmersive(z)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setInheritShowWhenLocked(z: Boolean) {
        super.setInheritShowWhenLocked(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setIntent(intent: Intent?) {
        super.setIntent(intent)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun setIntent(intent: Intent?, componentCaller: ComponentCaller?) {
        super.setIntent(intent, componentCaller)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setLocusContext(locusId: LocusId?, bundle: Bundle?) {
        super.setLocusContext(locusId, bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setMediaController(mediaController: MediaController?) {
        super.setMediaController(mediaController)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setPictureInPictureParams(pictureInPictureParams: PictureInPictureParams) {
        super.setPictureInPictureParams(pictureInPictureParams)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setProgress(v: Int) {
        super.setProgress(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setProgressBarIndeterminate(z: Boolean) {
        super.setProgressBarIndeterminate(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setProgressBarIndeterminateVisibility(z: Boolean) {
        super.setProgressBarIndeterminateVisibility(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setProgressBarVisibility(z: Boolean) {
        super.setProgressBarVisibility(z)
    }

    override fun setProxyCallbacks(arg1: Any?, arg2: Context?) {
        this.setProxyCallbacks((arg1 as IChimeraActivityProxy?), arg2)
    }

    fun setProxyCallbacks(proxy: IChimeraActivityProxy?, context: Context?) {
        this.chimeraActivityProxy = proxy
        super.setProxyCallbacks((proxy as IActivityProxy), context)
        this.attachBaseContext(context)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setRecentsScreenshotEnabled(z: Boolean) {
        super.setRecentsScreenshotEnabled(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setRequestedOrientation(v: Int) {
        super.setRequestedOrientation(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setResult(v: Int) {
        super.setResult(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setResult(v: Int, intent: Intent?) {
        super.setResult(v, intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setSecondaryProgress(v: Int) {
        super.setSecondaryProgress(v)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setShouldDockBigOverlays(z: Boolean) {
        super.setShouldDockBigOverlays(z)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setShowWhenLocked(z: Boolean) {
        super.setShowWhenLocked(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setTaskDescription(taskDescription: ActivityManager.TaskDescription?) {
        super.setTaskDescription(taskDescription)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setTitle(v: Int) {
        super.setTitle(v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setTitle(charSequence: CharSequence?) {
        super.setTitle(charSequence)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setTitleColor(v: Int) {
        super.setTitleColor(v)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setTranslucent(z: Boolean): Boolean {
        return super.setTranslucent(z)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setTurnScreenOn(z: Boolean) {
        super.setTurnScreenOn(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setVisible(z: Boolean) {
        super.setVisible(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun setVolumeControlStream(v: Int) {
        super.setVolumeControlStream(v)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun setVrModeEnabled(enabled: Boolean, requestedComponent: ComponentName) {
        super.setVrModeEnabled(enabled, requestedComponent)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun shouldDockBigOverlays(): Boolean {
        return super.shouldDockBigOverlays()
    }

    @ChimeraApiVersion(added = 0L)
    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return super.shouldShowRequestPermissionRationale(permission)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean {
        return super.shouldShowRequestPermissionRationale(permission, userId)
    }

    @ChimeraApiVersion(added = 0L)
    override fun shouldUpRecreateTask(intent: Intent?): Boolean {
        return super.shouldUpRecreateTask(intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun showAssist(bundle: Bundle?): Boolean {
        return super.showAssist(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun showLockTaskEscapeMessage() {
        super.showLockTaskEscapeMessage()
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return super.startActionMode(callback)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun startActionMode(callback: ActionMode.Callback?, v: Int): ActionMode? {
        return super.startActionMode(callback, v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivities(intents: Array<Intent?>) {
        super.startActivities(intents)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivities(intents: Array<Intent?>, options: Bundle?) {
        super.startActivities(intents, options)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivity(intent: Intent?, bundle: Bundle?) {
        super.startActivity(intent, bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivityForResult(intent: Intent?, v: Int) {
        super.startActivityForResult(intent, v)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivityForResult(intent: Intent?, v: Int, bundle: Bundle?) {
        super.startActivityForResult(intent, v, bundle)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun startActivityForResultAsUser(intent: Intent?, v: Int, bundle: Bundle?, userHandle: UserHandle?) {
        super.startActivityForResultAsUser(intent, v, bundle, userHandle)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun startActivityForResultAsUser(intent: Intent?, v: Int, userHandle: UserHandle?) {
        super.startActivityForResultAsUser(intent, v, userHandle)
    }

    @ChimeraApiVersion(added = 0x8DL)
    override fun startActivityForResultAsUser(intent: Intent?, s: String?, v: Int, bundle: Bundle?, userHandle: UserHandle?) {
        super.startActivityForResultAsUser(intent, s, v, bundle, userHandle)
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun startActivityFromChild(child: android.app.Activity, intent: Intent?, requestCode: Int) {
        super.startActivityFromChild(child, intent, requestCode)
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun startActivityFromChild(activity: android.app.Activity, intent: Intent?, v: Int, bundle: Bundle?) {
        super.startActivityFromChild(activity, intent, v, bundle)
    }

    @Deprecated("")
    override fun startActivityFromFragment(fragment: Fragment, intent: Intent?, v: Int) {
        super.startActivityFromFragment(fragment, intent, v)
    }

    @ChimeraApiVersion(added = 0x85L)
    @Deprecated("")
    override fun startActivityFromFragment(fragment: Fragment, intent: Intent?, v: Int, bundle: Bundle?) {
        super.startActivityFromFragment(fragment, intent, v, bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean {
        return super.startActivityIfNeeded(intent, requestCode)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean {
        return super.startActivityIfNeeded(intent, requestCode, options)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        super.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) {
        super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        super.startIntentSenderForResult(intentSender, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun startIntentSenderFromChild(activity: android.app.Activity?, intentSender: IntentSender?, v: Int, intent: Intent?, v1: Int, v2: Int, v3: Int) {
        super.startIntentSenderFromChild(activity, intentSender, v, intent, v1, v2, v3)
    }

    @ChimeraApiVersion(added = 0L)
    @Deprecated("")
    override fun startIntentSenderFromChild(activity: android.app.Activity?, intentSender: IntentSender?, v: Int, intent: Intent?, v1: Int, v2: Int, v3: Int, bundle: Bundle?) {
        super.startIntentSenderFromChild(activity, intentSender, v, intent, v1, v2, v3, bundle)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun startLocalVoiceInteraction(bundle: Bundle?) {
        super.startLocalVoiceInteraction(bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startLockTask() {
        super.startLockTask()
    }

    @ChimeraApiVersion(added = 0L)
    override fun startManagingCursor(cursor: Cursor?) {
        super.startManagingCursor(cursor)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startNextMatchingActivity(intent: Intent): Boolean {
        return super.startNextMatchingActivity(intent)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startNextMatchingActivity(intent: Intent, bundle: Bundle?): Boolean {
        return super.startNextMatchingActivity(intent, bundle)
    }

    @ChimeraApiVersion(added = 0L)
    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
    }

    @ChimeraApiVersion(added = 0L)
    override fun startSearch(s: String?, z: Boolean, bundle: Bundle?, z1: Boolean) {
        super.startSearch(s, z, bundle, z1)
    }

    @ChimeraApiVersion(added = 0x85L)
    override fun stopLocalVoiceInteraction() {
        super.stopLocalVoiceInteraction()
    }

    @ChimeraApiVersion(added = 0L)
    override fun stopLockTask() {
        super.stopLockTask()
    }

    @ChimeraApiVersion(added = 0L)
    override fun stopManagingCursor(cursor: Cursor?) {
        super.stopManagingCursor(cursor)
    }

    @ChimeraApiVersion(added = 0L)
    override fun takeKeyEvents(z: Boolean) {
        super.takeKeyEvents(z)
    }

    @ChimeraApiVersion(added = 0L)
    override fun triggerSearch(s: String?, bundle: Bundle?) {
        super.triggerSearch(s, bundle)
    }

    override fun unregisterActivityLifecycleCallbacks(callbacks: Application.ActivityLifecycleCallbacks) {
        super.unregisterActivityLifecycleCallbacks(callbacks)
    }

    @ChimeraApiVersion(added = 0L)
    override fun unregisterForContextMenu(view: View?) {
        super.unregisterForContextMenu(view)
    }
}
