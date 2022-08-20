/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.common;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.microg.gms.common.PublicApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The transport between the authenticator and the client.
 */
@PublicApi
public enum Transport implements Parcelable {
    BLUETOOTH_CLASSIC("bt"),
    BLUETOOTH_LOW_ENERGY("ble"),
    NFC("nfc"),
    USB("usb"),
    INTERNAL("internal"),
    @PublicApi(exclude = true)
    CABLE("cable");

    private final String value;

    Transport(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
    }

    @PublicApi(exclude = true)
    public static Transport fromString(String transport) throws UnsupportedTransportException {
        for (Transport value : values()) {
            if (value.value.equals(transport)) return value;
        }
        throw new UnsupportedTransportException("Transport " + transport + " not supported");
    }

    @PublicApi(exclude = true)
    public static List<Transport> parseTransports(JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) return null;
        Set<Transport> set = new HashSet<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String transport = jsonArray.getString(i);
            if (transport != null && !transport.isEmpty()) {
                try {
                    set.add(fromString(transport));
                } catch (UnsupportedTransportException e) {
                    Log.w("Transport", "Ignoring unrecognized transport " + transport);
                }
            }
        }
        return new ArrayList<>(set);
    }

    public static Creator<Transport> CREATOR = new Creator<Transport>() {
        @Override
        public Transport createFromParcel(Parcel source) {
            try {
                return Transport.fromString(source.readString());
            } catch (UnsupportedTransportException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Transport[] newArray(int size) {
            return new Transport[size];
        }
    };

    /**
     * Exception thrown when an unsupported or unrecognized transport is encountered.
     */
    public static class UnsupportedTransportException extends Exception {
        public UnsupportedTransportException(String errorMessage) {
            super(errorMessage);
        }
    }
}
