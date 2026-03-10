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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;

import org.microg.gms.common.PublicApi;
import org.microg.gms.gcm.GcmConstants;

/**
 * Implemented by the client application to provide an endpoint for the {@link com.google.android.gms.gcm.GcmNetworkManager}
 * to call back to when a task is ready to be executed.
 * <p/>
 * Clients must add this service to their manifest and implement
 * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}.
 * This service must provide an {@link IntentFilter} on the action
 * {@link com.google.android.gms.gcm.GcmTaskService#SERVICE_ACTION_EXECUTE_TASK}. Here's an example:
 * <pre>
 *     <service android:name="MyTaskService"
 *              android:permission="com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE"
 *              android:exported="true">
 *              <intent-filter>
 *                  <action android:name="com.google.android.gms.gcm.ACTION_TASK_READY"/>
 *              </intent-filter>
 *     </service>
 * </pre>
 * The return value of onRunTask(TaskParams) will determine what the manager does with subsequent
 * executions of this task. Specifically you can return {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_RESCHEDULE}
 * to have this task be re-executed again shortly subject to exponential back-off. Returning
 * {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_FAILURE} for a periodic task will only affect the executing
 * instance of the task, and future tasks will be executed as normal.
 * <p/>
 * Once a task is running it will not be cancelled, however a newly scheduled task with the same
 * tag will not be executed until the active task has completed. This newly scheduled task will
 * replace the previous task, regardless of whether the previous task returned
 * {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_RESCHEDULE}.
 * <p/>
 * Bear in mind that your service may receive multiple calls from the scheduler at once
 * (specifically if you've made multiple schedule requests that overlap). If this is the case, your
 * implementation of {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)} must be thread-safe.
 * <p/>
 * The scheduler will hold a {@link PowerManager.WakeLock} for your service, however
 * <strong>after three minutes of execution if your task has not returned it will be considered to
 * have timed out, and the wakelock will be released.</strong> Rescheduling your task at this point
 * will have no effect.
 * If you suspect your task will run longer than this you should start your own service
 * explicitly or use some other mechanism; this API is intended for relatively quick network
 * operations.
 * <p/>
 * Your task will run at priority Process.THREAD_PRIORITY_BACKGROUND. If this
 * is not appropriate, you should start your own service with suitably
 * conditioned threads.
 */
@PublicApi
public abstract class GcmTaskService extends Service {
    private static final String TAG = "GcmTaskService";

    /**
     * Action broadcast by the GcmNetworkManager to the requesting package when
     * a scheduled task is ready for execution.
     */
    public static final String SERVICE_ACTION_EXECUTE_TASK = GcmConstants.ACTION_TASK_READY;

    /**
     * Action that a {@link com.google.android.gms.gcm.GcmTaskService} is started with when the service needs to initialize
     * its tasks.
     */
    public static final String SERVICE_ACTION_INITIALIZE = GcmConstants.ACTION_TASK_INITIALZE;

    /**
     * You must protect your service with this permission to avoid being bound to by an
     * application other than Google Play Services.
     */
    public static final String SERVICE_PERMISSION = GcmConstants.PERMISSION_NETWORK_TASK;

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * When your package is removed or updated, all of its network tasks are cleared by the
     * GcmNetworkManager. You can override this method to reschedule them in the case of an
     * updated package. This is not called when your application is first installed.
     * <p/>
     * This is called on your application's main thread.
     */
    public void onInitializeTasks() {
        // To be overwritten
    }

    /**
     * Override this function to provide the logic for your task execution.
     *
     * @param params Parameters provided at schedule time with
     *               {@link com.google.android.gms.gcm.OneoffTask.Builder#setTag(java.lang.String)}
     * @return One of {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_SUCCESS},
     * {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_RESCHEDULE}, or
     * {@link com.google.android.gms.gcm.GcmNetworkManager#RESULT_FAILURE}.
     */
    public abstract int onRunTask(TaskParams params);

    /**
     * Receives the command to begin doing work, for which it spawns another thread.
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        intent.setExtrasClassLoader(PendingCallback.class.getClassLoader());
        if (SERVICE_ACTION_EXECUTE_TASK.equals(intent.getAction())) {
            String tag = intent.getStringExtra("tag");
            Parcelable callback = intent.getParcelableExtra("callback");
            Bundle extras = intent.getBundleExtra("extras");
            if (callback == null || !(callback instanceof PendingCallback)) {
                Log.w(TAG, tag + ": Invalid callback!");
                return START_NOT_STICKY;
            }

            // TODO ensure single instance

            // TODO run task in new thread
        } else if (SERVICE_ACTION_INITIALIZE.equals(intent.getAction())) {
            this.onInitializeTasks();

            // TODO ensure single instance
        }

        return START_NOT_STICKY;
    }

}
