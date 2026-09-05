package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.features.TextTransform
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class SelfTranslator(val toTranslate: String, val prefix: String) : Thread() {

    override fun run() {
        LanguageManager.languageSpeak?.let {
            val plainInput = TextTransform.normalizeForTranslation(toTranslate)
            val translator = Translator(plainInput, LanguageManager.languageSelf, it)
            val translateResult = translator.translate(plainInput)
            if (translateResult == null) {
                val details = TranslationManager.lastFailureSummary.take(180)
                Minecraft.getInstance().execute {
                    ChatPlus.sendMessage(
                        Component.literal(
                            buildString {
                                append("Could not translate your outgoing message. Nothing was sent.")
                                if (details.isNotBlank()) append(" [$details]")
                            }
                        ).withStyle(ChatFormatting.RED)
                    )
                }
                return
            }
            val pre = if (prefix.isEmpty()) {
                ""
            } else if (prefix.startsWith("/")) {
                prefix.trim()
            } else {
                "$prefix "
            }
            val text = pre + TextTransform.transformForSend(translateResult.translatedText)
            Minecraft.getInstance().execute {
                ChatPlusScreen.sendChatMessage(message = text)
            }
        }
    }
}
