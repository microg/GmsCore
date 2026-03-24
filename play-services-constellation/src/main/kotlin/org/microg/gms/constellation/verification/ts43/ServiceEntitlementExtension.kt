package org.microg.gms.constellation.verification.ts43

import android.content.Context
import android.telephony.TelephonyManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.microg.gms.constellation.proto.OdsaOperation
import org.microg.gms.constellation.proto.ServiceEntitlementRequest

fun ServiceEntitlementRequest.builder(
    telephonyManager: TelephonyManager,
    eapId: String,
    appIds: List<String> = emptyList()
) = ServiceEntitlementBuilder(
    imsi = telephonyManager.subscriberId ?: "",
    iccid = try {
        telephonyManager.simSerialNumber
    } catch (_: Exception) {
        null
    },
    terminalId = telephonyManager.imei,
    groupIdLevel1 = runCatching { telephonyManager.groupIdLevel1 }.getOrNull(),
    eapId = eapId,
    appIds = appIds,
    req = this
)

fun ServiceEntitlementRequest.userAgent(context: Context): String {
    val packageVersion = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
    val vendor = terminal_vendor.take(4)
    val model = terminal_model.take(10)
    val swVersion = terminal_software_version.take(20)
    return "PRD-TS43 term-$vendor/$model /$packageVersion OS-Android/$swVersion"
}

fun OdsaOperation.builder(
    telephonyManager: TelephonyManager,
    serviceEntitlementRequest: ServiceEntitlementRequest? = null,
    appIds: List<String> = emptyList()
) = ServiceEntitlementBuilder(
    imsi = telephonyManager.subscriberId ?: "",
    iccid = try {
        telephonyManager.simSerialNumber
    } catch (_: Exception) {
        null
    },
    terminalId = telephonyManager.imei,
    groupIdLevel1 = runCatching { telephonyManager.groupIdLevel1 }.getOrNull(),
    eapId = "", // Not needed for ODSA
    appIds = appIds,
    req = serviceEntitlementRequest ?: ServiceEntitlementRequest(),
    odsa = this
)

class ServiceEntitlementBuilder(
    private val imsi: String,
    private val iccid: String?,
    private val terminalId: String?,
    private val groupIdLevel1: String?,
    private val eapId: String,
    private val appIds: List<String>,
    private val req: ServiceEntitlementRequest,
    private val odsa: OdsaOperation? = null
) {
    fun buildBaseUrl(entitlementUrl: String): HttpUrl {
        val baseUrl = entitlementUrl.toHttpUrl()

        // GMS truncates these fields: vendor (4), model (10), sw_version (20)
        val vendor = req.terminal_vendor.take(4)
        val model = req.terminal_model.take(10)
        val swVersion = req.terminal_software_version.take(20)

        return baseUrl.newBuilder().apply {
            when {
                req.authentication_token.isNotEmpty() -> {
                    addQueryParameter("token", req.authentication_token)
                    if (imsi.isNotEmpty()) addQueryParameter("IMSI", imsi)
                }

                req.temporary_token.isNotEmpty() -> {
                    addQueryParameter("temporary_token", req.temporary_token)
                }

                eapId.isNotEmpty() -> {
                    addQueryParameter("EAP_ID", eapId)
                }
            }
            addQueryParameter("terminal_id", terminalId ?: req.terminal_id)
            if (req.gid1.isNotEmpty()) {
                addQueryParameter("GID1", req.gid1)
            } else if ((req.entitlement_version.toBigDecimalOrNull()?.toInt() ?: 0) >= 12) {
                groupIdLevel1?.takeIf { it.isNotEmpty() }?.let { addQueryParameter("GID1", it) }
            }
            if (req.app_version.isNotEmpty()) addQueryParameter("app_version", req.app_version)
            addQueryParameter("terminal_vendor", vendor)
            addQueryParameter("terminal_model", model)
            addQueryParameter("terminal_sw_version", swVersion)
            addQueryParameter("app_name", req.app_name)
            if (req.boost_type.isNotEmpty()) addQueryParameter("boost_type", req.boost_type)
            if (appIds.isNotEmpty()) {
                appIds.forEach { addQueryParameter("app", it) }
            } else {
                addQueryParameter("app", "ap2014")
            }
            addQueryParameter("vers", req.configuration_version.toString())
            addQueryParameter("entitlement_version", req.entitlement_version)
            if (req.notification_token.isNotEmpty()) {
                addQueryParameter("notif_action", req.notification_action.toString())
                addQueryParameter("notif_token", req.notification_token)
            }

            // Handle ODSA specific fields if present
            odsa?.let {
                addQueryParameter("operation", it.operation)
                if (it.operation_type != -1) {
                    addQueryParameter("operation_type", it.operation_type.toString())
                }
                if (it.operation_targets.isNotEmpty()) {
                    addQueryParameter("operation_targets", it.operation_targets.joinToString(","))
                }
                if (it.terminal_iccid.isNotEmpty()) addQueryParameter(
                    "terminal_iccid",
                    it.terminal_iccid
                )
                else iccid?.let { i -> addQueryParameter("terminal_iccid", i) }

                if (it.terminal_eid.isNotEmpty()) addQueryParameter("terminal_eid", it.terminal_eid)
                if (it.target_terminal_id.isNotEmpty()) addQueryParameter(
                    "target_terminal_id",
                    it.target_terminal_id
                )
                if (it.target_terminal_iccid.isNotEmpty()) addQueryParameter(
                    "target_terminal_iccid",
                    it.target_terminal_iccid
                )
                if (it.target_terminal_eid.isNotEmpty()) addQueryParameter(
                    "target_terminal_eid",
                    it.target_terminal_eid
                )
                if (it.target_terminal_model.isNotEmpty()) addQueryParameter(
                    "target_terminal_model",
                    it.target_terminal_model
                )
                if (it.target_terminal_serial_number.isNotEmpty()) addQueryParameter(
                    "target_terminal_sn",
                    it.target_terminal_serial_number
                )

                it.target_terminal_ids.forEach { id ->
                    addQueryParameter("target_terminal_imeis", id)
                }

                if (it.old_terminal_id.isNotEmpty()) addQueryParameter(
                    "old_terminal_id",
                    it.old_terminal_id
                )
                if (it.old_terminal_iccid.isNotEmpty()) addQueryParameter(
                    "old_terminal_iccid",
                    it.old_terminal_iccid
                )

                // Companion fields
                if (it.companion_terminal_id.isNotEmpty()) addQueryParameter(
                    "companion_terminal_id",
                    it.companion_terminal_id
                )
                if (it.companion_terminal_vendor.isNotEmpty()) addQueryParameter(
                    "companion_terminal_vendor",
                    it.companion_terminal_vendor
                )
                if (it.companion_terminal_model.isNotEmpty()) addQueryParameter(
                    "companion_terminal_model",
                    it.companion_terminal_model
                )
                if (it.companion_terminal_software_version.isNotEmpty()) addQueryParameter(
                    "companion_terminal_sw_version",
                    it.companion_terminal_software_version
                )
                if (it.companion_terminal_friendly_name.isNotEmpty()) addQueryParameter(
                    "companion_terminal_friendly_name",
                    it.companion_terminal_friendly_name
                )
                if (it.companion_terminal_service.isNotEmpty()) addQueryParameter(
                    "companion_terminal_service",
                    it.companion_terminal_service
                )
                if (it.companion_terminal_iccid.isNotEmpty()) addQueryParameter(
                    "companion_terminal_iccid",
                    it.companion_terminal_iccid
                )
                if (it.companion_terminal_eid.isNotEmpty()) addQueryParameter(
                    "companion_terminal_eid",
                    it.companion_terminal_eid
                )
            }
        }.build()
    }
}
