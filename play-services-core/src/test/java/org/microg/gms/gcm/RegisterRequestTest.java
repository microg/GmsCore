/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm;

import com.google.android.gms.BuildConfig;

import org.junit.Test;
import org.microg.gms.common.HttpFormClient.RequestContent;
import org.microg.gms.common.HttpFormClient.RequestHeader;

import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RegisterRequestTest {
    @Test
    public void gcmVersionMatchesBuildAndIsSentAsHeaderAndFormField() throws Exception {
        RegisterRequest request = new RegisterRequest();
        Field field = RegisterRequest.class.getDeclaredField("gcmVersion");
        field.setAccessible(true);

        assertEquals(BuildConfig.VERSION_CODE, field.getInt(request));

        RequestHeader header = field.getAnnotation(RequestHeader.class);
        assertNotNull(header);
        assertArrayEquals(new String[]{"gcm_ver"}, header.value());

        RequestContent content = field.getAnnotation(RequestContent.class);
        assertNotNull(content);
        assertArrayEquals(new String[]{"gcm_ver"}, content.value());
    }
}
