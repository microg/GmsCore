package com.google.android.gms.wearable.internal

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class PutDataResponse(
    val statusCode: Int,
    val item: DataItemParcelable? = null
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeInt(dest, 1, statusCode)
        SafeParcelWriter.writeParcelable(dest, 2, item, flags, false)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<PutDataResponse> {
        override fun createFromParcel(parcel: Parcel): PutDataResponse {
            var statusCode = 0
            var item: DataItemParcelable? = null

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    1 -> statusCode = SafeParcelReader.readInt(parcel, header)
                    2 -> item = SafeParcelReader.readParcelable(parcel, header, DataItemParcelable::class.java.classLoader)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return PutDataResponse(statusCode, item)
        }

        override fun newArray(size: Int): Array<PutDataResponse?> = arrayOfNulls(size)
    }
}
