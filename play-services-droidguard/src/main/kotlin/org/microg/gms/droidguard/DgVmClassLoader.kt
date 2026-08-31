/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import dalvik.system.DexClassLoader

/**
 * Named loader used for DroidGuard VM APKs.
 *
 * An anonymous DexClassLoader subclass leaks `org.microg` through Class.getName()
 * into /proc/self/maps, which the DG VM reads. A concrete named class matches
 * the stock GMS shape more closely.
 */
class DgVmClassLoader(
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader?
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent)
