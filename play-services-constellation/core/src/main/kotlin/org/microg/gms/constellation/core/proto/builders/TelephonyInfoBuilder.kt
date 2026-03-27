@file:RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
@file:SuppressLint("HardwareIds")
@file:Suppress("DEPRECATION")

package org.microg.gms.constellation.core.proto.builders

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import org.microg.gms.constellation.core.proto.NetworkSignal
import org.microg.gms.constellation.core.proto.SimNetworkInfo
import org.microg.gms.constellation.core.proto.SimOperatorInfo
import org.microg.gms.constellation.core.proto.TelephonyInfo
import java.security.MessageDigest

private const val TAG = "TelephonyInfoBuilder"

operator fun TelephonyInfo.Companion.invoke(context: Context, subscriptionId: Int): TelephonyInfo {
    val tm = context.getSystemService<TelephonyManager>()
    val targetTm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && subscriptionId >= 0) {
        tm?.createForSubscriptionId(subscriptionId)
    } else tm

    val simState = when (targetTm?.simState) {
        TelephonyManager.SIM_STATE_READY -> TelephonyInfo.SimState.SIM_STATE_READY
        else -> TelephonyInfo.SimState.SIM_STATE_NOT_READY
    }

    val phoneType = when (targetTm?.phoneType) {
        TelephonyManager.PHONE_TYPE_GSM -> TelephonyInfo.PhoneType.PHONE_TYPE_GSM
        TelephonyManager.PHONE_TYPE_CDMA -> TelephonyInfo.PhoneType.PHONE_TYPE_CDMA
        TelephonyManager.PHONE_TYPE_SIP -> TelephonyInfo.PhoneType.PHONE_TYPE_SIP
        else -> TelephonyInfo.PhoneType.PHONE_TYPE_UNKNOWN
    }

    val networkRoaming = if (targetTm?.isNetworkRoaming == true) {
        TelephonyInfo.RoamingState.ROAMING_ROAMING
    } else {
        TelephonyInfo.RoamingState.ROAMING_HOME
    }

    val cm = context.getSystemService<ConnectivityManager>()
    val activeNetwork = cm?.activeNetworkInfo
    val connectivityState = when {
        activeNetwork == null -> TelephonyInfo.ConnectivityState.CONNECTIVITY_UNKNOWN
        activeNetwork.isRoaming -> TelephonyInfo.ConnectivityState.CONNECTIVITY_ROAMING
        else -> TelephonyInfo.ConnectivityState.CONNECTIVITY_HOME
    }

    val simInfo = SimNetworkInfo(
        country_iso = targetTm?.simCountryIso?.lowercase() ?: "",
        operator_ = targetTm?.simOperator ?: "",
        operator_name = targetTm?.simOperatorName ?: ""
    )

    val networkInfo = SimNetworkInfo(
        country_iso = targetTm?.networkCountryIso?.lowercase() ?: "",
        operator_ = targetTm?.networkOperator ?: "",
        operator_name = targetTm?.networkOperatorName ?: ""
    )

    return TelephonyInfo(
        phone_type = phoneType,
        group_id_level1 = targetTm?.groupIdLevel1 ?: "",
        sim_info = simInfo,
        network_info = networkInfo,
        network_roaming = networkRoaming,
        connectivity_state = connectivityState,
        sms_capability = if (targetTm?.isSmsCapable == true) TelephonyInfo.SmsCapability.SMS_CAPABLE else TelephonyInfo.SmsCapability.SMS_NOT_CAPABLE,
        sim_state = simState,
        is_embedded = false
    )
}

fun NetworkSignal.Companion.getList(context: Context): List<NetworkSignal> {
    val connectivityInfos = mutableListOf<NetworkSignal>()
    try {
        val cm = context.getSystemService<ConnectivityManager>()
        cm?.activeNetworkInfo?.let { networkInfo ->
            val type = when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkSignal.Type.TYPE_WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkSignal.Type.TYPE_MOBILE
                else -> NetworkSignal.Type.TYPE_UNKNOWN
            }
            val state = when {
                networkInfo.isConnected -> NetworkSignal.State.STATE_CONNECTED
                networkInfo.isConnectedOrConnecting && !networkInfo.isConnected -> NetworkSignal.State.STATE_CONNECTING
                else -> NetworkSignal.State.STATE_DISCONNECTED
            }
            val availability =
                if (networkInfo.isAvailable) NetworkSignal.Availability.AVAILABLE else NetworkSignal.Availability.NOT_AVAILABLE

            connectivityInfos.add(
                NetworkSignal(
                    type = type,
                    availability = availability,
                    state = state
                )
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not retrieve network info", e)
    }
    return connectivityInfos
}

fun SimOperatorInfo.Companion.getList(context: Context): List<SimOperatorInfo> {
    val infos = mutableListOf<SimOperatorInfo>()
    try {
        val sm = context.getSystemService<SubscriptionManager>()
        val tm = context.getSystemService<TelephonyManager>()

        sm?.activeSubscriptionInfoList?.forEach { info ->
            val targetTM = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tm?.createForSubscriptionId(info.subscriptionId)
            } else tm

            targetTM?.subscriberId?.let { imsi ->
                val md = MessageDigest.getInstance("SHA-256")
                val hash = Base64.encodeToString(
                    md.digest(imsi.toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP
                )

                val simOperator = targetTM.simOperator ?: ""
                infos.add(
                    SimOperatorInfo(
                        sim_operator = simOperator,
                        imsi_hash = hash
                    )
                )
            }
        }
    } catch (e: SecurityException) {
        Log.e(TAG, "No permission to access SIM info for operator list", e)
    } catch (e: Exception) {
        Log.e(TAG, "Error hashing IMSI", e)
    }
    return infos
}
