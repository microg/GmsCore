/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import java.util.Collection;
import java.util.Collections;

import android.content.Context;

public class DynamiteModuleInfo {
    private Class<?> descriptor;
    private String moduleId;

    public DynamiteModuleInfo(String moduleId) {
        this.moduleId = moduleId;
        try {
            this.descriptor = Class.forName("com.google.android.gms.dynamite.descriptors." + moduleId + ".ModuleDescriptor");
        } catch (Exception e) {
            // Ignore
        }
    }

    public String getModuleId() {
        return moduleId;
    }

    public int getVersion() {
        try {
            return descriptor.getDeclaredField("MODULE_VERSION").getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }

    public Collection<String> getMergedPackages() {
        try {
            return (Collection<String>) descriptor.getDeclaredField("MERGED_PACKAGES").get(null);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public Collection<String> getMergedClasses() {
        try {
            return (Collection<String>) descriptor.getDeclaredField("MERGED_CLASSES").get(null);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public void init(Context dynamiteContext) {
        try {
            descriptor.getMethod("init", Context.class).invoke(null, dynamiteContext);
        } catch (Exception e) {
            // Ignore
        }
    }
}
