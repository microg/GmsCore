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
package com.google.android.gms.gcm

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import org.microg.gms.common.PublicApi

/**
 * Encapsulates the parameters of a task that you will schedule on the
 * [com.google.android.gms.gcm.GcmNetworkManager].
 *
 *
 * Construct instances of either [com.google.android.gms.gcm.PeriodicTask] or
 * [com.google.android.gms.gcm.OneoffTask] with the desired parameters/behaviour and
 * schedule them using [com.google.android.gms.gcm.GcmNetworkManager.schedule].
 */
@PublicApi
abstract class Task : Parcelable {
    /**
     * @return The [com.google.android.gms.gcm.GcmTaskService] component that this task
     * will execute on.
     */
    val serviceName: String?

    /**
     * @return The String identifier for this task, that is returned to
     * [com.google.android.gms.gcm.GcmTaskService.onRunTask]
     * when this task executes.
     */
    val tag: String?

    /**
     * @return Whether or not this task will update a pre-existing task in the scheduler queue.
     */
    val isUpdateCurrent: Boolean

    /**
     * @return Whether this task will be persisted across devices restarts or Google Play Services
     * crashes.
     */
    val isPersisted: Boolean

    /**
     * If the specified network is unavailable, your task **will not be run** until
     * it is.
     *
     * @return The network type that this task requires in order to run. See the NETWORK_TYPE_*
     * flavours for an explanation of what this value can be.
     */
    val requiredNetwork: Int

    /**
     * If the device is not charging and this is set to true, your task **will not be run
     ** *  until it is.
     *
     * @return Whether or not this task depends on the device being connected to power in order to
     * execute.
     */
    val requiresCharging: Boolean

    /**
     * @return The extra parameters for the task set by the client.
     */
    val extras: Bundle?

    internal constructor(builder: Builder) {
        serviceName = builder.gcmTaskService
        tag = builder.tag
        isUpdateCurrent = builder.updateCurrent
        isPersisted = builder.isPersisted
        requiredNetwork = builder.requiredNetworkState
        requiresCharging = builder.requiresCharging
        extras = builder.extras
    }

    internal constructor(`in`: Parcel) {
        serviceName = `in`.readString()
        tag = `in`.readString()
        isUpdateCurrent = `in`.readInt() == 1
        isPersisted = `in`.readInt() == 1
        requiredNetwork = NETWORK_STATE_ANY
        requiresCharging = false
        extras = null
    }

    override fun describeContents(): Int {
        return 0
    }

    open fun toBundle(bundle: Bundle) {
        bundle.putString("tag", tag)
        bundle.putBoolean("update_current", isUpdateCurrent)
        bundle.putBoolean("persisted", isPersisted)
        bundle.putString("service", serviceName)
        bundle.putInt("requiredNetwork", requiredNetwork)
        bundle.putBoolean("requiresCharging", requiresCharging)
        bundle.putBundle("retryStrategy", null) // TODO
        bundle.putBundle("extras", extras)
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(serviceName)
        parcel.writeString(tag)
        parcel.writeInt(if (isUpdateCurrent) 1 else 0)
        parcel.writeInt(if (isPersisted) 1 else 0)
    }

    /**
     *
     * Builder object to construct these tasks before sending them to the network manager. Use
     * either [com.google.android.gms.gcm.PeriodicTask.Builder] or
     * [com.google.android.gms.gcm.Task.Builder]
     */
    abstract class Builder {
        @JvmField
        var extras: Bundle? = null
        @JvmField
        var gcmTaskService: String? = null
        @JvmField
        var isPersisted = false
        @JvmField
        var requiredNetworkState = 0
        @JvmField
        var requiresCharging = false
        @JvmField
        var tag: String? = null
        @JvmField
        var updateCurrent = false
        abstract fun build(): Task?

        /**
         * Optional setter for specifying any extra parameters necessary for the task.
         */
        abstract fun setExtras(extras: Bundle?): Builder?

        /**
         * Optional setter to specify whether this task should be persisted across reboots. This
         * defaults to true for periodic tasks, and is not supported for one-off tasks.
         *
         *
         * Callers **must** hold the permission
         * android.Manifest.permission.RECEIVE_BOOT_COMPLETED, otherwise this setter is
         * ignored.
         *
         * @param isPersisted True if this task should be persisted across device reboots.
         */
        abstract fun setPersisted(isPersisted: Boolean): Builder?

        /**
         * Set the network state your task requires to run. **If the specified network is
         * unavailable your task will not be executed until it becomes available.**
         *
         *
         * The default for either a periodic or one-off task is
         * [com.google.android.gms.gcm.Task.NETWORK_STATE_CONNECTED]. Note that changing this to
         * [com.google.android.gms.gcm.Task.NETWORK_STATE_ANY] means there is no guarantee that data will be available
         * when your task executes.
         *
         *
         * In addition, the only guarantee for connectivity is at the moment of execution - it is
         * possible for the device to lose data shortly after your task begins executing.
         */
        abstract fun setRequiredNetwork(requiredNetworkState: Int): Builder?

        /**
         * Set whether your task requires that the device be connected to power in order to
         * execute.
         *
         *
         * Use this to defer nonessential operations whenever possible. Note that if you set this
         * field and the device is not connected to power **your task will not run**
         * until the device is plugged in.
         *
         *
         * One way to deal with your task not executing until the constraint is met is to schedule
         * another task without the constraints that is subject to some deadline that you can abide.
         * This task would be responsible for executing your fallback logic.
         */
        abstract fun setRequiresCharging(requiresCharging: Boolean): Builder?

        /**
         * Set whichever [com.google.android.gms.gcm.GcmTaskService] you implement to execute the logic for this task.
         *
         * @param gcmTaskService Endpoint against which you're scheduling this task.
         */
        abstract fun setService(gcmTaskService: Class<out GcmTaskService?>?): Builder?

        /**
         * Mandatory setter for specifying the tag identifer for this task. This tag will be
         * returned at execution time to your endpoint. See
         * [com.google.android.gms.gcm.GcmTaskService.onRunTask]
         *
         *
         * Maximum tag length is 100.
         *
         * @param tag String identifier for this task. Consecutive schedule calls for the same tag
         * will update any preexisting task with the same tag.
         */
        abstract fun setTag(tag: String?): Builder?

        /**
         * Optional setter to specify whether this task should override any preexisting tasks with
         * the same tag. This defaults to false, which means that a new task will not override an
         * existing one.
         *
         * @param updateCurrent True to update the current task with the parameters of the new.
         * Default false.
         */
        abstract fun setUpdateCurrent(updateCurrent: Boolean): Builder?

        init {
            throw UnsupportedOperationException()
        }
    }

    companion object {
        /**
         *
         * The maximum size allowed for extras bundle in bytes.
         *
         */
        const val EXTRAS_LIMIT_BYTES = 10240

        /**
         *
         * Specify using [com.google.android.gms.gcm.Task.Builder.setRequiredNetwork]
         * that your task will execute [...] of whether network is available.
         *
         */
        const val NETWORK_STATE_ANY = 2

        /**
         *
         * Specify using [com.google.android.gms.gcm.Task.Builder.setRequiredNetwork]
         * that your task will only execute if [...] sort of data connection is available -
         * either metered or unmetered. **This is the default.**
         */
        const val NETWORK_STATE_CONNECTED = 0

        /**
         *
         * Specify using [com.google.android.gms.gcm.Task.Builder.setRequiredNetwork]
         * that your task will only execute if there is an unmetered network connection available.
         *
         */
        const val NETWORK_STATE_UNMETERED = 1
        protected const val UNINITIALIZED: Long = -1
    }
}