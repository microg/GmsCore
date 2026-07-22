/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.app.PendingIntent;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.location.ILocationCallback;
import com.google.android.gms.location.ILocationListener;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class LocationReceiver extends AutoSafeParcelable {
    public static final int TYPE_LISTENER = 1;
    public static final int TYPE_CALLBACK = 2;
    public static final int TYPE_PENDING_INTENT = 3;
    public static final int TYPE_STATUS_CALLBACK = 4;
    public static final int TYPE_AVAILABILITY_STATUS_CALLBACK = 5;

    @Field(1)
    public int type;
    @Field(2)
    @Nullable
    public IBinder oldBinderReceiver;
    @Field(3)
    @Nullable
    public IBinder binderReceiver;
    @Field(4)
    @Nullable
    public PendingIntent pendingIntentReceiver;
    @Field(5)
    @Nullable
    public String moduleId;
    @Field(6)
    @Nullable
    public String listenerId;

    private LocationReceiver() {
    }

    public LocationReceiver(@NonNull ILocationListener listener) {
        this(listener, null);
    }

    public LocationReceiver(@NonNull ILocationListener listener, @Nullable String listenerId) {
        type = TYPE_LISTENER;
        binderReceiver = listener.asBinder();
        this.listenerId = listenerId;
    }

    public LocationReceiver(@NonNull ILocationCallback callback) {
        this(callback, null);
    }

    public LocationReceiver(@NonNull ILocationCallback callback, @Nullable String listenerId) {
        type = TYPE_CALLBACK;
        binderReceiver = callback.asBinder();
        this.listenerId = listenerId;
    }

    public LocationReceiver(@NonNull PendingIntent pendingIntent) {
        this(pendingIntent, null);
    }

    public LocationReceiver(@NonNull PendingIntent pendingIntent, @Nullable String listenerId) {
        type = TYPE_PENDING_INTENT;
        pendingIntentReceiver = pendingIntent;
        this.listenerId = listenerId;
    }

    public LocationReceiver(@NonNull ILocationStatusCallback callback) {
        this(callback, null);
    }

    public LocationReceiver(@NonNull ILocationStatusCallback callback, @Nullable String listenerId) {
        type = TYPE_STATUS_CALLBACK;
        binderReceiver = callback.asBinder();
        this.listenerId = listenerId;
    }

    public LocationReceiver(@NonNull ILocationAvailabilityStatusCallback callback) {
        type = TYPE_AVAILABILITY_STATUS_CALLBACK;
        binderReceiver = callback.asBinder();
    }

    public ILocationListener getListener() {
        if (type != TYPE_LISTENER) throw new IllegalStateException();
        return ILocationListener.Stub.asInterface(binderReceiver);
    }

    public ILocationCallback getCallback() {
        if (type != TYPE_CALLBACK) throw new IllegalStateException();
        return ILocationCallback.Stub.asInterface(binderReceiver);
    }

    public ILocationStatusCallback getStatusCallback() {
        if (type != TYPE_STATUS_CALLBACK) throw new IllegalStateException();
        return ILocationStatusCallback.Stub.asInterface(binderReceiver);
    }

    public ILocationAvailabilityStatusCallback getAvailabilityStatusCallback() {
        if (type != TYPE_AVAILABILITY_STATUS_CALLBACK) throw new IllegalStateException();
        return ILocationAvailabilityStatusCallback.Stub.asInterface(binderReceiver);
    }

    public static final Creator<LocationReceiver> CREATOR = new AutoCreator<>(LocationReceiver.class);
}
