/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import com.google.android.gms.phenotype.Configuration
import com.google.android.gms.phenotype.Configurations
import com.google.android.gms.phenotype.Flag

internal object LegacyConfigurationOptions {
    private val supportedLanguages = listOf(
        "bs", "pt", "ja", "ko", "fr", "it", "de", "zh", "nl",
        "iw", "he", "tr", "cs", "id", "in", "sv", "da", "no",
        "nb", "pl", "vi", "th", "fi", "uk", "ar", "el", "ru",
        "hu", "ro", "ca"
    )

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
            Flag("45763529", true, 0), // Make Gemini Live first-use consent open immediately from the Live button
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
            Flag("45661535", encodeRepeatedString(supportedLanguages), 0),
            Flag("45700179", encodeRepeatedString(supportedLanguages), 0)
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
            // Enable the RecyclerView language picker and allow languages to be pinned by swiping.
            Flag("LanguagePickerRecyclerView__language_picker_recycler_view", true, 0),
            Flag("LanguagePickerRecyclerView__enable_pinned_languages", true, 0),
            Flag("LanguagePickerRecyclerView__enable_swipe_onboarding", true, 0),
            // Expose the full-screen action in translated-result cards.
            Flag("FullScreenTranslation__enable_full_screen_translation", true, 0),
            // Enable Practice Tab
            Flag("SpeakEasy__enabled", true, 0),
            // Enable Layout adjustment
            Flag("OpenMic__enable_open_mic_2_august_launch_ui", true, 0),
            // Keep the legacy settings controls, including the auto-play switch.
            // The device picker replaces and hides that switch in recent Translate versions.
            Flag("OpenMic__enable_device_picker_ui", false, 0),
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
        "com.google.android.videos" to arrayOf(
            Flag("AppRefreshFeature__enable_entity_page_trailer_autoplay", true, 0),
        ),
    )

    private val namesByPackage = CONFIGURATION_OPTIONS.mapValues { (_, flags) -> flags.mapTo(hashSetOf()) { it.name } }

    fun isPreset(packageName: String, flag: Flag): Boolean =
        flag.flagType == 0 && flag.name in namesByPackage[packageName].orEmpty()

    fun mergeFlags(packageName: String, flags: List<Flag>): List<Flag> {
        val legacyFlags = CONFIGURATION_OPTIONS[packageName] ?: return flags
        val legacyNames = namesByPackage.getValue(packageName)
        return flags.filterNot { it.flagType == 0 && it.name in legacyNames } + legacyFlags
    }

    fun apply(packageName: String, configurations: Configurations): Configurations {
        val legacyNames = namesByPackage[packageName] ?: return configurations
        val values = configurations.field4.orEmpty().toMutableList()
        val index = values.indexOfFirst { it.id == 0 }
        val current = values.getOrNull(index)
        val merged = mergeFlags(packageName, current?.flags.orEmpty().toList())
        val replacement = Configuration().apply {
            id = 0
            flags = merged.toTypedArray()
            removeNames = current?.removeNames.orEmpty().filterNot { it in legacyNames }.toTypedArray()
        }
        if (index == -1) values.add(replacement) else values[index] = replacement
        configurations.field4 = values.toTypedArray()
        return configurations
    }

    private fun encodeRepeatedString(entries: List<String>): ByteArray {
        val out = mutableListOf<Byte>()
        for (entry in entries) {
            val bytes = entry.toByteArray(Charsets.UTF_8)
            out.add(0x0A.toByte())
            var size = bytes.size
            while (size >= 0x80) {
                out.add(((size and 0x7F) or 0x80).toByte())
                size = size ushr 7
            }
            out.add(size.toByte())
            out.addAll(bytes.toList())
        }
        return out.toByteArray()
    }
}
