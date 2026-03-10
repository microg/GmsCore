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

package com.google.android.gms.gcm;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.List;

import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.gcm.GcmConstants.ACTION_SCHEDULE;
import static org.microg.gms.gcm.GcmConstants.ACTION_TASK_READY;
import static org.microg.gms.gcm.GcmConstants.EXTRA_COMPONENT;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SCHEDULER_ACTION;
import static org.microg.gms.gcm.GcmConstants.EXTRA_TAG;
import static org.microg.gms.gcm.GcmConstants.SCHEDULER_ACTION_CANCEL;
import static org.microg.gms.gcm.GcmConstants.SCHEDULER_ACTION_CANCEL_ALL;
import static org.microg.gms.gcm.GcmConstants.SCHEDULER_ACTION_SCHEDULE;

/**
 * Class to create apps with robust "send-to-sync", which is the mechanism to sync data with
 * servers where new information is available.
 * <p/>
 * You can use the API to schedule network-oriented tasks, and let Google Play services batch
 * network operations across the system. This greatly simplifies the implementation of common
 * patterns, such as waiting for network connectivity, network retries, and backoff.
 * <p/>
 * Tasks must be scheduled based on an execution window in time. During this execution window
 * the scheduler will use its discretion in picking an optimal execution time, based on network
 * availability (whether the device has connectivity), network activity (whether packages are
 * actively being transferred). and load (how many other pending tasks are available for
 * execution at that point in time). <strong>If none of these factors are influential, the
 * scheduler will always wait until the end of the specified window.</strong>
 * <p/>
 * To receive the notification from the scheduler that a task is ready to be executed, your
 * client app must implement a {@link com.google.android.gms.gcm.GcmTaskService} and filter
 * on the action {@link com.google.android.gms.gcm.GcmTaskService#SERVICE_ACTION_EXECUTE_TASK}.
 * <p/>
 * Note that tags of arbitrary length are <strong>not</strong> allowed; if the tag you
 * provide is greater than 100 characters an exception will be thrown when you try to create your
 * {@link com.google.android.gms.gcm.Task} object.
 * <p/>
 * The service should be protected by the permission
 * com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE which is used by Google Play
 * Services. This prevents other code from invoking the broadcast receiver. Here is an excerpt from
 * a sample manifest:
 * <pre>
 * <service android:name=".MyUploadService"
 *     android:exported="true"
 *     android:permission="com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE" >
 *     <intent-filter>
 *        <action android:name="com.google.android.gms.gcm.ACTION_TASK_READY" />
 *     </intent-filter>
 * </service>
 * </pre>
 * An execution contains the tag identifier which your client app provides. This identifies
 * one "task", or piece of work, that you mean to perform. Consider the tag to be the key to which
 * your task logic is paired.
 * <pre>
 * // Schedule a task to occur between five and fifteen seconds from now:
 * OneoffTask myTask = new OneoffTask.Builder()
 *         .setService(MyGcmTaskService.class)
 *         .setExecutionWindow(
 *             5 * DateUtil.MINUTE_IN_SECONDS, 15 * DateUtil.MINUTE_IN_SECONDS)
 *         .setTag("test-upload")
 *         .build();
 * GcmNetworkManager.get(this).schedule(myTask);
 * ...
 * // Implement service logic to be notified when the task elapses:
 * MyUploadService extends GcmTaskService {
 *
 *     @Override public int onRunTask(TaskParams params) {
 *         // Do some upload work.
 *         return GcmNetworkManager.RESULT_SUCCESS;
 *     }
 * }
 * </pre>
 * To help in debugging your tasks, run
 * <code>adb shell dumpsys activity service GcmService --endpoints [...]</code>
 * If you want to execute your task immediately (for debugging) you can execute tasks from the
 * command line via:
 * <code>adb shell am broadcast -a "com.google.android.gms.gcm.ACTION_TRIGGER_TASK" \
 * -e component <COMPONENT_NAME> -e tag <TAG></code>
 * Where <strong>COMPONENT_NAME</strong>: The full
 * {@link ComponentName#flattenToString()} returned for your implementation of
 * {@link com.google.android.gms.gcm.GcmTaskService}.
 * <strong>TAG</strong>: the tag you want to have land in
 * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}
 * Example usage for the gradle target GcmTestProxy service:
 * <code>adb shell am broadcast -a "com.google.android.gms.gcm.ACTION_TRIGGER_TASK" \
 * -e component "com.google.android.gms.gcm.test.proxy/.internal.nstest.TestNetworkTaskService" \
 * -e tag "upload"</code>
 * <strong>This is only available if the device is a test-keys build. This will replace any
 * previously scheduled task with the same tag!</strong> This will have especially awkward effects
 * if your original task was a periodic, because the debug task is scheduled as a one-off.
 */
public class GcmNetworkManager {
    /**
     * Indicates a task has failed, but not to reschedule.
     */
    public static final int RESULT_FAILURE = 2;

    /**
     * Indicates a task has failed to execute, and must be retried with back-off.
     */
    public static final int RESULT_RESCHEDULE = 1;

    /**
     * Indicates a task has successfully been executed, and can be removed from the queue.
     */
    public static final int RESULT_SUCCESS = 0;

    private static GcmNetworkManager INSTANCE;

    private final Context context;

    private GcmNetworkManager(Context context) {
        this.context = context;
    }

    /**
     * Cancels all tasks previously scheduled against the provided GcmTaskService. Note that a
     * cancel will have no effect on an in-flight task.
     *
     * @param gcmTaskService The endpoint for which you want to cancel all outstanding tasks.
     */
    public void cancelAllTasks(Class<? extends GcmTaskService> gcmTaskService) {
        validateService(gcmTaskService.getName());
        Intent scheduleIntent = createScheduleIntent();
        if (scheduleIntent != null) {
            scheduleIntent.putExtra(EXTRA_SCHEDULER_ACTION, SCHEDULER_ACTION_CANCEL_ALL);
            scheduleIntent.putExtra(EXTRA_COMPONENT, new ComponentName(context, gcmTaskService));
            context.sendBroadcast(scheduleIntent);
        }
    }

    /**
     * Cancel a task, specified by tag.  Note that a cancel will have no effect on an in-flight
     * task.
     *
     * @param tag            The tag to uniquely identify this task on this endpoint.
     * @param gcmTaskService The endpoint for which you want to cancel all outstanding tasks.
     */
    public void cancelTask(String tag, Class<? extends GcmTaskService> gcmTaskService) {
        if (TextUtils.isEmpty(tag) || tag.length() < 100) throw new IllegalArgumentException("tag invalid");
        validateService(gcmTaskService.getName());
        Intent scheduleIntent = createScheduleIntent();
        if (scheduleIntent != null) {
            scheduleIntent.putExtra(EXTRA_SCHEDULER_ACTION, SCHEDULER_ACTION_CANCEL);
            scheduleIntent.putExtra(EXTRA_TAG, tag);
            scheduleIntent.putExtra(EXTRA_COMPONENT, new ComponentName(context, gcmTaskService));
            context.sendBroadcast(scheduleIntent);
        }
    }

    /**
     * Use this function to access the GcmNetworkManager API.
     *
     * @param context Context of the calling app.
     * @return GcmNetworkManager object.
     */
    public static GcmNetworkManager getInstance(Context context) {
        synchronized (GcmNetworkManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new GcmNetworkManager(context);
            }
            return INSTANCE;
        }
    }

    /**
     * Entry point to schedule a task with the network manager.
     * <p/>
     * If GooglePlayServices is unavailable (upgrading, missing, etc). This call will fail silently.
     * You should wrap it in a call to {@link com.google.android.gms.common.GooglePlayServicesUtil#isGooglePlayServicesAvailable(android.content.Context)}</p>
     *
     * @param task Task constructed using {@link com.google.android.gms.gcm.Task.Builder}. Can be
     *             an instance of {@link com.google.android.gms.gcm.PeriodicTask} or
     *             {@link com.google.android.gms.gcm.OneoffTask}.
     */
    public void schedule(Task task) {
        validateService(task.getServiceName());
        Intent scheduleIntent = createScheduleIntent();
        if (scheduleIntent != null) {
            Bundle extras = scheduleIntent.getExtras();
            extras.putString(EXTRA_SCHEDULER_ACTION, SCHEDULER_ACTION_SCHEDULE);
            task.toBundle(extras);
            scheduleIntent.putExtras(extras);
            context.sendBroadcast(scheduleIntent);
        }
    }

    private Intent createScheduleIntent() {
        if (!packageExists(GMS_PACKAGE_NAME)) return null;
        Intent scheduleIntent = new Intent(ACTION_SCHEDULE);
        scheduleIntent.setPackage(GMS_PACKAGE_NAME);
        scheduleIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        return scheduleIntent;
    }

    private boolean packageExists(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void validateService(String serviceName) {
        if (serviceName == null) throw new NullPointerException("No service provided");
        Intent taskIntent = new Intent(ACTION_TASK_READY);
        taskIntent.setPackage(context.getPackageName());
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> serviceResolves = pm.queryIntentServices(taskIntent, 0);
        if (serviceResolves == null || serviceResolves.isEmpty())
            throw new IllegalArgumentException("No service found");
        for (ResolveInfo info : serviceResolves) {
            if (serviceName.equals(info.serviceInfo.name)) return;
        }
        throw new IllegalArgumentException("Service not supported.");
    }
}
