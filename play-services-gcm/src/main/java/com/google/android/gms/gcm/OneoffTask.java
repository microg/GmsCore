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

import org.microg.gms.common.PublicApi;

/**
 * A task that will execute once,at some point within the specified window.
 * If one of {@link com.google.android.gms.gcm.GcmNetworkManager#cancelTask(java.lang.String, java.lang.Class<? extends com.google.android.gms.gcm.GcmTaskService>)} or
 * {@link com.google.android.gms.gcm.GcmNetworkManager#cancelAllTasks(java.lang.Class<? extends com.google.android.gms.gcm.GcmTaskService>)} is called before this
 * executes it will be cancelled.
 * <p/>
 * Note that you can request a one-off task to be executed at any point in the future, but to
 * prevent abuse the scheduler will only set an alarm at a minimum of 30 seconds in the
 * future. Your task can still be run earlier than this if some network event occurs to wake up
 * the scheduler.
 */
@PublicApi
public class OneoffTask extends com.google.android.gms.gcm.Task {
    private final long windowStart;
    private final long windowEnd;

    private OneoffTask(Builder builder) {
        super(builder);
        this.windowStart = builder.windowStart;
        this.windowEnd = builder.windowEnd;
    }

    private OneoffTask(Parcel source) {
        super(source);
        this.windowStart = source.readLong();
        this.windowEnd = source.readLong();
    }

    /**
     * @return The number of seconds from now by which this task must have executed.
     */
    public long getWindowEnd() {
        return windowEnd;
    }

    /**
     * @return The number of seconds from now at which this task is eligible for execution.
     */
    public long getWindowStart() {
        return windowStart;
    }

    /**
     * Insert the task object into the provided bundle for IPC. Use #fromBundle to recreate the
     * object on the other side.
     */
    public void toBundle(Bundle bundle) {
        super.toBundle(bundle);
        bundle.putLong("window_start", this.windowStart);
        bundle.putLong("window_end", this.windowEnd);
    }

    public String toString() {
        return super.toString()
                + " windowStart=" + this.getWindowStart()
                + " windowEnd=" + this.getWindowEnd();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeLong(this.windowStart);
        parcel.writeLong(this.windowEnd);
    }

    public static final Creator<OneoffTask> CREATOR = new Creator<OneoffTask>() {
        @Override
        public OneoffTask createFromParcel(Parcel source) {
            return new OneoffTask(source);
        }

        @Override
        public OneoffTask[] newArray(int size) {
            return new OneoffTask[size];
        }
    };

    public static class Builder extends Task.Builder {
        private long windowStart = -1;
        private long windowEnd = -1;

        public Builder() {
            this.isPersisted = false;
        }

        public OneoffTask build() {
            return new OneoffTask(this);
        }

        /**
         * Mandatory setter for creating a one-off task. You specify the earliest point in
         * time in the future from which your task might start executing, as well as the
         * latest point in time in the future at which your task must have executed.
         *
         * @param windowStartDelaySeconds Earliest point from which your task is eligible to
         *                                run.
         * @param windowEndDelaySeconds   Latest point at which your task must be run.
         */
        public OneoffTask.Builder setExecutionWindow(long windowStartDelaySeconds, long windowEndDelaySeconds) {
            this.windowEnd = windowEndDelaySeconds;
            this.windowStart = windowStartDelaySeconds;
            return this;
        }

        /**
         * Optional setter for specifying any extra parameters necessary for the task.
         */
        public OneoffTask.Builder setExtras(Bundle extras) {
            this.extras = extras;
            return this;
        }

        /**
         * Optional setter to specify whether this task should be persisted across reboots..
         * Callers <strong>must</strong> hold the permission
         * android.Manifest.permission.RECEIVE_BOOT_COMPLETED, otherwise this setter is
         * ignored.
         *
         * @param isPersisted True if this task should be persisted across device reboots.
         */
        public OneoffTask.Builder setPersisted(boolean isPersisted) {
            this.isPersisted = isPersisted;
            return this;
        }

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
        public OneoffTask.Builder setRequiredNetwork(int requiredNetworkState) {
            this.requiredNetworkState = requiredNetworkState;
            return this;
        }

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
        public OneoffTask.Builder setRequiresCharging(boolean requiresCharging) {
            this.requiresCharging = requiresCharging;
            return this;
        }

        /**
         * Set whichever {@link com.google.android.gms.gcm.GcmTaskService} you implement to execute the logic for this task.
         *
         * @param gcmTaskService Endpoint against which you're scheduling this task.
         */
        public OneoffTask.Builder setService(Class<? extends GcmTaskService> gcmTaskService) {
            this.gcmTaskService = gcmTaskService.getName();
            return this;
        }

        /**
         * Mandatory setter for specifying the tag identifer for this task. This tag will be
         * returned at execution time to your endpoint. See
         * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}
         * Maximum tag length is 100.<
         *
         * @param tag String identifier for this task. Consecutive schedule calls for the same
         *            tag will update any preexisting task with the same tag.
         */
        public OneoffTask.Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Optional setter to specify whether this task should override any preexisting tasks
         * with the same tag. This defaults to false, which means that a new task will not
         * override an existing one.
         *
         * @param updateCurrent True to update the current task with the parameters of the new.
         *                      Default false.
         */
        public OneoffTask.Builder setUpdateCurrent(boolean updateCurrent) {
            this.updateCurrent = updateCurrent;
            return this;
        }

    }

}