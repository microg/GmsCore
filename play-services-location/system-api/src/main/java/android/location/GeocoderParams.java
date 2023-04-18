/*
 * SPDX-FileCopyrightText: 2010, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package android.location;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Locale;

/**
 * This class contains extra parameters to pass to an IGeocodeProvider
 * implementation from the Geocoder class.  Currently this contains the
 * language, country and variant information from the Geocoder's locale
 * as well as the Geocoder client's package name for geocoder server
 * logging.  This information is kept in a separate class to allow for
 * future expansion of the IGeocodeProvider interface.
 *
 * @hide
 */
public class GeocoderParams implements Parcelable {
    /**
     * This object is only constructed by the Geocoder class
     *
     * @hide
     */
    public GeocoderParams(Context context, Locale locale) {
    }

    /**
     * Returns the client UID.
     */
    @RequiresApi(33)
    public int getClientUid() {
        return 0;
    }

    /**
     * returns the package name of the Geocoder's client
     */
    public String getClientPackage() {
        return null;
    }

    /**
     * Returns the client attribution tag.
     */
    @RequiresApi(33)
    public @Nullable String getClientAttributionTag() {
        return null;
    }

    /**
     * returns the Geocoder's locale
     */
    public Locale getLocale() {
        return null;
    }

    public static final Parcelable.Creator<GeocoderParams> CREATOR =
            new Parcelable.Creator<GeocoderParams>() {
                public GeocoderParams createFromParcel(Parcel in) {
                    return null;
                }

                public GeocoderParams[] newArray(int size) {
                    return null;
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
    }
}
