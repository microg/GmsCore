/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.cryptauth

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import android.util.Log

private const val TAG = "CryptAuthKeyDatabase"
private const val DATABASE_NAME = "cryptauthkeys.db"
private const val DATABASE_VERSION = 5

private const val TABLE_KEYS = "keys"
private const val COLUMN_KEY_HANDLE = "key_handle"
private const val COLUMN_KEY_NAME = "key_name"
private const val COLUMN_KEY_FORM = "key_form"
private const val COLUMN_KEY_TYPE = "key_type"
private const val COLUMN_ACCOUNT = "account"
private const val COLUMN_MASTER_KEY = "master_key"
private const val COLUMN_PUBLIC_KEY = "public_key"
private const val COLUMN_PRIVATE_KEY = "private_key"
private const val COLUMN_ACTIVE_STATUS = "active_status"
private const val COLUMN_CREATION_TIME = "creation_time"
private const val COLUMN_EXPORTABLE = "exportable"
private const val COLUMN_EXPIRATION_TIME = "expiration_time"

private const val KEY_FORM_ASYMMETRIC = 1
private const val KEY_FORM_SYMMETRIC = 2
private const val KEY_STATUS_ACTIVE = 1
private const val KEY_STATUS_INACTIVE = 2
private const val KEY_NOT_EXPORTABLE = 1
private const val KEY_EXPORTABLE = 2

private val KEY_PROJECTION = arrayOf(
    COLUMN_KEY_HANDLE,
    COLUMN_KEY_NAME,
    COLUMN_KEY_FORM,
    COLUMN_KEY_TYPE,
    COLUMN_ACCOUNT,
    COLUMN_MASTER_KEY,
    COLUMN_PUBLIC_KEY,
    COLUMN_PRIVATE_KEY,
    COLUMN_ACTIVE_STATUS,
    COLUMN_CREATION_TIME,
    COLUMN_EXPORTABLE,
    COLUMN_EXPIRATION_TIME,
)

private const val CREATE_KEYS_TABLE =
    "CREATE TABLE keys(key_handle STRING NOT NULL, key_name STRING NOT NULL, key_form INTEGER NOT NULL, " +
        "key_type STRING NOT NULL, account STRING NOT NULL, master_key STRING, public_key STRING, " +
        "private_key STRING, active_status INTEGER NOT NULL, creation_time INTEGER NOT NULL, " +
        "exportable INTEGER NOT NULL, expiration_time INTEGER NOT NULL, PRIMARY KEY(key_handle,account));"
private const val CREATE_METADATA_TABLE =
    "CREATE TABLE metadata(identifier STRING NOT NULL, account STRING NOT NULL, client_metadata STRING, " +
        "PRIMARY KEY(account,identifier));"
private const val CREATE_GROUP_KEYS_TABLE =
    "CREATE TABLE group_keys(key_name STRING NOT NULL, account STRING NOT NULL, key_state INTEGER, " +
        "public_key STRING, private_key STRING, PRIMARY KEY(key_name,account,key_state));"
private val CREATE_STATEMENTS = arrayOf(CREATE_KEYS_TABLE, CREATE_METADATA_TABLE, CREATE_GROUP_KEYS_TABLE)

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

internal class CryptAuthKeyDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    fun getAll(account: String, keyName: String): List<CryptAuthKeyRecord> {
        val cursor = readableDatabase.query(
            true,
            TABLE_KEYS,
            KEY_PROJECTION,
            "$COLUMN_ACCOUNT = ? AND $COLUMN_KEY_NAME = ?",
            arrayOf(account, keyName),
            null,
            null,
            null,
            null,
        )
        return readRecords(cursor)
    }

    fun getActive(account: String, keyName: String): CryptAuthKeyRecord? {
        val cursor = readableDatabase.query(
            true,
            TABLE_KEYS,
            KEY_PROJECTION,
            "$COLUMN_ACCOUNT = ? AND $COLUMN_KEY_NAME = ? AND $COLUMN_ACTIVE_STATUS = ?",
            arrayOf(account, keyName, KEY_STATUS_ACTIVE.toString()),
            null,
            null,
            null,
            null,
        )
        return readRecords(cursor).firstOrNull()
    }

    fun put(record: CryptAuthKeyRecord) {
        put(listOf(record))
    }

    fun put(records: List<CryptAuthKeyRecord>) {
        if (records.isEmpty()) return
        val database = writableDatabase
        database.beginTransaction()
        try {
            records.forEach { record -> put(database, record) }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun put(database: SQLiteDatabase, record: CryptAuthKeyRecord) {
        val encodedHandle = encode(record.handle)
        database.query(
            TABLE_KEYS,
            arrayOf(COLUMN_KEY_HANDLE),
            "$COLUMN_KEY_HANDLE = ? AND $COLUMN_ACCOUNT = ?",
            arrayOf(encodedHandle, record.account),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) return
        }
        if (record.active) {
            val inactive = ContentValues().apply {
                put(COLUMN_ACTIVE_STATUS, KEY_STATUS_INACTIVE)
            }
            database.update(
                TABLE_KEYS,
                inactive,
                "$COLUMN_KEY_NAME = ? AND $COLUMN_ACCOUNT = ?",
                arrayOf(record.keyName, record.account),
            )
        }
        val values = ContentValues()
        values.put(COLUMN_KEY_HANDLE, encodedHandle)
        values.put(COLUMN_KEY_NAME, record.keyName)
        values.put(COLUMN_KEY_FORM, keyForm(record.keyType))
        values.put(COLUMN_KEY_TYPE, record.keyType.name)
        values.put(COLUMN_ACCOUNT, record.account)
        putBytes(values, COLUMN_MASTER_KEY, record.masterKey)
        putBytes(values, COLUMN_PUBLIC_KEY, record.publicKey)
        putBytes(values, COLUMN_PRIVATE_KEY, record.privateKey)
        values.put(COLUMN_ACTIVE_STATUS, keyStatus(record.active))
        values.put(COLUMN_CREATION_TIME, record.creationTime)
        values.put(COLUMN_EXPORTABLE, exportableValue(record.exportable))
        values.put(COLUMN_EXPIRATION_TIME, record.expirationTime)
        if (database.insert(TABLE_KEYS, null, values) < 0L) {
            throw SQLException("Unable to store CryptAuth key")
        }
    }

    fun setActive(account: String, keyName: String, handle: ByteArray, active: Boolean): Int {
        val values = ContentValues()
        values.put(COLUMN_ACTIVE_STATUS, keyStatus(active))
        return writableDatabase.update(
            TABLE_KEYS,
            values,
            "$COLUMN_KEY_NAME = ? AND $COLUMN_KEY_HANDLE = ? AND $COLUMN_ACCOUNT = ?",
            arrayOf(keyName, encode(handle), account),
        )
    }

    fun delete(account: String, keyName: String, handle: ByteArray) {
        writableDatabase.delete(
            TABLE_KEYS,
            "$COLUMN_KEY_NAME = ? AND $COLUMN_KEY_HANDLE = ? AND $COLUMN_ACCOUNT = ?",
            arrayOf(keyName, encode(handle), account),
        )
    }

    override fun onCreate(database: SQLiteDatabase) {
        CREATE_STATEMENTS.forEach(database::execSQL)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        for (version in oldVersion + 1..newVersion) {
            when (version) {
                2 -> database.execSQL("ALTER TABLE keys ADD COLUMN exportable INTEGER DEFAULT 2")
                3 -> database.execSQL(CREATE_METADATA_TABLE)
                4 -> database.execSQL(CREATE_GROUP_KEYS_TABLE)
                5 -> {
                    database.execSQL("DROP TABLE IF EXISTS group_keys")
                    database.execSQL(CREATE_GROUP_KEYS_TABLE)
                }
            }
        }
    }

    private fun readRecords(cursor: Cursor): List<CryptAuthKeyRecord> {
        val records = ArrayList<CryptAuthKeyRecord>(cursor.count)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    records += readRecord(it)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Unable to read CryptAuth key record", e)
                }
            }
        }
        return records
    }

    private fun readRecord(cursor: Cursor): CryptAuthKeyRecord {
        val keyType = CryptAuthKeyType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY_TYPE)))
        require(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_KEY_FORM)) == keyForm(keyType)) {
            "CryptAuth key form does not match its type"
        }
        val activeStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE_STATUS))
        require(activeStatus == KEY_STATUS_ACTIVE || activeStatus == KEY_STATUS_INACTIVE) {
            "Invalid CryptAuth key status"
        }
        val exportable = when (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXPORTABLE))) {
            KEY_NOT_EXPORTABLE -> false
            KEY_EXPORTABLE -> true
            else -> throw IllegalArgumentException("Invalid CryptAuth key exportability")
        }
        return CryptAuthKeyRecord(
            account = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT)),
            keyName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)),
            handle = requireNotNull(readBytes(cursor, COLUMN_KEY_HANDLE)) { "CryptAuth key handle is missing" },
            keyType = keyType,
            active = activeStatus == KEY_STATUS_ACTIVE,
            creationTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME)),
            expirationTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_TIME)),
            exportable = exportable,
            masterKey = readBytes(cursor, COLUMN_MASTER_KEY),
            publicKey = readBytes(cursor, COLUMN_PUBLIC_KEY),
            privateKey = readBytes(cursor, COLUMN_PRIVATE_KEY),
        )
    }

    private fun readBytes(cursor: Cursor, column: String): ByteArray? {
        val index = cursor.getColumnIndexOrThrow(column)
        if (cursor.isNull(index)) return null
        return Base64.decode(cursor.getString(index), BASE64_FLAGS)
    }

    private fun putBytes(values: ContentValues, column: String, value: ByteArray?) {
        if (value == null) {
            values.putNull(column)
        } else {
            values.put(column, encode(value))
        }
    }

    private fun encode(value: ByteArray): String = Base64.encodeToString(value, BASE64_FLAGS)

    private fun keyForm(keyType: CryptAuthKeyType): Int = when (keyType) {
        CryptAuthKeyType.RAW128, CryptAuthKeyType.RAW256 -> KEY_FORM_SYMMETRIC
        CryptAuthKeyType.P256 -> KEY_FORM_ASYMMETRIC
    }

    private fun keyStatus(active: Boolean): Int = if (active) KEY_STATUS_ACTIVE else KEY_STATUS_INACTIVE

    private fun exportableValue(exportable: Boolean): Int = if (exportable) KEY_EXPORTABLE else KEY_NOT_EXPORTABLE

    companion object {
        @Volatile
        private var instance: CryptAuthKeyDatabase? = null

        fun getInstance(context: Context): CryptAuthKeyDatabase {
            val current = instance
            if (current != null) return current
            return synchronized(this) {
                instance ?: CryptAuthKeyDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
