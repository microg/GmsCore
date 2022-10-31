/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.oss.licenses;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.oss.licenses.License;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LicenseUtil {
    public static boolean hasLicenses(Context context) {
        Resources resources = context.getApplicationContext().getResources();
        try (InputStream is = resources.openRawResource(resources.getIdentifier("third_party_license_metadata", "raw", context.getPackageName()))) {
            if (is == null || is.available() <= 0) return false;
        } catch (IOException e) {
            return false;
        }
        try (InputStream is = resources.openRawResource(resources.getIdentifier("third_party_licenses", "raw", context.getPackageName()))) {
            if (is == null || is.available() <= 0) return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static List<License> getLicensesFromMetadata(Context context) {
        Resources resources = context.getApplicationContext().getResources();
        InputStream is = resources.openRawResource(resources.getIdentifier("third_party_license_metadata", "raw", context.getPackageName()));
        String metadata = readStringAndClose(is, Integer.MAX_VALUE);
        String[] lines = metadata.split("\n");
        List<License> licenses = new ArrayList<>(lines.length);
        for (String line : lines) {
            int spaceIndex = line.indexOf(' ');
            String[] position = line.substring(0, spaceIndex).split(":");
            if (spaceIndex <= 0 || position.length != 2) {
                throw new IllegalStateException("Invalid license meta-data line:\n" + line);
            }
            licenses.add(new License(line.substring(spaceIndex + 1), Long.parseLong(position[0]), Integer.parseInt(position[1]), ""));
        }
        return licenses;
    }

    public static String getLicenseText(Context context, License license) {
        if (license.getPath().isEmpty()) {
            Resources resources = context.getApplicationContext().getResources();
            InputStream is = resources.openRawResource(resources.getIdentifier("third_party_licenses", "raw", context.getPackageName()));
            try {
                if (is.skip(license.getOffset()) != license.getOffset()) {
                    throw new RuntimeException("Failed to read license");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read license", e);
            }
            return readStringAndClose(is, license.getLength());
        } else {
            try (JarFile jar = new JarFile(license.getPath())) {
                JarEntry entry = jar.getJarEntry("res/raw/third_party_licenses");
                if (entry == null) {
                    throw new RuntimeException(license.getPath() + " does not contain res/raw/third_party_licenses");
                } else {
                    InputStream is = jar.getInputStream(entry);
                    if (is.skip(license.getOffset()) != license.getOffset()) {
                        throw new RuntimeException("Failed to read license");
                    }
                    return readStringAndClose(is, license.getLength());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read license", e);
            }
        }
    }

    private static String readStringAndClose(InputStream is, int bytesToRead) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] bytes = new byte[1024];
            int read;
            while (bytesToRead > 0 && (read = is.read(bytes, 0, Math.min(bytes.length, bytesToRead))) != -1) {
                bos.write(bytes, 0, read);
                bytesToRead -= read;
            }
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read license or metadata", e);
        }
        try {
            return bos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding UTF8. This should always be supported.", e);
        }
    }
}
