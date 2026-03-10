/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.droidguard.core

import com.google.android.gms.droidguard.DroidGuardChimeraService
import org.microg.gms.chimera.ServiceLoader
import org.microg.gms.chimera.ServiceProxy

class DroidGuardService : ServiceProxy(ServiceLoader.static<DroidGuardChimeraService>())
