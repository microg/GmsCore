package com.google.android.gms.asterism

import android.os.Parcel
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Constructor
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import org.microg.gms.constellation.proto.AsterismClient
import org.microg.gms.constellation.proto.Consent
import org.microg.gms.constellation.proto.ConsentVersion

@SafeParcelable.Class
data class GetAsterismConsentRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val requestCode: Int,
    @JvmField @Param(2) @Field(2) val asterismClientValue: Int
) : AbstractSafeParcelable() {
    val asterismClient: AsterismClient
        get() = AsterismClient.fromValue(asterismClientValue) ?: AsterismClient.UNKNOWN_CLIENT

    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(GetAsterismConsentRequest::class.java)
    }
}

@SafeParcelable.Class
data class GetAsterismConsentResponse @Constructor constructor(
    @JvmField @Param(1) @Field(1) val requestCode: Int,
    @JvmField @Param(2) @Field(2) val consentStateValue: Int,
    @JvmField @Param(3) @Field(3) val gmscoreIidToken: String?,
    @JvmField @Param(4) @Field(4) val fid: String?,
    @JvmField @Param(5) @Field(5) val consentTypeValue: Int
) : AbstractSafeParcelable() {
    constructor(
        requestCode: Int,
        consentState: Consent,
        gmscoreIidToken: String?,
        fid: String?,
        consentVersion: ConsentVersion
    ) : this(
        requestCode,
        if (consentState == Consent.CONSENTED || consentState == Consent.CONSENT_UNKNOWN) consentState.value else Consent.NO_CONSENT.value,
        gmscoreIidToken,
        fid,
        consentVersion.value
    )

    override fun writeToParcel(out: Parcel, flags: Int) = CREATOR.writeToParcel(this, out, flags)

    companion object {
        @JvmField
        val CREATOR = findCreator(GetAsterismConsentResponse::class.java)
    }
}
