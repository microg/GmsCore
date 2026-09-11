/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import androidx.annotation.Keep

/**
 * Dynamite entry point for the Mobile Ads initialization / settings path.
 *
 * The AdMob SDK loads this class from the ads dynamite module by its fully-qualified name, so it must
 * exist under this exact FQCN and implement `IMobileAdsSettingManagerCreator`. This is the entry point
 * that gates ad initialization. Behaviour is identical to [MobileAdsSettingManagerCreatorImpl], which is
 * kept under its own name for SDKs that request the un-prefixed class.
 */
@Keep
class ChimeraMobileAdsSettingManagerCreatorImpl : MobileAdsSettingManagerCreatorImpl()
