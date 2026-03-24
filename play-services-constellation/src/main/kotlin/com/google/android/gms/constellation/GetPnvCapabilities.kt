package com.google.android.gms.constellation

import android.os.Parcel
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Constructor
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param

@SafeParcelable.Class
data class GetPnvCapabilitiesRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val policyId: String,
    @JvmField @Param(2) @Field(2) val verificationTypes: List<Int>,
    @JvmField @Param(3) @Field(3) val simSlotIndices: List<Int>
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(GetPnvCapabilitiesRequest::class.java)
    }
}

@SafeParcelable.Class
data class GetPnvCapabilitiesResponse @Constructor constructor(
    @JvmField @Param(1) @Field(1) val simCapabilities: List<SimCapability>
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(GetPnvCapabilitiesResponse::class.java)
    }
}

@SafeParcelable.Class
data class SimCapability @Constructor constructor(
    @JvmField @Param(1) @Field(1) val slotValue: Int,
    @JvmField @Param(2) @Field(2) val subscriberIdDigest: String,
    @JvmField @Param(3) @Field(3) val carrierId: Int,
    @JvmField @Param(4) @Field(4) val operatorName: String,
    @JvmField @Param(5) @Field(5) val verificationCapabilities: List<VerificationCapability>
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(SimCapability::class.java)
    }
}

@SafeParcelable.Class
data class VerificationCapability @Constructor constructor(
    @JvmField @Param(1) @Field(1) val verificationMethod: Int,
    @JvmField @Param(2) @Field(2) val statusValue: Int // Enums are typically passed as Ints in SafeParcelable
) : AbstractSafeParcelable() {
    constructor(verificationMethod: Int, status: VerificationStatus) : this(
        verificationMethod,
        status.value
    )

    val status: VerificationStatus
        get() = VerificationStatus.fromInt(statusValue)

    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(VerificationCapability::class.java)
    }
}

enum class VerificationStatus(val value: Int) {
    SUPPORTED(1),
    UNSUPPORTED_CARRIER(2),
    UNSUPPORTED_API_VERSION(3),
    UNSUPPORTED_SIM_NOT_READY(4);

    companion object {
        fun fromInt(value: Int): VerificationStatus = when (value) {
            2 -> UNSUPPORTED_CARRIER
            3 -> UNSUPPORTED_API_VERSION
            4 -> UNSUPPORTED_SIM_NOT_READY
            else -> SUPPORTED
        }
    }
}
