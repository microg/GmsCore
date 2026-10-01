package com.google.android.gms.wearable.internal

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class DataItemParcelable(
    val uri: Uri?,
    val assets: Bundle? = null,
    val data: ByteArray? = null
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeParcelable(dest, 1, uri, flags, false)
        SafeParcelWriter.writeBundle(dest, 2, assets, false)
        SafeParcelWriter.writeByteArray(dest, 3, data, false)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<DataItemParcelable> {
        override fun createFromParcel(parcel: Parcel): DataItemParcelable {
            var uri: Uri? = null
            var assets: Bundle? = null
            var data: ByteArray? = null

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    1 -> uri = SafeParcelReader.readParcelable(parcel, header, Uri::class.java.classLoader)
                    2 -> assets = SafeParcelReader.readBundle(parcel, header)
                    3 -> data = SafeParcelReader.createByteArray(parcel, header)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return DataItemParcelable(uri, assets, data)
        }

        override fun newArray(size: Int): Array<DataItemParcelable?> = arrayOfNulls(size)
    }
}
