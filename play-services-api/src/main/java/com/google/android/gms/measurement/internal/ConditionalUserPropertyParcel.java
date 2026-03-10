/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ConditionalUserPropertyParcel extends AutoSafeParcelable {
    @Field(2)
    public String appId;
    @Field(3)
    public String origin;
    @Field(4)
    public UserAttributeParcel userAttribute;
    @Field(5)
    public long creationTimestamp;
    @Field(6)
    public boolean active;
    @Field(7)
    public String triggerEventName;
    @Field(8)
    public EventParcel timedOutEvent;
    @Field(9)
    public long triggerTimeout;
    @Field(10)
    public EventParcel triggerEvent;
    @Field(11)
    public long timeToLive;
    @Field(12)
    public EventParcel expiredEvent;

    public static final Creator<ConditionalUserPropertyParcel> CREATOR = new AutoCreator<>(ConditionalUserPropertyParcel.class);
}
