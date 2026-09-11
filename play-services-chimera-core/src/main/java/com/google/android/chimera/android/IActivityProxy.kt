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

interface IActivityProxy {
    fun platform_addContentView(view: View?, params: ViewGroup.LayoutParams?)

    fun platform_clearOverrideActivityTransition(transitionType: Int)

    fun platform_closeContextMenu()

    fun platform_closeOptionsMenu()

    fun platform_convertFromTranslucent()

    fun platform_convertToTranslucent(
        listener: Any?,
        options: ActivityOptions?
    ): Boolean

    fun platform_createPendingResult(requestCode: Int, data: Intent, flags: Int): PendingIntent

    fun platform_dispatchGenericMotionEvent(event: MotionEvent?): Boolean

    fun platform_dispatchKeyEvent(event: KeyEvent?): Boolean

    fun platform_dispatchKeyShortcutEvent(event: KeyEvent?): Boolean

    fun platform_dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean

    fun platform_dispatchTouchEvent(event: MotionEvent?): Boolean

    fun platform_dispatchTrackballEvent(event: MotionEvent?): Boolean

    fun <T : View?> platform_findViewById(id: Int): T?

    fun platform_finish()

    fun platform_finishActivity(requestCode: Int)

    @Deprecated("")
    fun platform_finishActivityFromChild(child: Activity, requestCode: Int)

    fun platform_finishAffinity()

    fun platform_finishAfterTransition()

    fun platform_finishAndRemoveTask()

    @Deprecated("")
    fun platform_finishFromChild(child: Activity?)

    fun platform_getActionBar(): ActionBar?

    fun platform_getApplication(): Application?

    fun platform_getCaller(): ComponentCaller?

    fun platform_getCallingActivity(): ComponentName?

    fun platform_getCallingPackage(): String?

    fun platform_getChangingConfigurations(): Int

    fun platform_getComponentName(): ComponentName?

    fun platform_getContentScene(): Scene?

    fun platform_getContentTransitionManager(): TransitionManager?

    fun platform_getCurrentCaller(): ComponentCaller

    fun platform_getCurrentFocus(): View?

    @Deprecated("")
    fun platform_getFragmentManager(): FragmentManager?

    fun platform_getInitialCaller(): ComponentCaller

    fun platform_getIntent(): Intent?

    fun platform_getLastNonConfigurationInstance(): Any?

    fun platform_getLaunchedFromPackage(): String?

    fun platform_getLaunchedFromUid(): Int

    fun platform_getLayoutInflater(): LayoutInflater

    @Deprecated("")
    fun platform_getLoaderManager(): LoaderManager?

    fun platform_getLocalClassName(): String

    fun platform_getMaxNumPictureInPictureActions(): Int

    fun platform_getMediaController(): MediaController

    fun platform_getMenuInflater(): MenuInflater

    fun platform_getOnBackInvokedDispatcher(): OnBackInvokedDispatcher

    @Deprecated("")
    fun platform_getParent(): Activity

    fun platform_getParentActivityIntent(): Intent?

    fun platform_getPreferences(mode: Int): SharedPreferences?

    fun platform_getReferrer(): Uri?

    fun platform_getRequestedOrientation(): Int

    fun platform_getSearchEvent(): SearchEvent?

    fun platform_getSplashScreen(): SplashScreen

    fun platform_getTaskId(): Int

    fun platform_getTitle(): CharSequence?

    fun platform_getTitleColor(): Int

    fun platform_getVoiceInteractor(): VoiceInteractor?

    fun platform_getVolumeControlStream(): Int

    fun platform_getWindow(): Window?

    fun platform_getWindowManager(): WindowManager?

    fun platform_hasWindowFocus(): Boolean

    fun platform_invalidateOptionsMenu()

    fun platform_isActivityTransitionRunning(): Boolean

    @Deprecated("")
    fun platform_isBackgroundVisibleBehind(): Boolean

    fun platform_isChangingConfigurations(): Boolean

    @Deprecated("")
    fun platform_isChild(): Boolean

    fun platform_isDestroyed(): Boolean

    fun platform_isFinishing(): Boolean

    fun platform_isImmersive(): Boolean

    fun platform_isInMultiWindowMode(): Boolean

    fun platform_isInPictureInPictureMode(): Boolean

    fun platform_isLaunchedFromBubble(): Boolean

    fun platform_isLocalVoiceInteractionSupported(): Boolean

    fun platform_isTaskRoot(): Boolean

    fun platform_isVoiceInteraction(): Boolean

    fun platform_isVoiceInteractionRoot(): Boolean

    @Deprecated("")
    fun platform_managedQuery(
        uri: Uri?,
        projection: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        sortOrder: String?
    ): Cursor?

    fun platform_moveTaskToBack(nonRoot: Boolean): Boolean

    fun platform_navigateUpTo(intent: Intent?): Boolean

    @Deprecated("")
    fun platform_navigateUpToFromChild(child: Activity?, intent: Intent?): Boolean

    fun platform_onActionModeFinished(mode: ActionMode?)

    fun platform_onActionModeStarted(mode: ActionMode?)

    fun platform_onActivityReenter(resultCode: Int, data: Intent?)

    fun platform_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun platform_onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, caller: ComponentCaller)

    @Deprecated("")
    fun platform_onAttachFragment(fragment: Fragment?)

    fun platform_onAttachedToWindow()

    @Deprecated("")
    fun platform_onBackPressed()

    @Deprecated("Deprecated in Java")
    fun platform_onBackgroundVisibleBehindChanged(visible: Boolean)

    fun platform_onChildTitleChanged(child: Activity?, title: CharSequence?)

    fun platform_onConfigurationChanged(newConfig: Configuration)

    fun platform_onContentChanged()

    fun platform_onContextItemSelected(item: MenuItem): Boolean

    fun platform_onContextMenuClosed(menu: Menu)

    fun platform_onCreate(savedInstanceState: Bundle?)

    fun platform_onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?)

    fun platform_onCreateContextMenu(
        menu: ContextMenu?,
        view: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    )

    fun platform_onCreateDescription(): CharSequence?

    fun platform_onCreateNavigateUpTaskStack(builder: TaskStackBuilder?)

    fun platform_onCreateOptionsMenu(menu: Menu): Boolean

    fun platform_onCreatePanelMenu(featureId: Int, menu: Menu): Boolean

    fun platform_onCreatePanelView(featureId: Int): View?

    @Deprecated("")
    fun platform_onCreateThumbnail(outBitmap: Bitmap?, canvas: Canvas?): Boolean

    fun platform_onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View?

    fun platform_onCreateView(name: String, context: Context, attrs: AttributeSet): View?

    fun platform_onDestroy()

    fun platform_onDetachedFromWindow()

    fun platform_onGenericMotionEvent(event: MotionEvent?): Boolean

    fun platform_onKeyDown(keyCode: Int, event: KeyEvent?): Boolean

    fun platform_onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean

    fun platform_onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean

    fun platform_onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean

    fun platform_onKeyUp(keyCode: Int, event: KeyEvent?): Boolean

    fun platform_onLocalVoiceInteractionStarted()

    fun platform_onLocalVoiceInteractionStopped()

    fun platform_onMenuItemSelected(featureId: Int, item: MenuItem): Boolean

    fun platform_onMenuOpened(featureId: Int, menu: Menu): Boolean

    fun platform_onNavigateUp(): Boolean

    @Deprecated("")
    fun platform_onNavigateUpFromChild(child: Activity?): Boolean

    fun platform_onNewIntent(intent: Intent?)

    fun platform_onNewIntent(intent: Intent, caller: ComponentCaller)

    fun platform_onOptionsItemSelected(item: MenuItem): Boolean

    fun platform_onOptionsMenuClosed(menu: Menu)

    fun platform_onPanelClosed(featureId: Int, menu: Menu)

    fun platform_onPause()

    fun platform_onPointerCaptureChanged(hasCapture: Boolean)

    fun platform_onPostCreate(savedInstanceState: Bundle?)

    fun platform_onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?)

    fun platform_onPostResume()

    fun platform_onPrepareNavigateUpTaskStack(builder: TaskStackBuilder?)

    fun platform_onPrepareOptionsMenu(menu: Menu): Boolean

    fun platform_onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean

    fun platform_onProvideReferrer(): Uri?

    fun platform_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray)

    fun platform_onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray, userId: Int)

    fun platform_onRestart()

    fun platform_onRestoreInstanceState(savedInstanceState: Bundle)

    fun platform_onRestoreInstanceState(savedInstanceState: Bundle?, persistentState: PersistableBundle?)

    fun platform_onResume()

    fun platform_onSaveInstanceState(outState: Bundle)

    fun platform_onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle)

    fun platform_onSearchRequested(): Boolean

    fun platform_onSearchRequested(event: SearchEvent?): Boolean

    fun platform_onStart()

    @Deprecated("")
    fun platform_onStateNotSaved()

    fun platform_onStop()

    fun platform_onTitleChanged(title: CharSequence?, color: Int)

    fun platform_onTouchEvent(event: MotionEvent?): Boolean

    fun platform_onTrackballEvent(event: MotionEvent?): Boolean

    fun platform_onUserInteraction()

    fun platform_onUserLeaveHint()

    @Deprecated("")
    fun platform_onVisibleBehindCanceled()

    fun platform_onWindowAttributesChanged(params: WindowManager.LayoutParams?)

    fun platform_onWindowFocusChanged(hasFocus: Boolean)

    fun platform_onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode?

    fun platform_onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode?

    fun platform_openContextMenu(view: View?)

    fun platform_openOptionsMenu()

    fun platform_overrideActivityTransition(transitionType: Int, enterAnim: Int, exitAnim: Int)

    fun platform_overrideActivityTransition(
        transitionType: Int,
        enterAnim: Int,
        exitAnim: Int,
        backgroundColor: Int
    )

    @Deprecated("")
    fun platform_overridePendingTransition(enterAnim: Int, exitAnim: Int)

    @Deprecated("")
    fun platform_overridePendingTransition(enterAnim: Int, exitAnim: Int, backgroundColor: Int)

    fun platform_postponeEnterTransition()

    fun platform_recreate()

    fun platform_registerActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks)

    fun platform_registerForContextMenu(view: View?)

    fun platform_releaseInstance(): Boolean

    fun platform_reportFullyDrawn()

    fun platform_requestDragAndDropPermissions(event: DragEvent?): DragAndDropPermissions?

    fun platform_requestFullscreenMode(request: Int, approvalCallback: OutcomeReceiver<Void, Throwable>?)

    fun platform_requestPermissions(permissions: Array<String?>, requestCode: Int)

    fun platform_requestPermissions(permissions: Array<String?>, requestCode: Int, userId: Int)

    @Deprecated("")
    fun platform_requestVisibleBehind(visible: Boolean): Boolean

    fun platform_requestWindowFeature(featureId: Int): Boolean

    fun platform_requireViewById(id: Int): View?

    fun platform_runOnUiThread(action: Runnable?)

    fun platform_setActionBar(toolbar: Toolbar?)

    fun platform_setAllowCrossUidActivitySwitchFromBelow(allow: Boolean)

    fun platform_setContentTransitionManager(transitionManager: TransitionManager?)

    fun platform_setContentView(layoutResID: Int)

    fun platform_setContentView(view: View?)

    fun platform_setContentView(view: View?, params: ViewGroup.LayoutParams?)

    fun platform_setDefaultKeyMode(mode: Int)

    fun platform_setEnterSharedElementCallback(callback: SharedElementCallback?)

    fun platform_setExitSharedElementCallback(callback: SharedElementCallback?)

    fun platform_setFeatureDrawable(featureId: Int, drawable: Drawable?)

    fun platform_setFeatureDrawableAlpha(featureId: Int, alpha: Int)

    fun platform_setFeatureDrawableResource(featureId: Int, resId: Int)

    fun platform_setFeatureDrawableUri(featureId: Int, uri: Uri?)

    fun platform_setFinishOnTouchOutside(finish: Boolean)

    fun platform_setImmersive(immersive: Boolean)

    fun platform_setInheritShowWhenLocked(showWhenLocked: Boolean)

    fun platform_setIntent(intent: Intent?)

    fun platform_setIntent(intent: Intent?, caller: ComponentCaller?)

    fun platform_setLocusContext(locusId: LocusId?, bundle: Bundle?)

    fun platform_setMediaController(controller: MediaController?)

    fun platform_setPictureInPictureParams(params: PictureInPictureParams)

    @Deprecated("")
    fun platform_setProgress(progress: Int)

    @Deprecated("")
    fun platform_setProgressBarIndeterminate(indeterminate: Boolean)

    @Deprecated("")
    fun platform_setProgressBarIndeterminateVisibility(visible: Boolean)

    @Deprecated("")
    fun platform_setProgressBarVisibility(visible: Boolean)

    fun platform_setRecentsScreenshotEnabled(enabled: Boolean)

    fun platform_setRequestedOrientation(orientation: Int)

    fun platform_setResult(resultCode: Int)

    fun platform_setResult(resultCode: Int, data: Intent?)

    @Deprecated("")
    fun platform_setSecondaryProgress(secondaryProgress: Int)

    fun platform_setShouldDockBigOverlays(shouldDock: Boolean)

    fun platform_setShowWhenLocked(showWhenLocked: Boolean)

    fun platform_setTaskDescription(description: ActivityManager.TaskDescription?)

    fun platform_setTitle(titleId: Int)

    fun platform_setTitle(title: CharSequence?)

    @Deprecated("")
    fun platform_setTitleColor(textColor: Int)

    fun platform_setTranslucent(translucent: Boolean): Boolean

    fun platform_setTurnScreenOn(turnScreenOn: Boolean)

    fun platform_setVisible(visible: Boolean)

    fun platform_setVolumeControlStream(streamType: Int)

    fun platform_setVrModeEnabled(enabled: Boolean, requestedComponent: ComponentName)

    fun platform_shouldDockBigOverlays(): Boolean

    fun platform_shouldShowRequestPermissionRationale(permission: String): Boolean

    fun platform_shouldShowRequestPermissionRationale(permission: String, userId: Int): Boolean

    fun platform_shouldUpRecreateTask(targetIntent: Intent?): Boolean

    fun platform_showAssist(args: Bundle?): Boolean

    fun platform_showLockTaskEscapeMessage()

    fun platform_startActionMode(callback: ActionMode.Callback?): ActionMode?

    fun platform_startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode?

    fun platform_startActivities(intents: Array<Intent?>)

    fun platform_startActivities(intents: Array<Intent?>, options: Bundle?)

    fun platform_startActivity(intent: Intent?)

    fun platform_startActivity(intent: Intent?, options: Bundle?)

    fun platform_startActivityForResult(intent: Intent?, requestCode: Int)

    fun platform_startActivityForResult(intent: Intent?, requestCode: Int, options: Bundle?)

    fun platform_startActivityForResultAsUser(
        intent: Intent?,
        requestCode: Int,
        options: Bundle?,
        user: UserHandle?
    )

    fun platform_startActivityForResultAsUser(intent: Intent?, requestCode: Int, user: UserHandle?)

    fun platform_startActivityForResultAsUser(
        intent: Intent?,
        permission: String?,
        requestCode: Int,
        options: Bundle?,
        user: UserHandle?
    )

    @Deprecated("")
    fun platform_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int)

    @Deprecated("")
    fun platform_startActivityFromChild(child: Activity, intent: Intent?, requestCode: Int, options: Bundle?)

    @Deprecated("")
    fun platform_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int)

    @Deprecated("")
    fun platform_startActivityFromFragment(fragment: Fragment, intent: Intent?, requestCode: Int, options: Bundle?)

    fun platform_startActivityIfNeeded(intent: Intent, requestCode: Int): Boolean

    fun platform_startActivityIfNeeded(intent: Intent, requestCode: Int, options: Bundle?): Boolean

    fun platform_startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int)

    fun platform_startIntentSender(intentSender: IntentSender, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?)

    fun platform_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int)

    fun platform_startIntentSenderForResult(intentSender: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?)

    @Deprecated("")
    fun platform_startIntentSenderFromChild(
        child: Activity?,
        intentSender: IntentSender?,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int
    )

    @Deprecated("")
    fun platform_startIntentSenderFromChild(
        child: Activity?,
        intentSender: IntentSender?,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    )

    fun platform_startLocalVoiceInteraction(privateOptions: Bundle?)

    fun platform_startLockTask()

    @Deprecated("")
    fun platform_startManagingCursor(cursor: Cursor?)

    fun platform_startNextMatchingActivity(intent: Intent): Boolean

    fun platform_startNextMatchingActivity(intent: Intent, options: Bundle?): Boolean

    fun platform_startPostponedEnterTransition()

    fun platform_startSearch(
        initialQuery: String?,
        selectInitialQuery: Boolean,
        appSearchData: Bundle?,
        globalSearch: Boolean
    )

    fun platform_stopLocalVoiceInteraction()

    fun platform_stopLockTask()

    @Deprecated("")
    fun platform_stopManagingCursor(cursor: Cursor?)

    fun platform_takeKeyEvents(get: Boolean)

    fun platform_triggerSearch(query: String?, appSearchData: Bundle?)

    fun platform_unregisterActivityLifecycleCallbacks(callback: Application.ActivityLifecycleCallbacks)

    fun platform_unregisterForContextMenu(view: View?)
}
