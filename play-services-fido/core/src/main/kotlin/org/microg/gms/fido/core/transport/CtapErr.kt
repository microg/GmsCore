package org.microg.gms.fido.core.transport

/**
 * CTAP status code to value
 *
 * from https://fidoalliance.org/specs/fido-v2.2-ps-20250714/fido-client-to-authenticator-protocol-v2.2-ps-20250714.pdf#y%D4F%u2018A%E2%D9%27%21t%16%B7%04W%F4%215%D0J%D1
 */
enum class CtapErr(val statusCode: Byte, val description: String) {
    // CTAP2_OK
    CTAP1_ERR_SUCCESS(0x00, "Indicates successful response."),
    CTAP1_ERR_INVALID_COMMAND(0x01, "The command is not a valid CTAP command."),
    CTAP1_ERR_INVALID_PARAMETER(0x02, "The command included an invalid parameter."),
    CTAP1_ERR_INVALID_LENGTH(0x03, "Invalid message or item length."),
    CTAP1_ERR_INVALID_SEQ(0x04, "Invalid message sequencing."),
    CTAP1_ERR_TIMEOUT(0x05, "Message timed out."),
    CTAP1_ERR_CHANNEL_BUSY(0x06, "Channel busy. Client SHOULD retry the request after a short delay."),
    CTAP1_ERR_LOCK_REQUIRED(0x0A, "Command requires channel lock."),
    CTAP1_ERR_INVALID_CHANNEL(0x0B, "Command not allowed on this cid."),
    CTAP2_ERR_CBOR_UNEXPECTED_TYPE(0x11, "Invalid/unexpected CBOR error."),
    CTAP2_ERR_INVALID_CBOR(0x12, "Error when parsing CBOR."),
    CTAP2_ERR_MISSING_PARAMETER(0x14, "Missing non-optional parameter."),
    CTAP2_ERR_LIMIT_EXCEEDED(0x15, "Limit for number of items exceeded."),
    CTAP2_ERR_FP_DATABASE_FULL(0x17, "Fingerprint data base is full, e.g., during enrollment."),
    CTAP2_ERR_LARGE_BLOB_STORAGE_FULL(0x18, "Large blob storage is full."),
    CTAP2_ERR_CREDENTIAL_EXCLUDED(0x19, "Valid credential found in the exclude list."),
    CTAP2_ERR_PROCESSING(0x21, "Processing (Lengthy operation is in progress)."),
    CTAP2_ERR_INVALID_CREDENTIAL(0x22, "Credential not valid for the authenticator."),
    CTAP2_ERR_USER_ACTION_PENDING(0x23, "Authentication is waiting for user interaction."),
    CTAP2_ERR_OPERATION_PENDING(0x24, "Processing, lengthy operation is in progress."),
    CTAP2_ERR_NO_OPERATIONS(0x25, "No request is pending."),
    CTAP2_ERR_UNSUPPORTED_ALGORITHM(0x26, "Authenticator does not support requested algorithm."),
    CTAP2_ERR_OPERATION_DENIED(0x27, "Not authorized for requested operation."),
    CTAP2_ERR_KEY_STORE_FULL(0x28, "Internal key storage is full."),
    CTAP2_ERR_UNSUPPORTED_OPTION(0x2B, "Unsupported option."),
    CTAP2_ERR_INVALID_OPTION(0x2C, "Not a valid option for current operation."),
    CTAP2_ERR_KEEPALIVE_CANCEL(0x2D, "Pending keep alive was cancelled."),
    CTAP2_ERR_NO_CREDENTIALS(0x2E, "No valid credentials provided."),
    CTAP2_ERR_USER_ACTION_TIMEOUT(0x2F, "A user action timeout occurred."),
    CTAP2_ERR_NOT_ALLOWED(0x30, "Continuation command, such as, authenticatorGetNextAssertion not allowed."),
    CTAP2_ERR_PIN_INVALID(0x31, "PIN Invalid."),
    CTAP2_ERR_PIN_BLOCKED(0x32, "PIN Blocked."),
    CTAP2_ERR_PIN_AUTH_INVALID(0x33, "PIN authentication,pinUvAuthParam, verification failed."),
    CTAP2_ERR_PIN_AUTH_BLOCKED(0x34, "PIN authentication using pinUvAuthToken blocked. Requires power cycle to reset."),
    CTAP2_ERR_PIN_NOT_SET(0x35, "No PIN has been set."),
    CTAP2_ERR_PUAT_REQUIRED(0x36, "A pinUvAuthToken is required for the selected operation. See also the pinUvAuthToken option ID."),
    CTAP2_ERR_PIN_POLICY_VIOLATION(0x37, "PIN policy violation. Minimum PIN length or PIN complexity may trigger this error."),
    CTAP_RESERVED_FOR_FUTURE_USE(0x38, "Reserved for Future Use"),
    CTAP2_ERR_REQUEST_TOO_LARGE(0x39, "Authenticator cannot handle this request due to memory constraints."),
    CTAP2_ERR_ACTION_TIMEOUT(0x3A, "The current operation has timed out."),
    CTAP2_ERR_UP_REQUIRED(0x3B, "User presence is required for the requested operation."),
    CTAP2_ERR_UV_BLOCKED(0x3C, "Built-in user verification is disabled."),
    CTAP2_ERR_INTEGRITY_FAILURE(0x3D, "A checksum did not match."),
    CTAP2_ERR_INVALID_SUBCOMMAND(0x3E, "The requested subcommand is either invalid or not implemented."),
    CTAP2_ERR_UV_INVALID(0x3F, "Built-in user verification unsuccessful. The platform SHOULD retry."),
    CTAP2_ERR_UNAUTHORIZED_PERMISSION(0x40, "The permissions parameter contains an unauthorized permission."),
    CTAP1_ERR_OTHER(0x7F, "Other unspecified error."),
    CTAP2_ERR_SPEC_LAST(0xDF.toByte(), "CTAP 2 spec last error."),
    CTAP2_ERR_EXTENSION_FIRST(0xE0.toByte(), "Extension specific error."),
    CTAP2_ERR_EXTENSION_LAST(0xEF.toByte(), "Extension specific error."),
    CTAP2_ERR_VENDOR_FIRST(0xF0.toByte(), "Vendor specific error."),
    CTAP2_ERR_VENDOR_LAST(0xFF.toByte(), "Vendor specific error.");

    fun fullDescription() = "${this.name} (" +
            "${(this.statusCode.toInt() and 0xff).toString(16)}," +
            "${this.description})"

    companion object {
        fun fromByte(statusCode: Byte): CtapErr? {
            return CtapErr.entries.firstOrNull {
                it.statusCode == statusCode
            }
        }

        fun description(statusCode: Byte) = fromByte(statusCode)?.fullDescription()
            ?: "Unknown error (status=${(statusCode.toInt() and 0xff).toString(16)})"
    }
}