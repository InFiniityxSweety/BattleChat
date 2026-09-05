package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TranslateSpeakTextBarElement
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_DISPLAY_HEIGHT
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.translator.*
import com.ebicep.chatplus.util.ComponentUtil
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.client.ClientRawInputEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent


object TranslateMessage {

    val TRANSLATE_PREFIX_INPUT_WIDTH: Int
        get() = if (values.vanillaInputBox) 63 else 65
    var languageSpeakEnabled = false
    var inputTranslatePrefix: EditBox? = null

    init {
        EventBus.register<AddTextBarElementEvent>({ 0 }) {
            if (!values.translatorEnabled) {
                return@register
            }
            if (!values.translatorTextBarElementEnabled) {
                return@register
            }
            val textBarElement = TranslateSpeakTextBarElement(it.screen)
            textBarElement.init()
            it.elements.add(textBarElement)
        }
        EventBus.register<ChatScreenInitPostEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            val screen = it.screen

            inputTranslatePrefix = null
            if (languageSpeakEnabled) {
                val inputBoxSettings = values.inputBoxSettings
                screen as IMixinChatScreen
                screen.input?.x = TRANSLATE_PREFIX_INPUT_WIDTH + (if (values.vanillaInputBox) 5 else inputBoxSettings.startX)
                screen.input.width -= TRANSLATE_PREFIX_INPUT_WIDTH
                inputTranslatePrefix = EditBox(
                    screen.minecraft!!.fontFilterFishy,
                    if (values.vanillaInputBox) 4 else inputBoxSettings.startX,
                    if (values.vanillaInputBox) screen.height - EDIT_BOX_HEIGHT + 2 else inputBoxSettings.getCalculatedStartY(),
                    TRANSLATE_PREFIX_INPUT_WIDTH,
                    EDIT_BOX_DISPLAY_HEIGHT,
                    Component.translatable("chatPlus.editBox")
                )
                val editBox = inputTranslatePrefix as EditBox
                editBox.setMaxLength(inputBoxSettings.maxInputBoxInputLength)
                editBox.isBordered = false
                editBox.setCanLoseFocus(true)
                screen as IMixinScreen
                screen.callAddWidget(editBox)
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled && !values.translateKeepOnAfterChatClose) {
                languageSpeakEnabled = false
            }
        }
        EventBus.register<FindToggleEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled) {
                languageSpeakEnabled = false
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled) {
                it.returnFunction = inputTranslatePrefix != null &&
                        inputTranslatePrefix!!.isFocused &&
                        inputTranslatePrefix!!.mouseClicked(it.mouseButtonEvent, false)
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            if (inputTranslatePrefix == null) {
                return@register
            }
            val screen = it.screen
            val guiGraphics = it.guiGraphics
            val height = screen.height
            val minecraft = screen.minecraft!!
            val inputBoxSettings = values.inputBoxSettings
            val startX = if (values.vanillaInputBox) 2 else inputBoxSettings.startX - 2
            val startY = if (values.vanillaInputBox) height - EDIT_BOX_HEIGHT else inputBoxSettings.getCalculatedStartY() - MovableChat.InputBoxSettings.INPUT_BOX_PADDING
            guiGraphics.fill(
                startX,
                startY,
                startX + TRANSLATE_PREFIX_INPUT_WIDTH,
                if (values.vanillaInputBox) height - 2 else inputBoxSettings.getCalculatedStartY() + MovableChat.InputBoxSettings.PADDED_INPUT_BOX_HEIGHT,
                minecraft.options.getBackgroundColor(Int.MIN_VALUE)
            )
            guiGraphics.outline(
                startX,
                startY,
                TRANSLATE_PREFIX_INPUT_WIDTH,
                EDIT_BOX_DISPLAY_HEIGHT,
                0xFF55FF55.toInt()
            )
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            inputTranslatePrefix!!.extractRenderState(guiGraphics, mouseX, mouseY, it.partialTick)
            if (
                mouseX in startX until startX + TRANSLATE_PREFIX_INPUT_WIDTH &&
                mouseY in startY until startY + EDIT_BOX_HEIGHT
            ) {
                guiGraphics.setTooltipForNextFrame(
                    minecraft.font,
                    Component.translatable("chatPlus.translator.translateSpeakPrefix.tooltip"),
                    mouseX,
                    mouseY
                )
            }
        }
        EventBus.register<ChatScreenSendMessagePostEvent> {
            if (!values.translatorEnabled) {
                return@register
            }
            if (!languageSpeakEnabled) {
                return@register
            }
            // Commands must always pass through untouched, even while the
            // outgoing translation toggle is enabled.
            if (it.normalizeChatMessage.trimStart().startsWith("/")) {
                return@register
            }
            it.dontSendMessage = true
            SelfTranslator(it.normalizeChatMessage, if (inputTranslatePrefix == null) "" else inputTranslatePrefix!!.value).start()
        }
        EventBus.register<AddNewMessageEvent>({ -10 }) {
            if (!isTranslateMessage(it.rawComponent)) {
                handleTranslate(it.rawComponent)
            }
        }
        ClientRawInputEvent.KEY_PRESSED.register { minecraft, _, keyEvent ->
            if (ChatManager.isChatFocused()) {
                return@register EventResult.pass()
            }
            if (keyEvent.key() != values.translateKey.key.value || keyEvent.modifiers() != values.translateKey.modifier.toInt()) {
                return@register EventResult.pass()
            }
            if (minecraft.gui.screen() != null) {
                return@register EventResult.pass()
            }
            languageSpeakEnabled = true
            minecraft.gui.openChatScreen(ChatComponent.ChatMethod.MESSAGE)
            EventResult.interruptTrue()
        }
        EventBus.register<ChatScreenInputEvent> {
            if (it.checkRelease(values.translateToggleKey)) {
                return@register
            }
            TranslateSpeakTextBarElement.toggleTranslateSpeak(it.screen)
        }
        var translateClickCooldown = 0L
        EventBus.register<ChatScreenMouseClickedEvent>({ 100 }) { event ->
            if (!values.translatorEnabled || !values.translateClickEnabled) {
                return@register
            }
            if (event.button != 0 || !Minecraft.getInstance().hasControlDown()) {
                return@register
            }
            if (System.currentTimeMillis() - translateClickCooldown < 1_000) {
                return@register
            }
            ChatManager.globalSelectedTab.getHoveredOverMessageLine(event.mouseX, event.mouseY)?.let { message ->
                val selectedMessages = SelectChat.getAllSelectedMessages()
                val messages: List<ChatTab.ChatPlusGuiMessage> = if (selectedMessages.contains(message)) {
                    SelectChat.getSelectedMessagesOrdered().map { it.linkedMessage }
                } else {
                    listOf(message.linkedMessage)
                }

                // Incoming translation is always source=auto. Auto Detect is not a
                // meaningful target language, so fall back to German for BattleChat.
                val target = LanguageManager.languageTo
                    ?.takeUnless { language -> language.googleCode == "auto" }
                    ?: LanguageManager.findLanguageFromName("German")

                if (target == null) {
                    ChatPlus.sendMessage(
                        Component.literal("No valid target language is configured for incoming translation.")
                            .withStyle(ChatFormatting.RED)
                    )
                    return@let
                }

                translateClickCooldown = System.currentTimeMillis()
                event.returnFunction = true
                ClickTranslator(messages, target).start()
            }
        }
    }

    private fun isTranslateMessage(component: Component): Boolean {
        return component.contents is ComponentUtil.LiteralContentsIgnored &&
                (component.contents as ComponentUtil.LiteralContentsIgnored).isType(ComponentUtil.LiteralIgnoredType.TRANSLATE)
    }

    private fun handleTranslate(component: Component) {
        if (!values.translatorEnabled) {
            return
        }
        LanguageManager.languageTo?.let {
            Translator(ChatFormatting.stripFormatting(component.string)!!, LanguageManager.translateFrom, it).start()
        }
    }

    /**
     * Manual Ctrl+click translation.
     *
     * Each selected message is translated independently. The old ChatPlus implementation
     * concatenated selected lines with a section-sign delimiter and expected the provider
     * to preserve that delimiter, which is fragile and can make otherwise valid requests fail.
     */
    class ClickTranslator(
        private val lines: List<ChatTab.ChatPlusGuiMessage>,
        private val to: Language,
    ) : Thread() {

        override fun run() {
            var failures = 0

            lines.forEach { line ->
                val original = ChatFormatting.stripFormatting(line.guiMessage.content.string)?.trim().orEmpty()
                if (original.isBlank()) {
                    return@forEach
                }

                val translated = TranslationManager.translate(original, null, to)
                if (translated == null) {
                    failures++
                    return@forEach
                }

                if (translated.translatedText.trim().equals(original, ignoreCase = true)) {
                    Minecraft.getInstance().execute {
                        ChatPlus.sendMessage(
                            ComponentUtil.translatable(
                                "chatPlus.translator.sameMessage",
                                ChatFormatting.RED,
                                HoverEvent.ShowText(line.guiMessage.content.copy())
                            )
                        )
                    }
                    return@forEach
                }

                val fromLanguage = translated.from?.name ?: "Auto Detect"
                val component = ComponentUtil.literal(
                    translated.translatedText.trim() + " (" + fromLanguage + ")",
                    ChatFormatting.GREEN,
                    HoverEvent.ShowText(line.guiMessage.content.copy())
                )

                Minecraft.getInstance().execute {
                    ChatManager.globalSelectedTab.addNewMessage(
                        AddNewMessageEvent(
                            component.copy(),
                            component,
                            null,
                            null,
                            Minecraft.getInstance().gui.hud.guiTicks,
                            GuiMessageSource.PLAYER,
                            GuiMessageTag.system(),
                            false
                        )
                    )
                }
            }

            if (failures > 0) {
                Minecraft.getInstance().execute {
                    ChatPlus.sendMessage(
                        Component.literal(
                            if (failures == 1) {
                                "Translation failed. Please try again in a moment."
                            } else {
                                "$failures translations failed. Please try again in a moment."
                            }
                        ).withStyle(ChatFormatting.RED)
                    )
                }
            }
        }
    }

}
