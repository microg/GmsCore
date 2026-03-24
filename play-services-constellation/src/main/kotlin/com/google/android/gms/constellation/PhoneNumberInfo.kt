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
data class PhoneNumberInfo @Constructor constructor(
    @JvmField @Param(1) @Field(1) val version: Int,
    @JvmField @Param(2) @Field(2) val phoneNumber: String?,
    @JvmField @Param(3) @Field(3) val verificationTime: Long,
    @JvmField @Param(4) @Field(4) val extras: Bundle?
) : AbstractSafeParcelable() {

    override fun writeToParcel(out: Parcel, flags: Int) {
        CREATOR.writeToParcel(this, out, flags)
    }

    companion object {
        @JvmField
        val CREATOR = findCreator(PhoneNumberInfo::class.java)
    }
}
