/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.utils;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastSafeParcelableJsonResponse;
import com.google.android.gms.common.server.response.FastParser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public abstract class BinarySerializableFastSafeParcelableJson extends FastSafeParcelableJsonResponse {
    @SuppressWarnings("unchecked")
    protected static <T extends BinarySerializableFastSafeParcelableJson> SafeParcelableCreatorAndWriter<T> findCreator(java.lang.Class<T> tClass) {
        java.lang.Class<?> upmostClass = tClass;
        while (upmostClass.getEnclosingClass() != null) upmostClass = upmostClass.getEnclosingClass();
        String upmostClassName = upmostClass.getName();
        int idx = upmostClassName.lastIndexOf('.');
        String packagePrefix = idx > 0 ? upmostClassName.substring(0, idx + 1) : "";
        String creatorClassName = packagePrefix + tClass.getSimpleName() + "$000Creator";
        try {
            return (SafeParcelableCreatorAndWriter<T>) java.lang.Class.forName(creatorClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No Creator found for " + tClass.getName(), e);
        }
    }

    public final void av(byte[] bytes) {
        try {
            new FastParser<BinarySerializableFastSafeParcelableJson>()
                    .parse(new ByteArrayInputStream(bytes), this);
        } catch (FastParser.ParseException e) {
            throw new IllegalArgumentException("Unable to parse JSON payload", e);
        }
    }

    public final byte[] aw() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }
}
