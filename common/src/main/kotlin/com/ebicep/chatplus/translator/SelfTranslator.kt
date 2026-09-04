package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class SelfTranslator(val toTranslate: String, val prefix: String) : Thread() {

    override fun run() {
        LanguageManager.languageSpeak?.let {
            val translator = Translator(toTranslate, LanguageManager.languageSelf, it)
            val translateResult = translator.translate(toTranslate)
            if (translateResult == null) {
                ChatPlus.sendMessage(
                    Component.literal("Could not translate your outgoing message. Nothing was sent.")
                        .withStyle(ChatFormatting.RED)
                )
                return
            }
            val pre = if (prefix.isEmpty()) {
                ""
            } else if (prefix.startsWith("/")) {
                prefix.trim()
            } else {
                "$prefix "
            }
            val text = pre + translateResult.translatedText
            ChatPlusScreen.sendChatMessage(message = text)
        }
    }

}
