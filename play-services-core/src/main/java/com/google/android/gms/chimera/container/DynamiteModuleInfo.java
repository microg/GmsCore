/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera.container;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.CONTEXT_INCLUDE_CODE;

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

    public int getCreatePackageOptions() {
        try {
            return descriptor.getDeclaredField("CREATE_PACKAGE_OPTIONS").getInt(null);
        } catch (Exception e) {
            return CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY;
        }
    }

    public boolean isMergeClassLoader() {
        try {
            return descriptor.getDeclaredField("MERGE_CLASS_LOADER").getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }
}
