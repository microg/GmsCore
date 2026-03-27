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
        UnverifiedInfo.Reason.UNKNOWN_REASON -> Verification.Status.STATUS_PENDING
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

fun Verification.getState(): Verification.State {
    return when {
        verification_info != null -> Verification.State.VERIFIED
        pending_verification_info != null -> Verification.State.PENDING
        unverified_info != null -> Verification.State.NONE
        else -> Verification.State.UNKNOWN
    }
}

fun Verification.getVerificationStatus(): Verification.Status {
    return when (getState()) {
        Verification.State.VERIFIED -> Verification.Status.STATUS_VERIFIED

        Verification.State.PENDING -> {
            if (status != Verification.Status.STATUS_UNKNOWN) {
                status
            } else {
                Verification.Status.STATUS_PENDING
            }
        }

        Verification.State.NONE -> {
            unverified_info?.reason?.toVerificationStatus() ?: Verification.Status.STATUS_PENDING
        }

        Verification.State.UNKNOWN -> Verification.Status.STATUS_UNKNOWN
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Verification.toClientVerification(imsiToSlotMap: Map<String, Int>): PhoneNumberVerification {
    val verificationStatus = this.getVerificationStatus()
    var phoneNumber: String? = null
    var timestampMillis = System.currentTimeMillis()
    var verificationMethod = VerificationMethod.UNKNOWN
    var retryAfterSeconds = 0L
    val extras = buildClientExtras()

    when (this.getState()) {
        Verification.State.VERIFIED -> {
            val info = this.verification_info
            phoneNumber = info?.phone_number
            timestampMillis = info?.verification_time?.toEpochMilli() ?: System.currentTimeMillis()
            verificationMethod = info?.challenge_method ?: VerificationMethod.UNKNOWN
        }

        Verification.State.PENDING -> {
            verificationMethod =
                this.pending_verification_info?.challenge?.type ?: VerificationMethod.UNKNOWN
        }

        Verification.State.NONE -> {
            val info = this.unverified_info
            verificationMethod = info?.challenge_method ?: VerificationMethod.UNKNOWN
            retryAfterSeconds = info?.retry_after_time?.let { ts ->
                val now = System.currentTimeMillis() / 1000L
                (ts.epochSecond - now).coerceAtLeast(0L)
            } ?: 0L
        }

        else -> {}
    }

    val simImsi = this.association?.sim?.sim_info?.imsi?.firstOrNull()
    val simSlot = if (simImsi != null) imsiToSlotMap[simImsi] ?: 0 else 0
    val verificationToken = extras.getString("id_token")

    return PhoneNumberVerification(
        phoneNumber,
        timestampMillis,
        verificationMethod.toClientMethod(),
        simSlot,
        verificationToken,
        extras,
        verificationStatus.value,
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
        // GMS exposes this as a string inside the extras bundle.
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
        VerificationMethod.TS43 -> 9
        VerificationMethod.UNKNOWN -> 0
        else -> value
    }
}
