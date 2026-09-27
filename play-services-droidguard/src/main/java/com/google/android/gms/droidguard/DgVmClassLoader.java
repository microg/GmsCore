/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import dalvik.system.DexClassLoader;

/**
 * Named DexClassLoader for DroidGuard VM APKs.
 *
 * DG inspects the classloader chain via {@code getClass().getName()}. An anonymous
 * Kotlin object or a raw DexClassLoader subclass nested under {@code org.microg}
 * leaks microG into that name. Using this stock-GMS package class yields
 * {@code com.google.android.gms.droidguard.DgVmClassLoader}, matching stock GMS.
 */
public class DgVmClassLoader extends DexClassLoader {
    public DgVmClassLoader(
            String dexPath,
            String optimizedDirectory,
            String librarySearchPath,
            ClassLoader parent
    ) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
