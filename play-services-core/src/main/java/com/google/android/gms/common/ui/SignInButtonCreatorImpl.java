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

package com.google.android.gms.common.ui;

import android.content.Context;
import android.view.View;

import com.google.android.gms.common.internal.ISignInButtonCreator;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class SignInButtonCreatorImpl extends ISignInButtonCreator.Stub {
    @Override
    public IObjectWrapper createSignInButton(IObjectWrapper contextWrapper, int size, int color) {
        Context context = (Context) ObjectWrapper.unwrap(contextWrapper);
        // TODO: real view :)
        return ObjectWrapper.wrap(new View(context));
    }
}
