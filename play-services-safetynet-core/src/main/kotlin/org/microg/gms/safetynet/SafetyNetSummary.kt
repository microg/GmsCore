package org.microg.gms.safetynet

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.api.Status
import kotlin.properties.Delegates

data class SafetyNetSummary(
    val requestType: SafetyNetRequestType,


    // request data
    val packageName: String,
    val nonce: ByteArray?, // null with SafetyNetRequestType::RECAPTCHA
    val timestamp: Long,

    ) : Parcelable {

    var id by Delegates.notNull<Int>()

    // response data
    // note : responseStatus do not represent the actual status in case of an attestation, it will be in resultData
    var responseStatus: Status? = null
    var responseData: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SafetyNetSummary

        if (requestType != other.requestType) return false
        if (packageName != other.packageName) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (responseStatus != other.responseStatus) return false
        if (responseData != other.responseData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = requestType.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + nonce.hashCode()
        result = 31 * result + (responseStatus?.hashCode() ?: 0)
        result = 31 * result + (responseData?.hashCode() ?: 0)
        return result
    }


    // Parcelable implementation

    constructor(parcel: Parcel) : this(
        SafetyNetRequestType.valueOf(parcel.readString()!!),
        parcel.readString()!!,
        parcel.createByteArray(),
        parcel.readLong()
    ) {
        responseStatus = parcel.readParcelable(Status::class.java.classLoader)
        responseData = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(requestType.name)
        parcel.writeString(packageName)
        parcel.writeByteArray(nonce)
        parcel.writeLong(timestamp)
        parcel.writeParcelable(responseStatus, flags)
        parcel.writeString(responseData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SafetyNetSummary> {
        override fun createFromParcel(parcel: Parcel): SafetyNetSummary {
            return SafetyNetSummary(parcel)
        }

        override fun newArray(size: Int): Array<SafetyNetSummary?> {
            return arrayOfNulls(size)
        }
    }


}