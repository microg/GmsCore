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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;

/**
 * Encapsulates the parameters of a task that you will schedule on the
 * {@link com.google.android.gms.gcm.GcmNetworkManager}.
 * <p/>
 * Construct instances of either {@link com.google.android.gms.gcm.PeriodicTask} or
 * {@link com.google.android.gms.gcm.OneoffTask} with the desired parameters/behaviour and
 * schedule them using {@link com.google.android.gms.gcm.GcmNetworkManager#schedule(com.google.android.gms.gcm.Task)}.
 */
@PublicApi
public abstract class Task implements Parcelable {

    /**
     * <p>The maximum size allowed for extras bundle in bytes.
     * </p>
     */
    public static final int EXTRAS_LIMIT_BYTES = 10240;

    /**
     * <p>Specify using {@link com.google.android.gms.gcm.Task.Builder#setRequiredNetwork(int)}
     * that your task will execute [...] of whether network is available.
     * </p>
     */
    public static final int NETWORK_STATE_ANY = 2;

    /**
     * <p>Specify using {@link com.google.android.gms.gcm.Task.Builder#setRequiredNetwork(int)}
     * that your task will only execute if [...] sort of data connection is available -
     * either metered or unmetered. <strong>This is the default.</strong></p>
     */
    public static final int NETWORK_STATE_CONNECTED = 0;

    /**
     * <p>Specify using {@link com.google.android.gms.gcm.Task.Builder#setRequiredNetwork(int)}
     * that your task will only execute if there is an unmetered network connection available.
     * </p>
     */
    public static final int NETWORK_STATE_UNMETERED = 1;

    protected static final long UNINITIALIZED = -1;

    private final String serviceName;
    private final String tag;
    private final boolean updateCurrent;
    private final boolean persisted;
    private final int requiredNetwork;
    private final boolean requiresCharging;
    private final Bundle extras;

    Task(Builder builder) {
        this.serviceName = builder.gcmTaskService;
        this.tag = builder.tag;
        this.updateCurrent = builder.updateCurrent;
        this.persisted = builder.isPersisted;
        this.requiredNetwork = builder.requiredNetworkState;
        this.requiresCharging = builder.requiresCharging;
        this.extras = builder.extras;
    }

    Task(Parcel in) {
        this.serviceName = in.readString();
        this.tag = in.readString();
        this.updateCurrent = in.readInt() == 1;
        this.persisted = in.readInt() == 1;
        this.requiredNetwork = NETWORK_STATE_ANY;
        this.requiresCharging = false;
        this.extras = null;
    }

    public int describeContents() {
        return 0;
    }

    /**
     * @return The extra parameters for the task set by the client.
     */
    public Bundle getExtras() {
        return extras;
    }

    /**
     * If the specified network is unavailable, your task <strong>will not be run</strong> until
     * it is.
     *
     * @return The network type that this task requires in order to run. See the NETWORK_TYPE_*
     * flavours for an explanation of what this value can be.
     */
    public int getRequiredNetwork() {
        return requiredNetwork;
    }

    /**
     * If the device is not charging and this is set to true, your task <strong>will not be run
     * </strong> until it is.
     *
     * @return Whether or not this task depends on the device being connected to power in order to
     * execute.
     */
    public boolean getRequiresCharging() {
        return requiresCharging;
    }

    /**
     * @return The {@link com.google.android.gms.gcm.GcmTaskService} component that this task
     * will execute on.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @return The String identifier for this task, that is returned to
     * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}
     * when this task executes.
     */
    public String getTag() {
        return tag;
    }

    /**
     * @return Whether this task will be persisted across devices restarts or Google Play Services
     * crashes.
     */
    public boolean isPersisted() {
        return persisted;
    }

    /**
     * @return Whether or not this task will update a pre-existing task in the scheduler queue.
     */
    public boolean isUpdateCurrent() {
        return updateCurrent;
    }

    public void toBundle(Bundle bundle) {
        bundle.putString("tag", this.tag);
        bundle.putBoolean("update_current", this.updateCurrent);
        bundle.putBoolean("persisted", this.persisted);
        bundle.putString("service", this.serviceName);
        bundle.putInt("requiredNetwork", this.requiredNetwork);
        bundle.putBoolean("requiresCharging", this.requiresCharging);
        bundle.putBundle("retryStrategy", null); // TODO
        bundle.putBundle("extras", this.extras);
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(serviceName);
        parcel.writeString(tag);
        parcel.writeInt(updateCurrent ? 1 : 0);
        parcel.writeInt(persisted ? 1 : 0);
    }


    /**
     * <p>Builder object to construct these tasks before sending them to the network manager. Use
     * either {@link com.google.android.gms.gcm.PeriodicTask.Builder} or
     * {@link com.google.android.gms.gcm.Task.Builder}</p>
     */
    public abstract static class Builder {
        protected Bundle extras;
        protected String gcmTaskService;
        protected boolean isPersisted;
        protected int requiredNetworkState;
        protected boolean requiresCharging;
        protected String tag;
        protected boolean updateCurrent;

        public Builder() {
            throw new UnsupportedOperationException();
        }

        public abstract Task build();

        /**
         * Optional setter for specifying any extra parameters necessary for the task.
         */
        public abstract Task.Builder setExtras(Bundle extras);

        /**
         * Optional setter to specify whether this task should be persisted across reboots. This
         * defaults to true for periodic tasks, and is not supported for one-off tasks.
         * <p/>
         * Callers <strong>must</strong> hold the permission
         * android.Manifest.permission.RECEIVE_BOOT_COMPLETED, otherwise this setter is
         * ignored.
         *
         * @param isPersisted True if this task should be persisted across device reboots.
         */
        public abstract Task.Builder setPersisted(boolean isPersisted);

        /**
         * Set the network state your task requires to run. <strong>If the specified network is
         * unavailable your task will not be executed until it becomes available.</strong>
         * <p/>
         * The default for either a periodic or one-off task is
         * {@link com.google.android.gms.gcm.Task#NETWORK_STATE_CONNECTED}. Note that changing this to
         * {@link com.google.android.gms.gcm.Task#NETWORK_STATE_ANY} means there is no guarantee that data will be available
         * when your task executes.
         * <p/>
         * In addition, the only guarantee for connectivity is at the moment of execution - it is
         * possible for the device to lose data shortly after your task begins executing.
         */
        public abstract Task.Builder setRequiredNetwork(int requiredNetworkState);

        /**
         * Set whether your task requires that the device be connected to power in order to
         * execute.
         * <p/>
         * Use this to defer nonessential operations whenever possible. Note that if you set this
         * field and the device is not connected to power <strong>your task will not run</strong>
         * until the device is plugged in.
         * <p/>
         * One way to deal with your task not executing until the constraint is met is to schedule
         * another task without the constraints that is subject to some deadline that you can abide.
         * This task would be responsible for executing your fallback logic.
         */
        public abstract Task.Builder setRequiresCharging(boolean requiresCharging);

        /**
         * Set whichever {@link com.google.android.gms.gcm.GcmTaskService} you implement to execute the logic for this task.
         *
         * @param gcmTaskService Endpoint against which you're scheduling this task.
         */
        public abstract Task.Builder setService(Class<? extends GcmTaskService> gcmTaskService);

        /**
         * Mandatory setter for specifying the tag identifer for this task. This tag will be
         * returned at execution time to your endpoint. See
         * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}
         * <p/>
         * Maximum tag length is 100.
         *
         * @param tag String identifier for this task. Consecutive schedule calls for the same tag
         *            will update any preexisting task with the same tag.
         */
        public abstract Task.Builder setTag(String tag);

        /**
         * Optional setter to specify whether this task should override any preexisting tasks with
         * the same tag. This defaults to false, which means that a new task will not override an
         * existing one.
         *
         * @param updateCurrent True to update the current task with the parameters of the new.
         *                      Default false.
         */
        public abstract Task.Builder setUpdateCurrent(boolean updateCurrent);
    }
}