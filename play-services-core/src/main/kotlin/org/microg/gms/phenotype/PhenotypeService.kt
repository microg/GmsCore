/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.*
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PhenotypeService"

class PhenotypeService : BaseService(TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        callback.onPostInitComplete(0, PhenotypeServiceImpl(packageName).asBinder(), null)
    }
}

private val CONFIGURATION_OPTIONS = mapOf(
    "com.google.android.apps.search.assistant.mobile.user#com.google.android.googlequicksearchbox" to arrayOf(
        // Enable Gemini voice input for all devices
        Flag("45477527", true, 0),
        // Required for the dictation mic to actually start listening
        Flag("45722849", true, 0),
        // Enable Gemini AI chat auto-reply
        Flag("45628155", false, 0),
        Flag("45627469", true, 0),
        Flag("45627893", byteArrayOf(0x0A, 0x01, 0x2A), 0),
        Flag("45622089", true, 0),
        // Enable Gemini optional models
        Flag("45681308", true, 0),
        Flag("45688209", true, 0),
        Flag("45682144", true, 0),
        // Enable Gemini sharing and video features
        Flag("45638955", true, 0),
        Flag("45621205", true, 0),
        Flag("45616812", true, 0),
        // Enable Gemini NotebookLM in AttachmentMenuConfig
        Flag("45715239", true, 0),
        Flag("45713945", true, 0),
        // Enable Gemini related third-party applications
        Flag("45685960", true, 0),
        // Enable Gemini temporary chat and more menus.
        Flag("45737809", true, 0),
        Flag("45752052", true, 0),
        Flag("45762487", true, 0),
        Flag("45726191", true, 0),
        // Enable Gemini show Usage-limits
        Flag("45778691", true, 0),
        // Enable Gemini New Style
        Flag("45780401", true, 0), // make compose a persistent chat (drops temp-chat + account disc)
        Flag("45770038", true, 0), // top-bar model selector ("Gemini Flash" title)
        Flag("45774691", true, 0), // zero-state sparkle logo + greeting
        Flag("45781316", true, 0), // blue gradient background + new greeting style
        Flag("45723633", true, 0), // extend gradient to the bottom edge
        Flag("45743658", true, 0), // collapsed pill input: remove inline tools button
        Flag("45745989", true, 0), // collapsed pill input: single row
        Flag("45763680", "jac5gepd2wadh1mr9uui23r8hvf7soldfu9ncmt9sue1w8lj0lu8bnuf2h6d1rdn", 0), // new Gemini Live mode UI (light theme + center orb)
        Flag("45758814", true, 0), // Live entry-button icon (both required): makes the circle blue
        Flag("45772906", "ezfRlSUMzcT7TcF9pFvy4Pm1IgqzmfZGZEuJq4QGEgT3sJHCSk", 0), // keeps bars dark + drops the sparkle
        Flag("45777986", true, 0), // left drawer redesign: "Gemini" title + search row + account/settings disc at bottom
        Flag("45759951", true, 0), // left drawer: Notebooks section
        Flag("45774239", true, 0), // left drawer: "Content library" header (was "My content")
        Flag("45766823", true, 0), // account menu (bento): "Import memories" entry
        Flag("45375886", true, 0), // account menu (bento): "Help" entry
        Flag("45776451", "WAK9nI6e9uEntjWtz4HStoX8fuYlmugZVV97CJUmYCM6GtfKa1", 0), // model selector: anchored dropdown popup + "new" badge (paired with 45774744)
        Flag("45774744", true, 0), // title "Gemini Flash" (hides 3.5) + "Thinking level" row in dropdown
        Flag("45758693", true, 0), // attachment (+) sheet: two-line items with subtitles
        Flag("45772905", "PuPafs5Q7a6JqfuciluzqwUuVdymNGC3VXVdZtA2h0ZEfOAjV8", 0), // attachment (+) sheet: "Google Drive" source
        Flag("45732923", true, 0), // button: mic icon switches to stop/pause while listening
        Flag("45739782", true, 0), // waveform view
        Flag("45739784", "XPA2wPrzZu8z4UNM6cSMljkKkTy83/D99tlWfQ==", 0), // waveform view
        Flag("45760223", 30.0, 0), // waveform length
        Flag("45760225", 40.0, 0), // waveform length
        Flag("45760897", 4L, 0), // fixed (non-scrolling) bars
        Flag("45766415", 1.5, 0), // thin bars
        Flag("45766419", 8.0, 0), // bar animation
        Flag("45772982", 0L, 0), // pure black render (without it the bars are blue/gray)
        Flag("45750622", true, 0), // Fix "Conversation requires an update"
    ),
    "com.google.android.libraries.search.googleapp.user#com.google.android.googlequicksearchbox" to arrayOf(
        // Allow the "Saved" tab in the Google app to appear as "Activity."
        Flag("45644218", true, 0),
        Flag("45691184", true, 0)
    ),
    "com.google.android.inputmethod.latin#com.google.android.inputmethod.latin" to arrayOf(
        // Enable Gboard supports voice input in other languages
        Flag("enable_voice_in_chinese", true, 0),
        Flag("enable_voice_in_japanese", true, 0),
        Flag("enable_voice_in_korean", true, 0),
        Flag("enable_voice_in_handwriting", true, 0),
    ),
    "com.google.android.libraries.communications.conference.device#com.google.android.apps.tachyon" to arrayOf(
        // Enable Google Meet calling using mobile phone number
        Flag("45428442", true, 0),
        Flag("45620216", true, 0),
        // Start a Video Call
        Flag("45692241", true, 0),
        Flag("45680977", true, 0),
        Flag("45620220", true, 0),
        // Show Audio Call Button
        Flag("45613814", true, 0),
        // Show Group Call Button
        Flag("45620498", true, 0),
    ),
    "com.google.apps_mobile.common.services.gmail.android#com.google.android.gm" to arrayOf(
        Flag("45661535", encodeSupportedLanguageList(), 0),
        Flag("45700179", encodeSupportedLanguageList(), 0)
    ),
    "gmail_android.user#com.google.android.gm" to arrayOf(
        Flag("45624002", true, 0),
        Flag("45668769", true, 0),
        Flag("45633067", true, 0),
    ),
    "com.google.android.apps.photos" to arrayOf(
        Flag("45617431", true, 0),
        // Enable v3 movie editor; legacy MovieEditorActivity NPEs on a missing media extra
        Flag("45410021", true, 0),
        // Route highlight-video to the Varenyky local renderer
        Flag("45735699", true, 0),
        Flag("45709528", true, 0),
    ),
    // Android Auto (Gearhead) — lift Car App Library API max for third-party apps.
    // Without this, AA falls back to baked-in default "DEFAULT:7, gearhead:8" which
    // blocks Spotify / YouTube Music (require minCarAppApiLevel=8).
    "com.google.android.projection.gearhead" to arrayOf(
        Flag("Watevra__host_max_api_level", encodeRepeatedString(listOf("DEFAULT:8")), 0),
    ),
    "com.google.android.apps.translate" to arrayOf(
        // Enable Practice Tab
        Flag("SpeakEasy__enabled", true, 0),
        // Enable Layout adjustment
        Flag("OpenMic__enable_open_mic_2_august_launch_ui", true, 0),
        // Enable Handwriting Improvements
        Flag("HandwritingImprovements__enable_handwriting_improvements", true, 0),
    ),
    "com.google.labs.language.tailwind.mobile#com.google.android.apps.labs.language.tailwind" to arrayOf(
        // Enable NotebookLM video overview
        Flag("45741830", true, 0),
        // Enable notebook-guide (zero-state) follow-up chips
        Flag("45756972", true, 0),
        Flag("45767607", true, 0),
    ),
    "com.google.android.apps.messaging#com.google.android.apps.messaging" to arrayOf(
        Flag("bugle_phenotype__enable_penpal_conversation", true, 0),
        Flag("bugle_phenotype__bug_325090692_enable_penpal_dasher_check", false, 0),
    ),
    "com.google.android.ims.library" to arrayOf(
        Flag("RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met", true, 0)
    ),
    "com.google.android.apps.messaging#com.google.android.apps.messaging" to arrayOf(
        Flag("bugle_phenotype__enableDownloadCertificatesForMlsProvisioning", true, 0),
        Flag("bugle_phenotype__enableUploadKeyPackagesForMlsProvisioning", true, 0),
    )
)

class PhenotypeServiceImpl(val packageName: String?) : IPhenotypeService.Stub() {
    override fun register(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?) {
        Log.d(TAG, "register($packageName, $version, $p3, $p4)")
        callbacks.onRegistered(if (version != 0) Status.SUCCESS else Status.CANCELED)
    }

    override fun weakRegister(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: IntArray?, p5: ByteArray?) {
        Log.d(TAG, "weakRegister($packageName, $version, $p3, $p4, $p5)")
        callbacks.onWeakRegistered(Status.SUCCESS)
    }

    override fun unregister(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "unregister($packageName)")
        callbacks.onUnregistered(Status.SUCCESS)
    }

    override fun getConfigurationSnapshot(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        getConfigurationSnapshotWithToken(callbacks, packageName, user, null)
    }

    override fun commitToConfiguration(callbacks: IPhenotypeCallbacks, snapshotToken: String?) {
        Log.d(TAG, "commitToConfiguration($snapshotToken)")
        callbacks.onCommitedToConfiguration(Status.SUCCESS)
    }

    override fun getExperimentTokens(callbacks: IPhenotypeCallbacks, packageName: String?, logSourceName: String?) {
        getExperimentTokensForLogging(callbacks, packageName, logSourceName, null, this.packageName)
    }

    override fun getDogfoodsToken(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getDogfoodsToken()")
        callbacks.onDogfoodsToken(Status.SUCCESS, DogfoodsToken())
    }

    override fun setDogfoodsToken(callbacks: IPhenotypeCallbacks, p1: ByteArray?) {
        Log.d(TAG, "setDogfoodsToken($p1)")
        callbacks.onDogfoodsTokenSet(Status.SUCCESS)
    }

    override fun getFlag(callbacks: IPhenotypeCallbacks, packageName: String?, name: String?, type: Int) {
        Log.d(TAG, "getFlag($packageName, $name, $type)")
        callbacks.onFlag(Status.SUCCESS, null)
    }

    override fun getCommitedConfiguration(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "getCommitedConfiguration($packageName)")
        callbacks.onCommittedConfiguration(Status.SUCCESS, configurationsResult())
    }

    override fun getConfigurationSnapshotWithToken(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?) {
        Log.d(TAG, "getConfigurationSnapshotWithToken($packageName, $user, $p3)")
        if (packageName in CONFIGURATION_OPTIONS.keys) {
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult(arrayOf(Configuration().apply {
                id = 0
                flags = CONFIGURATION_OPTIONS[packageName]
                removeNames = emptyArray()
            })))
        } else {
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult())
        }
    }

    override fun syncAfterOperation(callbacks: IPhenotypeCallbacks, packageName: String?, version: Long) {
        Log.d(TAG, "syncAfterOperation($packageName, $version)")
        callbacks.onSyncFinished(Status.SUCCESS, version)
    }

    override fun registerSync(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?, p5: String?, p6: String?) {
        Log.d(TAG, "registerSync($packageName, $version, $p3, $p4, $p5, $p6)")
        callbacks.onConfiguration(Status.SUCCESS, configurationsResult())
    }

    override fun setFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?, flagType: Int, flagDataType: Int, flagValue: String?) {
        Log.d(TAG, "setFlagOverrides($packageName, $user, $flagName, $flagDataType, $flagType, $flagDataType, $flagValue)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun deleteFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "deleteFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun listFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "listFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun clearFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        Log.d(TAG, "clearFlagOverrides($packageName, $user)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun bulkRegister(callbacks: IPhenotypeCallbacks, infos: Array<out RegistrationInfo>?) {
        Log.d(TAG, "bulkRegister($infos)")
        callbacks.onRegistered(Status.SUCCESS)
    }

    override fun setAppSpecificProperties(callbacks: IPhenotypeCallbacks, packageName: String?, p2: ByteArray?) {
        Log.d(TAG, "setAppSpecificProperties($packageName, $p2)")
        callbacks.onAppSpecificPropertiesSet(Status.SUCCESS)
    }

    override fun getServingVersion(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getServingVersion()")
        callbacks.onServingVersion(Status.SUCCESS, 1)
    }

    override fun getExperimentTokensForLogging(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?, clientPackageName: String?) {
        Log.d(TAG, "getExperimentTokens($packageName, $user, $p3, $clientPackageName)")
        callbacks.onExperimentTokens(Status.SUCCESS, ExperimentTokens().apply {
            field2 = ""
        })
    }

    override fun syncAllAfterOperation(callbacks: IPhenotypeCallbacks?, p1: Long) {
        Log.d(TAG, "Not yet implemented: syncAllAfterOperation")
    }

    override fun setRuntimeProperties(callbacks: IStatusCallback?, p1: String?, p2: ByteArray?) {
        Log.d(TAG, "Not yet implemented: setRuntimeProperties")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
