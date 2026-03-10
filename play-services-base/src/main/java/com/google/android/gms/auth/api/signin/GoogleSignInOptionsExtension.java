/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import android.os.Bundle;
import com.google.android.gms.common.api.Scope;
import org.microg.gms.common.Hide;

import java.util.List;

/**
 * An interface for API specific extension for {@link GoogleSignInOptions}.
 *
 * @see GoogleSignInOptions.Builder#addExtension(GoogleSignInOptionsExtension).
 */
public interface GoogleSignInOptionsExtension {
    @Hide
    int GAMES = 1;
    @Hide
    int FITNESS = 3;

    @Hide
    int getExtensionType();

    @Hide
    Bundle toBundle();

    @Hide
    List<Scope> getImpliedScopes();
}
