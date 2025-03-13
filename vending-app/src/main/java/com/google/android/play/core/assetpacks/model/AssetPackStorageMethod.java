/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.model;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method used to store an asset pack.
 */
@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@IntDef({AssetPackStorageMethod.STORAGE_FILES, AssetPackStorageMethod.APK_ASSETS})
public @interface AssetPackStorageMethod {
    /**
     * The asset pack is extracted into a folder containing individual asset files.
     * <p>
     * Assets contained by this asset pack can be accessed via standard File APIs.
     */
    int STORAGE_FILES = 0;
    /**
     * The asset pack is installed as APKs containing asset files.
     * <p>
     * Assets contained by this asset pack can be accessed via Asset Manager.
     */
    int APK_ASSETS = 1;
}
