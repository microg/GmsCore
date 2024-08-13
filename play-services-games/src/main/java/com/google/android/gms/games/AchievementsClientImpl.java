/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.internal.IGamesCallbacks;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.microg.gms.common.api.ReturningGoogleApiCall;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AchievementsClientImpl extends GoogleApi<Games.GamesOptions> implements AchievementsClient {

    public AchievementsClientImpl(Context context, Games.GamesOptions options) {
        super(context, Games.API, options);
        Log.d(Games.TAG, "AchievementsClientImpl: options: " + options);
    }

    @Override
    public Task<Intent> getAchievementsIntent() {
        return scheduleTask((ReturningGoogleApiCall<Intent, GamesGmsClientImpl>) (client) -> client.getServiceInterface().getAchievementsIntent());
    }

    @Override
    public void increment(String id, int numSteps) {
        Tasks.withTimeout(incrementImmediate(id, numSteps), 5, TimeUnit.SECONDS);
    }

    @Override
    public Task<Boolean> incrementImmediate(String id, int numSteps) {
        return scheduleTask((ReturningGoogleApiCall<Boolean, GamesGmsClientImpl>) (client) -> {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicBoolean atomicBoolean = new AtomicBoolean();
            client.getServiceInterface().incrementAchievement(new IGamesCallbacks.Default() {
                @Override
                public void onAchievementUpdated(int statusCode, String achievementId) throws RemoteException {
                    super.onAchievementUpdated(statusCode, achievementId);
                    atomicBoolean.set(statusCode == 0);
                    countDownLatch.countDown();
                }
            }, id, numSteps, null, null);
            countDownLatch.await(5, TimeUnit.SECONDS);
            return atomicBoolean.get();
        });
    }

    @Override
    public Task<AnnotatedData<AchievementBuffer>> load(boolean forceReload) {
        return scheduleTask((ReturningGoogleApiCall<AnnotatedData<AchievementBuffer>, GamesGmsClientImpl>) (client) -> {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicReference<AnnotatedData<AchievementBuffer>> annotatedDataAtomicReference = new AtomicReference<>();
            client.getServiceInterface().loadAchievementsV2(new IGamesCallbacks.Default() {
                @Override
                public void onAchievementsLoaded(DataHolder data) throws RemoteException {
                    super.onAchievementsLoaded(data);
                    AchievementBuffer achievementBuffer = new AchievementBuffer(data);
                    AnnotatedData<AchievementBuffer> annotatedData = new AnnotatedData<>(achievementBuffer, false);
                    annotatedDataAtomicReference.set(annotatedData);
                    countDownLatch.countDown();
                }
            }, forceReload);
            countDownLatch.await(5, TimeUnit.SECONDS);
            return annotatedDataAtomicReference.get();
        });
    }

    @Override
    public void reveal(String id) {
        Tasks.withTimeout(revealImmediate(id), 5, TimeUnit.SECONDS);
    }

    @Override
    public Task<Void> revealImmediate(String id) {
        return scheduleTask((ReturningGoogleApiCall<Void, GamesGmsClientImpl>) (client) -> {
            client.getServiceInterface().revealAchievement(null, id, null, null);
            return Void.TYPE.newInstance();
        });
    }

    @Override
    public void setSteps(String id, int numSteps) {
        Tasks.withTimeout(setStepsImmediate(id, numSteps), 5, TimeUnit.SECONDS);
    }

    @Override
    public Task<Boolean> setStepsImmediate(String id, int numSteps) {
        return scheduleTask((ReturningGoogleApiCall<Boolean, GamesGmsClientImpl>) (client) -> {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicBoolean atomicBoolean = new AtomicBoolean();
            client.getServiceInterface().setAchievementSteps(new IGamesCallbacks.Default() {
                @Override
                public void onAchievementUpdated(int statusCode, String achievementId) throws RemoteException {
                    super.onAchievementUpdated(statusCode, achievementId);
                    atomicBoolean.set(statusCode == 0);
                    countDownLatch.countDown();
                }
            }, id, numSteps, null, null);
            countDownLatch.await(5, TimeUnit.SECONDS);
            return atomicBoolean.get();
        });
    }

    @Override
    public void unlock(String id) {
        Tasks.withTimeout(unlockImmediate(id), 5, TimeUnit.SECONDS);
    }

    @Override
    public Task<Void> unlockImmediate(String id) {
        return scheduleTask((ReturningGoogleApiCall<Void, GamesGmsClientImpl>) (client) -> {
            client.getServiceInterface().unlockAchievement(null, id, null, null);
            return Void.TYPE.newInstance();
        });
    }
}
