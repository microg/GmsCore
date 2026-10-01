package com.google.android.gms.wearable.internal

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

class NodeParcelable(
    val id: String,
    val displayName: String,
    val hopCount: Int = 1,
    val isNearby: Boolean = true
) : AbstractSafeParcelable() {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val header = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeString(dest, 1, id, false)
        SafeParcelWriter.writeString(dest, 2, displayName, false)
        SafeParcelWriter.writeInt(dest, 3, hopCount)
        SafeParcelWriter.writeBoolean(dest, 4, isNearby)
        SafeParcelWriter.finishObjectHeader(dest, header)
    }

    companion object CREATOR : Parcelable.Creator<NodeParcelable> {
        override fun createFromParcel(parcel: Parcel): NodeParcelable {
            var id = ""
            var displayName = ""
            var hopCount = 1
            var isNearby = true

            val end = SafeParcelReader.validateObjectHeader(parcel)
            while (parcel.dataPosition() < end) {
                val header = SafeParcelReader.readHeader(parcel)
                when (SafeParcelReader.getFieldId(header)) {
                    1 -> id = SafeParcelReader.createString(parcel, header) ?: ""
                    2 -> displayName = SafeParcelReader.createString(parcel, header) ?: ""
                    3 -> hopCount = SafeParcelReader.readInt(parcel, header)
                    4 -> isNearby = SafeParcelReader.readBoolean(parcel, header)
                    else -> SafeParcelReader.skipUnknownField(parcel, header)
                }
            }
            SafeParcelReader.ensureAtEnd(parcel, end)
            return NodeParcelable(id, displayName, hopCount, isNearby)
        }

        override fun newArray(size: Int): Array<NodeParcelable?> = arrayOfNulls(size)
    }
}
