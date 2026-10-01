package com.google.android.gms.wearable.internal

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class SendMessageResponse(
    val statusCode: Int,
    val requestId: Int
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeInt(dest, 1, statusCode)
        SafeParcelWriter.writeInt(dest, 2, requestId)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<SendMessageResponse> {
        override fun createFromParcel(parcel: Parcel): SendMessageResponse {
            var statusCode = 0
            var requestId = 0

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    1 -> statusCode = SafeParcelReader.readInt(parcel, header)
                    2 -> requestId = SafeParcelReader.readInt(parcel, header)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return SendMessageResponse(statusCode, requestId)
        }

        override fun newArray(size: Int): Array<SendMessageResponse?> = arrayOfNulls(size)
    }
}
