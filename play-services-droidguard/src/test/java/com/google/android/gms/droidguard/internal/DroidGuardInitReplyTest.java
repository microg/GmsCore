/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard.internal;

import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DroidGuardInitReplyTest {
    private static final Parcelable TEST_OBJECT = new Parcelable() {
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // The local JVM test exercises nullable-field semantics, not Android's Parcel runtime.
        }
    };

    @Test
    public void fromNullableFields_returnsNullWhenBothFieldsMissing() {
        assertNull(DroidGuardInitReply.fromNullableFields(null, null));
    }

    @Test
    public void fromNullableFields_preservesObjectWhenPfdMissing() {
        DroidGuardInitReply reply = DroidGuardInitReply.fromNullableFields(null, TEST_OBJECT);

        assertNotNull(reply);
        assertNull(reply.pfd);
        assertSame(TEST_OBJECT, reply.object);
    }

    @Test
    public void constructorPreservesParcelableObject() {
        DroidGuardInitReply reply = new DroidGuardInitReply(null, TEST_OBJECT);

        assertSame(TEST_OBJECT, reply.object);
    }
}
