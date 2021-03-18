/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import com.google.android.gms.tasks.Task

interface DroidGuardClient {
    fun getHandle(): Task<DroidGuardHandle>
}
