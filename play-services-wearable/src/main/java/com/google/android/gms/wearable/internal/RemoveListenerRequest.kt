package com.google.android.gms.wearable.internal

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class RemoveListenerRequest(
    val listener: IBinder? = null
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeStrongBinder(dest, 1, listener, false)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<RemoveListenerRequest> {
        override fun createFromParcel(parcel: Parcel): RemoveListenerRequest {
            var listener: IBinder? = null

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    1 -> listener = SafeParcelReader.readStrongBinder(parcel, header)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return RemoveListenerRequest(listener)
        }

        override fun newArray(size: Int): Array<RemoveListenerRequest?> = arrayOfNulls(size)
    }
}
