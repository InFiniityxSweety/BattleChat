package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabRefreshDisplayMessages
import com.ebicep.chatplus.features.chattabs.ChatTabRewrapDisplayMessages
import com.ebicep.chatplus.features.chatwindows.ChatTabSwitchEvent
import com.ebicep.chatplus.features.chatwindows.WindowSwitchEvent
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.FindTextBarElement
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TranslateToggleEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.util.ComponentUtil
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import java.awt.Color

object FindMessage {

    val FIND_COLOR = Color(255, 255, 85, 255).rgb
    private val FIND_BACKGROUND_COLOR = Color(FIND_COLOR).darker().rgb
    var findMode: FindMode = FindMode.OFF
    val findEnabled: Boolean
        get() = findMode != FindMode.OFF
    private var lastInput = ""
    private var lastInputRegex = Regex("")

    init {
        var lastMovedToMessage: Pair<Pair<ChatTab.ChatPlusGuiMessage, Int>, Long>? = null // <linked message, wrapped index>, tick
        EventBus.register<AddTextBarElementEvent>({ 100 }) {
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            if (Config.values.findMessageTextBarElementEnabled) {
                it.elements.add(FindTextBarElement(it.screen))
            }
        }
        var findShortcutUsed = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 1 }, { findShortcutUsed }) {
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            findShortcutUsed = Config.values.findMessageKey.isDown()
            if (findShortcutUsed) {
                toggle(it.screen)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (findEnabled) {
                findMode = FindMode.OFF
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatTabRewrapDisplayMessages> {
            findMode = FindMode.OFF
            ChatManager.globalSelectedTab.resetFilter()
        }
        EventBus.register<ChatTabRefreshDisplayMessages> {
            if (findEnabled && lastInput.isNotEmpty()) {
                if (findMode == FindMode.REGEX) {
                    try {
                        lastInputRegex = Regex(lastInput)
                    } catch (e: Exception) {
                        ChatPlus.sendMessage(ComponentUtil.literal("Invalid Regex", ChatFormatting.RED))
                        return@register
                    }
                }
                it.predicates.add { guiMessage ->
                    guiMessage.guiMessage.content.string.contains(lastInput, ignoreCase = true)
                }
            }
        }
        // when switching tabs/windows, refresh filter
        EventBus.register<ChatTabSwitchEvent> {
            if (findEnabled) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<WindowSwitchEvent> {
            if (findEnabled) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            val screen = Minecraft.getInstance().screen
            if (findEnabled && screen is IMixinChatScreen) {
                it.filtered = true
                val filter = screen.input?.value
                if (filter != null && !it.component.string.contains(filter, ignoreCase = true)) {
                    it.addMessage = false
                }
            }
        }
        EventBus.register<ChatScreenInputBoxEditEvent> {
            if (findEnabled) {
                lastInput = it.str
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (findEnabled && Config.values.findMessageHighlightInputBox) {
                it.screen as IMixinChatScreen
                val editBox = it.screen.input ?: return@register
                it.guiGraphics.renderOutline(
                    editBox.x - 2,
                    editBox.y - 5,
                    editBox.width - 1,
                    editBox.height,
                    FIND_COLOR
                )
            }
        }
        EventBus.register<TranslateToggleEvent> {
            if (findEnabled) {
                findMode = FindMode.OFF
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (findEnabled) {
                ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                    val linkedMessage = message.linkedMessage
                    lastMovedToMessage = Pair(Pair(linkedMessage, message.wrappedIndex), Events.currentTick + 60)
                    findMode = FindMode.OFF
                    ChatManager.globalSelectedTab.moveToMessage(it.screen, message)
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.findMessageLinePriority }) {
            lastMovedToMessage?.let { message ->
                if (message.first.first !== it.chatPlusGuiMessageLine.linkedMessage ||
                    message.first.second != it.chatPlusGuiMessageLine.wrappedIndex
                ) {
                    return@let
                }
                if (message.second < Events.currentTick) {
                    return@let
                }
                it.backgroundColor = FIND_BACKGROUND_COLOR
            }
        }
        EventBus.register<ChatRenderLineTextEvent>({ -5 }) {
            if (findEnabled && it.chatWindow.tabSettings.selectedTab.wasFiltered) {
                val chatPlusGuiMessageLine = it.chatPlusGuiMessageLine
                val line = it.line
                val guiGraphics = it.guiGraphics
                val verticalChatOffset = it.verticalChatOffset
                val renderer = it.chatWindow.renderer
                val internalX = renderer.internalX
                val scale = renderer.scale
                val lineHeight = renderer.lineHeight

                val mode = if (findMode == FindMode.DEFAULT) {
                    FindMode.REGEX//Config.values.findMessageDefaultMode
                } else {
                    findMode
                }

                val ranges =
                    if (mode == FindMode.REGEX) ComponentUtil.getWidthRanges(line.content, chatPlusGuiMessageLine.content, lastInputRegex)
                    else ComponentUtil.getWidthRange(line.content, chatPlusGuiMessageLine.content, lastInput)
                ranges.forEach {
                    guiGraphics.fill0(
                        internalX / scale + it.first,
                        verticalChatOffset - lineHeight.toFloat(),
                        internalX / scale + it.second,
                        verticalChatOffset,
                        Color(255, 255, 85, 200).rgb
                    )
                }

            }
        }
    }

    fun toggle(chatPlusScreen: ChatScreen, newFindMode: FindMode = FindMode.DEFAULT) {
        chatPlusScreen as IMixinChatScreen
        findMode = if (findEnabled) {
            FindMode.OFF
        } else {
            newFindMode
        }
        EventBus.post(FindToggleEvent(findEnabled))
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        lastInput = chatPlusScreen.input?.value ?: ""
        if (!findEnabled) {
            ChatManager.globalSelectedTab.resetFilter()
        } else {
            ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
        }
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callRebuildWidgets()
    }

    enum class FindMode {
        OFF,
        DEFAULT,
        REGEX,
        CONTAINS,
    }

}