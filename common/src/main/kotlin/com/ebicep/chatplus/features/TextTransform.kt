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
 * Human-language translation always works on normalized/plain text. When both
 * translation and a style are enabled, translation happens first and styling is
 * applied afterwards. The same normalizer is used for Ctrl+click translation of
 * incoming stylized text (including Enchantment/SGA).
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

    private val enchantment = mapOf(
        'a' to "ᔑ", 'b' to "ʖ", 'c' to "ᓵ", 'd' to "↸", 'e' to "ᒷ", 'f' to "⎓",
        'g' to "⊣", 'h' to "⍑", 'i' to "╎", 'j' to "⋮", 'k' to "ꖌ", 'l' to "ꖎ",
        'm' to "ᒲ", 'n' to "リ", 'o' to "𝙹", 'p' to "!¡", 'q' to "ᑑ", 'r' to "∷",
        's' to "ᓭ", 't' to "ℸ̣", 'u' to "⚍", 'v' to "⍊", 'w' to "∴", 'x' to "̇/",
        'y' to "||", 'z' to "⨅"
    )

    private val bold = alphabet(
        "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙",
        "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳",
        "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗"
    )
    private val italic = alphabet(
        "𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍",
        "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧"
    )
    private val boldItalic = alphabet(
        "𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁",
        "𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛"
    )
    private val monospace = alphabet(
        "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉",
        "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣",
        "𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿"
    )
    private val fraktur = alphabet(
        "𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ",
        "𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"
    )
    private val doubleStruck = alphabet(
        "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ",
        "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫",
        "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡"
    )
    private val script = alphabet(
        "𝒜ℬ𝒞𝒟ℰℱ𝒢ℋℐ𝒥𝒦ℒℳ𝒩𝒪𝒫𝒬ℛ𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵",
        "𝒶𝒷𝒸𝒹ℯ𝒻ℊ𝒽𝒾𝒿𝓀𝓁𝓂𝓃ℴ𝓅𝓆𝓇𝓈𝓉𝓊𝓋𝓌𝓍𝓎𝓏"
    )
    private val circled = alphabet(
        "ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ",
        "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ",
        "⓪①②③④⑤⑥⑦⑧⑨"
    )

    private val upsideDown = mapOf(
        'a' to "ɐ", 'b' to "q", 'c' to "ɔ", 'd' to "p", 'e' to "ǝ", 'f' to "ɟ",
        'g' to "ƃ", 'h' to "ɥ", 'i' to "ᴉ", 'j' to "ɾ", 'k' to "ʞ", 'l' to "l",
        'm' to "ɯ", 'n' to "u", 'o' to "o", 'p' to "d", 'q' to "b", 'r' to "ɹ",
        's' to "s", 't' to "ʇ", 'u' to "n", 'v' to "ʌ", 'w' to "ʍ", 'x' to "x",
        'y' to "ʎ", 'z' to "z", '0' to "0", '1' to "Ɩ", '2' to "ᄅ", '3' to "Ɛ",
        '4' to "ㄣ", '5' to "ϛ", '6' to "9", '7' to "ㄥ", '8' to "8", '9' to "6",
        '.' to "˙", ',' to "'", '?' to "¿", '!' to "¡", '(' to ")", ')' to "(",
        '[' to "]", ']' to "[", '{' to "}", '}' to "{", '<' to ">", '>' to "<"
    )

    private val reversibleSingleGlyphStyles: List<Map<Char, String>> = listOf(
        smallCaps, bold, italic, boldItalic, monospace, fraktur, doubleStruck, script, circled
    )

    private val singleGlyphReverse: Map<String, String> by lazy {
        buildMap {
            reversibleSingleGlyphStyles.forEach { style ->
                style.forEach { (plain, styled) -> put(styled, plain.toString()) }
            }
        }
    }

    private val upsideDownReverse: Map<String, String> by lazy {
        upsideDown.entries.associate { (plain, styled) -> styled to plain.toString() }
    }

    init {
        load()

        EventBus.register<AddTextBarElementEvent>({ -20 }) {
            it.elements.add(TextTransformTextBarElement(it.screen))
        }

        EventBus.register<ChatScreenSendMessagePreEvent>({ 100 }) {
            val text = it.message
            if (text.trimStart().startsWith("/")) return@register
            if (TranslateMessage.languageSpeakEnabled) return@register
            it.message = transformForSend(text)
        }
    }

    fun setMode(newMode: TextTransformMode) {
        if (mode == newMode) return
        mode = newMode
        save()
    }

    fun transformForSend(text: String): String {
        return when (mode) {
            TextTransformMode.NORMAL -> text
            TextTransformMode.ENCHANTMENT -> mapCharacters(text, enchantment)
            TextTransformMode.SMALL_CAPS -> mapCharacters(text, smallCaps)
            TextTransformMode.FULLWIDTH -> fullwidth(text)
            TextTransformMode.BOLD -> mapCharacters(text, bold, preserveCase = true)
            TextTransformMode.ITALIC -> mapCharacters(text, italic, preserveCase = true)
            TextTransformMode.BOLD_ITALIC -> mapCharacters(text, boldItalic, preserveCase = true)
            TextTransformMode.MONOSPACE -> mapCharacters(text, monospace, preserveCase = true)
            TextTransformMode.FRAKTUR -> mapCharacters(text, fraktur, preserveCase = true)
            TextTransformMode.DOUBLE_STRUCK -> mapCharacters(text, doubleStruck, preserveCase = true)
            TextTransformMode.SCRIPT -> mapCharacters(text, script, preserveCase = true)
            TextTransformMode.CIRCLED -> mapCharacters(text, circled, preserveCase = true)
            TextTransformMode.UPSIDE_DOWN -> upsideDown(text)
        }
    }

    /** Converts BattleChat-supported stylized Unicode back to readable text before translation. */
    fun normalizeForTranslation(input: String): String {
        var text = input

        if (looksUpsideDown(text)) {
            text = splitGlyphs(text).asReversed().joinToString("") { upsideDownReverse[it] ?: it }
        }

        if (enchantment.values.any { token -> token.any { it.code > 127 } && text.contains(token) }) {
            enchantment.entries.sortedByDescending { it.value.length }.forEach { (plain, styled) ->
                text = text.replace(styled, plain.toString())
            }
        }

        text = buildString {
            splitGlyphs(text).forEach { glyph -> append(singleGlyphReverse[glyph] ?: decodeFullwidthGlyph(glyph)) }
        }

        return text
    }

    private fun looksUpsideDown(text: String): Boolean {
        val glyphs = splitGlyphs(text)
        return glyphs.any { glyph -> glyph.any { it.code > 127 } && upsideDownReverse.containsKey(glyph) }
    }

    private fun decodeFullwidthGlyph(glyph: String): String {
        if (glyph.length != 1) return glyph
        val code = glyph[0].code
        return when {
            code == 0x3000 -> " "
            code in 0xFF01..0xFF5E -> (code - 0xFEE0).toChar().toString()
            else -> glyph
        }
    }

    private fun mapCharacters(text: String, mapping: Map<Char, String>, preserveCase: Boolean = false): String {
        return buildString {
            text.forEach { char ->
                val key = if (preserveCase) char else char.lowercaseChar()
                append(mapping[key] ?: mapping[char.lowercaseChar()] ?: char.toString())
            }
        }
    }

    private fun fullwidth(text: String): String {
        return buildString {
            text.forEach { char ->
                when {
                    char == ' ' -> append('　')
                    char.code in 33..126 -> append((char.code + 0xFEE0).toChar())
                    else -> append(char)
                }
            }
        }
    }

    private fun upsideDown(text: String): String {
        return buildString {
            text.reversed().forEach { char -> append(upsideDown[char.lowercaseChar()] ?: char.toString()) }
        }
    }

    private fun alphabet(upper: String, lower: String, digits: String? = null): Map<Char, String> {
        val upperGlyphs = splitGlyphs(upper)
        val lowerGlyphs = splitGlyphs(lower)
        return buildMap {
            ('A'..'Z').forEachIndexed { index, char -> put(char, upperGlyphs[index]) }
            ('a'..'z').forEachIndexed { index, char -> put(char, lowerGlyphs[index]) }
            digits?.let {
                val digitGlyphs = splitGlyphs(it)
                ('0'..'9').forEachIndexed { index, char -> put(char, digitGlyphs[index]) }
            }
        }
    }

    private fun splitGlyphs(text: String): List<String> {
        return text.codePoints().toArray().map { codePoint -> String(Character.toChars(codePoint)) }
    }

    private fun load() {
        val file = stateFile
        if (!file.exists()) return
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
    BOLD("Bold"),
    ITALIC("Italic"),
    BOLD_ITALIC("Bold Italic"),
    MONOSPACE("Monospace"),
    FRAKTUR("Fraktur"),
    DOUBLE_STRUCK("Double Struck"),
    SCRIPT("Script"),
    CIRCLED("Circled"),
    UPSIDE_DOWN("Upside Down"),
}
