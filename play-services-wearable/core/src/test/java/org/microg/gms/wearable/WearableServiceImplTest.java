/*
 * Copyright (C) 2023 microG Project Team
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

package org.microg.gms.wearable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.OpenChannelResponse;
import com.google.android.gms.wearable.consent.TermsOfServiceActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class WearableServiceImplTest {

    private WearableServiceImpl wearableService;

    @Mock
    private WearableImpl wearable;

    @Mock
    private IWearableCallbacks callbacks;

    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;
        wearableService = new WearableServiceImpl(context, wearable, "com.test.app");
    }

    @Test
    public void testPutConfig_whenTosNotAccepted_shouldLaunchTosActivity() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("WearablePrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("tos_accepted", false).apply();

        ConnectionConfiguration config = new ConnectionConfiguration("test", "test", 0, 0, false);
        wearableService.putConfig(callbacks, config);

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(TermsOfServiceActivity.class.getCanonicalName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void testPutConfig_whenTosAccepted_shouldCreateConnection() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("WearablePrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("tos_accepted", true).apply();

        ConnectionConfiguration config = new ConnectionConfiguration("test", "test", 0, 0, false);
        wearableService.putConfig(callbacks, config);

        verify(callbacks).onStatus(Status.SUCCESS);
    }

    @Test
    public void testOpenChannel() throws Exception {
        wearableService.openChannel(callbacks, "test_node", "/test_path");

        verify(callbacks).onOpenChannelResponse(any(OpenChannelResponse.class));
    }
}
