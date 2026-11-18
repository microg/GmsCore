/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.documentscanner.internal;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.ACTION_SCAN_DOCUMENT;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_CALLING_APP_NAME;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_IMAGE_HASHES;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_IMAGE_URIS;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_PAGE_COUNT;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_PDF_URI;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_SCANNING_RESULT;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import org.microg.gms.common.Constants;
import org.microg.mlkit.vision.documentscanner.GmsDocumentScanningResultImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GmsDocumentScanningDelegateActivity extends ComponentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleActivityResult);
        if (savedInstanceState != null) {
            // Coming from a previous creation, child was already started
            return;
        }

        // TODO: Also delegate to self if available
        Intent delegatedIntent = new Intent(ACTION_SCAN_DOCUMENT)
                .setPackage(Constants.GMS_PACKAGE_NAME)
                .putExtra(EXTRA_CALLING_APP_NAME, getApplicationLabel())
                .putExtras(getIntent())
                .setFlags(FLAG_GRANT_READ_URI_PERMISSION);
        launcher.launch(delegatedIntent);
    }

    private String getApplicationLabel() {
        ApplicationInfo applicationInfo = getApplicationInfo();
        int i = applicationInfo.labelRes;
        return i != 0 ? getString(i) : getPackageManager().getApplicationLabel(applicationInfo).toString();
    }

    private void handleActivityResult(ActivityResult result) {
        new Thread(() -> {
            GmsDocumentScanningResult scanningResult = parseResult(result.getResultCode(), result.getData());
            runOnUiThread(() -> {
                if (scanningResult == null) {
                    setResult(RESULT_CANCELED);
                } else {
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_SCANNING_RESULT, scanningResult));
                }
                finish();
            });
        }).start();
    }

    @Nullable
    private GmsDocumentScanningResult parseResult(int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) return null;
        //noinspection deprecation
        List<Uri> imageUris = data.getParcelableArrayListExtra(EXTRA_RESULT_IMAGE_URIS);
        List<String> imageHashes = data.getStringArrayListExtra(EXTRA_RESULT_IMAGE_HASHES);
        //noinspection deprecation
        Uri pdfUri = data.getParcelableExtra(EXTRA_RESULT_PDF_URI);
        int pageCount = data.getIntExtra(EXTRA_RESULT_PAGE_COUNT, 0);

        List<Uri> localImageUris = new ArrayList<>();
        if (imageUris != null) {
            for (Uri uri : imageUris) {
                Uri localUri = copyToLocalCache(uri, ".jpg");
                if (localUri == null) return null;
                localImageUris.add(localUri);
            }
        }
        Uri localPdfUri = copyToLocalCache(pdfUri, ".pdf");
        if (localImageUris.isEmpty() && localPdfUri == null) return null;

        return GmsDocumentScanningResultImpl.create(localImageUris, imageHashes, localPdfUri, pageCount);
    }

    @Nullable
    private Uri copyToLocalCache(@Nullable Uri uri, String suffix) {
        if (uri == null || uri.getScheme() == null) return null;

        File localCacheDir = new File(getCacheDir(), "mlkit_docscan_ui_client");
        if (!localCacheDir.exists()) {
            if (!localCacheDir.mkdirs()) return null;
        }

        File file = new File(localCacheDir, SystemClock.elapsedRealtimeNanos() + suffix);
        try {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    byte[] bArr = new byte[1024];
                    while (true) {
                        int i = inputStream.read(bArr);
                        if (i == -1) {
                            break;
                        }
                        fileOutputStream.write(bArr, 0, i);
                    }
                    fileOutputStream.flush();
                    return Uri.fromFile(file);
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}
