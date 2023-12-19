package com.google.android.gms.potokens.internal

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.potokens.PoToken
import com.google.android.gms.potokens.utils.PoTokenHelper

class PoTokensApiServiceImpl(private val context: Context, private val packageName: String) :
    IPoTokensService.Stub() {

    @Throws(RemoteException::class)
    override fun responseStatus(call: IStatusCallback, code: Int) {
        Log.d(TAG, "responseStatus this is success")
        call.onResult(Status.SUCCESS)
    }

    @Throws(RemoteException::class)
    override fun responseStatusToken(call: ITokenCallbacks, i: Int, bArr: ByteArray) {
        Log.d(TAG, "responseStatusToken this is packageName,$packageName")

        val poTokenHelper = PoTokenHelper()
        val bytes = poTokenHelper.callPoToken(context, packageName, bArr)
        Log.d(TAG, "responseStatusToken this is result," + bytes.size)

        call.responseToken(Status.SUCCESS, PoToken(bytes))
        Log.d(TAG, "responseStatusToken this is result end")
    }

}