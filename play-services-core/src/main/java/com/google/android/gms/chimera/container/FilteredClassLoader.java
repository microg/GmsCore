/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import android.util.Log;

import java.util.Collection;
import java.util.HashSet;

public class FilteredClassLoader extends ClassLoader {
    private static ClassLoader rootClassLoader;
    private final Collection<String> allowedClasses;
    private final Collection<String> allowedPackages;

    static {
        rootClassLoader = ClassLoader.getSystemClassLoader();
        if (rootClassLoader == null) {
            rootClassLoader = FilteredClassLoader.class.getClassLoader();
            while (rootClassLoader.getParent() != null) {
                rootClassLoader = rootClassLoader.getParent();
            }
        }
    }

    public FilteredClassLoader(ClassLoader parent, Collection<String> allowedClasses, Collection<String> allowedPackages) {
        super(parent);
        this.allowedClasses = new HashSet<>(allowedClasses);
        this.allowedPackages = new HashSet<>(allowedPackages);
    }

    private String getPackageName(String name) {
        int lastIndex = name.lastIndexOf(".");
        if (lastIndex <= 0) return "";
        return name.substring(0, lastIndex);
    }

    private String getClassName(String name) {
        int lastIndex = name.indexOf("$");
        if (lastIndex <= 0) return name;
        return name.substring(0, lastIndex);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.")) return rootClassLoader.loadClass(name);
        if (allowedClasses.contains(name) || allowedClasses.contains(getClassName(name)))
            return super.loadClass(name, resolve);
        if (allowedClasses.contains("!" + name) || allowedClasses.contains("!" + getClassName(name)))
            return rootClassLoader.loadClass(name);
        if (allowedPackages.contains(getPackageName(name))) return super.loadClass(name, resolve);
        return rootClassLoader.loadClass(name);
    }
}
