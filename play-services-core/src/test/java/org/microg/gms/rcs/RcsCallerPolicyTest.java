/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RcsCallerPolicyTest {
    @Test
    public void constellationAllowsMessagesAndGmsSurfaces() {
        assertTrue(RcsCallerPolicy.isConstellationPackageAllowed("com.google.android.apps.messaging"));
        assertTrue(RcsCallerPolicy.isConstellationPackageAllowed("com.google.android.gms"));
        assertTrue(RcsCallerPolicy.isConstellationPackageAllowed("com.google.android.ims"));
        assertTrue(RcsCallerPolicy.isConstellationPackageAllowed("com.google.firebase.pnv"));
    }

    @Test
    public void constellationRejectsUnrelatedPackages() {
        assertFalse(RcsCallerPolicy.isConstellationPackageAllowed("com.example.unrelated"));
        assertFalse(RcsCallerPolicy.isConstellationPackageAllowed(""));
        assertFalse(RcsCallerPolicy.isConstellationPackageAllowed(null));
    }

    @Test
    public void asterismAllowsOnlyConsentCallers() {
        assertTrue(RcsCallerPolicy.isAsterismPackageAllowed("com.google.android.apps.messaging"));
        assertTrue(RcsCallerPolicy.isAsterismPackageAllowed("com.google.android.ims"));
        assertFalse(RcsCallerPolicy.isAsterismPackageAllowed("com.google.android.gms"));
        assertFalse(RcsCallerPolicy.isAsterismPackageAllowed("com.example.unrelated"));
    }
}
