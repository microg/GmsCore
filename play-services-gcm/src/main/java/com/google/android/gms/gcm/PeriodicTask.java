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
 * A periodic task is one that will recur at the specified interval, without needing to be
 * rescheduled.
 * Schedule a task that will recur until the user calls one of
 * {@link com.google.android.gms.gcm.GcmNetworkManager#cancelAllTasks(java.lang.Class<? extends com.google.android.gms.gcm.GcmTaskService>)}, or
 * {@link com.google.android.gms.gcm.GcmNetworkManager#cancelTask(java.lang.String, java.lang.Class<? extends com.google.android.gms.gcm.GcmTaskService>)} with
 * an identifying tag.
 * <p/>
 * Periodic tasks will not be scheduled if their period is below a certain minimum
 * (currently 30 seconds).
 */
@PublicApi
public class PeriodicTask extends com.google.android.gms.gcm.Task {

    protected long mFlexInSeconds;

    protected long mIntervalInSeconds;

    private PeriodicTask(Builder builder) {
        super(builder);
        this.mIntervalInSeconds = builder.periodInSeconds;
        this.mFlexInSeconds = Math.min(builder.flexInSeconds, mIntervalInSeconds);
    }

    private PeriodicTask(Parcel source) {
        super(source);
        mIntervalInSeconds = source.readLong();
        mFlexInSeconds = Math.min(source.readLong(), mIntervalInSeconds);
    }


    /**
     * @return The number of seconds before the end of the period returned via
     * {@link com.google.android.gms.gcm.PeriodicTask#getPeriod()} that this periodic task can be executed early.
     */
    public long getFlex() {
        return mFlexInSeconds;
    }

    /**
     * @return The period for this task. The number of seconds between subsequent executions.
     */
    public long getPeriod() {
        return mIntervalInSeconds;
    }

    /**
     * Insert the task object into the provided bundle for IPC. Use #fromBundle to recreate the
     * object on the other side.
     */
    public void toBundle(Bundle bundle) {
        super.toBundle(bundle);
        bundle.putLong("period", this.mIntervalInSeconds);
        bundle.putLong("period_flex", this.mFlexInSeconds);
    }

    public String toString() {
        return super.toString() + " period=" + this.getPeriod() + " flex=" + this.getFlex();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeLong(this.mIntervalInSeconds);
        parcel.writeLong(this.mFlexInSeconds);
    }

    public static final Creator<PeriodicTask> CREATOR = new Creator<PeriodicTask>() {
        @Override
        public PeriodicTask createFromParcel(Parcel source) {
            return new PeriodicTask(source);
        }

        @Override
        public PeriodicTask[] newArray(int size) {
            return new PeriodicTask[size];
        }
    };

    public static class Builder extends com.google.android.gms.gcm.Task.Builder {
        private long flexInSeconds = -1;
        private long periodInSeconds = -1;

        public Builder() {
            isPersisted = true;
        }

        public PeriodicTask build() {
            return new PeriodicTask(this);
        }

        /**
         * Optional setter for specifying any extra parameters necessary for the task.
         */
        public PeriodicTask.Builder setExtras(Bundle extras) {
            this.extras = extras;
            return this;
        }

        /**
         * Optional setter for specifying how close to the end of the period set in
         * {@link com.google.android.gms.gcm.PeriodicTask.Builder#setPeriod(long)} you are willing to execute.
         * <p/>
         * For example, specifying a period of 30 seconds, with a flex value of 10 seconds
         * will allow the scheduler to determine the best moment between the 20th and 30th
         * second at which to execute your task.
         */
        public PeriodicTask.Builder setFlex(long flexInSeconds) {
            this.flexInSeconds = flexInSeconds;
            return this;
        }

        /**
         * Mandatory setter for creating a periodic task. This specifies that you would like
         * this task to recur at most once every <code>mIntervalInSeconds.</code>
         * <p/>
         * By default you have no control over where within this period the task will execute.
         * If you want to restrict the task to run within a certain timeframe from the end of
         * the period, use {@link com.google.android.gms.gcm.PeriodicTask.Builder#setFlex(long)}
         */
        public PeriodicTask.Builder setPeriod(long periodInSeconds) {
            this.periodInSeconds = periodInSeconds;
            return this;
        }

        /**
         * Optional setter to specify whether this task should be persisted across reboots. This
         * defaults to true for periodic tasks,
         * <p/>
         * Callers <strong>must</strong> hold the permission
         * android.Manifest.permission.RECEIVE_BOOT_COMPLETED, otherwise this setter is
         * ignored.
         *
         * @param isPersisted True if this task should be persisted across device reboots.
         */
        public PeriodicTask.Builder setPersisted(boolean isPersisted) {
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
        public PeriodicTask.Builder setRequiredNetwork(int requiredNetworkState) {
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
        public PeriodicTask.Builder setRequiresCharging(boolean requiresCharging) {
            this.requiresCharging = requiresCharging;
            return this;
        }

        /**
         * <p>Set whichever {@link com.google.android.gms.gcm.GcmTaskService} you implement to execute the logic for this task.</p>
         *
         * @param gcmTaskService Endpoint against which you're scheduling this task.
         */
        public PeriodicTask.Builder setService(Class<? extends GcmTaskService> gcmTaskService) {
            this.gcmTaskService = gcmTaskService.getName();
            return this;
        }

        /**
         * Mandatory setter for specifying the tag identifer for this task. This tag will be
         * returned at execution time to your endpoint. See
         * {@link com.google.android.gms.gcm.GcmTaskService#onRunTask(com.google.android.gms.gcm.TaskParams)}
         * <p/>
         * Maximum tag length is 100.
         *
         * @param tag String identifier for this task. Consecutive schedule calls for the same
         *            tag will update any preexisting task with the same tag.
         */
        public PeriodicTask.Builder setTag(String tag) {
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
        public PeriodicTask.Builder setUpdateCurrent(boolean updateCurrent) {
            this.updateCurrent = updateCurrent;
            return this;
        }
    }
}