/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import java.util.concurrent.TimeUnit

@RequiresApi(21)
object MandatoryAppsJob {

    private const val IMMEDIATE_JOB_ID = 0x6d616e64
    private const val PERIODIC_JOB_ID = IMMEDIATE_JOB_ID + 1
    private const val CONSTRAINED_JOB_BASE_ID = PERIODIC_JOB_ID + 1

    private const val EXTRA_CONSTRAINTS = "constraints"

    private val PERIOD_MILLIS = TimeUnit.HOURS.toMillis(6)

    fun schedule(context: Context) {
        val scheduler = context.jobScheduler()

        scheduler.schedule(buildImmediateJob(context))
        scheduler.schedule(buildPeriodicJob(context))
    }

    fun scheduleImmediate(context: Context) {
        context.jobScheduler().schedule(buildImmediateJob(context))
    }

    fun scheduleUnder(context: Context, constraints: InstallConstraints) {
        context.jobScheduler().schedule(buildConstrainedJob(context, constraints))
    }

    fun cancel(context: Context) {
        val scheduler = context.jobScheduler()

        scheduler.cancel(IMMEDIATE_JOB_ID)
        scheduler.cancel(PERIODIC_JOB_ID)

        for (mask in 0 until InstallConstraints.MASK_COUNT) {
            scheduler.cancel(CONSTRAINED_JOB_BASE_ID + mask)
        }
    }

    fun constraintsOf(extras: PersistableBundle): InstallConstraints =
        InstallConstraints.fromMask(extras.getInt(EXTRA_CONSTRAINTS, 0))

    private fun buildImmediateJob(context: Context): JobInfo {
        val builder = jobBuilder(context, IMMEDIATE_JOB_ID)

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setExpedited(true)
        }

        return builder.build()
    }

    private fun buildPeriodicJob(context: Context): JobInfo =
        jobBuilder(context, PERIODIC_JOB_ID)
            .setPeriodic(PERIOD_MILLIS)
            .setPersisted(true)
            .build()

    private fun buildConstrainedJob(context: Context, constraints: InstallConstraints): JobInfo {
        val extras = PersistableBundle()
        extras.putInt(EXTRA_CONSTRAINTS, constraints.toMask())

        return JobInfo.Builder(
            CONSTRAINED_JOB_BASE_ID + constraints.toMask(),
            ComponentName(context, MandatoryAppsJobService::class.java)
        )
            .setRequiredNetworkType(constraints.networkType())
            .setRequiresCharging(constraints.requiresCharging)
            .setRequiresDeviceIdle(constraints.requiresDeviceIdle)
            .setExtras(extras)
            .setPersisted(true)
            .build()
    }

    private fun jobBuilder(context: Context, jobId: Int): JobInfo.Builder =
        JobInfo.Builder(jobId, ComponentName(context, MandatoryAppsJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

    private fun InstallConstraints.networkType(): Int =
        if (requiresUnmeteredNetwork) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY

    private fun Context.jobScheduler(): JobScheduler =
        getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
}
