package com.google.android.gms.constellation

import android.os.Parcel
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Constructor
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter

@SafeParcelable.Class
data class GetIidTokenRequest @Constructor constructor(
    @JvmField @Param(1) @Field(1) val projectNumber: Long?
) : AbstractSafeParcelable() {
    override fun writeToParcel(out: Parcel, flags: Int) {
        CREATOR.writeToParcel(this, out, flags)
    }

    companion object {
        @JvmField
        val CREATOR: SafeParcelableCreatorAndWriter<GetIidTokenRequest> =
            findCreator(GetIidTokenRequest::class.java)
    }
}

@SafeParcelable.Class
data class GetIidTokenResponse @Constructor constructor(
    @JvmField @Param(1) @Field(1) val iidToken: String,
    @JvmField @Param(2) @Field(2) val fid: String,
    @JvmField @Param(3) @Field(value = 3, type = "byte[]") val signature: ByteArray?,
    @JvmField @Param(4) @Field(4) val timestamp: Long
) : AbstractSafeParcelable() {

    override fun writeToParcel(out: Parcel, flags: Int) {
        CREATOR.writeToParcel(this, out, flags)
    }

    companion object {
        @JvmField
        val CREATOR: SafeParcelableCreatorAndWriter<GetIidTokenResponse> =
            findCreator(GetIidTokenResponse::class.java)
    }
}