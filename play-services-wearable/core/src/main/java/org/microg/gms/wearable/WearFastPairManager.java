package org.microg.gms.wearable;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WearFastPairManager {
    private static final String TAG = "WearFastPairManager";
    private static final String PREFS = "wearable_fast_pair_account_keys";
    private static final int ACCOUNT_KEY_LENGTH = 16;
    private static final byte ACCOUNT_KEY_PREFIX = 0x04;

    public static final class AccountKeyRecord {
        public final String accountName;
        public final byte[] accountKey;

        AccountKeyRecord(String accountName, byte[] accountKey) {
            this.accountName = accountName;
            this.accountKey = accountKey;
        }
    }

    public final Context context;
    private final SecureRandom random = new SecureRandom();

    public WearFastPairManager(Context context) {
        this.context = context;
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<AccountKeyRecord> getAccountKeys() {
        List<AccountKeyRecord> result = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs().getAll().entrySet()) {
            if (!(e.getValue() instanceof String)) continue;
            try {
                result.add(new AccountKeyRecord(e.getKey(), Base64.decode((String) e.getValue(), Base64.NO_WRAP)));
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Skipping malformed account key for " + e.getKey());
            }
        }
        return result;
    }

    public synchronized AccountKeyRecord getAccountKey(String accountName) {
        if (accountName == null) return null;
        for (AccountKeyRecord r : getAccountKeys()) {
            if (accountName.equals(r.accountName)) return r;
        }
        return null;
    }

    public synchronized AccountKeyRecord getOrCreateAccountKey(String accountName) {
        AccountKeyRecord existing = getAccountKey(accountName);
        if (existing != null) return existing;
        byte[] key = new byte[ACCOUNT_KEY_LENGTH];
        random.nextBytes(key);
        key[0] = ACCOUNT_KEY_PREFIX;
        prefs().edit().putString(accountName, Base64.encodeToString(key, Base64.NO_WRAP)).apply();
        return new AccountKeyRecord(accountName, key);
    }
}