/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.finsky.ApplicationTag
import com.google.android.finsky.DeviceData
import com.google.android.finsky.DeviceDataEmptyA
import com.google.android.finsky.ExpDeviceInfo
import com.google.android.finsky.ExpDeviceInfoWrapper
import com.google.android.finsky.ExperimentFlag
import com.google.android.finsky.ExperimentResponseData
import com.google.android.finsky.ExperimentTokenStore
import com.google.android.finsky.ExperimentVersion
import com.google.android.finsky.ExperimentsDataWrapper
import com.google.android.finsky.ExperimentsFlagsProto
import com.google.android.finsky.ExperimentsInfo
import com.google.android.finsky.FlagValueProto
import com.google.android.finsky.UnknowMsg
import com.google.android.finsky.action
import com.google.android.finsky.experimentRequestData
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.microg.vending.billing.FINSKY_REGULAR
import org.microg.vending.billing.FINSKY_STABLE
import org.microg.vending.billing.GServices.getString
import org.microg.vending.billing.VENDING_PACKAGE_NAME
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays
import java.util.List
import java.util.Locale
import java.util.Objects
import java.util.TreeSet
import java.util.zip.GZIPOutputStream

object ExperimentAndConfigs {
    val TAG: String = ExperimentAndConfigs::class.java.simpleName
    private const val version = 84122130L
    private const val baselineCL = 636944598L

    private fun buildBaseGpInfo(pkgName: String, fixed64: Long): ExperimentsInfo.Builder {
        val experimentFlag = ExperimentFlag.Builder().flag(fixed64).build()
        val experimentInfo = ExperimentVersion.Builder()
            .expPkgName(pkgName)
            .version(version)
            .experimentFlagValue(experimentFlag)
            .baselineCL(baselineCL) //cli
            .pkgName(VENDING_PACKAGE_NAME).build()

        val msg = UnknowMsg.Builder().field1(1).build()
        return ExperimentsInfo.Builder()
            .experimentVersionValue(experimentInfo)
            .unKnowBytesC(msg.encodeByteString())
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun buildRequestData(context: Context): experimentRequestData {
        return buildRequestData(context, "NEW_USER_SYNC", null, null)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun buildRequestData(
        context: Context,
        actions: String,
        pkgName: String?,
        account: Account?
    ): experimentRequestData {
        @SuppressLint("HardwareIds") val deviceData = createDeviceData(account, context)
        val finskyRegularInfo: ExperimentsInfo.Builder
        var finskyStableInfo: ExperimentsInfo.Builder? = null
        if (actions == "NEW_USER_SYNC_ALL_ACCOUNT") {
            finskyRegularInfo = buildBaseGpInfo(FINSKY_REGULAR, -1)
            finskyStableInfo = buildBaseGpInfo(FINSKY_STABLE, -1)
        } else {
            finskyRegularInfo = buildBaseGpInfo(FINSKY_REGULAR, 0)
        }

        if (actions.contains("NEW_USER_SYNC")) {
            val result = experimentRequestData.Builder()
            result.deviceDataValue(deviceData)
                .bytesTag(ByteString.of())
                .actionType(action.NEW_USER_SYNC)
                .unknowFieldG(128)
                .expPkgName(FINSKY_REGULAR)
            val experimentsInfoValue = result.experimentsInfo.toMutableList()
            experimentsInfoValue.add(finskyRegularInfo.build())
            if (finskyStableInfo != null) experimentsInfoValue.add(finskyStableInfo.build())
            result.experimentsInfo = experimentsInfoValue
            return result.build()
        }

        if (actions == "NEW_APPLICATION_SYNC") {
            try {
                PhenotypeDatabase(context).writableDatabase.use { db ->
                    val applicationTags: MutableList<ApplicationTag> = ArrayList()
                    db.rawQuery(
                        "SELECT partitionId, tag FROM ApplicationTags WHERE packageName = ? AND user = ? AND version = ?",
                        arrayOf<String?>(pkgName, account!!.name, version.toString())
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            applicationTags.add(
                                ApplicationTag.Builder()
                                    .partitionId(cursor.getLong(0))
                                    .tag(ByteString.of(*cursor.getBlob(1)))
                                    .build()
                            )
                        }
                    }
                    finskyRegularInfo.applicationTagValue(applicationTags)
                    db.rawQuery(
                        "SELECT tokensTag FROM ExperimentTokens WHERE packageName = ? AND user = ? AND version = ? AND isCommitted = 0",
                        arrayOf<String?>(pkgName, account.name, version.toString())
                    ).use { cursor ->
                        if (cursor.moveToNext()) {
                            finskyRegularInfo.tokensTag(ByteString.of(*cursor.getBlob(0)))
                        }
                    }
                    val experimentsInfo = buildBaseGpInfo(FINSKY_STABLE, 0).build()
                    var bytesTag: ByteArray? = null
                    db.rawQuery(
                        "SELECT bytesTag FROM RequestTags WHERE user = ?",
                        arrayOf(account.name)
                    ).use { cursor ->
                        if (cursor.moveToNext()) {
                            bytesTag = cursor.getBlob(0)
                        }
                    }
                    checkNotNull(bytesTag)
                    return experimentRequestData.Builder()
                        .deviceDataValue(deviceData)
                        .experimentsInfo(
                            List.of<ExperimentsInfo>(
                                finskyRegularInfo.build(),
                                experimentsInfo
                            )
                        )
                        .bytesTag(ByteString.of(*bytesTag!!))
                        .actionType(action.NEW_APPLICATION_SYNC)
                        .unknowFieldG(128)
                        .expPkgName(FINSKY_STABLE)
                        .build()
                }
            } catch (e: Exception) {
                Log.w(TAG, "buildRequestData: ", e)
                throw RuntimeException(e)
            }
        }

        throw RuntimeException("request experimentsandconfigs has Unknow action")
    }

    private fun createExpDeviceInfo(context: Context): ExpDeviceInfo {
        @SuppressLint("HardwareIds") val builder = ExpDeviceInfo.Builder()
        builder.androidId(
            getString(context.contentResolver, "android_id", "")!!
                .toLong()
        )
        builder.sdkInt(Build.VERSION.SDK_INT)
        builder.buildId(Build.ID)
        builder.buildDevice(Build.DEVICE)
        builder.manufacturer(Build.MANUFACTURER)
        builder.model(Build.MODEL)
        builder.product(Build.PRODUCT)
        builder.unknowEmpty("")
        builder.fingerprint(Build.FINGERPRINT)
        builder.country(Locale.getDefault().country)
        builder.locale(Locale.getDefault().toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.supportAbis(Arrays.asList(*Build.SUPPORTED_ABIS))
        }
        return builder.build()
    }

    private fun createDeviceData(account: Account?, context: Context): DeviceData {
        val expDeviceInfo = createExpDeviceInfo(context)
        val expDeviceInfoWrapper = ExpDeviceInfoWrapper.Builder()
            .unknowFieldb(4)
            .expDeviceInfoValue(expDeviceInfo)
            .build()
        return DeviceData.Builder()
            .hasAccount((if (account == null) 0 else 1).toLong())
            .expDeviceInfoWrapperValue(expDeviceInfoWrapper)
            .unknowFlagf(false)
            .unknowEmptyE(ByteString.of())
            .unknkowFieldG(DeviceDataEmptyA.Builder().build())
            .build()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun postRequest(
        experimentRequestData: experimentRequestData,
        context: Context?,
        accountName: String,
        token: String
    ) {
        try {
            val url =
                URL("https://www.googleapis.com/experimentsandconfigs/v1/getExperimentsAndConfigs" + "?r=" + experimentRequestData.actionType?.value + "&c=" + experimentRequestData.unknowFieldG)

            val httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.connectTimeout = 30000
            httpURLConnection.readTimeout = 30000
            httpURLConnection.doOutput = true
            httpURLConnection.instanceFollowRedirects = false
            httpURLConnection.setRequestProperty("Accept-Encoding", null)
            httpURLConnection.setRequestProperty("Content-Type", "application/x-protobuf")
            httpURLConnection.setRequestProperty("Content-Encoding", "gzip")
            httpURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpURLConnection.setRequestProperty(
                "User-Agent",
                "Android-Finsky/41.2.21-31 [0] [PR] 636997666 (api=3,versionCode=84122130,sdk=31,device=redroid_arm64,hardware=redroid,product=redroid_arm64,platformVersionRelease=12,model=redroid12_arm64,buildId=SQ1D.220205.004,isWideScreen=0,supportedAbis=arm64-v8a;armeabi-v7a;armeabi) (redroid_arm64 SQ1D.220205.004); gzip"
            )
            val byteArrayOutputStream = ByteArrayOutputStream()
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(experimentRequestData.encode())
                gzipOutputStream.finish()
            }
            val compressedData = byteArrayOutputStream.toByteArray()
            httpURLConnection.outputStream.use { os ->
                os.write(compressedData)
                os.flush()
            }
            val responseCode = httpURLConnection.responseCode
            Log.d(TAG, "postRequest responseCode: $responseCode")
            if (responseCode >= 200 && responseCode < 300) {
                val experimentResponseData =
                    ExperimentResponseData.ADAPTER.decode(toByteArray(httpURLConnection.inputStream))

                PhenotypeDatabase(context).writableDatabase.use { db ->
                    if (experimentResponseData.bytesTag != null) {
                        db.rawQuery(
                            "SELECT user FROM RequestTags WHERE user = ?1",
                            arrayOf(accountName)
                        ).use { cursor ->
                            if (cursor.count > 0) {
                                db.execSQL(
                                    "UPDATE RequestTags SET user = ?1, bytesTag = ?2 WHERE user = ?1",
                                    arrayOf<Any>(
                                        accountName,
                                        experimentResponseData.bytesTag.toByteArray()
                                    )
                                )
                            } else {
                                db.execSQL(
                                    "INSERT INTO RequestTags (user, bytesTag) VALUES (?, ?)",
                                    arrayOf<Any>(
                                        accountName,
                                        experimentResponseData.bytesTag.toByteArray()
                                    )
                                )
                            }
                        }
                    }
                    for (experimentsDataWrapper in experimentResponseData.experiments) {
                        val experimentVersionValue = experimentsDataWrapper.experimentVersionValue
                        val pkgName = experimentVersionValue?.expPkgName
                        val version = experimentVersionValue?.version
                        for (expFlagsGroup in experimentsDataWrapper.expFlagsGroupValue) {
                            val partitionId = expFlagsGroup.applicationTagValue?.partitionId

                            for (expFlag in expFlagsGroup.expFlags) {
                                var longValue: Long? = null
                                var booleValue: Long? = null
                                var doubleValue: Double? = null
                                var stringValue: String? = null
                                var extensionValue: ByteString? = "".encodeUtf8()
                                if (expFlag.valueType == null) continue
                                when (expFlag.valueType) {
                                    1 -> longValue = expFlag.longValue
                                    2 -> booleValue = if (expFlag.boolValue == true) 1L else 0L
                                    3 -> doubleValue = expFlag.doubleValue
                                    4 -> stringValue = expFlag.stringValue
                                    5 -> {
                                        extensionValue =
                                            if (expFlag.extensionValueValue == null) null else expFlag.extensionValueValue.mvalue
                                        continue
                                    }

                                    else -> continue
                                }
                                val flagType = 0
                                db.execSQL(
                                    "INSERT OR REPLACE INTO Flags(packageName, version, flagType, partitionId, user, name, committed, intVal, boolVal, floatVal, stringVal, extensionVal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                    arrayOf<Any?>(
                                        pkgName,
                                        version,
                                        flagType,
                                        partitionId,
                                        accountName,
                                        expFlag.flagName,
                                        flagType,
                                        longValue,
                                        booleValue,
                                        doubleValue,
                                        stringValue,
                                        extensionValue!!.toByteArray()
                                    )
                                )
                            }
                            db.execSQL(
                                "DELETE FROM ExperimentTokens WHERE packageName = ? AND version = ? AND user = ? AND isCommitted = 0",
                                arrayOf<Any?>(pkgName, version, accountName)
                            )
                            db.execSQL(
                                "INSERT INTO ExperimentTokens (packageName, version, user, isCommitted, experimentToken, serverToken, configHash, servingVersion, tokensTag, flagsHash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                arrayOf<Any?>(
                                    pkgName,
                                    version,
                                    accountName,
                                    0,
                                    experimentsDataWrapper.experimentToken?.toByteArray(),
                                    experimentsDataWrapper.serverToken,
                                    calculateHash(experimentsDataWrapper).toString(),
                                    experimentResponseData.servingVersion,
                                    experimentsDataWrapper.tokensTag?.toByteArray(),
                                    0
                                )
                            )
                            db.execSQL(
                                "DELETE FROM ApplicationTags WHERE packageName = ? AND version = ? AND user = ? AND partitionId = ?",
                                arrayOf<Any?>(pkgName, version, accountName, partitionId)
                            )
                            db.execSQL(
                                "INSERT OR REPLACE INTO ApplicationTags (packageName, version, partitionId, user, tag) VALUES (?, ?, ?, ?, ?)",
                                arrayOf<Any?>(
                                    pkgName,
                                    version,
                                    partitionId,
                                    accountName,
                                    expFlagsGroup.applicationTagValue?.tag?.toByteArray()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun buildExperimentsFlag(context: Context, accountName: String, pkgName: String) {
        PhenotypeDatabase(context).readableDatabase.use { database ->
            var hasFlagOverrides: Boolean
            database.rawQuery("SELECT EXISTS(SELECT NULL FROM FlagOverrides)", arrayOf())
                .use { cursor ->
                    cursor.moveToNext()
                    hasFlagOverrides = cursor.getInt(0) > 0
                }
            var expValue: ExperimentsValues? = null
            val overFlags = ArrayList<FlagsValue?>()
            if (hasFlagOverrides) {
                database.rawQuery(
                    "SELECT flagType, name, intVal, boolVal, floatVal, stringVal, extensionVal FROM FlagOverrides WHERE packageName = ? AND user = \'*\' AND committed = 0",
                    arrayOf(pkgName)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        overFlags.add(getFlagsValue(cursor))
                    }
                }
                for (flag in overFlags) {
                    if (flag!!.name == "__phenotype_server_token" && flag.valueType == 3) {
                        expValue = ExperimentsValues(null, flag.stringVal, 0)
                    }
                }
            }

            database.rawQuery(
                "SELECT experimentToken,serverToken,servingVersion FROM ExperimentTokens WHERE packageName = ? AND version = ? AND user = ? AND isCommitted = 0",
                arrayOf(pkgName, version.toString(), accountName)
            ).use { cursor ->
                cursor.moveToNext()
                if (expValue == null) {
                    expValue =
                        ExperimentsValues(cursor.getBlob(0), cursor.getString(1), cursor.getLong(2))
                }
            }
            val flags = TreeSet<FlagsValue?>()
            database.rawQuery(
                "SELECT flagType, name, intVal, boolVal, floatVal, stringVal, extensionVal FROM Flags WHERE packageName = ? AND version = ? AND user = ? AND committed = 0 ORDER BY name",
                arrayOf(pkgName, version.toString(), accountName)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    flags.add(getFlagsValue(cursor))
                }
            }
            for (flagsValue in overFlags) {
                flags.remove(flagsValue)
                flags.add(flagsValue)
            }
            val flagsValueMap = HashMap<Int, ArrayList<FlagsValue?>?>()
            for (flag in flags) {
                if (flagsValueMap[flag!!.flagType] == null) flagsValueMap[flag.flagType] =
                    ArrayList()
                Objects.requireNonNull(flagsValueMap[flag.flagType])!!.add(flag)
            }
            val flagTypeList = ArrayList<FlagTypeValue>()
            for (flagType in flagsValueMap.keys) {
                flagTypeList.add(
                    FlagTypeValue(
                        flagType, Objects.requireNonNull(
                            flagsValueMap[flagType]
                        )!!.toTypedArray<FlagsValue?>()
                    )
                )
            }

            commit(database, pkgName, accountName)
            database.rawQuery(
                "SELECT configHash FROM ExperimentTokens WHERE packageName = ? AND version = ? AND user = ? AND isCommitted = ?",
                arrayOf(pkgName, version.toString(), accountName, "1")
            ).use { cursor ->
                cursor.moveToNext()
                val configHash = cursor.getString(0)
                val expeIntroduce = "$pkgName $accountName $configHash"

                val configuration = ExperimentsFlagsConfiguration(
                    expeIntroduce,
                    expValue!!.serverToken,
                    flagTypeList.toTypedArray<FlagTypeValue>(),
                    false,
                    expValue!!.experimentToken,
                    expValue!!.servingVersion
                )
                val ExperimentsFlagsProto = buildExperimentsFlagsProto(configuration)
                writeExperimentsFlag(ExperimentsFlagsProto, context, pkgName, accountName)
            }
        }
    }

    private fun commit(database: SQLiteDatabase, pkgName: String, accountName: String) {
        database.execSQL(
            "INSERT OR REPLACE INTO ExperimentTokens SELECT packageName, version, user, 1 AS isCommitted, experimentToken, serverToken, configHash, servingVersion, tokensTag, flagsHash FROM ExperimentTokens WHERE packageName = ? AND version = ? AND user = ? AND isCommitted = 0",
            arrayOf<Any>(pkgName, version.toString(), accountName)
        )
        database.execSQL(
            "DELETE FROM Flags WHERE packageName = ? AND committed = 1",
            arrayOf(pkgName)
        )
        database.execSQL(
            "INSERT INTO Flags SELECT packageName, version, flagType, partitionId, user, name, intVal, boolVal, floatVal, stringVal, extensionVal, 1 AS committed FROM Flags WHERE packageName = ? AND version = ? AND user = ? AND committed = 0",
            arrayOf(pkgName, version.toString(), accountName)
        )
    }

    @JvmStatic
    fun readExperimentsFlag(
        context: Context,
        pkgName: String,
        username: String?
    ): ExperimentsDataRead? {
        val file = File(
            context.filesDir,
            if (FINSKY_REGULAR == pkgName) (if (TextUtils.isEmpty(username)) "experiment-flags-regular-null-account" else "experiment-flags-regular-" + Uri.encode(
                username
            )) else "experiment-flags-process-stable"
        )
        if (!file.exists()) {
            Log.d(TAG, "File " + file.name + " not exists")
            return null
        }
        try {
            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))
            if (inputStream.readByte().toInt() != 1) {
                throw IOException("Unrecognized file version.")
            }
            val result = ExperimentsDataRead()
            result.setBaseToken(
                inputStream.readUTF(), inputStream.readUTF(), ExperimentTokenStore.ADAPTER.decode(
                    Base64.decode(inputStream.readUTF(), 3)
                )
            )
            var endOfFlag = 0
            while (endOfFlag == 0) {
                when (inputStream.readByte()) {
                    0.toByte() -> endOfFlag = 1
                    1.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readByte().toLong())
                    2.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readShort().toLong())
                    3.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readInt().toLong())
                    4.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readLong())
                    5.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readUTF())
                    6.toByte() -> {
                        val key = inputStream.readUTF()
                        val length = inputStream.readInt()
                        if (length >= 0) {
                            val value = ByteArray(length)
                            inputStream.readFully(value)
                            result.putFlag(key, value)
                            break
                        }
                        throw RuntimeException("Bytes flag has negative length.")
                    }

                    7.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readDouble())
                    8.toByte() -> result.putFlag(inputStream.readUTF(), inputStream.readBoolean())
                    else -> throw RuntimeException("Unknown flag type")
                }
            }
            inputStream.close()
            return result
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun writeExperimentsFlag(
        ExperimentsFlagsProto: ExperimentsFlagsProto,
        context: Context,
        pkgName: String,
        username: String
    ) {
        try {
            val file = File(
                context.filesDir,
                if (FINSKY_REGULAR == pkgName) (if (TextUtils.isEmpty(username)) "experiment-flags-regular-null-account" else "experiment-flags-regular-" + Uri.encode(
                    username
                )) else "experiment-flags-process-stable"
            )
            val dataOutputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
            dataOutputStream.writeByte(1)
            dataOutputStream.writeUTF(ExperimentsFlagsProto.serverToken)
            dataOutputStream.writeUTF(ExperimentsFlagsProto.expeIntroduce)
            dataOutputStream.writeUTF(
                Base64.encodeToString(
                    buildExperimentsTokenProto(
                        context,
                        username,
                        pkgName
                    ).encode(), 3
                )
            )
            for (flag in ExperimentsFlagsProto.flagValues) {
                if (flag.intVal != null) {
                    val value = flag.intVal
                    if (value >= -0x80L && value <= 0x7FL) {
                        dataOutputStream.writeByte(1)
                        dataOutputStream.writeUTF(flag.name)
                        dataOutputStream.writeByte((value.toInt()))
                    } else if (value >= -0x8000L && value <= 0x7FFFL) {
                        dataOutputStream.writeByte(2)
                        dataOutputStream.writeUTF(flag.name)
                        dataOutputStream.writeShort((value.toInt()))
                    } else if (value >= -0x80000000L && value <= 0x7FFFFFFFL) {
                        dataOutputStream.writeByte(3)
                        dataOutputStream.writeUTF(flag.name)
                        dataOutputStream.writeInt((value.toInt()))
                    } else {
                        dataOutputStream.writeByte(4)
                        dataOutputStream.writeUTF(flag.name)
                        dataOutputStream.writeLong(value)
                    }
                } else if (flag.boolVal != null) {
                    dataOutputStream.writeByte(8)
                    dataOutputStream.writeUTF(flag.name)
                    dataOutputStream.writeBoolean(flag.boolVal)
                } else if (flag.floatVal != null) {
                    dataOutputStream.writeByte(7)
                    dataOutputStream.writeUTF(flag.name)
                    dataOutputStream.writeDouble(flag.floatVal)
                } else if (flag.stringVal != null) {
                    dataOutputStream.writeByte(5)
                    dataOutputStream.writeUTF(flag.name)
                    dataOutputStream.writeUTF(flag.stringVal)
                } else if (flag.extensionVal != null) {
                    dataOutputStream.writeByte(6)
                    dataOutputStream.writeUTF(flag.name)
                    dataOutputStream.writeInt(flag.extensionVal.size)
                    dataOutputStream.write(
                        flag.extensionVal.toByteArray(),
                        0,
                        flag.extensionVal.size
                    )
                }
            }
            Log.d(TAG, "Finished writing experiment flags into file " + file.name)
            dataOutputStream.writeByte(0)
            dataOutputStream.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun buildExperimentsTokenProto(
        context: Context,
        user: String,
        pkgName: String
    ): ExperimentTokenStore {
        PhenotypeDatabase(context).readableDatabase.use { db ->
            db.rawQuery(
                "SELECT experimentToken FROM ExperimentTokens WHERE user = ? AND packageName = ? AND version = ? AND isCommitted = 1",
                arrayOf<String>(user, pkgName, version.toString())
            ).use { cursor ->
                cursor.moveToNext()
                val ExperimentTokenStore_ = ExperimentTokenStore.Builder()
                ExperimentTokenStore_.experimentToken =
                    Arrays.asList<ByteString>(ByteString.of(*cursor.getBlob(0)))
                return ExperimentTokenStore_.build()
            }
        }
    }

    private fun buildExperimentsFlagsProto(configuration: ExperimentsFlagsConfiguration): ExperimentsFlagsProto {
        val builder = ExperimentsFlagsProto.Builder()
            .expeIntroduce(configuration.expeIntroduce)
            .serverToken(configuration.serverToken)
            .unknowFlagB(configuration.unknowFlagB)
            .servingVersion(configuration.servingVersion)
        if (configuration.experimentToken != null)  {
            builder.experimentToken(ByteString.of(*configuration.experimentToken))
        }
        val flagValueProtos = builder.flagValues.toMutableList()
        for (typeValue in configuration.array) {
            for (flagsValue in typeValue.values) {
                flagValue2proto(flagsValue)?.let { flagValueProtos.add(it) }
            }
        }
        builder.flagValues = flagValueProtos
        return builder.build()
    }

    private fun flagValue2proto(value: FlagsValue?): FlagValueProto? {
        when (value!!.valueType) {
            0 -> return FlagValueProto.Builder()
                .name(value.name)
                .intVal(value.intVal.toLong()).build()

            1 -> return FlagValueProto.Builder()
                .name(value.name)
                .boolVal(value.boolVal).build()

            2 -> return FlagValueProto.Builder()
                .name(value.name)
                .floatVal(value.floatVal.toDouble()).build()

            3 -> return FlagValueProto.Builder()
                .name(value.name)
                .stringVal(value.stringVal).build()

            4 -> return FlagValueProto.Builder()
                .name(value.name)
                .extensionVal(ByteString.of(*value.extensionVal?:byteArrayOf())).build()
        }
        return null
    }

    private fun getFlagsValue(cursor: Cursor): FlagsValue? {
        val flagType = cursor.getInt(0)
        val name = cursor.getString(1)
        if (!cursor.isNull(2)) {
            return FlagsValue(flagType, name, cursor.getInt(2))
        } else if (!cursor.isNull(3)) {
            return FlagsValue(flagType, name, cursor.getInt(3) != 0)
        } else if (!cursor.isNull(4)) {
            return FlagsValue(flagType, name, cursor.getFloat(4))
        } else if (!cursor.isNull(5)) {
            return FlagsValue(flagType, name, cursor.getString(5))
        } else if (!cursor.isNull(6)) {
            return FlagsValue(flagType, name, cursor.getString(6))
        }
        return null
    }

    private fun calculateHash(experimentsDataWrapper: ExperimentsDataWrapper): Int {
        var hash = 0
        for (expFlagsGroup in experimentsDataWrapper.expFlagsGroupValue) {
            var applicationTag = expFlagsGroup.applicationTagValue
            if (applicationTag == null) applicationTag = ApplicationTag.Builder().build()
            var hashCode = applicationTag!!.partitionId.hashCode()
            for (b in applicationTag.tag!!.toByteArray()) {
                hashCode = hashCode * 0x1F + b
            }
            hash = hash * 17 xor hashCode
        }
        return hash
    }

    @JvmStatic
    fun toByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(1024)

        while ((inputStream.read(data, 0, data.size).also { nRead = it }) != -1) {
            buffer.write(data, 0, nRead)
        }
        buffer.flush()
        return buffer.toByteArray()
    }


    class ExperimentsDataRead {
        @JvmField
        var serverToken: String? = null
        var expeIntroduce: String? = null
        var experimentToken: ExperimentTokenStore? = null
        val flagMap: MutableMap<String, Any> = HashMap()

        fun setBaseToken(
            serverToken: String?,
            expeIntroduce: String?,
            experimentToken: ExperimentTokenStore?
        ) {
            this.serverToken = serverToken
            this.expeIntroduce = expeIntroduce
            this.experimentToken = experimentToken
        }

        fun putFlag(name: String, value: Boolean) {
            flagMap[name] = value
        }

        fun putFlag(name: String, value: Long) {
            flagMap[name] = value
        }

        fun putFlag(name: String, value: Double) {
            flagMap[name] = value
        }

        fun putFlag(name: String, value: String) {
            flagMap[name] = value
        }

        fun putFlag(name: String, value: ByteArray) {
            flagMap[name] = value
        }
    }

    internal class ExperimentsFlagsConfiguration(
        val expeIntroduce: String,
        val serverToken: String?,
        val array: Array<FlagTypeValue>,
        val unknowFlagB: Boolean,
        val experimentToken: ByteArray?,
        val servingVersion: Long
    )

    internal class FlagTypeValue(private val flagType: Int, val values: Array<FlagsValue?>)

    internal class ExperimentsValues(
        var experimentToken: ByteArray?,
        var serverToken: String?,
        var servingVersion: Long
    )

    class FlagsValue : Comparable<FlagsValue?> {
        val flagType: Int
        val name: String
        var intVal: Int = 0
        var boolVal: Boolean = false
        var floatVal: Float = 0f
        var stringVal: String? = null
        var extensionVal: ByteArray? = null
        var valueType: Int

        constructor(flagType: Int, name: String, intVal: Int) {
            this.valueType = 0
            this.flagType = flagType
            this.name = name
            this.intVal = intVal
        }

        constructor(flagType: Int, name: String, boolVal: Boolean) {
            this.valueType = 1
            this.flagType = flagType
            this.name = name
            this.boolVal = boolVal
        }

        constructor(flagType: Int, name: String, floatVal: Float) {
            this.valueType = 2
            this.flagType = flagType
            this.name = name
            this.floatVal = floatVal
        }

        constructor(flagType: Int, name: String, stringVal: String?) {
            this.valueType = 3
            this.flagType = flagType
            this.name = name
            this.stringVal = stringVal
        }

        override fun compareTo(flagValue: FlagsValue?): Int {
            if (flagValue == null) {
                return -1
            }
            return name.compareTo(flagValue?.name!!)
        }

        val value: Any?
            get() {
                when (this.valueType) {
                    0 -> return intVal
                    1 -> return boolVal
                    2 -> return floatVal
                    3 -> return stringVal
                    4 -> return extensionVal
                }
                return null
            }

        override fun equals(obj: Any?): Boolean {
            if (this.valueType == (obj as FlagsValue?)!!.valueType) {
                when (this.valueType) {
                    0 -> return this.intVal == obj!!.intVal
                    1 -> return this.boolVal == obj!!.boolVal
                    2 -> return this.floatVal == obj!!.floatVal
                    3 -> return this.stringVal == obj!!.stringVal
                    4 -> return this.extensionVal == obj!!.extensionVal
                }
            }
            return false
        }
    }
}
