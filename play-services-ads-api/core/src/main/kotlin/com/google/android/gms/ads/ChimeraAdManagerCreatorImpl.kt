/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import androidx.annotation.Keep

/**
 * Dynamite entry point for the banner/interstitial (AdManager) path.
 *
 * The AdMob SDK loads this class from the ads dynamite module by its fully-qualified name, so it must
 * exist under this exact FQCN and implement `IAdManagerCreator`. Behaviour is identical to
 * [AdManagerCreatorImpl], which is kept under its own name for SDKs that request the un-prefixed class.
 */
@Keep
class ChimeraAdManagerCreatorImpl : AdManagerCreatorImpl()
