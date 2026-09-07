/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.cryptauth

import android.content.Context

/** Shared persistence for account-scoped CryptAuth key records. */
class CryptAuthKeyStore(context: Context) {
    private val database = CryptAuthKeyDatabase.getInstance(context.applicationContext)

    /** Returns all records for [account] and [keyName]. */
    fun getAll(account: String, keyName: String): List<CryptAuthKeyRecord> {
        require(account.isNotBlank()) { "CryptAuth account must not be blank" }
        require(keyName.isNotBlank()) { "CryptAuth key name must not be blank" }
        return database.getAll(account, keyName)
    }

    /** Returns the active record for [account] and [keyName], or null when none is active. */
    fun getActive(account: String, keyName: String): CryptAuthKeyRecord? {
        require(account.isNotBlank()) { "CryptAuth account must not be blank" }
        require(keyName.isNotBlank()) { "CryptAuth key name must not be blank" }
        return database.getActive(account, keyName)
    }

    /** Saves [record] unless its handle and account are already present. */
    fun put(record: CryptAuthKeyRecord) {
        database.put(record)
    }

    /** Saves all [records] in one transaction. */
    fun put(records: List<CryptAuthKeyRecord>) {
        database.put(records)
    }

    /** Changes the active state of the record identified by [account], [keyName], and [handle]. */
    fun setActive(account: String, keyName: String, handle: ByteArray, active: Boolean) {
        require(account.isNotBlank()) { "CryptAuth account must not be blank" }
        require(keyName.isNotBlank()) { "CryptAuth key name must not be blank" }
        require(handle.isNotEmpty()) { "CryptAuth key handle must not be empty" }
        check(database.setActive(account, keyName, handle, active) == 1) { "CryptAuth key is unavailable" }
    }

    /** Deletes the record identified by [account], [keyName], and [handle]. */
    fun delete(account: String, keyName: String, handle: ByteArray) {
        require(account.isNotBlank()) { "CryptAuth account must not be blank" }
        require(keyName.isNotBlank()) { "CryptAuth key name must not be blank" }
        require(handle.isNotEmpty()) { "CryptAuth key handle must not be empty" }
        database.delete(account, keyName, handle)
    }
}
