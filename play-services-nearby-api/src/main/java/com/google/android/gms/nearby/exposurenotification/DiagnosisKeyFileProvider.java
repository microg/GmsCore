/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider which holds a list of diagnosis key files and can open/supply them one by one as they are ready to be processed.
 */
@PublicApi
public class DiagnosisKeyFileProvider {
    private List<File> files;

    public DiagnosisKeyFileProvider(List<File> files) {
        this.files = new ArrayList<>(files);
    }

    public List<File> getFiles() {
        return files;
    }
}
