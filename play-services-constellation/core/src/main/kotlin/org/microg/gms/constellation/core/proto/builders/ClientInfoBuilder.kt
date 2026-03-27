@file:RequiresApi(Build.VERSION_CODES.O)

package org.microg.gms.constellation.core.proto.builders

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.google.android.gms.tasks.await
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.GServices
import org.microg.gms.constellation.core.authManager
import org.microg.gms.constellation.core.proto.ClientInfo
import org.microg.gms.constellation.core.proto.CountryInfo
import org.microg.gms.constellation.core.proto.DeviceID
import org.microg.gms.constellation.core.proto.DeviceType
import org.microg.gms.constellation.core.proto.DroidGuardSignals
import org.microg.gms.constellation.core.proto.GaiaSignals
import org.microg.gms.constellation.core.proto.GaiaToken
import org.microg.gms.constellation.core.proto.NetworkSignal
import org.microg.gms.constellation.core.proto.SimOperatorInfo
import org.microg.gms.constellation.core.proto.UserProfileType
import java.util.Locale

private const val TAG = "ClientInfoBuilder"
private const val PREFS_NAME = "constellation_prefs"

@SuppressLint("HardwareIds")
suspend operator fun ClientInfo.Companion.invoke(context: Context, iidToken: String): ClientInfo {
    return ClientInfo(
        context,
        RequestBuildContext(
            iidToken = iidToken,
            gaiaTokens = GaiaToken.getList(context)
        )
    )
}

@SuppressLint("HardwareIds")
suspend operator fun ClientInfo.Companion.invoke(
    context: Context,
    buildContext: RequestBuildContext
): ClientInfo {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val locale = Locale.getDefault().let { "${it.language}_${it.country}" }

    return ClientInfo(
        device_id = DeviceID(context, buildContext.iidToken),
        client_public_key = context.authManager.getOrCreateKeyPair().public.encoded.toByteString(),
        locale = locale,
        gmscore_version_number = Constants.GMS_VERSION_CODE / 1000,
        gmscore_version = packageInfo.versionName ?: "",
        android_sdk_version = Build.VERSION.SDK_INT,
        user_profile_type = UserProfileType.REGULAR_USER,
        gaia_tokens = buildContext.gaiaTokens,
        country_info = CountryInfo(context),
        connectivity_infos = NetworkSignal.getList(context),
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        partial_sim_infos = SimOperatorInfo.getList(context),
        device_type = DeviceType.DEVICE_TYPE_PHONE,
        is_wearable_standalone = false,
        gaia_signals = GaiaSignals(context),
        device_fingerprint = Build.FINGERPRINT,
        droidguard_signals = DroidGuardSignals(context),
    )
}

@SuppressLint("HardwareIds")
operator fun DeviceID.Companion.invoke(context: Context, iidToken: String): DeviceID {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var gmsAndroidId = prefs.getLong("gms_android_id", 0L)
    if (gmsAndroidId == 0L) {
        gmsAndroidId = GServices.getLong(context.contentResolver, "android_id", 0L)
        if (gmsAndroidId == 0L) {
            val androidIdStr =
                android.provider.Settings.Secure.getString(context.contentResolver, "android_id")
            gmsAndroidId =
                androidIdStr?.toLongOrNull(16) ?: (Build.ID.hashCode()
                    .toLong() and 0x7FFFFFFFFFFFFFFFL)
        }
        prefs.edit { putLong("gms_android_id", gmsAndroidId) }
    }

    val userSerial = try {
        val userManager = context.getSystemService<android.os.UserManager>()
        userManager?.getSerialNumberForUser(android.os.Process.myUserHandle()) ?: 0L
    } catch (_: Exception) {
        0L
    }

    var primaryDeviceId = prefs.getLong("primary_device_id", 0L)
    val isSystemUser = try {
        val userManager = context.getSystemService<android.os.UserManager>()
        userManager?.isSystemUser ?: true
    } catch (_: Exception) {
        true
    }
    if (primaryDeviceId == 0L && isSystemUser) {
        primaryDeviceId = gmsAndroidId
        prefs.edit { putLong("primary_device_id", primaryDeviceId) }
    }

    return DeviceID(
        iid_token = iidToken,
        primary_device_id = primaryDeviceId,
        user_serial = userSerial,
        gms_android_id = gmsAndroidId
    )
}

operator fun CountryInfo.Companion.invoke(context: Context): CountryInfo {
    val simCountries = mutableListOf<String>()
    val networkCountries = mutableListOf<String>()

    try {
        val sm = context.getSystemService<SubscriptionManager>()
        val tm = context.getSystemService<TelephonyManager>()

        sm?.activeSubscriptionInfoList?.forEach { info ->
            val targetTM =
                tm?.createForSubscriptionId(info.subscriptionId)

            targetTM?.networkCountryIso?.let { networkCountries.add(it.lowercase()) }
            info.countryIso?.let { simCountries.add(it.lowercase()) }
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission to access country info", e)
    }

    if (simCountries.isEmpty()) {
        val tm = context.getSystemService<TelephonyManager>()
        val simCountry = tm?.simCountryIso?.lowercase()?.takeIf { it.isNotBlank() }
        if (simCountry != null) simCountries.add(simCountry)
    }
    if (networkCountries.isEmpty()) {
        val tm = context.getSystemService<TelephonyManager>()
        val networkCountry = tm?.networkCountryIso?.lowercase()?.takeIf { it.isNotBlank() }
        if (networkCountry != null) networkCountries.add(networkCountry)
    }

    return CountryInfo(sim_countries = simCountries, network_countries = networkCountries)
}

suspend operator fun DroidGuardSignals.Companion.invoke(context: Context): DroidGuardSignals? {
    val cachedToken = ConstellationStateStore.loadDroidGuardToken(context)
    if (!cachedToken.isNullOrBlank()) {
        return DroidGuardSignals(droidguard_token = cachedToken, droidguard_result = "")
    }

    return try {
        val client = com.google.android.gms.droidguard.DroidGuard.getClient(context)
        val data = mapOf(
            "package_name" to context.packageName,
            "timestamp" to System.currentTimeMillis().toString()
        )
        val result = client.getResults("constellation_verify", data, null).await()
        DroidGuardSignals(droidguard_result = result, droidguard_token = "")
    } catch (e: Exception) {
        Log.w(TAG, "DroidGuard generation failed", e)
        null
    }
}
