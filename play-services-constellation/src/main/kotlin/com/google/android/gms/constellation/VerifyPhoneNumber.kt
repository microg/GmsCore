package com.google.android.gms.constellation

import android.os.Bundle
import android.os.Parcel
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Constructor
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter

@SafeParcelable.Class
data class VerifyPhoneNumberRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val policyId: String,
    @JvmField @Param(2) @Field(2) val timeout: Long,
    @JvmField @Param(3) @Field(3) val idTokenRequest: IdTokenRequest,
    @JvmField @Param(4) @Field(4) val extras: Bundle,
    @JvmField @Param(5) @Field(5) val targetedSims: List<ImsiRequest>,
    @JvmField @Param(6) @Field(6) val silent: Boolean,
    @JvmField @Param(7) @Field(7) val apiVersion: Int,
    @JvmField @Param(8) @Field(8) val verificationMethodsValues: List<Int>
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(VerifyPhoneNumberRequest::class.java)
    }
}

@SafeParcelable.Class
data class IdTokenRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val idToken: String,
    @JvmField @Param(2) @Field(2) val subscriberHash: String
) : AbstractSafeParcelable() {

    override fun writeToParcel(out: Parcel, flags: Int) {
        CREATOR.writeToParcel(this, out, flags)
    }

    companion object {
        @JvmField
        val CREATOR: SafeParcelableCreatorAndWriter<IdTokenRequest> =
            findCreator(IdTokenRequest::class.java)
    }
}

@SafeParcelable.Class
data class ImsiRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val imsi: String,
    @JvmField @Param(2) @Field(2) val phoneNumberHint: String
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(ImsiRequest::class.java)
    }
}

@SafeParcelable.Class
data class VerifyPhoneNumberResponse @Constructor constructor(
    @JvmField @Param(1) @Field(1) val verifications: Array<PhoneNumberVerification>,
    @JvmField @Param(2) @Field(2) val extras: Bundle
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(VerifyPhoneNumberResponse::class.java)
    }
}

@SafeParcelable.Class
data class PhoneNumberVerification @Constructor constructor(
    @JvmField @Param(1) @Field(1) val phoneNumber: String?,
    @JvmField @Param(2) @Field(2) val timestampMillis: Long,
    @JvmField @Param(3) @Field(3) val verificationMethod: Int,
    @JvmField @Param(4) @Field(4) val simSlot: Int,
    @JvmField @Param(5) @Field(5) val verificationToken: String?,
    @JvmField @Param(6) @Field(6) val extras: Bundle?,
    @JvmField @Param(7) @Field(7) val verificationStatus: Int,
    @JvmField @Param(8) @Field(8) val retryAfterSeconds: Long
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(PhoneNumberVerification::class.java)
    }
}