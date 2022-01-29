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

package com.google.android.gms.location.places.internal;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlaceImpl extends AutoSafeParcelable implements Place {
    @SafeParceled(1000)
    private final int versionCode = 2;
    @SafeParceled(1)
    public String id;
    @SafeParceled(2)
    public Bundle addressComponents;
    @SafeParceled(4)
    public LatLng latLng;
    @SafeParceled(5)
    public float levelNumber;
    @SafeParceled(6)
    public LatLngBounds viewport;
    @SafeParceled(7)
    public String timezoneId;
    @SafeParceled(8)
    public Uri websiteUri;
    @SafeParceled(9)
    public boolean isPermanentlyClosed;
    @SafeParceled(10)
    public float rating;
    @SafeParceled(11)
    public int priceLevel;
    @SafeParceled(12)
    public long timestampSecs;
    @SafeParceled(value = 13, subClass = Integer.class)
    public List<Integer> typesDeprecated = new ArrayList<Integer>();
    @SafeParceled(14)
    public String address;
    @SafeParceled(15)
    public String phoneNumber;
    @SafeParceled(16)
    public String regularOpenHours;
    @SafeParceled(value = 17, subClass = String.class)
    public List<String> attributions = new ArrayList<String>();
    @SafeParceled(19)
    public String name;
    @SafeParceled(value = 20, subClass = Integer.class)
    public List<Integer> placeTypes = new ArrayList<Integer>();

    @Override
    public CharSequence getAddress() {
        return address;
    }

    @Override
    public CharSequence getAttributions() {
        return Arrays.toString(attributions.toArray());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LatLng getLatLng() {
        return latLng;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public CharSequence getName() {
        return name;
    }

    @Override
    public CharSequence getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public List<Integer> getPlaceTypes() {
        return placeTypes;
    }

    @Override
    public int getPriceLevel() {
        return priceLevel;
    }

    @Override
    public float getRating() {
        return rating;
    }

    @Override
    public LatLngBounds getViewport() {
        return viewport;
    }

    @Override
    public Uri getWebsiteUri() {
        return websiteUri;
    }

    @Override
    public Place freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    public static final Creator<PlaceImpl> CREATOR = new AutoSafeParcelable.AutoCreator<PlaceImpl>(PlaceImpl.class);
}
