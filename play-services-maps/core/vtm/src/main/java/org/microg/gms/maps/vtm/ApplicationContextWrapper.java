/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.vtm;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;

import org.microg.gms.common.Constants;

public class ApplicationContextWrapper extends ContextWrapper {
    private Context applicationContext;

    public ApplicationContextWrapper(Context base, Context applicationContext) {
        super(base);
        this.applicationContext = applicationContext;
    }

    public static ApplicationContextWrapper gmsContextWithAttachedApplicationContext(Context applicationContext) {
        try {
            Context context = applicationContext.createPackageContext(Constants.GMS_PACKAGE_NAME, CONTEXT_INCLUDE_CODE & CONTEXT_IGNORE_SECURITY);
            return new ApplicationContextWrapper(context, applicationContext);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ApplicationContextWrapper matchingApplicationContext(Context context) {
        return new ApplicationContextWrapper(context, context);
    }

    @Override
    public Context getApplicationContext() {
        return applicationContext;
    }
}
