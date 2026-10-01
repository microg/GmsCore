package com.google.android.gms.wearable

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class PutDataRequest(
    val uri: Uri?,
    val assets: Bundle? = null,
    val data: ByteArray? = null
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeParcelable(dest, 2, uri, flags, false)
        SafeParcelWriter.writeBundle(dest, 4, assets, false)
        SafeParcelWriter.writeByteArray(dest, 5, data, false)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<PutDataRequest> {
        override fun createFromParcel(parcel: Parcel): PutDataRequest {
            var uri: Uri? = null
            var assets: Bundle? = null
            var data: ByteArray? = null

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    2 -> uri = SafeParcelReader.readParcelable(parcel, header, Uri::class.java.classLoader)
                    4 -> assets = SafeParcelReader.readBundle(parcel, header)
                    5 -> data = SafeParcelReader.createByteArray(parcel, header)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return PutDataRequest(uri, assets, data)
        }

        override fun newArray(size: Int): Array<PutDataRequest?> = arrayOfNulls(size)
    }
}
