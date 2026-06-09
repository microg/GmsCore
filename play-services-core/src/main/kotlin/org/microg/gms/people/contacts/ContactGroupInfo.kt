/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.people.contacts

data class ContactGroupInfo(val isDefault: Boolean = false, val groupId: String?, val groupTitle: String?, val syncStr: String?, val created: Boolean, val deleted: Boolean, val updated: Boolean)