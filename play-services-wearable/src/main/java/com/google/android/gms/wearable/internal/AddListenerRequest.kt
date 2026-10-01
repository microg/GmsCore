package com.google.android.gms.wearable.internal

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class AddListenerRequest(
    val listener: IBinder? = null
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeStrongBinder(dest, 1, listener, false)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<AddListenerRequest> {
        override fun createFromParcel(parcel: Parcel): AddListenerRequest {
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
            return AddListenerRequest(listener)
        }

        override fun newArray(size: Int): Array<AddListenerRequest?> = arrayOfNulls(size)
    }
}
