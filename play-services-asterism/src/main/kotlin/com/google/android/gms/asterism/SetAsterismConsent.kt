package com.google.android.gms.asterism

import android.os.Bundle
import android.os.Parcel
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Constructor
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import org.microg.gms.constellation.proto.AsterismClient
import org.microg.gms.constellation.proto.Consent
import org.microg.gms.constellation.proto.ConsentSource
import org.microg.gms.constellation.proto.ConsentVersion
import org.microg.gms.constellation.proto.FlowContext

@SafeParcelable.Class
data class SetAsterismConsentRequest
@Constructor
constructor(
    @JvmField @Param(1) @Field(1) val requestCode: Int,
    @JvmField @Param(2) @Field(2) val asterismClientValue: Int,
    @JvmField @Param(3) @Field(3) val flowContextValue: Int,
    @JvmField @Param(4) @Field(value = 4, type = "int[]") val tosResourceIds: IntArray?,
    @JvmField @Param(5) @Field(5) val timestamp: Long?,
    @JvmField @Param(6) @Field(6) val consentValue: Int,
    @JvmField @Param(7) @Field(7) val extras: Bundle?,
    @JvmField @Param(8) @Field(8) val statusValue: Int,
    @JvmField @Param(9) @Field(9) val tosUrl: String?,
    @JvmField @Param(10) @Field(10) val language: String?,
    @JvmField @Param(11) @Field(11) val country: String?,
    @JvmField @Param(12) @Field(12) val tosVersion: String?,
    @JvmField @Param(13) @Field(13) val tosContentTitle: String?,
    @JvmField @Param(14) @Field(14) val accountName: String?,
    @JvmField @Param(15) @Field(15) val consentVariant: String?,
    @JvmField @Param(16) @Field(16) val consentTrigger: String?,
    @JvmField @Param(17) @Field(17) val rcsFlowContextValue: Int,
    @JvmField @Param(18) @Field(18) val deviceConsentSourceValue: Int,
    @JvmField @Param(19) @Field(19) val deviceConsentVersionValue: Int,
) : AbstractSafeParcelable() {
    enum class Status(val value: Int) {
        RCS_DEFAULT(0),
        RCS_LEGAL_FYI(1),
        DEVICE_PNVR(2),
        ON_DEMAND(3),
        EXPIRED(4);

        companion object {
            fun fromValue(value: Int): Status = entries.find { it.value == value } ?: EXPIRED
        }
    }

    val asterismClient: AsterismClient
        get() = AsterismClient.fromValue(asterismClientValue) ?: AsterismClient.UNKNOWN_CLIENT

    val consent: Consent
        get() = Consent.fromValue(consentValue) ?: Consent.CONSENT_UNKNOWN

    val rcsFlowContext: FlowContext
        get() = FlowContext.fromValue(rcsFlowContextValue) ?: FlowContext.FLOW_CONTEXT_UNSPECIFIED

    val deviceConsentSource: ConsentSource
        get() = ConsentSource.fromValue(deviceConsentSourceValue)
            ?: ConsentSource.SOURCE_UNSPECIFIED

    val deviceConsentVersion: ConsentVersion
        get() = ConsentVersion.fromValue(deviceConsentVersionValue).let {
            if (it == null || it == ConsentVersion.CONSENT_VERSION_UNSPECIFIED) {
                ConsentVersion.PHONE_VERIFICATION_DEFAULT
            } else {
                it
            }
        }

    fun isDevicePnvrFlow(): Boolean {
        return asterismClient == AsterismClient.CONSTELLATION &&
                deviceConsentSourceValue > 0 &&
                deviceConsentVersionValue > 0
    }

    val status: Status
        get() = Status.fromValue(statusValue)

    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(SetAsterismConsentRequest::class.java)
    }
}

@SafeParcelable.Class
data class SetAsterismConsentResponse
@Constructor
constructor(
    @JvmField @Param(1) @Field(1) val requestCode: Int,
    @JvmField @Param(2) @Field(2) val gmscoreIidToken: String?,
    @JvmField @Param(3) @Field(3) val fid: String?
) : AbstractSafeParcelable() {

    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(SetAsterismConsentResponse::class.java)
    }
}
