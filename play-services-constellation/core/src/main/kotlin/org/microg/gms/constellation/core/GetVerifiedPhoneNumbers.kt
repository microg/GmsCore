@file:SuppressLint("NewApi")

package org.microg.gms.constellation.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse.PhoneNumberVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersRequest
import org.microg.gms.constellation.core.proto.GetVerifiedPhoneNumbersRequest.PhoneNumberSelection
import org.microg.gms.constellation.core.proto.IIDTokenAuth
import org.microg.gms.constellation.core.proto.TokenOption
import org.microg.gms.constellation.core.proto.VerifiedPhoneNumber
import java.util.UUID

private const val TAG = "GetVerifiedPhoneNumbers"

internal suspend fun fetchVerifiedPhoneNumbers(
    context: Context,
    bundle: Bundle,
    callingPackage: String = bundle.getString("calling_package") ?: Constants.GMS_PACKAGE_NAME
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

internal fun VerifiedPhoneNumber.toPhoneNumberInfo(): PhoneNumberInfo {
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

internal fun VerifiedPhoneNumber.toPhoneNumberVerification(): PhoneNumberVerification {
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
