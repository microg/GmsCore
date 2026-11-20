/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.interfaces.Detector;
import org.microg.gms.common.PublicApi;

@PublicApi
public interface TextRecognizer extends Detector<Text>, OptionalModuleApi {
    @NonNull
    Task<Text> process(@NonNull InputImage image);

    @NonNull
    Task<Text> process(@NonNull MlImage image);

    @Override
    void close();
}
