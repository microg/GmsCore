/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import com.google.android.chimera.annotation.ChimeraApiVersion

@ChimeraApiVersion(added = 0L)
class InvalidConfigException : Exception {
    constructor(s: String?) : super(s)

    constructor(s: String?, throwable0: Throwable?) : super(s, throwable0)

    constructor(throwable0: Throwable?) : super(throwable0)
}
