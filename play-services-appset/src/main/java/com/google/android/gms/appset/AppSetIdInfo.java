/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.appset;

import androidx.annotation.IntDef;
import org.microg.gms.common.Hide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contains information about app set ID.
 */
public class AppSetIdInfo {
    /**
     * The app set ID is scoped to the app.
     */
    public static final int SCOPE_APP = 1;
    /**
     * The app set ID is scoped to a developer account on an app store. All apps from the same developer on a device will have
     * the same developer scoped app set ID.
     */
    public static final int SCOPE_DEVELOPER = 2;

    private final String id;
    private final @Scope int scope;

    @Hide
    public AppSetIdInfo(String id, @Scope int scope) {
        this.id = id;
        this.scope = scope;
    }

    /**
     * Gets the app set ID.
     *
     * @return the app set ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the {@link AppSetIdInfo.Scope} of the app set ID. Possible values include {@link #SCOPE_APP} and {@link #SCOPE_DEVELOPER}.
     *
     * @return the app set ID's {@link AppSetIdInfo.Scope}.
     */
    public @Scope int getScope() {
        return scope;
    }

    /**
     * Allowed constants for {@link AppSetIdInfo#getScope()}.
     * <p>
     * Supported constants:
     * <ul>
     *     <li>{@link #SCOPE_APP}</li>
     *     <li>{@link #SCOPE_DEVELOPER}</li>
     * </ul>
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SCOPE_APP, SCOPE_DEVELOPER})
    public @interface Scope {

    }
}
