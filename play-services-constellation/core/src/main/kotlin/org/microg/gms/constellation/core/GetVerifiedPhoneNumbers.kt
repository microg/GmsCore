@file:SuppressLint("NewApi")
package org.microg.gms.constellation.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse.PhoneNumberVerification
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersRequest
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersRequest.PhoneNumberSelection
import org.microg.gms.constellation.core.proto.IIDTokenAuth
import org.microg.gms.constellation.core.proto.TokenOption
import java.util.UUID
import org.microg.gms.constellation.core.proto.VerifiedPhoneNumber

private const val TAG = "GetVerifiedPhoneNumbers"

suspend fun handleGetVerifiedPhoneNumbers(
    context: Context,
    callbacks: IConstellationCallbacks,
    bundle: Bundle
) = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw Exception("Unsupported SDK")
        }

        val phoneNumbers = fetchVerifiedPhoneNumbers(context, bundle).map { it.toPhoneNumberInfo() }

        callbacks.onPhoneNumberVerified(Status.SUCCESS, phoneNumbers, ApiMetadata.DEFAULT)
    } catch (e: Exception) {
        Log.e(TAG, "Error in GetVerifiedPhoneNumbers (read-only)", e)
        callbacks.onPhoneNumberVerified(Status.INTERNAL_ERROR, emptyList(), ApiMetadata.DEFAULT)
    }
}

internal suspend fun fetchVerifiedPhoneNumbers(
    context: Context,
    bundle: Bundle,
    callingPackage: String = bundle.getString("calling_package") ?: context.packageName
): List<VerifiedPhoneNumber> = withContext(Dispatchers.IO) {
    val authManager = context.authManager
    val sessionId = UUID.randomUUID().toString()
    val selections = extractPhoneNumberSelections(bundle)
    val certificateHash = bundle.getString("certificate_hash") ?: ""
    val tokenNonce = bundle.getString("token_nonce") ?: ""

    val iidToken = authManager.getIidToken(IidTokenPhenotypes.READ_ONLY_PROJECT_NUMBER)
    val iidTokenAuth = if (VerifyPhoneNumberApiPhenotypes.ENABLE_CLIENT_SIGNATURE) {
        val (signatureBytes, signTimestamp) = authManager.signIidToken(iidToken)
        IIDTokenAuth(
            iid_token = iidToken,
            client_sign = signatureBytes.toByteString(),
            sign_timestamp = signTimestamp
        )
    } else {
        IIDTokenAuth(iid_token = iidToken)
    }

    val getRequest = GetVerifiedPhoneNumbersRequest(
        session_id = sessionId,
        iid_token_auth = iidTokenAuth,
        phone_number_selections = selections,
        token_option = TokenOption(
            certificate_hash = certificateHash,
            token_nonce = tokenNonce,
            package_name = callingPackage
        )
    )

    Log.d(TAG, "Calling GetVerifiedPhoneNumbers RPC (read-only mode)...")
    val response = RpcClient.phoneNumberClient
        .GetVerifiedPhoneNumbers()
        .execute(getRequest)
    Log.d(TAG, "GetVerifiedPhoneNumbers response: ${response.phone_numbers.size} numbers")
    response.phone_numbers
}

internal fun List<VerifiedPhoneNumber>.toVerifyPhoneNumberResponse(): VerifyPhoneNumberResponse {
    return VerifyPhoneNumberResponse(
        map { it.toPhoneNumberVerification() }.toTypedArray(),
        Bundle.EMPTY
    )
}

private fun VerifiedPhoneNumber.toPhoneNumberInfo(): PhoneNumberInfo {
    val extras = Bundle().apply {
        if (id_token.isNotEmpty()) {
            putString("id_token", id_token)
        }
        putInt("rcs_state", rcs_state.value)
    }

    return PhoneNumberInfo(
        1,
        phone_number,
        verification_time?.toEpochMilli() ?: 0L,
        extras
    )
}

private fun VerifiedPhoneNumber.toPhoneNumberVerification(): PhoneNumberVerification {
    val extras = Bundle().apply {
        putInt("rcs_state", rcs_state.value)
    }

    // GMS read-only V2 leaves method/slot unset and returns a verified record directly.
    return PhoneNumberVerification(
        phone_number,
        verification_time?.toEpochMilli() ?: 0L,
        0,
        -1,
        id_token.ifEmpty { null },
        extras,
        1,
        -1L
    )
}

private fun extractPhoneNumberSelections(bundle: Bundle): List<PhoneNumberSelection> {
    val selections = mutableListOf<PhoneNumberSelection>()
    val selectionInts = bundle.getIntegerArrayList("phone_number_selection")

    if (!selectionInts.isNullOrEmpty()) {
        selections.addAll(selectionInts.mapNotNull { PhoneNumberSelection.fromValue(it) })
    } else {
        when (bundle.getString("rcs_read_option", "")) {
            "READ_PROVISIONED" -> {
                selections.add(PhoneNumberSelection.CONSTELLATION)
                selections.add(PhoneNumberSelection.RCS)
            }

            "READ_PROVISIONED_ONLY" -> selections.add(PhoneNumberSelection.RCS)
            else -> selections.add(PhoneNumberSelection.CONSTELLATION)
        }
    }
    return selections
}
