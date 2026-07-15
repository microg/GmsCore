/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import dalvik.system.DexClassLoader

/**
 * Named DexClassLoader for DroidGuard VM bytecode.
 *
 * Anonymous DexClassLoader instances leak "org.microg" in class names visible to
 * DroidGuard's /proc/self/maps inspection, causing tachyon_registration failures.
 */
class DgVmClassLoader(
    dexPath: String,
    optimizedDirectory: String,
    librarySearchPath: String?,
    parent: ClassLoader
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent)
