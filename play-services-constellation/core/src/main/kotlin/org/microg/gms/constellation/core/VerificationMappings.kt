package org.microg.gms.constellation.core

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.google.android.gms.constellation.VerifyPhoneNumberResponse.PhoneNumberVerification
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.UnverifiedInfo
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.VerificationMethod

fun UnverifiedInfo.Reason.toVerificationStatus(): Verification.Status {
    return when (this) {
        UnverifiedInfo.Reason.UNKNOWN_REASON -> Verification.Status.STATUS_UNKNOWN
        UnverifiedInfo.Reason.THROTTLED -> Verification.Status.STATUS_THROTTLED
        UnverifiedInfo.Reason.FAILED -> Verification.Status.STATUS_FAILED
        UnverifiedInfo.Reason.SKIPPED -> Verification.Status.STATUS_SKIPPED
        UnverifiedInfo.Reason.NOT_REQUIRED -> Verification.Status.STATUS_NOT_REQUIRED
        UnverifiedInfo.Reason.PHONE_NUMBER_ENTRY_REQUIRED ->
            Verification.Status.STATUS_PHONE_NUMBER_ENTRY_REQUIRED

        UnverifiedInfo.Reason.INELIGIBLE -> Verification.Status.STATUS_INELIGIBLE
        UnverifiedInfo.Reason.DENIED -> Verification.Status.STATUS_DENIED
        UnverifiedInfo.Reason.NOT_IN_SERVICE -> Verification.Status.STATUS_NOT_IN_SERVICE
    }
}

val Verification.state: Verification.State
    get() = when {
        verification_info != null -> Verification.State.VERIFIED
        pending_verification_info != null -> Verification.State.PENDING
        unverified_info != null -> Verification.State.NONE
        else -> Verification.State.UNKNOWN
    }

val Verification.effectiveStatus: Verification.Status
    get() = when (state) {
        Verification.State.VERIFIED -> Verification.Status.STATUS_VERIFIED
        Verification.State.PENDING ->
            if (status != Verification.Status.STATUS_UNKNOWN) {
                status
            } else {
                Verification.Status.STATUS_PENDING
            }

        Verification.State.NONE ->
            unverified_info?.reason?.toVerificationStatus() ?: Verification.Status.STATUS_UNKNOWN

        Verification.State.UNKNOWN -> Verification.Status.STATUS_UNKNOWN
    }

@RequiresApi(Build.VERSION_CODES.O)
fun Verification.toClientVerification(imsiToSlotMap: Map<String, Int>): PhoneNumberVerification {
    val clientStatus = effectiveStatus.toClientStatus()
    val extras = buildClientExtras()
    val simImsi = association?.sim?.sim_info?.imsi?.firstOrNull()
    val simSlot = if (simImsi != null) imsiToSlotMap[simImsi] ?: -1 else -1

    var phoneNumber = ""
    var timestampMillis = 0L
    var verificationMethod = VerificationMethod.UNKNOWN
    var verificationToken: String? = null
    var retryAfterSeconds = -1L

    when (state) {
        Verification.State.VERIFIED -> {
            val info = verification_info
            val verifiedPhoneNumber = info?.phone_number
            require(!verifiedPhoneNumber.isNullOrEmpty()) { "Verified phone number is empty" }
            phoneNumber = verifiedPhoneNumber
            timestampMillis = info.verification_time?.toEpochMilli() ?: 0L
            verificationMethod = info.challenge_method
            verificationToken = extras.getString("id_token")
            extras.remove("phone_number")
            extras.remove("id_token")
            extras.remove("verification_time_millis")
        }

        Verification.State.PENDING -> {
            verificationMethod =
                pending_verification_info?.challenge?.type ?: VerificationMethod.UNKNOWN
        }

        Verification.State.NONE -> {
            val info = unverified_info
            verificationMethod = info?.challenge_method ?: VerificationMethod.UNKNOWN
            retryAfterSeconds = info?.retry_after_time?.let { ts ->
                val now = System.currentTimeMillis() / 1000L
                (ts.epochSecond - now).coerceAtLeast(0L)
            } ?: -1L
        }

        Verification.State.UNKNOWN -> Unit
    }

    extras.remove("verification_method")
    extras.remove("sim_slot_index")

    return PhoneNumberVerification(
        phoneNumber,
        timestampMillis,
        verificationMethod.toClientMethod(),
        simSlot,
        verificationToken,
        extras,
        clientStatus,
        retryAfterSeconds
    )
}

private fun Verification.buildClientExtras(): Bundle {
    val bundle = Bundle()
    for (param in api_params) {
        bundle.putParam(param)
    }

    val slotIndex = association?.sim?.sim_slot?.slot_index
    if (slotIndex != null && slotIndex >= 0) {
        bundle.putString("sim_slot_index", slotIndex.toString())
    }
    return bundle
}

private fun Bundle.putParam(param: Param) {
    if (param.key == "verification_method") {
        param.value_.toIntOrNull()?.let {
            putInt(param.key, it)
            return
        }
    }
    putString(param.key, param.value_)
}

fun VerificationMethod.toClientMethod(): Int {
    return when (this) {
        VerificationMethod.UNKNOWN -> 0
        VerificationMethod.TS43 -> 9
        else -> value
    }
}

fun Verification.Status.toClientStatus(): Int {
    return when (this) {
        Verification.Status.STATUS_UNKNOWN,
        Verification.Status.STATUS_NONE -> 0

        Verification.Status.STATUS_PENDING -> 6
        Verification.Status.STATUS_VERIFIED -> 1
        Verification.Status.STATUS_THROTTLED -> 3
        Verification.Status.STATUS_FAILED -> 2
        Verification.Status.STATUS_SKIPPED -> 4
        Verification.Status.STATUS_NOT_REQUIRED -> 5
        Verification.Status.STATUS_PHONE_NUMBER_ENTRY_REQUIRED -> 7
        Verification.Status.STATUS_INELIGIBLE -> 8
        Verification.Status.STATUS_DENIED -> 9
        Verification.Status.STATUS_NOT_IN_SERVICE -> 10
    }
}
