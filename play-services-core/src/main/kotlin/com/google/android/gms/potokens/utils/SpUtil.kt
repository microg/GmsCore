package com.google.android.gms.potokens.utils

import android.content.Context
import android.content.SharedPreferences

class SpUtil private constructor() {
    fun save(key: String?, value: Any?) {
        when (value) {
            is String -> {
                mSp!!.edit().putString(key, value).apply()
            }

            is Boolean -> {
                mSp!!.edit().putBoolean(key, value).apply()
            }

            is Int -> {
                mSp!!.edit().putInt(key, value).apply()
            }

            is Long -> {
                mSp!!.edit().putLong(key, value.toLong()).apply()
            }
        }
    }

    fun getString(key: String?, defValue: String?): String? {
        return mSp!!.getString(key, defValue)
    }

    fun getLong(key: String?, defValue: Long): Long {
        return mSp!!.getLong(key, defValue)
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return mSp!!.getBoolean(key, defValue)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return mSp!!.getInt(key, defValue)
    }

    companion object {
        private val instance = SpUtil()
        private var mSp: SharedPreferences? = null
        operator fun get(context: Context): SpUtil {
            if (mSp == null) {
                mSp = context.getSharedPreferences("com.google.android.gms.potokens", Context.MODE_PRIVATE)
            }
            return instance
        }
    }
}