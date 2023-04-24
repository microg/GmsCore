/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.ClientIdentity;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;

import java.util.*;

/**
 * The request object for apps to get notified when user's activity changes.
 */
@PublicApi
public class ActivityTransitionRequest extends AutoSafeParcelable {
    private static final String EXTRA = "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_REQUEST";

    @Field(value = 1, subClass = ActivityTransition.class)
    private List<ActivityTransition> activityTransitions;
    @Field(2)
    private String tag;
    @Field(value = 3, subClass = ClientIdentity.class)
    private List<ClientIdentity> clients;
    @Field(4)
    private String contextAttributionTag;

    /**
     * The comparator used to determine if two transitions are the same. It's different from {@link ActivityTransition#equals(Object)} because in the future we
     * may add latency to activity transition and the latency value should not be compared against.
     */
    public static final Comparator<ActivityTransition> IS_SAME_TRANSITION = new Comparator<ActivityTransition>() {
        @Override
        public int compare(ActivityTransition o1, ActivityTransition o2) {
            int res = Integer.compare(o1.getActivityType(), o2.getActivityType());
            if (res != 0) return res;
            res = Integer.compare(o1.getTransitionType(), o2.getTransitionType());
            return res;
        }
    };

    private ActivityTransitionRequest() {
    }

    /**
     * Creates an {@link ActivityTransitionRequest} object by specifying a list of interested activity transitions.
     *
     * @param transitions a list of interested activity transitions
     * @throws NullPointerException     if {@code transitions} is {@code null}
     * @throws IllegalArgumentException if {@code transitions} is an empty list or if there are duplicated transitions in this list
     */
    public ActivityTransitionRequest(List<ActivityTransition> transitions) {
        if (transitions == null) throw new NullPointerException("transitions can't be null");
        if (transitions.isEmpty()) throw new IllegalArgumentException("transitions can't be empty.");
        Set<ActivityTransition> set = new TreeSet<ActivityTransition>(IS_SAME_TRANSITION);
        set.addAll(transitions);
        if (transitions.size() != set.size()) throw new IllegalArgumentException("Found duplicated transition");
        this.activityTransitions = Collections.unmodifiableList(transitions);
    }

    /**
     * Serializes this request to the given intent.
     *
     * @param intent the intent to serailize this object to
     */
    public void serializeToIntentExtra(Intent intent) {
        intent.putExtra(EXTRA, SafeParcelUtil.asByteArray(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivityTransitionRequest that = (ActivityTransitionRequest) o;

        if (!Objects.equals(activityTransitions, that.activityTransitions)) return false;
        if (!Objects.equals(tag, that.tag)) return false;
        if (!Objects.equals(clients, that.clients)) return false;
        return Objects.equals(contextAttributionTag, that.contextAttributionTag);
    }

    @Override
    public int hashCode() {
        int result = activityTransitions != null ? activityTransitions.hashCode() : 0;
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (clients != null ? clients.hashCode() : 0);
        result = 31 * result + (contextAttributionTag != null ? contextAttributionTag.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityTransitionRequest [mTransitions=" + activityTransitions + ", mTag=" + tag + ", mClients" + clients + ", mAttributionTag=" + contextAttributionTag + "]";
    }

    public static final Creator<ActivityTransitionRequest> CREATOR = new AutoCreator<>(ActivityTransitionRequest.class);
}
