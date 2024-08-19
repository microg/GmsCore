/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PhenotypeDatabase(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "phenotype.db"
        const val DATABASE_VERSION = 0x20
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("  CREATE TABLE IF NOT EXISTS Packages(\n    packageName TEXT NOT NULL PRIMARY KEY,\n    version INTEGER NOT NULL,\n    params BLOB,\n    dynamicParams BLOB,\n    weak INTEGER NOT NULL,\n    androidPackageName TEXT NOT NULL,\n    isSynced INTEGER,\n    serializedDeclarativeRegInfo BLOB DEFAULT NULL,\n    configTier INTEGER DEFAULT NULL,\n    baselineCl INTEGER DEFAULT NULL,\n    heterodyneInfo BLOB DEFAULT NULL,\n    runtimeProperties BLOB DEFAULT NULL,\n    declarativeRegistrationInfo BLOB DEFAULT NULL\n  )\n")
        db.execSQL("CREATE INDEX IF NOT EXISTS androidPackageName ON Packages (androidPackageName)")
        db.execSQL("  CREATE TABLE IF NOT EXISTS ApplicationStates(\n    packageName TEXT NOT NULL PRIMARY KEY,\n    user TEXT NOT NULL,\n    version INTEGER NOT NULL,\n    patchable INTEGER\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS MultiCommitApplicationStates(\n    packageName TEXT NOT NULL,\n    user TEXT NOT NULL,\n    version INTEGER NOT NULL,\n    PRIMARY KEY(packageName, user)\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS LogSources(\n    logSourceName TEXT NOT NULL,\n    packageName TEXT NOT NULL,\n    PRIMARY KEY(logSourceName, packageName)\n    )\n")
        db.execSQL("CREATE INDEX IF NOT EXISTS packageName ON LogSources(packageName)")
        db.execSQL("  CREATE TABLE IF NOT EXISTS WeakExperimentIds(\n    packageName TEXT NOT NULL,\n    experimentId INTEGER NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS ExperimentTokens(\n    packageName TEXT NOT NULL,\n    version INTEGER NOT NULL,\n    user TEXT NOT NULL,\n    isCommitted INTEGER NOT NULL,\n    experimentToken BLOB NOT NULL,\n    serverToken TEXT NOT NULL,\n    configHash TEXT NOT NULL DEFAULT \'\',\n    servingVersion INTEGER NOT NULL DEFAULT 0,\n    tokensTag BLOB DEFAULT NULL,\n    flagsHash INTEGER DEFAULT NULL,\n    PRIMARY KEY(packageName, version, user, isCommitted)\n  )\n")
        db.execSQL("CREATE INDEX IF NOT EXISTS committed ON ExperimentTokens(packageName, version, user, isCommitted)")
        db.execSQL("  CREATE TABLE IF NOT EXISTS ExternalExperimentTokens(\n    packageName TEXT NOT NULL PRIMARY KEY,\n    experimentToken BLOB NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS Flags(\n    packageName TEXT NOT NULL,\n    version INTEGER NOT NULL,\n    flagType INTEGER NOT NULL,\n    partitionId INTEGER NOT NULL,\n    user TEXT NOT NULL,\n    name TEXT NOT NULL,\n    intVal INTEGER,\n    boolVal INTEGER,\n    floatVal REAL,\n    stringVal TEXT,\n    extensionVal BLOB,\n    committed INTEGER NOT NULL,\n    PRIMARY KEY(packageName, version, flagType, partitionId, user, name, committed)\n  );\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS RequestTags(\n    user TEXT NOT NULL PRIMARY KEY,\n    bytesTag BLOB NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS ApplicationTags(\n    packageName TEXT NOT NULL,\n    version INTEGER NOT NULL,\n    partitionId INTEGER NOT NULL,\n    user TEXT NOT NULL,\n    tag BLOB NOT NULL,\n    PRIMARY KEY(packageName, version, partitionId, user)\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS CrossLoggedExperimentTokens(\n    fromPackageName TEXT NOT NULL,\n    fromVersion INTEGER NOT NULL,\n    fromUser TEXT NOT NULL,\n    toPackageName TEXT NOT NULL,\n    toVersion INTEGER NOT NULL,\n    isCommitted INTEGER NOT NULL,\n    token BLOB NOT NULL,\n    provenance INTEGER NOT NULL\n  )\n")
        db.execSQL("  CREATE INDEX IF NOT EXISTS apply ON CrossLoggedExperimentTokens(\n    fromPackageName,\n    fromVersion,\n    fromUser,\n    toPackageName,\n    toVersion,\n    isCommitted\n  )\n")
        db.execSQL("CREATE INDEX IF NOT EXISTS remove ON CrossLoggedExperimentTokens(toPackageName)")
        db.execSQL("  CREATE TABLE IF NOT EXISTS ChangeCounts(\n    packageName TEXT NOT NULL PRIMARY KEY,\n    count INTEGER NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS DogfoodsToken(\n    \"key\" INTEGER NOT NULL PRIMARY KEY,\n    token BLOB\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS LastFetch(\n    \"key\" INTEGER NOT NULL PRIMARY KEY,\n    servertimestamp INTEGER NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS FlagOverrides(\n    packageName TEXT NOT NULL,\n    user TEXT NOT NULL,\n    name TEXT NOT NULL,\n    flagType INTEGER NOT NULL,\n    intVal INTEGER,\n    boolVal INTEGER,\n    floatVal REAL,\n    stringVal TEXT,\n    extensionVal BLOB,\n    committed,\n    PRIMARY KEY(packageName, user, name, committed)\n  );\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS LastSyncAfterRequest(\n    packageName TEXT NOT NULL PRIMARY KEY,\n    servingVersion INTEGER NOT NULL DEFAULT 0,\n    androidPackageName TEXT DEFAULT NULL\n  )\n")
        db.execSQL("  CREATE TABLE IF NOT EXISTS StorageInfos (\n    androidPackageName TEXT UNIQUE NOT NULL,\n    secret BLOB NOT NULL,\n    deviceEncryptedSecret BLOB NOT NULL\n  )\n")
        db.execSQL("  CREATE TABLE AppWideProperties (\n    androidPackageName TEXT UNIQUE NOT NULL,\n    appWideProperties BLOB NOT NULL\n  );\n")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        super.onDowngrade(db, oldVersion, newVersion)
        db.execSQL("DROP TABLE IF EXISTS android_packages;")
        db.execSQL("DROP TABLE IF EXISTS config_packages;")
        db.execSQL("DROP TABLE IF EXISTS config_packages_to_log_sources;")
        db.execSQL("DROP TABLE IF EXISTS cross_logged_tokens;")
        db.execSQL("DROP TABLE IF EXISTS flag_overrides;")
        db.execSQL("DROP TABLE IF EXISTS log_sources;")
        db.version = newVersion
        db.setForeignKeyConstraintsEnabled(true)
    }
}
