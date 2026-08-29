/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DroidGuardInitReplyTest {

    @Test
    public void createFromParcel_returnsNullWhenBothFieldsMissing() {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(null, 0);
        parcel.writeParcelable(null, 0);
        parcel.setDataPosition(0);

        assertNull(DroidGuardInitReply.CREATOR.createFromParcel(parcel));
        parcel.recycle();
    }

    @Test
    public void createFromParcel_preservesObjectWhenPfdMissing() {
        Bundle extras = new Bundle();
        extras.putString("flow", "tachyon_registration");

        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(null, 0);
        parcel.writeParcelable(extras, 0);
        parcel.setDataPosition(0);

        DroidGuardInitReply reply = DroidGuardInitReply.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertNotNull(reply);
        assertNull(reply.pfd);
        assertNotNull(reply.object);
        assertEquals("tachyon_registration", ((Bundle) reply.object).getString("flow"));
    }

    @Test
    public void roundTrip_preservesParcelableExtras() {
        Bundle extras = new Bundle();
        extras.putString("clientVersion", "252432031");

        DroidGuardInitReply original = new DroidGuardInitReply(null, extras);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        DroidGuardInitReply restored = DroidGuardInitReply.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertNotNull(restored);
        Parcelable restoredExtras = restored.object;
        assertNotNull(restoredExtras);
        assertEquals("252432031", ((Bundle) restoredExtras).getString("clientVersion"));
    }
}
