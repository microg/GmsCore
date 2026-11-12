/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import com.google.android.gms.phenotype.Configuration
import com.google.android.gms.phenotype.Configurations
import com.google.android.gms.phenotype.ExperimentTokens
import com.google.android.gms.phenotype.Flag
import com.google.android.gms.phenotype.FlagOverride
import java.io.File
import java.security.SecureRandom
import okio.ByteString
import okio.ByteString.Companion.toByteString

internal class PhenotypeDatabase(private val storageContext: Context) : SQLiteOpenHelper(
    storageContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val sharedPublicationLock = Any()
    private val syncRegistrations = synchronized(syncRegistrationsByDatabase) {
        syncRegistrationsByDatabase.getOrPut(storageContext.getDatabasePath(DATABASE_NAME).canonicalPath) {
            SyncRegistrations()
        }
    }

    private class SyncRegistrations {
        var nextRevision = 0L
        val revisions = mutableMapOf<String, Long>()
    }

    private class SyncRegistrationChangedException : IllegalStateException("Registration changed during sync")

    data class SyncRequestSnapshot(val request: SyncRequest, val registrationRevisions: Map<String, Long>)

    private fun invalidateSyncRegistration(packageName: String) {
        synchronized(syncRegistrations) { syncRegistrations.revisions.remove(packageName) }
    }

    private fun invalidateSyncRegistrations() = synchronized(syncRegistrations) {
        syncRegistrations.revisions.clear()
    }

    fun checkSyncRegistrations(revisions: Map<String, Long>) = synchronized(syncRegistrations) {
        if (revisions.any { (name, revision) -> syncRegistrations.revisions[name] != revision }) {
            throw SyncRegistrationChangedException()
        }
    }

    companion object {
        private const val DATABASE_NAME = "phenotype.db"
        private const val DATABASE_VERSION = 1
        private const val SYNC_QUERY_BATCH_SIZE = 400
        private val syncRegistrationsByDatabase = mutableMapOf<String, SyncRegistrations>()
        private const val ALL_USERS = "ALL_USERS"
        private const val EXTERNAL_IDS_PACKAGE_PREFIX = "__internal.external_ids#"

        const val TABLE_PACKAGES = "Packages"
        const val TABLE_FLAGS = "Flags"
        const val TABLE_EXPERIMENT_TOKENS = "ExperimentTokens"
        const val TABLE_CROSS_TOKENS = "CrossLoggedExperimentTokens"
        const val TABLE_LOG_SOURCES = "LogSources"
        const val TABLE_FLAG_OVERRIDES = "FlagOverrides"
        const val TABLE_APPLICATION_STATES = "ApplicationStates"
        const val TABLE_LAST_FETCH = "LastFetch"
        const val TABLE_APPLICATION_TAGS = "ApplicationTags"
        const val TABLE_WEAK_EXPERIMENT_IDS = "WeakExperimentIds"
        const val TABLE_REQUEST_TAGS = "RequestTags"
        const val TABLE_STORAGE_INFOS = "StorageInfos"
        const val TABLE_CHANGE_COUNTS = "ChangeCounts"
        private const val TABLE_APP_WIDE_PROPERTIES = "AppWideProperties"
        private const val TABLE_EXTERNAL_EXPERIMENTS = "ExternalExperiments"
        private const val TABLE_LAST_SYNC_AFTER = "LastSyncAfterRequest"
        private const val TABLE_DOGFOODS_TOKEN = "DogfoodsToken"
        const val TABLE_EXPERIMENT_STATES = "ExperimentStates"
        private const val COL_SCOPE = "scope"
        private const val SYNC_SCOPE_PACKAGE = 0
        private const val SYNC_SCOPE_APPLICATION = 1

        const val COL_PACKAGE_NAME = "packageName"
        const val COL_VERSION = "version"
        const val COL_PARAMS = "params"
        const val COL_DYNAMIC_PARAMS = "dynamicParams"
        const val COL_HETERODYNE_INFO = "heterodyneInfo"
        const val COL_BASELINE_CL = "baselineCl"
        const val COL_RUNTIME_PROPERTIES = "runtimeProperties"
        const val COL_REGISTRATION_SOURCE = "registrationSource"
        private const val COL_CLIENT_DECLARATIVE = "serializedDeclarativeRegInfo"
        private const val COL_DECLARATIVE = "declarativeRegistrationInfo"
        const val COL_WEAK = "weak"
        const val COL_ANDROID_PACKAGE_NAME = "androidPackageName"
        const val COL_IS_SYNCED = "isSynced"
        const val COL_CONFIG_TIER = "configTier"

        const val COL_FLAG_TYPE = "flagType"
        const val COL_PARTITION_ID = "partitionId"
        const val COL_USER = "user"
        const val COL_NAME = "name"
        const val COL_INT_VAL = "intVal"
        const val COL_BOOL_VAL = "boolVal"
        const val COL_FLOAT_VAL = "floatVal"
        const val COL_STRING_VAL = "stringVal"
        const val COL_EXTENSION_VAL = "extensionVal"
        const val COL_COMMITTED = "committed"

        const val COL_IS_COMMITTED = "isCommitted"
        const val COL_EXPERIMENT_TOKEN = "experimentToken"
        const val COL_SERVER_TOKEN = "serverToken"
        const val COL_CONFIG_HASH = "configHash"
        const val COL_SERVING_VERSION = "servingVersion"
        const val COL_TOKENS_TAG = "tokensTag"
        const val COL_FLAGS_HASH = "flagsHash"

        const val COL_LOG_SOURCE_NAME = "logSourceName"

        const val COL_KEY = "key"
        const val COL_SERVER_TIMESTAMP = "servertimestamp"
        const val COL_TIMESTAMP = "timestamp"

        const val COL_SECRET = "secret"
        const val COL_DEVICE_ENCRYPTED_SECRET = "deviceEncryptedSecret"

        const val FLAG_TYPE_INT = 1
        const val FLAG_TYPE_BOOL = 2
        const val FLAG_TYPE_FLOAT = 3
        const val FLAG_TYPE_STRING = 4
        const val FLAG_TYPE_BYTES = 5

        private val directBootAwarePackages = setOf(
            PHENOTYPE_CONTENT_AUTHORITY,
            "com.google.android.gms.gcm",
            "com.google.android.apps.scone",
            "com.google.android.gms.clearcut.public",
            "com.google.android.gms.playlog.uploader",
            "com.google.android.gms.usagereporting",
            "com.google.android.gms.permissions",
            "com.google.android.gms.security.fmd",
            "com.google.android.marvin.talkback.directboot",
            "com.google.android.gms.time.device"
        )

        @Volatile
        private var credentialInstance: PhenotypeDatabase? = null
        @Volatile
        private var deviceInstance: PhenotypeDatabase? = null

        fun getInstance(context: Context): PhenotypeDatabase {
            val deviceProtected = SDK_INT >= 24 && try {
                context.isDeviceProtectedStorage
            } catch (_: NullPointerException) {
                false
            }
            val current = if (deviceProtected) deviceInstance else credentialInstance
            return current ?: synchronized(this) {
                val stored = if (deviceProtected) deviceInstance else credentialInstance
                stored ?: run {
                    val storageContext = if (deviceProtected) context else context.applicationContext
                    PhenotypeDatabase(storageContext).also {
                        if (deviceProtected) deviceInstance = it else credentialInstance = it
                    }
                }
            }
        }

        fun isDirectBootAware(packageName: String): Boolean = packageName in directBootAwarePackages
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_DOGFOODS_TOKEN (\"key\" INTEGER NOT NULL PRIMARY KEY, token BLOB)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_EXPERIMENT_STATES (
            stateId INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PACKAGE_NAME TEXT NOT NULL,
            $COL_USER TEXT NOT NULL DEFAULT '',
            $COL_VERSION INTEGER NOT NULL,
            $COL_CONFIG_HASH TEXT NOT NULL DEFAULT '',
            $COL_COMMITTED INTEGER NOT NULL DEFAULT 0
        )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_CROSS_TOKENS (
            fromPackageName TEXT NOT NULL, fromVersion INTEGER NOT NULL, fromUser TEXT NOT NULL,
            toPackageName TEXT NOT NULL, toVersion INTEGER NOT NULL, isCommitted INTEGER NOT NULL,
            token BLOB NOT NULL, provenance INTEGER NOT NULL)"""
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CHANGE_COUNTS ($COL_PACKAGE_NAME TEXT PRIMARY KEY, count INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APP_WIDE_PROPERTIES ($COL_ANDROID_PACKAGE_NAME TEXT PRIMARY KEY, properties BLOB)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_EXTERNAL_EXPERIMENTS (namespace TEXT PRIMARY KEY, $COL_ANDROID_PACKAGE_NAME TEXT NOT NULL, tokens BLOB NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_LAST_SYNC_AFTER ($COL_SCOPE INTEGER NOT NULL, \"$COL_KEY\" TEXT NOT NULL, $COL_SERVING_VERSION INTEGER NOT NULL DEFAULT 0, PRIMARY KEY ($COL_SCOPE, \"$COL_KEY\"))")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PACKAGES (
                $COL_PACKAGE_NAME TEXT NOT NULL PRIMARY KEY,
                $COL_VERSION INTEGER NOT NULL,
                $COL_PARAMS BLOB,
                $COL_DYNAMIC_PARAMS BLOB,
                $COL_HETERODYNE_INFO BLOB,
                $COL_BASELINE_CL INTEGER NOT NULL DEFAULT -1,
                $COL_RUNTIME_PROPERTIES BLOB,
                $COL_REGISTRATION_SOURCE INTEGER NOT NULL DEFAULT 0,
                $COL_CLIENT_DECLARATIVE BLOB,
                $COL_DECLARATIVE BLOB,
                $COL_WEAK INTEGER NOT NULL DEFAULT 0,
                $COL_ANDROID_PACKAGE_NAME TEXT NOT NULL,
                $COL_IS_SYNCED INTEGER DEFAULT 0,
                $COL_CONFIG_TIER INTEGER
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FLAGS (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_VERSION INTEGER NOT NULL,
                $COL_FLAG_TYPE INTEGER NOT NULL,
                $COL_PARTITION_ID INTEGER NOT NULL DEFAULT 0,
                $COL_USER TEXT NOT NULL DEFAULT '',
                $COL_NAME TEXT NOT NULL,
                $COL_INT_VAL INTEGER,
                $COL_BOOL_VAL INTEGER,
                $COL_FLOAT_VAL REAL,
                $COL_STRING_VAL TEXT,
                $COL_EXTENSION_VAL BLOB,
                $COL_COMMITTED INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY($COL_PACKAGE_NAME, $COL_VERSION, $COL_FLAG_TYPE, $COL_PARTITION_ID, $COL_USER, $COL_NAME, $COL_COMMITTED)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_EXPERIMENT_TOKENS (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_VERSION INTEGER NOT NULL,
                $COL_USER TEXT NOT NULL DEFAULT '',
                $COL_IS_COMMITTED INTEGER NOT NULL DEFAULT 0,
                $COL_EXPERIMENT_TOKEN BLOB,
                $COL_SERVER_TOKEN TEXT NOT NULL DEFAULT '',
                $COL_CONFIG_HASH TEXT NOT NULL DEFAULT '',
                $COL_SERVING_VERSION INTEGER NOT NULL DEFAULT 0,
                $COL_TOKENS_TAG BLOB,
                $COL_FLAGS_HASH INTEGER,
                PRIMARY KEY($COL_PACKAGE_NAME, $COL_VERSION, $COL_USER, $COL_IS_COMMITTED)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_LOG_SOURCES (
                $COL_LOG_SOURCE_NAME TEXT NOT NULL,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                PRIMARY KEY($COL_LOG_SOURCE_NAME, $COL_PACKAGE_NAME)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FLAG_OVERRIDES (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_USER TEXT NOT NULL DEFAULT '',
                $COL_NAME TEXT NOT NULL,
                $COL_FLAG_TYPE INTEGER NOT NULL,
                $COL_INT_VAL INTEGER,
                $COL_BOOL_VAL INTEGER,
                $COL_FLOAT_VAL REAL,
                $COL_STRING_VAL TEXT,
                $COL_EXTENSION_VAL BLOB,
                $COL_COMMITTED INTEGER DEFAULT 0,
                PRIMARY KEY($COL_PACKAGE_NAME, $COL_USER, $COL_NAME, $COL_COMMITTED)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_APPLICATION_STATES (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_USER TEXT NOT NULL DEFAULT '',
                $COL_VERSION INTEGER NOT NULL,
                PRIMARY KEY($COL_PACKAGE_NAME, $COL_USER)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_LAST_FETCH (
                "$COL_KEY" TEXT NOT NULL PRIMARY KEY,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_APPLICATION_TAGS (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_VERSION INTEGER NOT NULL,
                $COL_PARTITION_ID INTEGER NOT NULL,
                $COL_USER TEXT NOT NULL DEFAULT '',
                tag BLOB NOT NULL,
                PRIMARY KEY($COL_PACKAGE_NAME, $COL_VERSION, $COL_PARTITION_ID, $COL_USER)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_WEAK_EXPERIMENT_IDS (
                $COL_PACKAGE_NAME TEXT NOT NULL,
                experimentId INTEGER NOT NULL,
                PRIMARY KEY($COL_PACKAGE_NAME, experimentId)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REQUEST_TAGS (
                $COL_USER TEXT NOT NULL PRIMARY KEY,
                bytesTag BLOB
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_STORAGE_INFOS (
                androidPackageName TEXT NOT NULL PRIMARY KEY,
                $COL_SECRET BLOB,
                $COL_DEVICE_ENCRYPTED_SECRET BLOB
            )
        """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_flags_package ON $TABLE_FLAGS($COL_PACKAGE_NAME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_flags_package_version ON $TABLE_FLAGS($COL_PACKAGE_NAME, $COL_VERSION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_experiment_states_package_user ON $TABLE_EXPERIMENT_STATES($COL_PACKAGE_NAME, $COL_USER, stateId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun registerPackage(
        registration: DeclarativeRegistration, user: String?,
        resource: DeclarativeRegistration?, baselineCl: Long?
    ): FetchReason {
        val originalName = registration.configPackage.orEmpty()
        val androidPackageName = registration.androidPackage.orEmpty()
        val version = registration.version?.toLong() ?: registration.versionCode ?: 0L
        val weak = registration.weak == true
        if (originalName.isEmpty()) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package name")
        if (androidPackageName.isEmpty()) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No Android package")
        }
        if (registration.logSources.any { it.isEmpty() }) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid log source")
        }
        if (!weak && registration.weakExperimentIds.isNotEmpty()) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Experiment ids for weak registration only")
        }
        val source = registration.registrationSource?.value ?: 0
        if (source !in 1..2) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Unknown registration type")
        }
        val normalizedParams = registration.params?.takeIf { it.size > 0 }?.toByteArray()
        val resourceBytes = resource?.encode()
        val clientDeclaration = resourceBytes.takeIf { source == RegistrationSource.CLIENT.value }
        val suppliedInfo = registration.heterodyneInfo
        val resourceInfo = resource?.heterodyneInfo
        val heterodyne = if (suppliedInfo == null && resourceInfo != null)
            resourceInfo.toByteArray() else suppliedInfo?.takeIf { it.size > 0 }?.toByteArray()
        val db = writableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) db.beginTransaction()
        try {
            val packageName = resolvePackageName(originalName, androidPackageName)
            if (packageName != originalName) {
                if (deletePackageRows(db, originalName) > 0) db.delete(TABLE_REQUEST_TAGS, null, null)
            }
            var exists = false
            var eligible = false
            var versionMatch = false
            var paramsMatch = false
            var changed = true
            var fastPath = false
            var dynamicParams: ByteArray? = null
            var runtimeProperties: ByteArray? = null
            var oldResource: ByteArray? = null
            val oldLogSources = db.query(
                TABLE_LOG_SOURCES, arrayOf(COL_LOG_SOURCE_NAME), "$COL_PACKAGE_NAME = ?",
                arrayOf(packageName), null, null, null
            ).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            db.query(
                TABLE_PACKAGES, arrayOf(
                    COL_ANDROID_PACKAGE_NAME, COL_VERSION, COL_PARAMS, COL_DYNAMIC_PARAMS,
                    COL_WEAK, COL_BASELINE_CL, COL_HETERODYNE_INFO, COL_IS_SYNCED, COL_CLIENT_DECLARATIVE,
                    COL_RUNTIME_PROPERTIES, COL_DECLARATIVE
                ), "$COL_PACKAGE_NAME = ?", arrayOf(packageName),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    exists = true
                    if (cursor.getString(0) != androidPackageName) {
                        throw PhenotypeException(
                            PhenotypeStatusCode.REGISTRATION_CONFLICT,
                            "Configuration package is registered with a different Android package"
                        )
                    }
                    val oldClientDeclaration = if (cursor.isNull(8)) null else cursor.getBlob(8)
                    val oldDynamic = if (cursor.isNull(3)) null else cursor.getBlob(3)
                    dynamicParams = oldDynamic?.takeUnless {
                        registration.deleteRuntimeProperties == true || it.contentEquals(normalizedParams)
                    }
                    runtimeProperties = if (cursor.isNull(9)) null else cursor.getBlob(9)
                    oldResource = if (cursor.isNull(10)) null else cursor.getBlob(10)
                    fastPath = source == RegistrationSource.RESOURCE.value &&
                            oldClientDeclaration.contentEquals(registration.copy(configPackage = packageName).encode())
                    if (fastPath) {
                        val oldInfo = if (cursor.isNull(6)) null else cursor.getBlob(6)
                        if (suppliedInfo != null && suppliedInfo.size > 0 && !suppliedInfo.toByteArray().contentEquals(oldInfo)) {
                            invalidateSyncRegistration(packageName)
                            db.update(
                                TABLE_PACKAGES, ContentValues().apply { put(COL_HETERODYNE_INFO, suppliedInfo.toByteArray()) },
                                "$COL_PACKAGE_NAME = ?", arrayOf(packageName)
                            )
                        }
                    } else {
                        val sameVersion = cursor.getLong(1) == version
                        val sameParams = normalizedParams.contentEquals(if (cursor.isNull(2)) null else cursor.getBlob(2))
                        changed = !sameVersion || !sameParams || !oldDynamic.contentEquals(dynamicParams) ||
                                (cursor.getInt(4) != 0) != weak || (baselineCl != null && baselineCl != cursor.getLong(5)) ||
                                !heterodyne.contentEquals(if (cursor.isNull(6)) null else cursor.getBlob(6)) ||
                                !clientDeclaration.contentEquals(oldClientDeclaration)
                        if (user != null || cursor.isNull(7) || cursor.getInt(7) != 0) {
                            eligible = true
                            versionMatch = sameVersion
                            paramsMatch = sameParams
                        }
                    }
                }
            }
            if (!fastPath) {
                val declarationChanged = resourceBytes != null && !resourceBytes.contentEquals(oldResource)
                if (changed || declarationChanged) {
                    invalidateSyncRegistration(packageName)
                    val values = ContentValues().apply {
                        put(COL_PACKAGE_NAME, packageName)
                        put(COL_VERSION, version)
                        put(COL_ANDROID_PACKAGE_NAME, androidPackageName)
                        put(COL_WEAK, if (weak) 1 else 0)
                        put(COL_IS_SYNCED, 0)
                        put(COL_BASELINE_CL, baselineCl ?: registration.baselineCl ?: 0L)
                        put(COL_PARAMS, normalizedParams)
                        put(COL_DYNAMIC_PARAMS, dynamicParams)
                        put(COL_RUNTIME_PROPERTIES, runtimeProperties)
                        put(COL_HETERODYNE_INFO, heterodyne)
                        put(COL_REGISTRATION_SOURCE, source)
                        put(COL_CLIENT_DECLARATIVE, clientDeclaration)
                        if (!exists || declarationChanged) put(COL_DECLARATIVE, resourceBytes)
                    }
                    if (exists) db.update(TABLE_PACKAGES, values, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    else db.insertOrThrow(TABLE_PACKAGES, null, values)
                }
                db.delete(TABLE_WEAK_EXPERIMENT_IDS, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                for (id in registration.weakExperimentIds.distinct()) {
                    db.insertOrThrow(TABLE_WEAK_EXPERIMENT_IDS, null, ContentValues().apply {
                        put(COL_PACKAGE_NAME, packageName)
                        put("experimentId", id)
                    })
                }
            }
            val logSources = registrationLogSources(packageName, if (fastPath) oldLogSources else registration.logSources)
            for (removed in oldLogSources - logSources) {
                db.delete(TABLE_LOG_SOURCES, "$COL_PACKAGE_NAME = ? AND $COL_LOG_SOURCE_NAME = ?", arrayOf(packageName, removed))
            }
            for (added in logSources - oldLogSources) {
                db.insertOrThrow(TABLE_LOG_SOURCES, null, ContentValues().apply {
                    put(COL_PACKAGE_NAME, packageName)
                    put(COL_LOG_SOURCE_NAME, added)
                })
            }
            if (!versionMatch && (paramsMatch || dynamicParams != null) && user == null) {
                versionMatch = db.query(
                    TABLE_EXPERIMENT_TOKENS, arrayOf(COL_PACKAGE_NAME),
                    "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ?", arrayOf(packageName, version.toString()),
                    null, null, null, "1"
                ).use { it.moveToFirst() }
            }
            val missingUserTag = user != null && db.query(
                TABLE_REQUEST_TAGS, arrayOf(COL_USER),
                "$COL_USER = ?", arrayOf(user), null, null, null, "1"
            ).use { !it.moveToFirst() }
            val reason = when {
                fastPath -> FetchReason.UNSPECIFIED
                missingUserTag -> FetchReason.NEW_USER_SYNC
                !eligible -> if (user == null) FetchReason.NEW_APPLICATION_SYNC else FetchReason.NEW_APPLICATION
                !versionMatch -> if (user == null) FetchReason.NEW_REGISTER_VERSION_SYNC else FetchReason.NEW_REGISTER_VERSION
                !paramsMatch && dynamicParams == null ->
                    if (user == null) FetchReason.NEW_REGISTER_OTHER_SYNC else FetchReason.NEW_REGISTER_OTHER

                else -> FetchReason.UNSPECIFIED
            }
            if (ownsTransaction) db.setTransactionSuccessful()
            return reason
        } finally {
            if (ownsTransaction) db.endTransaction()
        }
    }

    private fun registrationLogSources(packageName: String, requested: Collection<String>): Set<String> {
        val policy = configurationMetadata(packageName)?.logSources
            ?: packageName.substringBefore('#').takeUnless { it == packageName }
                ?.let { configurationMetadata(it)?.logSources }
            ?: return requested.toSet()
        return if (policy.replace == true) policy.names.toSet() else requested.toSet() + policy.names
    }

    private fun configurationMetadata(packageName: String): ConfigurationPackageMetadata? {
        val encodedName = packageName.replace("_", "_u_").replace(".", "_d_").replace("#", "_p_")
        val flag = internalFlag("dynamic_ph_pkgcfg_$encodedName") ?: return null
        if (flag.dataType != Flag.DATA_TYPE_BYTES) return null
        return try {
            ConfigurationPackageMetadata.ADAPTER.decode(flag.getBytes())
        } catch (_: java.io.IOException) {
            Log.w(PHENOTYPE_LOG_TAG, "Invalid namespace metadata")
            null
        }
    }

    fun getSyncAfterPackages(packageName: String?, androidPackageName: String?, servingVersion: Long): List<String> {
        if ((packageName == null) == (androidPackageName == null)) {
            throw PhenotypeException(
                PhenotypeStatusCode.INVALID_ARGUMENT,
                "Exactly one configuration package or Android package is required"
            )
        }
        val column = if (packageName != null) COL_PACKAGE_NAME else COL_ANDROID_PACKAGE_NAME
        val scope = if (packageName != null) SYNC_SCOPE_PACKAGE else SYNC_SCOPE_APPLICATION
        val args = arrayOf(requireNotNull(packageName ?: androidPackageName))
        val db = readableDatabase
        db.beginTransaction()
        try {
            var hasUnsyncedPackage = false
            val packages = db.query(
                TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME, COL_IS_SYNCED), "$column = ?",
                args, null, null, COL_PACKAGE_NAME
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(0))
                        if (cursor.getInt(1) == 0) hasUnsyncedPackage = true
                    }
                }
            }
            if (packages.isEmpty()) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            val repeated = db.query(
                TABLE_LAST_SYNC_AFTER, arrayOf(COL_SERVING_VERSION),
                "$COL_SCOPE = ? AND \"$COL_KEY\" = ? AND $COL_SERVING_VERSION = ?",
                arrayOf(scope.toString(), args[0], servingVersion.toString()),
                null, null, null
            ).use { it.moveToFirst() }
            if (repeated && !hasUnsyncedPackage) return emptyList()
            val hasOutdatedPackage = db.rawQuery(
                """
                SELECT EXISTS (
                    SELECT 1 FROM $TABLE_PACKAGES packages
                    LEFT JOIN $TABLE_EXPERIMENT_TOKENS tokens
                        ON tokens.$COL_PACKAGE_NAME = packages.$COL_PACKAGE_NAME
                        AND tokens.$COL_IS_COMMITTED = 0
                    WHERE packages.$column = ?
                        AND (tokens.$COL_PACKAGE_NAME IS NULL OR tokens.$COL_SERVING_VERSION < ?)
                )
            """.trimIndent(), arrayOf(args[0], servingVersion.toString())
            ).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) != 0
            }
            return if (hasUnsyncedPackage || hasOutdatedPackage) packages else emptyList()
        } finally {
            db.endTransaction()
        }
    }

    fun recordSyncAfter(packageName: String?, androidPackageName: String?, servingVersion: Long) {
        require((packageName == null) != (androidPackageName == null))
        val values = ContentValues().apply {
            put(COL_SCOPE, if (packageName != null) SYNC_SCOPE_PACKAGE else SYNC_SCOPE_APPLICATION)
            put(COL_KEY, packageName ?: androidPackageName)
            put(COL_SERVING_VERSION, servingVersion)
        }
        check(
            writableDatabase.insertWithOnConflict(
                TABLE_LAST_SYNC_AFTER, null, values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        ) { "Sync completion record failed" }
    }

    fun incrementChangeCount(db: SQLiteDatabase, packageName: String) {
        db.execSQL("INSERT OR IGNORE INTO $TABLE_CHANGE_COUNTS ($COL_PACKAGE_NAME, count) VALUES (?, 0)", arrayOf(packageName))
        db.execSQL("UPDATE $TABLE_CHANGE_COUNTS SET count = count + 1 WHERE $COL_PACKAGE_NAME = ?", arrayOf(packageName))
    }

    private fun getChangeCount(packageName: String): Long =
        readableDatabase.rawQuery("SELECT count FROM $TABLE_CHANGE_COUNTS WHERE $COL_PACKAGE_NAME = ?", arrayOf(packageName)).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }

    fun getConfigPackageNamesByAndroidPackage(androidPackageName: String): List<String> =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME),
            "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName), null, null, COL_PACKAGE_NAME
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    fun getResourceConfigPackageNamesByAndroidPackage(androidPackageName: String): List<String> =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME),
            "$COL_ANDROID_PACKAGE_NAME = ? AND $COL_REGISTRATION_SOURCE = ?",
            arrayOf(androidPackageName, RegistrationSource.RESOURCE.value.toString()), null, null, COL_PACKAGE_NAME
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    fun getResourceRegistrationOwners(): Set<String> =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_ANDROID_PACKAGE_NAME),
            "$COL_REGISTRATION_SOURCE = ?", arrayOf(RegistrationSource.RESOURCE.value.toString()),
            null, null, COL_ANDROID_PACKAGE_NAME
        ).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private fun internalFlag(name: String): Flag? =
        getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, name)

    fun resolvePackageName(packageName: String, caller: String): String {
        if (packageName.isEmpty() || packageName == "all" || caller == "com.google.android.apps.mobileutilities") return packageName
        val namespace = packageName.substringBefore('#')
        val metadata = configurationMetadata(namespace) ?: return packageName
        val subpackage = metadata.subpackage ?: return packageName
        val owner = if (subpackage.forceCaller == true) caller else packageName.substringAfter('#', caller)
        return "$namespace#$owner"
    }

    fun allowsThirdPartyNamespace(namespace: String): Boolean {
        val configured = internalFlag("content_provider_3p_whitelist")?.takeIf { it.dataType == Flag.DATA_TYPE_STRING }?.getString()
        val allowed = configured ?: "com.google.android.gms.clearcut.public,com.google.android.gms.vision.sdk," +
        "com.google.android.gms.measurement,com.google.android.gms.icing,com.google.android.cruiser," +
        "com.google.android.gms.auth_account,com.google.android.gms.maps,com.google.android.gms.tflite," +
        "com.google.android.libraries.mapsplatform.transportation.driver,com.google.android.gms.wallet_dynamite," +
        "com.android.billingclient,nav_android_sdk,transportation_consumer_sdk_android," +
        "com.google.android.gms.cast_dynamite,com.google.android.gms.providerinstaller"
        return namespace.isNotEmpty() && namespace in allowed.split(',')
    }

    fun setAppSpecificProperties(packageName: String, androidPackageName: String, properties: ByteArray?): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = db.query(
                TABLE_PACKAGES, arrayOf(COL_PARAMS, COL_DYNAMIC_PARAMS),
                "$COL_PACKAGE_NAME = ? AND $COL_ANDROID_PACKAGE_NAME = ?",
                arrayOf(packageName, androidPackageName), null, null, null
            ).use { cursor ->
                if (!cursor.moveToFirst()) return -1
                val params = if (cursor.isNull(0)) null else cursor.getBlob(0)
                val current = if (cursor.isNull(1)) null else cursor.getBlob(1)
                val desired = if (params.contentEquals(properties)) null else properties
                if (current.contentEquals(desired)) return 0
                ContentValues().apply { put(COL_DYNAMIC_PARAMS, desired) }
            }
            val changed = db.update(TABLE_PACKAGES, values, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
            if (changed > 0) invalidateSyncRegistration(packageName)
            db.setTransactionSuccessful()
            return changed
        } finally {
            db.endTransaction()
        }
    }

    fun setRuntimeProperties(packageName: String, androidPackageName: String, properties: ByteArray?): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.query(
                TABLE_PACKAGES, arrayOf(COL_RUNTIME_PROPERTIES),
                "$COL_PACKAGE_NAME = ? AND $COL_ANDROID_PACKAGE_NAME = ?",
                arrayOf(packageName, androidPackageName), null, null, null
            ).use { cursor ->
                if (!cursor.moveToFirst()) return -1
                val current = if (cursor.isNull(0)) null else cursor.getBlob(0)
                if (current.contentEquals(properties)) return 0
            }
            val changed = db.update(
                TABLE_PACKAGES,
                ContentValues().apply { put(COL_RUNTIME_PROPERTIES, properties) },
                "$COL_PACKAGE_NAME = ?", arrayOf(packageName)
            )
            if (changed > 0) invalidateSyncRegistration(packageName)
            db.setTransactionSuccessful()
            return changed
        } finally {
            db.endTransaction()
        }
    }

    fun setAppWideProperties(androidPackageName: String, properties: ByteArray?) {
        require(androidPackageName.isNotEmpty()) { "No calling package" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = db.query(
                TABLE_APP_WIDE_PROPERTIES, arrayOf("properties"),
                "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName), null, null, null
            ).use {
                if (it.moveToFirst() && !it.isNull(0)) it.getBlob(0) else null
            }
            if (current.contentEquals(properties)) return
            check(db.insertWithOnConflict(TABLE_APP_WIDE_PROPERTIES, null, ContentValues().apply {
                put(COL_ANDROID_PACKAGE_NAME, androidPackageName)
                put("properties", properties)
            }, SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "Application properties write failed" }
            invalidateSyncRegistrations()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getDogfoodsToken(): ByteArray? = readableDatabase.query(
        TABLE_DOGFOODS_TOKEN, arrayOf("token"),
        "\"key\" = 0", null, null, null, null
    ).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getBlob(0) else null
    }

    fun getServingVersion(): Long = readableDatabase.query(
        TABLE_LAST_FETCH, arrayOf(COL_TIMESTAMP),
        "$COL_KEY = ?", arrayOf("last_sync"), null, null, null
    ).use {
        if (it.moveToFirst()) it.getLong(0) else -1L
    }

    fun setDogfoodsToken(token: ByteArray?) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (getDogfoodsToken().contentEquals(token)) return
            db.replaceOrThrow(TABLE_DOGFOODS_TOKEN, null, ContentValues().apply {
                put("key", 0)
                put("token", token)
            })
            invalidateSyncRegistrations()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun createSyncRequest(packageVersions: Map<String, Long>, users: List<String>, reason: FetchReason): SyncRequestSnapshot {
        require(users.isNotEmpty() && users.distinct().size == users.size) { "Invalid request accounts" }
        val db = readableDatabase
        db.beginTransaction()
        try {
            val accounts = users.filter { it.isNotEmpty() }
            val names = packageVersions.keys.filterNot { it.startsWith(PHENOTYPE_INTERNAL_PACKAGE_PREFIX) }

            data class RequestRegistration(
                val version: PackageVersion, val params: ByteString?,
                val heterodyne: ByteString?, val runtime: ByteString?
            )

            val registrations = mutableMapOf<String, RequestRegistration>()
            val partitionTags = mutableMapOf<ConfigurationKey, MutableList<PartitionTag>>()
            val tokensTags = mutableMapOf<ConfigurationKey, ByteString>()
            for (batch in names.chunked(SYNC_QUERY_BATCH_SIZE)) {
                val placeholders = batch.joinToString(",") { "?" }
                db.query(
                    TABLE_PACKAGES, arrayOf(
                        COL_PACKAGE_NAME, COL_VERSION, COL_ANDROID_PACKAGE_NAME,
                        COL_PARAMS, COL_DYNAMIC_PARAMS, COL_BASELINE_CL, COL_HETERODYNE_INFO, COL_RUNTIME_PROPERTIES
                    ),
                    "$COL_PACKAGE_NAME IN ($placeholders)", batch.toTypedArray(), null, null, null
                ).use { row ->
                    while (row.moveToNext()) {
                        val name = row.getString(0)
                        check(row.getLong(1) == packageVersions[name]) { "Registration changed before sync" }
                        val params = if (row.isNull(3)) null else row.getBlob(3).toByteString()
                        val dynamic = if (row.isNull(4)) null else row.getBlob(4).toByteString()
                        val heterodyne = if (row.isNull(6)) null else row.getBlob(6).toByteString()
                        val runtime = if (row.isNull(7)) null else row.getBlob(7).toByteString()
                        registrations[name] = RequestRegistration(
                            PackageVersion(
                                packageName = name, version = row.getLong(1),
                                baselineCl = row.getLong(5), androidPackageName = row.getString(2)
                            ),
                            if (runtime != null) params else dynamic ?: params, heterodyne, runtime
                        )
                    }
                }
                check(batch.all { it in registrations }) { "Registration changed before sync" }
                for (userBatch in users.chunked(SYNC_QUERY_BATCH_SIZE)) {
                    val userPlaceholders = userBatch.joinToString(",") { "?" }
                    val args = (batch + userBatch).toTypedArray()
                    val selection = "p.$COL_PACKAGE_NAME IN ($placeholders) AND t.$COL_USER IN ($userPlaceholders)"
                    val join = "INNER JOIN $TABLE_PACKAGES p ON t.$COL_PACKAGE_NAME = p.$COL_PACKAGE_NAME " +
                            "AND t.$COL_VERSION = p.$COL_VERSION"
                    db.query(
                        "$TABLE_APPLICATION_TAGS t $join", arrayOf(
                            "t.$COL_PACKAGE_NAME", "t.$COL_VERSION",
                            "t.$COL_USER", "t.$COL_PARTITION_ID", "t.tag"
                        ), selection, args, null, null,
                        "t.$COL_PARTITION_ID"
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            val key = ConfigurationKey(cursor.getString(0), cursor.getLong(1), cursor.getString(2))
                            partitionTags.getOrPut(key) { mutableListOf() }.add(
                                PartitionTag(cursor.getLong(3), cursor.getBlob(4).toByteString())
                            )
                        }
                    }
                    db.query(
                        "$TABLE_EXPERIMENT_TOKENS t $join", arrayOf(
                            "t.$COL_PACKAGE_NAME",
                            "t.$COL_VERSION", "t.$COL_USER", "t.$COL_TOKENS_TAG"
                        ),
                        "$selection AND t.$COL_IS_COMMITTED = 0", args, null, null, null
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            if (!cursor.isNull(3)) tokensTags[ConfigurationKey(
                                cursor.getString(0), cursor.getLong(1),
                                cursor.getString(2)
                            )] = cursor.getBlob(3).toByteString()
                        }
                    }
                }
            }
            val requests = mutableListOf<PackageRequest>()
            for (name in names) {
                val registration = registrations.getValue(name)
                for (user in users) {
                    val key = ConfigurationKey(name, packageVersions.getValue(name), user)
                    requests.add(
                        PackageRequest(
                            version = registration.version.copy(
                                signatureInfo = SignatureInfo(if (user.isEmpty()) -1 else accounts.indexOf(user))
                            ),
                            params = registration.params, partitionTags = partitionTags[key] ?: emptyList(),
                            tokensTag = tokensTags[key], heterodyneInfo = registration.heterodyne,
                            runtimeProperties = registration.runtime
                        )
                    )
                }
            }
            val properties = mutableMapOf<String, AppWideProperties>()
            db.query(
                TABLE_APP_WIDE_PROPERTIES, arrayOf(COL_ANDROID_PACKAGE_NAME, "properties"),
                "properties IS NOT NULL", null, null, null, COL_ANDROID_PACKAGE_NAME
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val decoded = AppWideProperties.ADAPTER.decode(cursor.getBlob(1))
                        properties[cursor.getString(0)] = AppWideProperties(decoded.properties ?: ByteString.EMPTY)
                    } catch (_: java.io.IOException) {
                        Log.w(PHENOTYPE_LOG_TAG, "Ignoring malformed application-wide properties")
                    }
                }
            }
            val tagKey = if (users.size > 1) ALL_USERS else users.single()
            val requestTag = db.query(
                TABLE_REQUEST_TAGS, arrayOf("bytesTag"), "$COL_USER = ?",
                arrayOf(tagKey), null, null, null
            ).use {
                if (it.moveToFirst() && !it.isNull(0)) it.getBlob(0).toByteString() else ByteString.EMPTY
            }
            val clientInfo = ClientInfo(
                numAccounts = if (users.size == 1) accounts.size.toLong() else null,
                dogfoodsToken = getDogfoodsToken()?.toByteString() ?: ByteString.EMPTY
            )
            val revisions = synchronized(syncRegistrations) {
                names.associateWith { syncRegistrations.revisions.getOrPut(it) { ++syncRegistrations.nextRevision } }
            }
            return SyncRequestSnapshot(
                SyncRequest(
                    clientInfo = clientInfo, packages = requests, bytesTag = requestTag,
                    fetchReason = reason, appProperties = properties
                ), revisions
            )
        } finally {
            db.endTransaction()
        }
    }

    fun updateRequestTag(db: SQLiteDatabase, users: List<String>, tag: ByteString?) {
        if (tag == null || tag.size == 0) return
        val key = if (users.size > 1) ALL_USERS else users.single()
        val old = db.query(
            TABLE_REQUEST_TAGS, arrayOf("bytesTag"), "$COL_USER = ?",
            arrayOf(key), null, null, null
        ).use {
            if (it.moveToFirst() && !it.isNull(0)) it.getBlob(0).toByteString() else null
        }
        if (old == tag) return
        fun write(user: String, bytes: ByteArray) {
            db.insertOrThrow(TABLE_REQUEST_TAGS, null, ContentValues().apply {
                put(COL_USER, user)
                put("bytesTag", bytes)
            })
        }
        db.delete(TABLE_REQUEST_TAGS, "$COL_USER = ?", arrayOf(key))
        write(key, tag.toByteArray())
        if (users.size > 1) {
            for (user in users) {
                db.delete(TABLE_REQUEST_TAGS, "$COL_USER = ?", arrayOf(user))
                write(user, byteArrayOf())
            }
        } else {
            db.delete(TABLE_REQUEST_TAGS, "$COL_USER = ?", arrayOf(ALL_USERS))
        }
    }

    fun setExternalExperiments(androidPackageName: String, namespace: String, tokens: List<ByteArray>) {
        require(androidPackageName.isNotEmpty()) { "No calling package" }
        val encoded = ExternalExperimentTokenList(tokens.map { it.toByteString() }).encode()
        val db = writableDatabase
        db.beginTransaction()
        try {
            registerPackage(
                clientRegistration(
                    "$EXTERNAL_IDS_PACKAGE_PREFIX$namespace", 0, androidPackageName,
                    emptyArray(), null, true, intArrayOf()
                ), null, null, -1L
            )
            check(db.insertWithOnConflict(TABLE_EXTERNAL_EXPERIMENTS, null, ContentValues().apply {
                put("namespace", namespace)
                put(COL_ANDROID_PACKAGE_NAME, androidPackageName)
                put("tokens", encoded)
            }, SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "External experiments write failed" }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getExternalExperiments(namespace: String): List<ByteArray>? =
        readableDatabase.query(
            TABLE_EXTERNAL_EXPERIMENTS, arrayOf("tokens"), "namespace = ?",
            arrayOf(namespace), null, null, null
        ).use {
            if (it.moveToFirst()) ExternalExperimentTokenList.ADAPTER.decode(it.getBlob(0)).tokens.map { token -> token.toByteArray() }
            else null
        }

    fun getPackageVersion(packageName: String): Long? =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_VERSION), "$COL_PACKAGE_NAME = ?",
            arrayOf(packageName), null, null, null
        ).use {
            if (it.moveToFirst()) it.getLong(0) else null
        }

    fun packageExists(packageName: String): Boolean =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME),
            "$COL_PACKAGE_NAME = ?", arrayOf(packageName), null, null, null, "1"
        ).use { it.moveToFirst() }

    fun getAndroidPackageName(packageName: String): String? =
        readableDatabase.query(
            TABLE_PACKAGES, arrayOf(COL_ANDROID_PACKAGE_NAME), "$COL_PACKAGE_NAME = ?",
            arrayOf(packageName), null, null, null
        ).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    fun getAndroidPackageNames(packageNames: Collection<String>): Map<String, String> {
        val names = packageNames.distinct()
        if (names.isEmpty()) return emptyMap()
        val placeholders = names.joinToString(",") { "?" }
        return readableDatabase.query(
            TABLE_PACKAGES,
            arrayOf(COL_PACKAGE_NAME, COL_ANDROID_PACKAGE_NAME),
            "$COL_PACKAGE_NAME IN ($placeholders)", names.toTypedArray(), null, null, null
        ).use { cursor ->
            buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1)) }
        }
    }

    fun getPendingUpdatePackages(androidPackageName: String? = null): Set<String> {
        val applicationFilter = if (androidPackageName == null) "" else
            "AND packages.$COL_ANDROID_PACKAGE_NAME = ?"
        val args = androidPackageName?.let { arrayOf(it) }
        return readableDatabase.rawQuery(
            """
            SELECT DISTINCT packages.$COL_PACKAGE_NAME
            FROM $TABLE_PACKAGES packages
            LEFT JOIN $TABLE_EXPERIMENT_TOKENS committed
                ON committed.$COL_PACKAGE_NAME = packages.$COL_PACKAGE_NAME
                AND committed.$COL_IS_COMMITTED = 1
            INNER JOIN $TABLE_EXPERIMENT_TOKENS pending
                ON pending.$COL_PACKAGE_NAME = packages.$COL_PACKAGE_NAME
                AND pending.$COL_USER = IFNULL(committed.$COL_USER, '')
                AND pending.$COL_IS_COMMITTED = 0
                AND pending.$COL_VERSION = (
                    SELECT MAX(candidate.$COL_VERSION)
                    FROM $TABLE_EXPERIMENT_TOKENS candidate
                    WHERE candidate.$COL_PACKAGE_NAME = packages.$COL_PACKAGE_NAME
                        AND candidate.$COL_USER = IFNULL(committed.$COL_USER, '')
                        AND candidate.$COL_IS_COMMITTED = 0
                        AND candidate.$COL_VERSION <= packages.$COL_VERSION
                )
            WHERE (
                committed.$COL_PACKAGE_NAME IS NULL
                OR pending.$COL_EXPERIMENT_TOKEN IS NOT committed.$COL_EXPERIMENT_TOKEN
                OR pending.$COL_SERVER_TOKEN IS NOT committed.$COL_SERVER_TOKEN
                OR pending.$COL_CONFIG_HASH IS NOT committed.$COL_CONFIG_HASH
            )
            $applicationFilter
            ORDER BY packages.$COL_PACKAGE_NAME
        """.trimIndent(), args
        ).use { cursor ->
            linkedSetOf<String>().apply { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
    }

    private fun deletePackageRows(db: SQLiteDatabase, packageName: String): Int {
        invalidateSyncRegistration(packageName)
        val args = arrayOf(packageName)
        val deleted = db.delete(
            TABLE_CROSS_TOKENS, "fromPackageName = ? OR toPackageName = ?",
            arrayOf(packageName, packageName)
        ) +
                listOf(
                    TABLE_PACKAGES, TABLE_FLAGS, TABLE_EXPERIMENT_TOKENS, TABLE_EXPERIMENT_STATES, TABLE_LOG_SOURCES,
                    TABLE_FLAG_OVERRIDES, TABLE_APPLICATION_STATES, TABLE_WEAK_EXPERIMENT_IDS, TABLE_APPLICATION_TAGS
                )
                    .sumOf { db.delete(it, "$COL_PACKAGE_NAME = ?", args) }
        if (packageName.startsWith(EXTERNAL_IDS_PACKAGE_PREFIX)) {
            db.delete(
                TABLE_EXTERNAL_EXPERIMENTS, "namespace = ?",
                arrayOf(packageName.removePrefix(EXTERNAL_IDS_PACKAGE_PREFIX))
            )
        }
        if (deleted > 0) invalidateSyncRegistrations()
        return deleted
    }

    private fun deleteDeviceProtectedPackageState(packageNames: Collection<String>, unregister: Boolean) {
        if (SDK_INT < 24 || storageContext.isDeviceProtectedStorage) return
        val mirroredPackages = packageNames.filter(::isDirectBootAware)
        if (mirroredPackages.isEmpty()) return
        PhenotypeDatabase(storageContext.createDeviceProtectedStorageContext()).use { helper ->
            val target = helper.writableDatabase
            target.beginTransaction()
            try {
                var deleted = 0
                for (packageName in mirroredPackages) {
                    if (unregister) {
                        deleted += helper.deletePackageRows(target, packageName)
                    } else {
                        val args = arrayOf(packageName)
                        target.delete(TABLE_FLAGS, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", args)
                        target.delete(TABLE_EXPERIMENT_TOKENS, "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", args)
                        target.delete(TABLE_FLAG_OVERRIDES, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", args)
                        target.delete(TABLE_APPLICATION_STATES, "$COL_PACKAGE_NAME = ?", args)
                        target.delete(TABLE_EXPERIMENT_STATES, "$COL_PACKAGE_NAME = ?", args)
                        target.delete(TABLE_CROSS_TOKENS, "fromPackageName = ? AND isCommitted = 1", args)
                    }
                }
                if (deleted > 0) target.delete(TABLE_REQUEST_TAGS, null, null)
                target.setTransactionSuccessful()
            } finally {
                target.endTransaction()
            }
        }
    }

    @JvmOverloads
    fun unregisterPackage(packageName: String, androidPackageName: String? = null): Boolean = synchronized(sharedPublicationLock) {
        val db = writableDatabase
        var sharedStorage: StorageInfo? = null
        val success = try {
            db.beginTransaction()
            try {
                val owner = androidPackageName ?: getAndroidPackageName(packageName)
                var deleted = deletePackageRows(db, packageName)
                if (owner != null && getConfigPackageNamesByAndroidPackage(owner).isEmpty()) {
                    sharedStorage = getStorageInfo(owner)
                    deleted += db.delete(TABLE_STORAGE_INFOS, "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(owner))
                }
                if (deleted > 0) db.delete(TABLE_REQUEST_TAGS, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            true
        } catch (e: Exception) {
            Log.e(PHENOTYPE_LOG_TAG, "Failed to unregister $packageName", e)
            false
        }
        if (!success) return false
        sharedStorage?.let { PhenotypeSharedStorage.delete(storageContext, it) }
        return try {
            deleteDeviceProtectedPackageState(listOf(packageName), unregister = true)
            true
        } catch (e: Exception) {
            Log.e(PHENOTYPE_LOG_TAG, "Failed to remove direct-boot state for $packageName", e)
            false
        }
    }

    fun unregisterByAndroidPackage(androidPackageName: String): Int = synchronized(sharedPublicationLock) {
        val db = writableDatabase
        var sharedStorage: StorageInfo? = null
        var packages = emptyList<String>()
        db.beginTransaction()
        try {
            sharedStorage = getStorageInfo(androidPackageName)
            packages = getConfigPackageNamesByAndroidPackage(androidPackageName)
            val deleted = packages.sumOf { deletePackageRows(db, it) }
            if (deleted > 0) db.delete(TABLE_REQUEST_TAGS, null, null)
            db.delete(TABLE_EXTERNAL_EXPERIMENTS, "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName))
            if (db.delete(TABLE_APP_WIDE_PROPERTIES, "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName)) > 0) {
                invalidateSyncRegistrations()
            }
            db.delete(TABLE_STORAGE_INFOS, "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        sharedStorage?.let { PhenotypeSharedStorage.delete(storageContext, it) }
        deleteDeviceProtectedPackageState(packages, unregister = true)
        return packages.size
    }

    fun clearCommittedConfigurations(androidPackageName: String) = synchronized(sharedPublicationLock) {
        val db = writableDatabase
        var sharedStorage: StorageInfo? = null
        var packages = emptyList<String>()
        db.beginTransaction()
        try {
            packages = getConfigPackageNamesByAndroidPackage(androidPackageName)
            for (packageName in packages) {
                val args = arrayOf(packageName)
                db.delete(TABLE_FLAGS, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", args)
                db.delete(TABLE_EXPERIMENT_TOKENS, "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", args)
                db.delete(TABLE_FLAG_OVERRIDES, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", args)
                db.delete(TABLE_APPLICATION_STATES, "$COL_PACKAGE_NAME = ?", args)
                db.update(
                    TABLE_EXPERIMENT_STATES, ContentValues().apply { put(COL_COMMITTED, 0) },
                    "$COL_PACKAGE_NAME = ?", args
                )
                db.delete(TABLE_CROSS_TOKENS, "fromPackageName = ? AND isCommitted = 1", args)
            }
            if (packages.isNotEmpty()) {
                sharedStorage = getStorageInfo(androidPackageName)
                db.delete(TABLE_STORAGE_INFOS, "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        sharedStorage?.let { PhenotypeSharedStorage.delete(storageContext, it) }
        deleteDeviceProtectedPackageState(packages, unregister = false)
    }

    fun getFlags(packageName: String, version: Long, user: String = "", committed: Int = 0): List<Flag> =
        readFlags(
            TABLE_FLAGS,
            "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_COMMITTED = ?",
            arrayOf(packageName, version.toString(), user, committed.toString()),
            COL_NAME
        )

    private fun parseFlagFromCursor(cursor: Cursor): Flag? {
        val flagType = cursor.getInt(0)
        val name = cursor.getString(1)

        return when {
            !cursor.isNull(2) -> Flag(name, cursor.getLong(2), flagType)
            !cursor.isNull(3) -> Flag(name, cursor.getInt(3) != 0, flagType)
            !cursor.isNull(4) -> Flag(name, cursor.getDouble(4), flagType)
            !cursor.isNull(5) -> Flag(name, cursor.getString(5), flagType)
            !cursor.isNull(6) -> Flag(name, cursor.getBlob(6), flagType)
            else -> null
        }
    }

    private fun readFlags(
        table: String,
        selection: String,
        args: Array<String>,
        orderBy: String = COL_NAME
    ): List<Flag> =
        readableDatabase.query(
            table, arrayOf(
                COL_FLAG_TYPE, COL_NAME, COL_INT_VAL, COL_BOOL_VAL,
                COL_FLOAT_VAL, COL_STRING_VAL, COL_EXTENSION_VAL
            ), selection, args, null, null, orderBy
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) parseFlagFromCursor(cursor)?.let(::add) }
        }

    fun getCommittedFlag(packageName: String, name: String, flagType: Int? = null): Flag? {
        val selection = buildString {
            append("$COL_PACKAGE_NAME = ? AND $COL_NAME = ? AND $COL_COMMITTED = 1")
            if (flagType != null) append(" AND $COL_FLAG_TYPE = ?")
        }
        val args = if (flagType == null) arrayOf(packageName, name)
        else arrayOf(packageName, name, flagType.toString())
        return mergeFlags(
            readFlags(TABLE_FLAGS, selection, args),
            readFlags(TABLE_FLAG_OVERRIDES, selection, args)
        ).values.minByOrNull { it.flagType }
    }

    private fun mergeFlags(flags: List<Flag>, overrides: List<Flag>): Map<Pair<Int, String>, Flag> {
        val merged = linkedMapOf<Pair<Int, String>, Flag>()
        for (flag in flags + overrides) merged[flag.flagType to flag.name] = flag
        return merged
    }

    private fun flagsEqual(first: Flag, second: Flag): Boolean {
        if (first.name != second.name || first.flagType != second.flagType || first.dataType != second.dataType) return false
        return when (first.dataType) {
            Flag.DATA_TYPE_LONG -> first.getLong() == second.getLong()
            Flag.DATA_TYPE_BOOL -> first.getBool() == second.getBool()
            Flag.DATA_TYPE_DOUBLE -> java.lang.Double.compare(first.getDouble(), second.getDouble()) == 0
            Flag.DATA_TYPE_STRING -> first.getString() == second.getString()
            Flag.DATA_TYPE_BYTES -> first.getBytes().contentEquals(second.getBytes())
            else -> false
        }
    }

    private fun configurationResult(
        flags: Collection<Flag>, deleted: Collection<Flag>, token: String,
        tokens: TokenInfo, delta: Boolean
    ): Configurations {
        val valuesByType = flags.groupBy { it.flagType }
        val deletedByType = deleted.groupBy { it.flagType }
        return Configurations().apply {
            snapshotToken = token
            serverToken = tokens.serverToken
            version = tokens.servingVersion
            field4 = (valuesByType.keys + deletedByType.keys).sorted().map { type ->
                Configuration().apply {
                    id = type
                    this.flags = valuesByType[type].orEmpty().sortedBy { it.name }.toTypedArray()
                    removeNames = deletedByType[type].orEmpty().map { it.name }.sorted().toTypedArray()
                }
            }.toTypedArray()
            field5 = delta
            field6 = tokens.experimentToken
        }
    }

    private fun tokensWithOverrides(tokens: TokenInfo?, overrides: List<Flag>): TokenInfo {
        if (overrides.isEmpty()) return tokens ?: TokenInfo.EMPTY
        val serverToken = overrides.find { it.name == "__phenotype_server_token" && it.dataType == Flag.DATA_TYPE_STRING }
            ?.getString() ?: tokens?.serverToken.orEmpty()
        return TokenInfo(null, serverToken, 0L)
    }

    fun getRegistrationSnapshot(): Configurations {
        val registrations = readableDatabase.query(
            TABLE_PACKAGES,
            arrayOf(COL_PACKAGE_NAME, COL_VERSION, COL_ANDROID_PACKAGE_NAME),
            null, null, null, null, null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    val registration = DeclarativeRegistration(
                        configPackage = name, versionCode = cursor.getLong(1),
                        androidPackage = cursor.getString(2)
                    )
                    add(Flag(name, registration.encode(), 0))
                }
            }
        }
        return Configurations().apply {
            snapshotToken = ""
            serverToken = ""
            field4 = arrayOf(Configuration().apply {
                id = 0
                flags = registrations.toTypedArray()
                removeNames = emptyArray()
            })
        }
    }

    fun getCommittedConfiguration(packageName: String): Configurations? {
        val db = readableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) db.beginTransaction()
        try {
            if (!packageExists(packageName)) return null
            val selection = "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1"
            val args = arrayOf(packageName)
            val overrides = readFlags(TABLE_FLAG_OVERRIDES, selection, args)
            val flags = mergeFlags(readFlags(TABLE_FLAGS, selection, args), overrides)
            val token = db.query(
                TABLE_EXPERIMENT_TOKENS,
                arrayOf(COL_EXPERIMENT_TOKEN, COL_SERVER_TOKEN, COL_SERVING_VERSION),
                "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", args,
                null, null, null, "1"
            ).use { cursor ->
                if (!cursor.moveToFirst()) null else TokenInfo(
                    if (cursor.isNull(0)) null else cursor.getBlob(0),
                    cursor.getString(1) ?: "", cursor.getLong(2)
                )
            }
            return configurationResult(flags.values, emptyList(), "", tokensWithOverrides(token, overrides), false)
        } finally {
            if (ownsTransaction) db.endTransaction()
        }
    }

    fun getExperimentTokens(packageName: String?, logSourceName: String?, caller: String?): ExperimentTokens? {
        if ((packageName == null) == (logSourceName == null)) {
            throw PhenotypeException(
                PhenotypeStatusCode.INVALID_ARGUMENT,
                "Exactly one configuration package or log source is required"
            )
        }
        if (caller == null) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Calling package must be specified")
        }
        val db = readableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) db.beginTransaction()
        try {
            val deviceScope = packageName == ".metrics" || logSourceName in experimentLogSources(
                "DeviceScopeConfig__log_sources", setOf(
                    "BATTERY_STATS", "DISK_STATS", "DROP_BOX", "DROP_BOX_EASTWORLD",
                    "EASTWORLD_BATTERY", "FINGERPRINT_STATS", "GNSS_PLATFORM_STATS", "GRAPHICS_STATS",
                    "IPCONNECTIVITY_PLATFORM_STATS", "LAUNCHER_STATS", "NETSTATS", "NOTIFICATION_STATS",
                    "PROC_STATS", "SETTINGS_STATS", "STORAGED", "TELECOM_PLATFORM_STATS", "TELEPHONY_PLATFORM_STATS",
                    "WESTWORLD", "WESTWORLD_METADATA", "WIFI_PLATFORM_STATS"
                )
            )
            val encodedCaller = caller.replace("_", "_u_").replace(".", "_d_").replace("#", "_p_")
            val appSources = experimentLogSources("dynamic_ph_appcfg_$encodedCaller", emptySet())
            val appScope = packageName?.startsWith("app:") == true ||
                    logSourceName != null && (logSourceName in appSources || "_ALL_APP_LOG_SOURCES" in appSources)
            val names = when {
                deviceScope -> db.query(TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME), null, null, null, null, COL_PACKAGE_NAME)
                    .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

                appScope -> getConfigPackageNamesByAndroidPackage(packageName?.substring(4) ?: caller)
                packageName != null -> listOf(resolvePackageName(packageName, caller))
                else -> db.rawQuery(
                    """SELECT DISTINCT p.packageName FROM Packages p
                    JOIN LogSources l ON p.packageName = l.packageName
                    WHERE l.logSourceName = ? AND p.androidPackageName = ? ORDER BY p.packageName""",
                    arrayOf(logSourceName, caller)
                ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            }
            val hasOverrides = db.query(
                TABLE_FLAG_OVERRIDES, arrayOf(COL_PACKAGE_NAME), "$COL_COMMITTED = 1",
                null, null, null, null, "1"
            ).use { it.moveToFirst() }
            val batched = names.size > 1
            val nameSet = names.toHashSet()
            val registeredVersions = mutableMapOf<String, Long>()
            val committedStates = mutableMapOf<String, Pair<String, Long>>()
            val committedTokens = mutableMapOf<ConfigurationKey, ByteArray?>()
            val weakIds = mutableMapOf<String, MutableList<Int>>()
            val externalExperiments = mutableMapOf<String, List<ByteArray>>()
            if (batched) {
                db.query(
                    TABLE_PACKAGES, arrayOf(COL_PACKAGE_NAME, COL_VERSION),
                    null, null, null, null, null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (name in nameSet) registeredVersions[name] = cursor.getLong(1)
                    }
                }
                db.query(
                    TABLE_APPLICATION_STATES, arrayOf(COL_PACKAGE_NAME, COL_USER, COL_VERSION),
                    null, null, null, null, "$COL_PACKAGE_NAME, $COL_USER"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (name in nameSet && name !in committedStates) {
                            committedStates[name] = cursor.getString(1).orEmpty() to cursor.getLong(2)
                        }
                    }
                }
                db.query(
                    TABLE_EXPERIMENT_TOKENS,
                    arrayOf(COL_PACKAGE_NAME, COL_VERSION, COL_USER, COL_EXPERIMENT_TOKEN),
                    "$COL_IS_COMMITTED = 1", null, null, null, null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (name in nameSet) {
                            committedTokens[ConfigurationKey(name, cursor.getLong(1), cursor.getString(2).orEmpty())] =
                                if (cursor.isNull(3)) null else cursor.getBlob(3)
                        }
                    }
                }
                db.query(
                    TABLE_WEAK_EXPERIMENT_IDS, arrayOf(COL_PACKAGE_NAME, "experimentId"),
                    null, null, null, null, "$COL_PACKAGE_NAME, experimentId"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (name in nameSet) weakIds.getOrPut(name) { mutableListOf() }.add(cursor.getInt(1))
                    }
                }
                if (deviceScope && !hasOverrides) {
                    val namespaces = names.mapTo(hashSetOf()) { it.removePrefix(EXTERNAL_IDS_PACKAGE_PREFIX) }
                    db.query(
                        TABLE_EXTERNAL_EXPERIMENTS, arrayOf("namespace", "tokens"),
                        null, null, null, null, null
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            val namespace = cursor.getString(0)
                            if (namespace in namespaces) {
                                externalExperiments[namespace] = ExternalExperimentTokenList.ADAPTER
                                    .decode(cursor.getBlob(1)).tokens.map { it.toByteArray() }
                            }
                        }
                    }
                }
            }
            val values = names.mapNotNull { name ->
                val committed = if (batched) committedStates[name] else
                    db.query(
                        TABLE_APPLICATION_STATES, arrayOf(COL_USER, COL_VERSION),
                        "$COL_PACKAGE_NAME = ?", arrayOf(name), null, null, null
                    ).use {
                        if (it.moveToFirst()) (it.getString(0) ?: "") to it.getLong(1) else null
                    }
                val version = committed?.second ?: (if (batched) registeredVersions[name] else getPackageVersion(name))
                    ?.takeIf { it >= 0 } ?: return@mapNotNull null
                val account = committed?.first ?: ""
                val direct = if (committed == null) null else if (batched) {
                    committedTokens[ConfigurationKey(name, version, account)]
                } else db.query(
                    TABLE_EXPERIMENT_TOKENS, arrayOf(COL_EXPERIMENT_TOKEN),
                    "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_IS_COMMITTED = 1",
                    arrayOf(name, version.toString(), account), null, null, null, "1"
                ).use {
                    if (it.moveToFirst() && !it.isNull(0)) it.getBlob(0) else null
                }
                val packageWeakIds = if (direct != null) null else if (batched) {
                    weakIds[name].orEmpty().toIntArray()
                } else db.query(
                    TABLE_WEAK_EXPERIMENT_IDS, arrayOf("experimentId"),
                    "$COL_PACKAGE_NAME = ?", arrayOf(name), null, null, "experimentId"
                ).use {
                    buildList { while (it.moveToNext()) add(it.getInt(0)) }.toIntArray()
                }
                ExperimentTokens().apply {
                    field2 = account
                    gaia = emptyArray()
                    pseudo = emptyArray()
                    always = emptyArray()
                    other = emptyArray()
                    weak = packageWeakIds
                    if (hasOverrides) {
                        weak = (packageWeakIds ?: intArrayOf()) + 14900366
                    } else {
                        this.direct = direct
                        if (deviceScope) {
                            val namespace = name.removePrefix(EXTERNAL_IDS_PACKAGE_PREFIX)
                            external = (if (batched) externalExperiments[namespace]
                            else getExternalExperiments(namespace))?.toTypedArray()
                        } else {
                            val byType = mutableMapOf<Int, MutableList<ByteArray>>()
                            db.query(
                                true, TABLE_CROSS_TOKENS, arrayOf("token", "provenance", "fromUser"),
                                "toPackageName = ? AND toVersion = ? AND isCommitted = 1", arrayOf(name, version.toString()),
                                null, null, null, null
                            ).use {
                                while (it.moveToNext()) byType.getOrPut(it.getInt(1)) { mutableListOf() }.add(it.getBlob(0))
                            }
                            gaia = byType[1].orEmpty().toTypedArray()
                            pseudo = byType[2].orEmpty().toTypedArray()
                            always = byType[3].orEmpty().toTypedArray()
                            other = byType[4].orEmpty().toTypedArray()
                        }
                    }
                }
            }
            if (values.isEmpty()) return null
            if (values.size == 1) return values.single()
            return ExperimentTokens().apply {
                field2 = values.first().field2.takeIf { account -> values.all { it.field2 == account } } ?: ""
                gaia = values.flatMap { it.gaia.orEmpty().toList() }.toTypedArray()
                pseudo = values.flatMap { it.pseudo.orEmpty().toList() }.toTypedArray()
                always = values.flatMap { it.always.orEmpty().toList() }.toTypedArray()
                other = values.flatMap { it.other.orEmpty().toList() }.toTypedArray()
                weak = if (values.none { it.weak != null }) null else values.flatMap { it.weak?.toList().orEmpty() }.toIntArray()
                directs = values.mapNotNull { it.direct }.takeIf { it.isNotEmpty() }?.toTypedArray()
                external = if (values.none { it.external != null }) null else values.flatMap { it.external.orEmpty().toList() }.toTypedArray()
            }
        } finally {
            if (ownsTransaction) db.endTransaction()
        }
    }

    private fun experimentLogSources(flagName: String, default: Set<String>): Set<String> {
        val flag = internalFlag(flagName) ?: return default
        if (flag.dataType != Flag.DATA_TYPE_BYTES) return default
        return try {
            ExperimentLogSources.ADAPTER.decode(flag.getBytes()).names.toSet()
        } catch (_: java.io.IOException) {
            Log.w(PHENOTYPE_LOG_TAG, "Invalid experiment log-source metadata")
            default
        }
    }

    fun deleteUncommittedFlagsByPartition(
        db: SQLiteDatabase,
        packageName: String,
        version: Long,
        user: String,
        partitionId: Long
    ): Int {
        return db.delete(
            TABLE_FLAGS,
            "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_PARTITION_ID = ? AND $COL_COMMITTED = 0",
            arrayOf(packageName, version.toString(), user, partitionId.toString())
        )
    }

    fun insertFlagDirect(
        db: SQLiteDatabase,
        packageName: String,
        version: Long,
        name: String,
        flagType: Int,
        intVal: Long? = null,
        boolVal: Boolean? = null,
        floatVal: Double? = null,
        stringVal: String? = null,
        extensionVal: ByteArray? = null,
        partitionId: Long = 0L,
        user: String = "",
        committed: Int = 0
    ) {
        val values = ContentValues().apply {
            put(COL_PACKAGE_NAME, packageName)
            put(COL_VERSION, version)
            put(COL_NAME, name)
            put(COL_FLAG_TYPE, flagType)
            put(COL_PARTITION_ID, partitionId)
            put(COL_USER, user)
            put(COL_COMMITTED, committed)
            intVal?.let { put(COL_INT_VAL, it) }
            boolVal?.let { put(COL_BOOL_VAL, if (it) 1 else 0) }
            floatVal?.let { put(COL_FLOAT_VAL, it) }
            stringVal?.let { put(COL_STRING_VAL, it) }
            extensionVal?.let { put(COL_EXTENSION_VAL, it) }
        }
        db.insertWithOnConflict(TABLE_FLAGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            .also { check(it != -1L) { "Flag write failed" } }
    }

    fun insertExperimentTokenDirect(
        db: SQLiteDatabase,
        packageName: String,
        version: Long,
        user: String = "",
        experimentToken: ByteArray?,
        serverToken: String,
        servingVersion: Long,
        committed: Int = 0,
        configHash: String = "0",
        tokensTag: ByteArray? = null,
        configurationChanged: Boolean
    ) {
        val values = ContentValues().apply {
            put(COL_PACKAGE_NAME, packageName)
            put(COL_VERSION, version)
            put(COL_USER, user)
            put(COL_IS_COMMITTED, committed)
            put(COL_EXPERIMENT_TOKEN, experimentToken)
            put(COL_SERVER_TOKEN, serverToken)
            put(COL_SERVING_VERSION, servingVersion)
            put(COL_CONFIG_HASH, configHash)
            put(COL_TOKENS_TAG, tokensTag)
        }
        db.insertWithOnConflict(TABLE_EXPERIMENT_TOKENS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            .also { check(it != -1L) { "Experiment token write failed" } }
        if (committed == 0 &&
            (configurationChanged || latestExperimentState(packageName, user, version) == null)
        ) {
            recordExperimentState(db, packageName, user, version, configHash)
        }
    }

    fun pruneHistory(db: SQLiteDatabase) {
        val registeredPackages = "SELECT $COL_PACKAGE_NAME FROM $TABLE_PACKAGES"
        for (table in listOf(TABLE_FLAGS, TABLE_EXPERIMENT_TOKENS, TABLE_APPLICATION_TAGS, TABLE_EXPERIMENT_STATES)) {
            db.delete(table, "$COL_PACKAGE_NAME NOT IN ($registeredPackages)", null)
        }
        // A cross token's source may legitimately be unregistered while its target is registered.
        db.delete(TABLE_CROSS_TOKENS, "toPackageName NOT IN ($registeredPackages)", null)
        // Keep each version's latest state for registration rollback, and retain committed delta bases.
        db.delete(
            TABLE_EXPERIMENT_STATES, """
            $COL_COMMITTED = 0 AND stateId NOT IN (
                SELECT MAX(stateId) FROM $TABLE_EXPERIMENT_STATES
                GROUP BY $COL_PACKAGE_NAME, $COL_USER, $COL_VERSION
            )
        """.trimIndent(), null
        )
    }

    private fun recordExperimentState(
        db: SQLiteDatabase,
        packageName: String,
        user: String,
        version: Long,
        configHash: String
    ) {
        db.insertOrThrow(TABLE_EXPERIMENT_STATES, null, ContentValues().apply {
            put(COL_PACKAGE_NAME, packageName)
            put(COL_USER, user)
            put(COL_VERSION, version)
            put(COL_CONFIG_HASH, configHash)
            put(COL_COMMITTED, 0)
        })
    }

    private fun latestExperimentState(packageName: String, user: String, version: Long): ExperimentState? =
        readableDatabase.query(
            TABLE_EXPERIMENT_STATES,
            arrayOf("stateId", COL_VERSION, COL_CONFIG_HASH),
            "$COL_PACKAGE_NAME = ? AND $COL_USER = ? AND $COL_VERSION = ?", arrayOf(packageName, user, version.toString()),
            null, null, "stateId DESC", "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else ExperimentState(
                cursor.getLong(0), user, cursor.getLong(1), cursor.getString(2).orEmpty()
            )
        }

    private fun committedExperimentState(packageName: String): ExperimentState? =
        readableDatabase.query(
            TABLE_EXPERIMENT_STATES,
            arrayOf("stateId", COL_USER, COL_VERSION, COL_CONFIG_HASH),
            "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName),
            null, null, "stateId DESC", "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else ExperimentState(
                cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getString(3).orEmpty()
            )
        }

    private fun experimentState(packageName: String, user: String, stateId: Long): ExperimentState? =
        readableDatabase.query(
            TABLE_EXPERIMENT_STATES,
            arrayOf(COL_VERSION, COL_CONFIG_HASH),
            "$COL_PACKAGE_NAME = ? AND $COL_USER = ? AND stateId = ?",
            arrayOf(packageName, user, stateId.toString()), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else ExperimentState(
                stateId, user, cursor.getLong(0), cursor.getString(1).orEmpty()
            )
        }

    private fun recordOverrideStates(db: SQLiteDatabase, packageName: String, user: String) {
        if (!packageExists(packageName)) return
        val users = if (user != PHENOTYPE_WILDCARD_USER) setOf(user) else buildSet {
            add("")
            add(PHENOTYPE_WILDCARD_USER)
            for ((table, column) in listOf(
                TABLE_EXPERIMENT_TOKENS to COL_USER,
                TABLE_APPLICATION_STATES to COL_USER,
                TABLE_REQUEST_TAGS to COL_USER
            )) {
                db.query(true, table, arrayOf(column), null, null, null, null, null, null).use { cursor ->
                    while (cursor.moveToNext()) cursor.getString(0).orEmpty()
                        .takeUnless { it == ALL_USERS }?.let(::add)
                }
            }
        }
        for (account in users) {
            val (version, token) = pendingVersionAndToken(packageName, account)
            recordExperimentState(db, packageName, account, version, token?.configHash.orEmpty())
        }
    }

    fun markPackageSyncedDirect(db: SQLiteDatabase, packageName: String, version: Long) {
        val values = ContentValues().apply {
            put(COL_IS_SYNCED, 1)
        }
        check(
            db.update(
                TABLE_PACKAGES, values, "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ?",
                arrayOf(packageName, version.toString())
            ) == 1
        ) { "Registration changed during sync" }
    }

    fun getExperimentToken(packageName: String, version: Long, user: String = "", committed: Int = 0): TokenInfo? =
        readableDatabase.query(
            TABLE_EXPERIMENT_TOKENS,
            arrayOf(COL_EXPERIMENT_TOKEN, COL_SERVER_TOKEN, COL_SERVING_VERSION, COL_CONFIG_HASH, COL_TOKENS_TAG),
            "$COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_IS_COMMITTED = ?",
            arrayOf(packageName, version.toString(), user, committed.toString()),
            null, null, null
        ).use {
            if (it.moveToFirst()) {
                TokenInfo(
                    experimentToken = if (!it.isNull(0)) it.getBlob(0) else null,
                    serverToken = it.getString(1) ?: "",
                    servingVersion = it.getLong(2),
                    configHash = it.getString(3).orEmpty(),
                    tokensTag = if (it.isNull(4)) null else it.getBlob(4)
                )
            } else {
                null
            }
        }

    fun getFlagOverrides(packageName: String, user: String = "", committed: Int = 0): List<Flag> {
        if (user == PHENOTYPE_WILDCARD_USER) return readFlags(
            TABLE_FLAG_OVERRIDES,
            "$COL_PACKAGE_NAME = ? AND $COL_USER = ? AND $COL_COMMITTED = ?",
            arrayOf(packageName, user, committed.toString())
        )
        val flags = readFlags(
            TABLE_FLAG_OVERRIDES,
            "$COL_PACKAGE_NAME = ? AND $COL_USER IN (?, ?) AND $COL_COMMITTED = ?",
            arrayOf(packageName, PHENOTYPE_WILDCARD_USER, user, committed.toString()),
            "CASE WHEN $COL_USER = '$PHENOTYPE_WILDCARD_USER' THEN 0 ELSE 1 END, $COL_NAME"
        )
        return mergeFlags(emptyList(), flags).values.toList()
    }

    private fun overrideValues(packageName: String, user: String, flag: Flag, committed: Int): ContentValues {
        require(!flag.name.isNullOrEmpty()) { "No flag name" }
        return ContentValues().apply {
            put(COL_PACKAGE_NAME, packageName)
            put(COL_USER, user)
            put(COL_NAME, flag.name)
            put(COL_FLAG_TYPE, if (flag.flagType == -1000) 0 else flag.flagType)
            put(COL_COMMITTED, committed)
            when (flag.dataType) {
                Flag.DATA_TYPE_LONG -> put(COL_INT_VAL, flag.getLong())
                Flag.DATA_TYPE_BOOL -> put(COL_BOOL_VAL, if (flag.getBool()) 1 else 0)
                Flag.DATA_TYPE_DOUBLE -> put(COL_FLOAT_VAL, flag.getDouble())
                Flag.DATA_TYPE_STRING -> put(COL_STRING_VAL, requireNotNull(flag.getString()))
                Flag.DATA_TYPE_BYTES -> put(COL_EXTENSION_VAL, requireNotNull(flag.getBytes()))
                else -> throw IllegalArgumentException("Unrecognized flag type")
            }
        }
    }

    fun setFlagOverrides(packageName: String, user: String, flags: List<Flag>): Boolean {
        require(packageName.isNotEmpty()) { "No package name" }
        require(flags.none { it.flagType == -1000 } || user == PHENOTYPE_WILDCARD_USER) {
            "Flag commit requires user *"
        }
        val values = flags.map { overrideValues(packageName, user, it, 0) }
        val db = writableDatabase
        db.beginTransaction()
        try {
            var committed = false
            for ((index, value) in values.withIndex()) {
                db.insertWithOnConflict(TABLE_FLAG_OVERRIDES, null, value, SQLiteDatabase.CONFLICT_REPLACE)
                    .also { check(it != -1L) { "Override write failed" } }
                if (flags[index].flagType == -1000) {
                    value.put(COL_COMMITTED, 1)
                    db.insertWithOnConflict(TABLE_FLAG_OVERRIDES, null, value, SQLiteDatabase.CONFLICT_REPLACE)
                        .also { check(it != -1L) { "Committed override write failed" } }
                    committed = true
                }
            }
            incrementChangeCount(db, packageName)
            recordOverrideStates(db, packageName, user)
            db.setTransactionSuccessful()
            return committed
        } finally {
            db.endTransaction()
        }
    }

    fun listFlagOverrides(packageName: String?, user: String?, name: String?, pendingOnly: Boolean = false): List<FlagOverride> {
        if (name?.endsWith('*') == true) {
            throw PhenotypeException(
                PhenotypeStatusCode.INVALID_ARGUMENT,
                "Prefix searches are no longer supported"
            )
        }
        val filters = listOf(COL_PACKAGE_NAME to packageName, COL_USER to user, COL_NAME to name).filter { it.second != null }
        val conditions = filters.map { "${it.first} = ?" } + if (pendingOnly) listOf("$COL_COMMITTED = 0") else emptyList()
        return readableDatabase.query(
            TABLE_FLAG_OVERRIDES,
            arrayOf(
                COL_FLAG_TYPE, COL_NAME, COL_INT_VAL, COL_BOOL_VAL, COL_FLOAT_VAL, COL_STRING_VAL,
                COL_EXTENSION_VAL, COL_PACKAGE_NAME, COL_USER, COL_COMMITTED
            ),
            conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND "), filters.map { it.second!! }.toTypedArray(),
            null, null, "$COL_PACKAGE_NAME, $COL_USER, $COL_NAME, $COL_COMMITTED"
        ).use { cursor ->
            val result = mutableListOf<FlagOverride>()
            while (cursor.moveToNext()) {
                val flag = parseFlagFromCursor(cursor) ?: throw IllegalStateException("Override has no value")
                result.add(FlagOverride(cursor.getString(7), cursor.getString(8), flag, cursor.getInt(9) != 0))
            }
            result
        }
    }

    fun deleteFlagOverrides(packageName: String?, user: String?, name: String?): List<FlagOverride> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val deleted = listFlagOverrides(packageName, user, name, pendingOnly = true)
            for (entry in deleted) {
                db.delete(
                    TABLE_FLAG_OVERRIDES,
                    "$COL_PACKAGE_NAME = ? AND $COL_USER = ? AND $COL_NAME = ? AND $COL_COMMITTED = 0",
                    arrayOf(entry.packageName, entry.user, entry.flag.name)
                )
            }
            for (changed in deleted.map { it.packageName }.distinct()) incrementChangeCount(db, changed)
            for ((changedPackage, changedUser) in deleted.map { it.packageName to it.user }.distinct()) {
                recordOverrideStates(db, changedPackage, changedUser)
            }
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    private fun pendingVersionAndToken(packageName: String, user: String): Pair<Long, TokenInfo?> =
        readableDatabase.rawQuery(
            """
            SELECT selected.$COL_VERSION, token.$COL_PACKAGE_NAME, token.$COL_EXPERIMENT_TOKEN,
                   token.$COL_SERVER_TOKEN, token.$COL_SERVING_VERSION, token.$COL_CONFIG_HASH, token.$COL_TOKENS_TAG
            FROM (
                SELECT packages.$COL_PACKAGE_NAME,
                       COALESCE((
                           SELECT MAX(pending.$COL_VERSION)
                           FROM $TABLE_EXPERIMENT_TOKENS pending
                           WHERE pending.$COL_PACKAGE_NAME = packages.$COL_PACKAGE_NAME
                             AND pending.$COL_USER = ?
                             AND pending.$COL_IS_COMMITTED = 0
                             AND pending.$COL_VERSION <= packages.$COL_VERSION
                       ), packages.$COL_VERSION) AS $COL_VERSION
                FROM $TABLE_PACKAGES packages
                WHERE packages.$COL_PACKAGE_NAME = ?
            ) selected
            LEFT JOIN $TABLE_EXPERIMENT_TOKENS token
              ON token.$COL_PACKAGE_NAME = selected.$COL_PACKAGE_NAME
             AND token.$COL_VERSION = selected.$COL_VERSION
             AND token.$COL_USER = ?
             AND token.$COL_IS_COMMITTED = 0
        """.trimIndent(), arrayOf(user, packageName, user)
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            val version = cursor.getLong(0)
            val token = if (cursor.isNull(1)) null else TokenInfo(
                experimentToken = if (cursor.isNull(2)) null else cursor.getBlob(2),
                serverToken = cursor.getString(3).orEmpty(),
                servingVersion = cursor.getLong(4),
                configHash = cursor.getString(5).orEmpty(),
                tokensTag = if (cursor.isNull(6)) null else cursor.getBlob(6)
            )
            version to token
        }

    private fun committedConfigHash(packageName: String): String? =
        readableDatabase.rawQuery(
            """
            SELECT COALESCE(token.$COL_CONFIG_HASH, '')
            FROM $TABLE_APPLICATION_STATES application
            LEFT JOIN $TABLE_EXPERIMENT_TOKENS token
              ON token.$COL_PACKAGE_NAME = application.$COL_PACKAGE_NAME
             AND token.$COL_VERSION = application.$COL_VERSION
             AND token.$COL_USER = application.$COL_USER
             AND token.$COL_IS_COMMITTED = 1
            WHERE application.$COL_PACKAGE_NAME = ?
            LIMIT 1
        """.trimIndent(), arrayOf(packageName)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else null
        }

    private fun snapshotMetadata(
        packageName: String,
        user: String,
        version: Long,
        currentConfigHash: String,
        baseSnapshotToken: String?,
        hasOverrides: Boolean
    ): SnapshotMetadata = readableDatabase.rawQuery(
        """
        SELECT CASE WHEN application.$COL_PACKAGE_NAME IS NULL THEN NULL
                    ELSE COALESCE(committedToken.$COL_CONFIG_HASH, '') END,
               latest.stateId, latest.$COL_CONFIG_HASH,
               committedState.stateId, COALESCE(changes.count, 0)
        FROM (SELECT 1) seed
        LEFT JOIN $TABLE_APPLICATION_STATES application
          ON application.rowid = (
              SELECT rowid FROM $TABLE_APPLICATION_STATES WHERE $COL_PACKAGE_NAME = ? LIMIT 1
          )
        LEFT JOIN $TABLE_EXPERIMENT_TOKENS committedToken
          ON committedToken.$COL_PACKAGE_NAME = application.$COL_PACKAGE_NAME
         AND committedToken.$COL_VERSION = application.$COL_VERSION
         AND committedToken.$COL_USER = application.$COL_USER
         AND committedToken.$COL_IS_COMMITTED = 1
        LEFT JOIN $TABLE_EXPERIMENT_STATES latest
          ON latest.stateId = (
              SELECT MAX(stateId) FROM $TABLE_EXPERIMENT_STATES
              WHERE $COL_PACKAGE_NAME = ? AND $COL_USER = ? AND $COL_VERSION = ?
          )
        LEFT JOIN $TABLE_EXPERIMENT_STATES committedState
          ON committedState.stateId = (
              SELECT MAX(stateId) FROM $TABLE_EXPERIMENT_STATES
              WHERE $COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1
          )
        LEFT JOIN $TABLE_CHANGE_COUNTS changes ON changes.$COL_PACKAGE_NAME = ?
        LIMIT 1
    """.trimIndent(), arrayOf(packageName, packageName, user, version.toString(), packageName, packageName)
    ).use { cursor ->
        check(cursor.moveToFirst()) { "Snapshot state query returned no row" }
        val committedHash = if (cursor.isNull(0)) null else cursor.getString(0).orEmpty()
        val latestState = if (cursor.isNull(1)) -1L else cursor.getLong(1)
        val committedState = if (cursor.isNull(3)) -1L else cursor.getLong(3)
        val base = SnapshotTokenInfo.parse(baseSnapshotToken, true)
        val delta = !hasOverrides && base != null && base.source == packageName && base.target == packageName &&
                if (latestState != -1L) {
                    base.isPhixitToken && committedState != -1L && base.experimentStateToCommit == committedState
                } else {
                    committedHash != null && base.configHash == committedHash
                }
        val token = SnapshotTokenInfo(
            user = user,
            source = packageName,
            version = version,
            changeCount = cursor.getLong(4),
            configHash = if (latestState == -1L) currentConfigHash else cursor.getString(2).orEmpty(),
            target = packageName,
            committedConfigHash = committedHash.orEmpty(),
            isDelta = delta,
            experimentStateToCommit = latestState,
            committedExperimentState = committedState,
            latestExperimentState = latestState,
            isPhixitToken = latestState != -1L
        ).encode()
        SnapshotMetadata(token, delta)
    }

    fun commitConfiguration(snapshotToken: String): String {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val encodedToken = if (snapshotToken.startsWith(PHENOTYPE_CURRENT_TOKEN_PREFIX)) {
                val parts = snapshotToken.split(':', limit = 3)
                if (parts.size != 3 || parts[2].isEmpty()) {
                    throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid snapshot token")
                }
                if (!packageExists(parts[2])) {
                    throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
                }
                getConfigurationSnapshot(parts[2], parts[1]).snapshotToken
            } else snapshotToken
            val parsed = SnapshotTokenInfo.parse(encodedToken, true)
                ?: throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid snapshot token")
            if (parsed.source.isEmpty() || parsed.source != parsed.target) {
                throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid snapshot target")
            }
            if (!packageExists(parsed.target)) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            val (pendingVersion, _) = pendingVersionAndToken(parsed.target, parsed.user)
            val token = if (parsed.isPhixitToken) {
                if (parsed.version != pendingVersion) {
                    throw PhenotypeException(
                        PhenotypeStatusCode.NEWER_SNAPSHOT_AVAILABLE,
                        "Snapshot version is no longer current"
                    )
                }
                val state = experimentState(parsed.target, parsed.user, parsed.experimentStateToCommit)
                    ?: throw PhenotypeException(
                        PhenotypeStatusCode.SNAPSHOT_STATE_UNAVAILABLE,
                        "Snapshot state is no longer available"
                    )
                val latest = latestExperimentState(parsed.target, parsed.user, pendingVersion)
                if (state.version != parsed.version || latest == null ||
                    latest.id != parsed.latestExperimentState || state.id != latest.id
                ) {
                    throw PhenotypeException(
                        PhenotypeStatusCode.NEWER_SNAPSHOT_AVAILABLE,
                        "A newer snapshot is available"
                    )
                }
                if (parsed.isDelta && committedExperimentState(parsed.target)?.id != parsed.committedExperimentState) {
                    throw PhenotypeException(
                        PhenotypeStatusCode.COMMITTED_SNAPSHOT_BASE_CHANGED,
                        "Committed snapshot base changed"
                    )
                }
                parsed.copy(configHash = state.configHash)
            } else {
                if (parsed.version != pendingVersion || parsed.changeCount != getChangeCount(parsed.target) ||
                    (parsed.isDelta && parsed.committedConfigHash != committedConfigHash(parsed.target).orEmpty())
                ) {
                    throw PhenotypeException(PhenotypeStatusCode.STALE_SNAPSHOT_TOKEN, "Stale snapshot token")
                }
                parsed
            }
            val packageName = token.target
            val args = arrayOf(packageName, token.version.toString(), token.user)
            val overrides = getFlagOverrides(packageName, token.user)
            db.delete(TABLE_FLAGS, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName))
            db.execSQL(
                """
                INSERT INTO $TABLE_FLAGS ($COL_PACKAGE_NAME, $COL_VERSION, $COL_FLAG_TYPE, $COL_PARTITION_ID,
                    $COL_USER, $COL_NAME, $COL_INT_VAL, $COL_BOOL_VAL, $COL_FLOAT_VAL, $COL_STRING_VAL, $COL_EXTENSION_VAL, $COL_COMMITTED)
                SELECT $COL_PACKAGE_NAME, $COL_VERSION, $COL_FLAG_TYPE, $COL_PARTITION_ID,
                    $COL_USER, $COL_NAME, $COL_INT_VAL, $COL_BOOL_VAL, $COL_FLOAT_VAL, $COL_STRING_VAL, $COL_EXTENSION_VAL, 1
                FROM $TABLE_FLAGS WHERE $COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_COMMITTED = 0
            """.trimIndent(), args
            )
            db.delete(TABLE_FLAG_OVERRIDES, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName))
            for (flag in overrides) {
                db.insertWithOnConflict(
                    TABLE_FLAG_OVERRIDES, null, overrideValues(packageName, token.user, flag, 1),
                    SQLiteDatabase.CONFLICT_REPLACE
                ).also { check(it != -1L) { "Committed override write failed" } }
            }
            if (overrides.isEmpty()) {
                db.delete(TABLE_CROSS_TOKENS, "fromPackageName = ? AND isCommitted = 1", arrayOf(packageName))
                db.execSQL(
                    """
                    INSERT INTO $TABLE_CROSS_TOKENS (fromPackageName, fromVersion, fromUser, toPackageName,
                        toVersion, isCommitted, token, provenance)
                    SELECT fromPackageName, fromVersion, fromUser, toPackageName, toVersion, 1, token, provenance
                    FROM $TABLE_CROSS_TOKENS WHERE fromPackageName = ? AND fromVersion = ? AND fromUser = ? AND isCommitted = 0
                """.trimIndent(), args
                )
                db.delete(TABLE_EXPERIMENT_TOKENS, "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", arrayOf(packageName))
                db.execSQL(
                    """
                    INSERT INTO $TABLE_EXPERIMENT_TOKENS ($COL_PACKAGE_NAME, $COL_VERSION, $COL_USER, $COL_IS_COMMITTED,
                        $COL_EXPERIMENT_TOKEN, $COL_SERVER_TOKEN, $COL_CONFIG_HASH, $COL_SERVING_VERSION, $COL_TOKENS_TAG, $COL_FLAGS_HASH)
                    SELECT $COL_PACKAGE_NAME, $COL_VERSION, $COL_USER, 1,
                        $COL_EXPERIMENT_TOKEN, $COL_SERVER_TOKEN, $COL_CONFIG_HASH, $COL_SERVING_VERSION, $COL_TOKENS_TAG, $COL_FLAGS_HASH
                    FROM $TABLE_EXPERIMENT_TOKENS
                    WHERE $COL_PACKAGE_NAME = ? AND $COL_VERSION = ? AND $COL_USER = ? AND $COL_IS_COMMITTED = 0
                """.trimIndent(), args
                )
            }
            db.delete(TABLE_APPLICATION_STATES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
            db.insertOrThrow(TABLE_APPLICATION_STATES, null, ContentValues().apply {
                put(COL_PACKAGE_NAME, packageName)
                put(COL_USER, token.user)
                put(COL_VERSION, token.version)
            })
            if (token.isPhixitToken) {
                db.update(
                    TABLE_EXPERIMENT_STATES, ContentValues().apply { put(COL_COMMITTED, 0) },
                    "$COL_PACKAGE_NAME = ?", arrayOf(packageName)
                )
                check(
                    db.update(
                        TABLE_EXPERIMENT_STATES, ContentValues().apply { put(COL_COMMITTED, 1) },
                        "stateId = ?", arrayOf(token.experimentStateToCommit.toString())
                    ) == 1
                ) {
                    "Committed experiment state disappeared"
                }
            }
            db.setTransactionSuccessful()
            return packageName
        } finally {
            db.endTransaction()
        }
    }

    fun getConfigurationSnapshot(packageName: String, user: String = "", baseSnapshotToken: String? = null): Configurations =
        LegacyConfigurationOptions.apply(packageName, readConfigurationSnapshot(packageName, user, baseSnapshotToken))

    private fun readConfigurationSnapshot(packageName: String, user: String, baseSnapshotToken: String?): Configurations {
        if (user.isNotEmpty() && isDirectBootAware(packageName)) {
            throw PhenotypeException(
                PhenotypeStatusCode.INVALID_ARGUMENT,
                "Can't commit to $user for direct boot aware package $packageName"
            )
        }
        val db = readableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) db.beginTransaction()
        try {
            val (version, token) = pendingVersionAndToken(packageName, user)
            val overrides = getFlagOverrides(packageName, user)
            val current = mergeFlags(getFlags(packageName, version, user), overrides)
            val metadata = snapshotMetadata(
                packageName, user, version, token?.configHash.orEmpty(),
                baseSnapshotToken, overrides.isNotEmpty()
            )
            val tokens = tokensWithOverrides(token, overrides)
            if (metadata.delta) {
                val committed = getCommittedConfiguration(packageName)!!.field4.orEmpty()
                    .flatMap { it.flags.orEmpty().toList() }
                val old = mergeFlags(committed, emptyList())
                val changed = current.filter { (key, flag) -> old[key]?.let { !flagsEqual(flag, it) } ?: true }.values
                val removed = old.filterKeys { it !in current }.values
                return configurationResult(changed, removed, metadata.token, tokens, true)
            }
            val removed = if (baseSnapshotToken == null) emptyList() else
                mergeFlags(readFlags(TABLE_FLAGS, "$COL_PACKAGE_NAME = ?", arrayOf(packageName)), emptyList())
                    .filterKeys { it !in current }.values.toList()
            return configurationResult(current.values, removed, metadata.token, tokens, false)
        } catch (e: PhenotypeException) {
            if (e.statusCode != PhenotypeStatusCode.PACKAGE_NOT_REGISTERED) throw e
            return Configurations().apply {
                snapshotToken = ""
                serverToken = ""
                field4 = emptyArray()
            }
        } finally {
            if (ownsTransaction) db.endTransaction()
        }
    }

    fun getConfigurationAndCommit(packageName: String, user: String): Configurations? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val configuration = getConfigurationSnapshot(packageName, user)
            if (configuration.snapshotToken.isEmpty()) {
                db.setTransactionSuccessful()
                return null
            }
            commitConfiguration(configuration.snapshotToken)
            db.setTransactionSuccessful()
            return configuration
        } finally {
            db.endTransaction()
        }
    }

    private fun copyCommittedPackageToDeviceStorage(context: Context, packageName: String) {
        if (SDK_INT < 24 || !isDirectBootAware(packageName)) return
        val source = readableDatabase
        val ownsSourceTransaction = !source.inTransaction()
        if (ownsSourceTransaction) source.beginTransaction()
        try {
            PhenotypeDatabase(context.createDeviceProtectedStorageContext()).use { helper ->
                val target = helper.writableDatabase
                target.beginTransaction()
                try {
                    target.delete(TABLE_LOG_SOURCES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    target.delete(TABLE_FLAGS, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName))
                    target.delete(TABLE_FLAG_OVERRIDES, "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName))
                    target.delete(TABLE_EXPERIMENT_TOKENS, "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", arrayOf(packageName))
                    target.delete(TABLE_CROSS_TOKENS, "fromPackageName = ? AND isCommitted = 1", arrayOf(packageName))
                    target.delete(TABLE_APPLICATION_STATES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    target.delete(TABLE_EXPERIMENT_STATES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))

                    helper.invalidateSyncRegistration(packageName)
                    copyRows(source, target, TABLE_PACKAGES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    copyRows(source, target, TABLE_LOG_SOURCES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    copyRows(
                        source, target, TABLE_FLAGS,
                        "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName)
                    )
                    copyRows(
                        source, target, TABLE_FLAG_OVERRIDES,
                        "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName)
                    )
                    copyRows(
                        source, target, TABLE_EXPERIMENT_TOKENS,
                        "$COL_PACKAGE_NAME = ? AND $COL_IS_COMMITTED = 1", arrayOf(packageName)
                    )
                    copyRows(
                        source, target, TABLE_CROSS_TOKENS,
                        "fromPackageName = ? AND isCommitted = 1", arrayOf(packageName)
                    )
                    copyRows(source, target, TABLE_APPLICATION_STATES, "$COL_PACKAGE_NAME = ?", arrayOf(packageName))
                    copyRows(
                        source, target, TABLE_EXPERIMENT_STATES,
                        "$COL_PACKAGE_NAME = ? AND $COL_COMMITTED = 1", arrayOf(packageName)
                    )
                    target.setTransactionSuccessful()
                } finally {
                    target.endTransaction()
                }
            }
        } finally {
            if (ownsSourceTransaction) source.endTransaction()
        }
    }

    fun publishCommittedConfiguration(context: Context, packageName: String) {
        copyCommittedPackageToDeviceStorage(context, packageName)
        context.contentResolver.notifyChange(
            android.net.Uri.Builder().scheme("content").authority(PHENOTYPE_CONTENT_AUTHORITY)
                .appendPath(packageName).build(), null
        )
        for (target in listOf("com.google.android.apps.internal.mobdog", "com.google.android.apps.mobileutilities")) {
            try {
                context.packageManager.getApplicationInfo(target, 0)
                context.sendBroadcast(
                    Intent("$PHENOTYPE_CONTENT_AUTHORITY.COMMIT")
                        .setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY).setPackage(target)
                        .putExtra("package_name", packageName)
                )
            } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            }
        }
    }

    private fun copyRows(
        source: SQLiteDatabase, target: SQLiteDatabase, table: String,
        selection: String, args: Array<String>
    ) {
        source.query(table, null, selection, args, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues(cursor.columnCount)
                for (index in cursor.columnNames.indices) {
                    val name = cursor.columnNames[index]
                    when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> values.putNull(name)
                        Cursor.FIELD_TYPE_INTEGER -> values.put(name, cursor.getLong(index))
                        Cursor.FIELD_TYPE_FLOAT -> values.put(name, cursor.getDouble(index))
                        Cursor.FIELD_TYPE_STRING -> values.put(name, cursor.getString(index))
                        Cursor.FIELD_TYPE_BLOB -> values.put(name, cursor.getBlob(index))
                    }
                }
                check(target.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
                    "Failed to publish $table"
                }
            }
        }
    }

    fun getOrCreateStorageInfo(context: Context, androidPackageName: String): StorageInfo = synchronized(sharedPublicationLock) {
        val db = writableDatabase
        var created = false
        db.beginTransaction()
        val result = try {
            var secret: ByteArray? = null
            var deviceSecret: ByteArray? = null
            db.query(
                TABLE_STORAGE_INFOS, arrayOf(COL_SECRET, COL_DEVICE_ENCRYPTED_SECRET),
                "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName), null, null, null
            ).use {
                if (it.moveToFirst()) {
                    if (!it.isNull(0)) secret = it.getBlob(0)
                    if (!it.isNull(1)) deviceSecret = it.getBlob(1)
                }
            }
            val result = StorageInfo(secret ?: generateSecret(), deviceSecret ?: generateSecret())
            if (secret == null || deviceSecret == null) {
                created = true
                val values = ContentValues().apply {
                    put(COL_ANDROID_PACKAGE_NAME, androidPackageName)
                    put(COL_SECRET, result.secret)
                    put(COL_DEVICE_ENCRYPTED_SECRET, result.deviceEncryptedSecret)
                }
                check(
                    db.insertWithOnConflict(
                        TABLE_STORAGE_INFOS, null, values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    ) != -1L
                ) { "Storage secret write failed" }
            }
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
        if (created) {
            publishSharedConfigurationsForKnownUsers(context, getConfigPackageNamesByAndroidPackage(androidPackageName))
        }
        return result
    }

    private fun generateSecret(): ByteArray {
        val secret = ByteArray(16)
        SecureRandom().nextBytes(secret)
        return secret
    }

    fun publishSharedConfigurations(
        context: Context,
        configurations: Collection<Pair<String, String>>,
        serverTimestamp: Long
    ): List<File> = synchronized(sharedPublicationLock) {
        val requested = configurations.distinct()
        if (requested.isEmpty()) return emptyList()
        val storageInfos = sharedStorageInfos()
        val published = mutableListOf<File>()
        var failureCount = 0
        var firstFailure: Exception? = null
        for ((packageName, user) in requested) {
            val storageInfo = storageInfos[packageName] ?: continue
            try {
                published.add(publishSharedConfiguration(context, packageName, user, serverTimestamp, storageInfo))
            } catch (e: Exception) {
                failureCount++
                if (firstFailure == null) firstFailure = e
                Log.w(PHENOTYPE_LOG_TAG, "Failed to publish shared configuration for $packageName (${e.javaClass.simpleName})")
            }
        }
        if (failureCount > 0) {
            throw IllegalStateException("Failed to publish $failureCount shared configuration(s)", firstFailure)
        }
        published
    }

    fun publishSharedConfigurationsForKnownUsers(
        context: Context,
        packageNames: Collection<String>,
        serverTimestamp: Long = 0L,
        additionalUsers: Collection<String> = emptyList()
    ): List<File> {
        val users = linkedSetOf<String>()
        readableDatabase.query(
            TABLE_REQUEST_TAGS, arrayOf(COL_USER), "$COL_USER != ?",
            arrayOf(ALL_USERS), null, null, COL_USER
        ).use { cursor ->
            while (cursor.moveToNext()) users.add(cursor.getString(0))
        }
        users.addAll(additionalUsers.filter { it != PHENOTYPE_WILDCARD_USER })
        if (users.isEmpty()) users.add("")
        return publishSharedConfigurations(
            context,
            packageNames.distinct().flatMap { packageName -> users.map { packageName to it } }, serverTimestamp
        )
    }

    fun republishSharedConfigurations(context: Context) {
        val packageNames = readableDatabase.rawQuery(
            """
            SELECT DISTINCT packages.$COL_PACKAGE_NAME
            FROM $TABLE_PACKAGES packages
            INNER JOIN $TABLE_STORAGE_INFOS storage
                ON storage.$COL_ANDROID_PACKAGE_NAME = packages.$COL_ANDROID_PACKAGE_NAME
        """.trimIndent(), null
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        publishSharedConfigurationsForKnownUsers(context, packageNames)
    }

    private fun publishSharedConfiguration(
        context: Context,
        packageName: String,
        user: String,
        serverTimestamp: Long,
        storageInfo: StorageInfo
    ): File {
        val deviceProtected = SDK_INT >= 24 && packageName in directBootAwarePackages
        val secret = if (deviceProtected) storageInfo.deviceEncryptedSecret else storageInfo.secret
        val targetContext = if (deviceProtected) context.createDeviceProtectedStorageContext() else context
        val db = readableDatabase
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) db.beginTransaction()
        val (metadata, flags) = try {
            val (version, token) = pendingVersionAndToken(packageName, user)
            val currentFlags = LegacyConfigurationOptions.mergeFlags(packageName, getFlags(packageName, version, user))
            val currentOverrides = getFlagOverrides(packageName, user)
            val overrides = linkedMapOf<String, Flag>()
            for (flag in currentOverrides) {
                if (!LegacyConfigurationOptions.isPreset(packageName, flag)) flag.name?.let { overrides[it] = flag }
            }
            val snapshotToken = snapshotMetadata(
                packageName, user, version, token?.configHash.orEmpty(),
                null, currentOverrides.isNotEmpty()
            ).token
            SharedConfigurationMetadata(
                snapshotToken = snapshotToken,
                experimentToken = (token?.experimentToken ?: byteArrayOf()).toByteString(),
                serverToken = token?.serverToken.orEmpty(),
                serverTimestamp = serverTimestamp,
                overrides = overrides.mapValues { (name, flag) -> flag.toSharedOverride(name) }
            ) to currentFlags
        } finally {
            if (ownsTransaction) db.endTransaction()
        }
        val payload = FlagsContentCodec.encodeSharedFile(
            metadata,
            FlagsContentCodec.fromLegacyFlags(flags)
        )
        return PhenotypeSharedStorage.write(targetContext, secret, packageName, user, payload)
    }

    private fun sharedStorageInfos(): Map<String, StorageInfo> = readableDatabase.rawQuery(
        """
        SELECT packages.$COL_PACKAGE_NAME, storage.$COL_SECRET, storage.$COL_DEVICE_ENCRYPTED_SECRET
        FROM $TABLE_PACKAGES packages
        INNER JOIN $TABLE_STORAGE_INFOS storage
            ON storage.$COL_ANDROID_PACKAGE_NAME = packages.$COL_ANDROID_PACKAGE_NAME
        WHERE storage.$COL_SECRET IS NOT NULL AND storage.$COL_DEVICE_ENCRYPTED_SECRET IS NOT NULL
    """.trimIndent(), null
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), StorageInfo(cursor.getBlob(1), cursor.getBlob(2)))
        }
    }

    private fun getStorageInfo(androidPackageName: String): StorageInfo? =
        readableDatabase.query(
            TABLE_STORAGE_INFOS, arrayOf(COL_SECRET, COL_DEVICE_ENCRYPTED_SECRET),
            "$COL_ANDROID_PACKAGE_NAME = ?", arrayOf(androidPackageName), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0) || cursor.isNull(1)) null
            else StorageInfo(cursor.getBlob(0), cursor.getBlob(1))
        }

    private fun Flag.toSharedOverride(name: String): SharedOverrideValue = when (dataType) {
        Flag.DATA_TYPE_LONG -> SharedOverrideValue(longValue = getLong(), name = name)
        Flag.DATA_TYPE_BOOL -> SharedOverrideValue(boolValue = getBool(), name = name)
        Flag.DATA_TYPE_DOUBLE -> SharedOverrideValue(doubleValue = getDouble(), name = name)
        Flag.DATA_TYPE_STRING -> SharedOverrideValue(stringValue = requireNotNull(getString()), name = name)
        Flag.DATA_TYPE_BYTES -> SharedOverrideValue(bytesValue = requireNotNull(getBytes()).toByteString(), name = name)
        else -> throw IllegalArgumentException("Unrecognized flag type: $dataType")
    }


}

private data class ConfigurationKey(val packageName: String, val version: Long, val user: String)

private data class ExperimentState(
    val id: Long,
    val user: String,
    val version: Long,
    val configHash: String
)

private data class SnapshotMetadata(val token: String, val delta: Boolean)

internal data class TokenInfo(
    val experimentToken: ByteArray?,
    val serverToken: String,
    val servingVersion: Long,
    val configHash: String = "",
    val tokensTag: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TokenInfo
        if (experimentToken != null) {
            if (other.experimentToken == null) return false
            if (!experimentToken.contentEquals(other.experimentToken)) return false
        } else if (other.experimentToken != null) return false
        if (serverToken != other.serverToken) return false
        if (servingVersion != other.servingVersion) return false
        if (configHash != other.configHash || !tokensTag.contentEquals(other.tokensTag)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = experimentToken?.contentHashCode() ?: 0
        result = 31 * result + serverToken.hashCode()
        result = 31 * result + servingVersion.hashCode()
        result = 31 * result + configHash.hashCode()
        result = 31 * result + (tokensTag?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        val EMPTY = TokenInfo(null, "", 0L)
    }
}

internal data class SnapshotTokenInfo(
    val user: String,
    val source: String,
    val version: Long,
    val changeCount: Long,
    val configHash: String,
    val target: String,
    val committedConfigHash: String,
    val isDelta: Boolean,
    val experimentStateToCommit: Long = -1L,
    val committedExperimentState: Long = -1L,
    val latestExperimentState: Long = -1L,
    val isPhixitToken: Boolean = false
) {
    fun encode(): String {
        val fields = mutableListOf(
            user, source, version.toString(), changeCount.toString(),
            configHash, target, committedConfigHash, if (isDelta) "1" else "0"
        )
        if (isPhixitToken) fields.addAll(
            listOf(
                experimentStateToCommit.toString(),
                committedExperimentState.toString(), latestExperimentState.toString()
            )
        )
        return fields.joinToString(" ")
    }

    companion object {
        @JvmOverloads
        fun parse(tokenString: String?, phixit: Boolean = true): SnapshotTokenInfo? {
            if (tokenString == null) return null
            val parts = tokenString.split(' ', limit = if (phixit) 11 else 8)
            if (parts.size < 7) return null
            return try {
                SnapshotTokenInfo(
                    user = parts[0], source = parts[1], version = parts[2].toLong(),
                    changeCount = parts[3].toLong(), configHash = parts[4],
                    target = parts[5], committedConfigHash = parts[6],
                    isDelta = parts.size == 7 || parts[7] == "1",
                    experimentStateToCommit = if (parts.size == 11) parts[8].toLong() else -1L,
                    committedExperimentState = if (parts.size == 11) parts[9].toLong() else -1L,
                    latestExperimentState = if (parts.size == 11) parts[10].toLong() else -1L,
                    isPhixitToken = parts.size == 11
                )
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}
