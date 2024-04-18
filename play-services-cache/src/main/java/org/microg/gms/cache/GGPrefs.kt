package org.microg.gms.cache

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val PROPERTIES_KEY = "gg_properties"
private const val ADVERTISING_ID_KEY = "advertising_id"

val Context.dataStore by preferencesDataStore(PROPERTIES_KEY)

object GGPrefs {
    private val TAG = "GGPrefs"
    private val EMPTY_AD_ID =
        "00000000-0000-0000-0000-000000000000"//randomUUID().toString() //"00000000-0000-0000-0000-000000000000"
    private val ADVERTISING_PREF_KEY = stringPreferencesKey(ADVERTISING_ID_KEY)
    private val dispatcher = Dispatchers.Main
    private val mainScope by lazy { CoroutineScope(dispatcher + SupervisorJob()) }


    fun getAdvertisingId(context: Context) =
        (runBlocking { context.dataStore.data.first() })[ADVERTISING_PREF_KEY] ?: EMPTY_AD_ID

    fun setAdvertisingId(context: Context, value: String) {
        mainScope.launch {
            context.dataStore.edit { settings ->
                Log.d(TAG, "setAdvertisingId $value")
                settings[ADVERTISING_PREF_KEY] = value
            }
        }
    }
}