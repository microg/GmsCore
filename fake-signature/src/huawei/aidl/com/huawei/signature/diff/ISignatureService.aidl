/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.huawei.signature.diff;

/**
 * Interface for Huawei Differentiated Signature Capability
 * See https://forums.developer.huawei.com/forumPortal/en/topic/0202128603315033024
 */
interface ISignatureService {
    String[] querySignature(String packageName, boolean suggested);
}