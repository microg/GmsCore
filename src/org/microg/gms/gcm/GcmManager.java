/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.gcm;

import android.content.Context;

import org.microg.gms.common.Utils;

public class GcmManager {
    public static String register(Context context, String app, String sender, String info) {
        new RegisterRequest()
                .build(Utils.getBuild(context))
                .sender(sender)
                .info(info)
                .app(app, Utils.getFirstPackageSignatureDigest(context, app), 0); // TODO
        return null;
    }
}
