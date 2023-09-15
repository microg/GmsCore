/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.huawei.signature.diff;

import static com.huawei.signature.diff.AppListDatabaseOpenHelper.COLUMN_NAME;
import static com.huawei.signature.diff.AppListDatabaseOpenHelper.TABLE_APPLIST;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import org.microg.signature.fake.R;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Date;

public class SignatureService extends Service {
    private static final String TAG = "SignatureService";
    private SQLiteDatabase database;
    private AppListDatabaseOpenHelper openHelper;
    private long start;

    @Override
    public void onCreate() {
        super.onCreate();
        this.openHelper = new AppListDatabaseOpenHelper(this);
        this.database = openHelper.getReadableDatabase();
        this.start = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        this.openHelper.close();
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("Started: " + new Date(start));
    }

    private final ISignatureService.Stub binder = new ISignatureService.Stub() {

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (Binder.getCallingPid() > 10000) {
                Log.w(TAG, "Illegal access from app");
                reply.writeException(new UnsupportedOperationException("Illegal"));
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public String[] querySignature(String packageName, boolean suggested) throws RemoteException {
            try (Cursor cursor = database.query(TABLE_APPLIST, null, COLUMN_NAME + "=?",
                    new String[]{packageName}, null, null, null)) {
                switch (cursor.getCount()) {
                    case 0:
                        return getResult(suggested);
                    case 1:
                        if (cursor.moveToFirst()) {
                            int shouldFake = cursor.getInt(1);
                            return getResult(shouldFake == 1);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("result size: " + cursor.getCount());
                }

            } catch (Exception e) {
                Log.w(TAG, e);
            }
            return getResult(false);
        }

        private String[] getResult(boolean useFakeSignature) {
            if (useFakeSignature) {
                return new String[]{getString(R.string.fake_signature),};
            } else {
                return new String[]{getString(R.string.real_signature),};
            }
        }
    };
}