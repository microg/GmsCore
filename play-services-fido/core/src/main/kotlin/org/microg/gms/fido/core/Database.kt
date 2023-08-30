/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getLongOrNull
import org.microg.gms.fido.core.transport.Transport

class Database(context: Context) : SQLiteOpenHelper(context, "fido.db", null, VERSION) {

    fun isPrivileged(packageName: String, signatureDigest: String): Boolean = readableDatabase.use {
        it.count(TABLE_PRIVILEGED_APPS, "$COLUMN_PACKAGE_NAME = ? AND $COLUMN_SIGNATURE_DIGEST = ?", packageName, signatureDigest) > 0
    }

    fun wasUsed(): Boolean = readableDatabase.use { it.count(TABLE_KNOWN_REGISTRATIONS) > 0 }

    fun getKnownRegistrationTransport(rpId: String, credentialId: String) = readableDatabase.use {
        val c = it.query(TABLE_KNOWN_REGISTRATIONS, arrayOf(COLUMN_TRANSPORT), "$COLUMN_RP_ID = ? AND $COLUMN_CREDENTIAL_ID = ?", arrayOf(rpId, credentialId), null, null, null)
        try {
            if (c.moveToFirst()) Transport.valueOf(c.getString(0)) else null
        } finally {
            c.close()
        }
    }

    fun insertPrivileged(packageName: String, signatureDigest: String) = writableDatabase.use {
        it.insertWithOnConflict(TABLE_PRIVILEGED_APPS, null, ContentValues().apply {
            put(COLUMN_PACKAGE_NAME, packageName)
            put(COLUMN_SIGNATURE_DIGEST, signatureDigest)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }, CONFLICT_REPLACE)
    }

    fun insertKnownRegistration(rpId: String, credentialId: String, transport: Transport) = writableDatabase.use {
        it.insertWithOnConflict(TABLE_KNOWN_REGISTRATIONS, null, ContentValues().apply {
            put(COLUMN_RP_ID, rpId)
            put(COLUMN_CREDENTIAL_ID, credentialId)
            put(COLUMN_TRANSPORT, transport.name)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }, CONFLICT_REPLACE)
    }

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE $TABLE_PRIVILEGED_APPS($COLUMN_PACKAGE_NAME TEXT, $COLUMN_SIGNATURE_DIGEST TEXT, $COLUMN_TIMESTAMP INT, UNIQUE($COLUMN_PACKAGE_NAME, $COLUMN_SIGNATURE_DIGEST) ON CONFLICT REPLACE);")
        }
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE $TABLE_KNOWN_REGISTRATIONS($COLUMN_RP_ID TEXT, $COLUMN_CREDENTIAL_ID TEXT, $COLUMN_TRANSPORT TEXT, $COLUMN_TIMESTAMP INT, UNIQUE($COLUMN_RP_ID, $COLUMN_CREDENTIAL_ID) ON CONFLICT REPLACE)")
        }
    }

    companion object {
        const val VERSION = 2
        private const val TABLE_PRIVILEGED_APPS = "privileged_apps"
        private const val TABLE_KNOWN_REGISTRATIONS = "known_registrations"
        private const val COLUMN_PACKAGE_NAME = "package_name"
        private const val COLUMN_SIGNATURE_DIGEST = "signature_digest"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_RP_ID = "rp_id"
        private const val COLUMN_CREDENTIAL_ID = "credential_id"
        private const val COLUMN_TRANSPORT = "transport"
    }
}

fun SQLiteDatabase.count(table: String, selection: String? = null, vararg selectionArgs: String): Long {
    val it = if (selection == null) {
        rawQuery("SELECT COUNT(*) FROM $table", null)
    } else {
        rawQuery("SELECT COUNT(*) FROM $table WHERE $selection", selectionArgs)
    }
    return try {
        if (it.moveToFirst()) {
            it.getLongOrNull(0) ?: 0
        } else {
            0
        }
    } finally {
        it.close()
    }
}