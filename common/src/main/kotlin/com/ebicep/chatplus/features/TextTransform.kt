package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.configDirectoryPath
import com.ebicep.chatplus.config.json
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.TextTransformTextBarElement
import com.ebicep.chatplus.hud.ChatScreenSendMessagePreEvent
import kotlinx.serialization.Serializable
import java.io.File

/**
 * BattleChat outgoing text transformations.
 *
 * This is intentionally separate from human-language translation. Commands are
 * never transformed, and when send-translation is enabled the transformation is
 * applied only after translation so the translation provider always receives
 * normal readable text.
 */
object TextTransform {

    @Serializable
    private data class PersistedState(val mode: String = TextTransformMode.NORMAL.name)

    private val stateFile: File
        get() = File(configDirectoryPath, "text-style.json")

    var mode: TextTransformMode = TextTransformMode.NORMAL
        private set

    private val smallCaps = mapOf(
        'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ꜰ",
        'g' to "ɢ", 'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ",
        'm' to "ᴍ", 'n' to "ɴ", 'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ǫ", 'r' to "ʀ",
        's' to "ꜱ", 't' to "ᴛ", 'u' to "ᴜ", 'v' to "ᴠ", 'w' to "ᴡ", 'x' to "ˣ",
        'y' to "ʏ", 'z' to "ᴢ"
    )

    // Unicode transliteration commonly used to represent Minecraft's
    // Standard Galactic Alphabet in normal chat text. This is sendable text;
    // it does not rely on the client-only minecraft:alt font.
    private val enchantment = mapOf(
        'a' to "ᔑ", 'b' to "ʖ", 'c' to "ᓵ", 'd' to "↸", 'e' to "ᒷ", 'f' to "⎓",
        'g' to "⊣", 'h' to "⍑", 'i' to "╎", 'j' to "⋮", 'k' to "ꖌ", 'l' to "ꖎ",
        'm' to "ᒲ", 'n' to "リ", 'o' to "𝙹", 'p' to "!¡", 'q' to "ᑑ", 'r' to "∷",
        's' to "ᓭ", 't' to "ℸ̣", 'u' to "⚍", 'v' to "⍊", 'w' to "∴", 'x' to "̇/",
        'y' to "||", 'z' to "⨅"
    )

    init {
        load()

        EventBus.register<AddTextBarElementEvent>({ -20 }) {
            it.elements.add(TextTransformTextBarElement(it.screen))
        }

        // Run before ChatTabs applies an auto-prefix so only the user's message
        // body is transformed. Translation mode is handled by SelfTranslator.
        EventBus.register<ChatScreenSendMessagePreEvent>({ 100 }) {
            val text = it.message
            if (text.trimStart().startsWith("/")) {
                return@register
            }
            if (TranslateMessage.languageSpeakEnabled) {
                return@register
            }
            it.message = transformForSend(text)
        }
    }

    fun setMode(newMode: TextTransformMode) {
        if (mode == newMode) {
            return
        }
        mode = newMode
        save()
    }

    fun transformForSend(text: String): String {
        return when (mode) {
            TextTransformMode.NORMAL -> text
            TextTransformMode.ENCHANTMENT -> mapCharacters(text, enchantment)
            TextTransformMode.SMALL_CAPS -> mapCharacters(text, smallCaps)
            TextTransformMode.FULLWIDTH -> fullwidth(text)
        }
    }

    private fun mapCharacters(text: String, mapping: Map<Char, String>): String {
        return buildString {
            text.forEach { char ->
                val mapped = mapping[char.lowercaseChar()]
                append(mapped ?: char.toString())
            }
        }
    }

    private fun fullwidth(text: String): String {
        return buildString {
            text.forEach { char ->
                if (char.code in 33..126) {
                    append((char.code + 0xFEE0).toChar())
                } else {
                    append(char)
                }
            }
        }
    }

    private fun load() {
        val file = stateFile
        if (!file.exists()) {
            return
        }
        runCatching {
            val state = json.decodeFromString(PersistedState.serializer(), file.readText())
            mode = TextTransformMode.entries.firstOrNull { it.name == state.mode } ?: TextTransformMode.NORMAL
        }.onFailure {
            ChatPlus.LOGGER.warn("Could not load BattleChat text-style state; using Normal", it)
            mode = TextTransformMode.NORMAL
        }
    }

    private fun save() {
        runCatching {
            val file = stateFile
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(PersistedState.serializer(), PersistedState(mode.name)))
        }.onFailure {
            ChatPlus.LOGGER.warn("Could not save BattleChat text-style state", it)
        }
    }
}

enum class TextTransformMode(val displayName: String) {
    NORMAL("Normal"),
    ENCHANTMENT("Enchantment"),
    SMALL_CAPS("Small Caps"),
    FULLWIDTH("Fullwidth"),
}
