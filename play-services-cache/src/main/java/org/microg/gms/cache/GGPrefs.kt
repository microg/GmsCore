package org.microg.gms.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID.randomUUID

private const val PROPERTIES_KEY = "gg_properties"
private const val ADVERTISING_ID_KEY = "advertising_id"

val Context.dataStore by preferencesDataStore(PROPERTIES_KEY)

object GGPrefs {

    private val EMPTY_AD_ID = randomUUID().toString() //"00000000-0000-0000-0000-000000000000"
    private val ADVERTISING_PREF_KEY = stringPreferencesKey(ADVERTISING_ID_KEY)
    private val dispatcher = Dispatchers.Main
    private val mainScope by lazy { CoroutineScope(dispatcher + SupervisorJob()) }


    fun advertisingId(context: Context): Flow<String> = context.dataStore.data
            .map { preferences ->
                // No type safety.
                preferences[ADVERTISING_PREF_KEY] ?: EMPTY_AD_ID
            }

    fun advertisingIdStateFlow(context: Context) = advertisingId(context).stateIn(mainScope, SharingStarted.Eagerly, EMPTY_AD_ID)

    fun setAdvertisingId(context: Context, value: String) {
        mainScope.launch {
            context.dataStore.edit { settings ->
                settings[ADVERTISING_PREF_KEY] = value
            }
        }
    }
}