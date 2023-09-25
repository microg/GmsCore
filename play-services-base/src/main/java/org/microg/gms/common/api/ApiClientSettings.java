/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import android.view.View;

import java.util.Set;

public class ApiClientSettings {
    public String accountName;
    public String packageName;
    public Integer sessionId;
    public Set<String> scopes;
    public int gravityForPopups;
    public View viewForPopups;
}
