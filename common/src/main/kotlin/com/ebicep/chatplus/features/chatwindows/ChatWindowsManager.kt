package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.MessageSourceMode
import com.ebicep.chatplus.features.chattabs.ServerChatTabSettings
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ChatComponent

object ChatWindowsManager {

    fun createDefaultWindow(): ChatWindow {
        return ChatWindow().also { window ->
            val playerSettings = ServerChatTabSettings("(?s).*").also {
                it.name = "Chat"
                it.messageSourceMode = MessageSourceMode.PLAYER
                it.priority = 100
                it.skipOthers = true
            }
            val serverSettings = ServerChatTabSettings("(?s).*").also {
                it.name = "Server"
                it.messageSourceMode = MessageSourceMode.SERVER
                it.priority = 0
            }

            window.tabSettings.hideTabs = false
            window.tabSettings.tabs = mutableListOf(
                ChatTab(mutableListOf(playerSettings)),
                ChatTab(mutableListOf(serverSettings)),
            )
        }
    }

    init {
        EventBus.register<ChatScreenMouseClickedEvent>({ 10000 }) {
            // check if mouse in inside widows starting from last
            val chatWindows = Config.values.chatWindows
            for (i in chatWindows.size - 1 downTo 0) {
                val chatWindow = chatWindows[i]
                if (chatWindow.generalSettings.disabled) {
                    continue
                }
                if (insideWindow(chatWindow, it.mouseX, it.mouseY)) {
                    selectWindow(chatWindow)
                    return@register
                }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent> {
            val chatWindow = it.chatWindow
            if (chatWindow.generalSettings.disabled) {
                return@register
            }
            val outline = chatWindow.outlineSettings
            if (!ChatManager.isChatFocused() && !(!ChatManager.isChatFocused() && outline.showWhenChatNotOpen)) {
                return@register
            }
            if (!outline.enabled) {
                return@register
            }
            val selectedTab = chatWindow.tabSettings.selectedTab
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                val outlineBoxType = outline.outlineBoxType
                val outlineTabType = outline.outlineTabType
                outlineBoxType.render(outlineTabType, guiGraphics, chatWindow, selectedTab, renderer)
                if (!chatWindow.tabSettings.hideTabs) {
                    outlineTabType.render(outlineBoxType, guiGraphics, chatWindow, selectedTab, renderer)
                }
            }
        }
        EventBus.register<GetMinYEvent> {
            val tabSettings = it.chatWindow.tabSettings
            if (tabSettings.hideTabs) {
                return@register
            }
            if (tabSettings.position == TabSettings.Position.TOP) {
                it.minY += CHAT_TAB_HEIGHT
            }
        }
        EventBus.register<GetMaxYEvent> {
            val tabSettings = it.chatWindow.tabSettings
            if (tabSettings.hideTabs) {
                return@register
            }
            if (tabSettings.position == TabSettings.Position.BOTTOM) {
                it.maxY -= CHAT_TAB_HEIGHT
            }
        }
        EventBus.register<GetDefaultYEvent> {
            val tabSettings = it.chatWindow.tabSettings
            if (tabSettings.hideTabs) {
                return@register
            }
            if (tabSettings.position == TabSettings.Position.BOTTOM) {
                it.y -= CHAT_TAB_HEIGHT
            }
        }
        EventBus.register<GetMaxHeightEvent> {
            val tabSettings = it.chatWindow.tabSettings
            if (tabSettings.hideTabs) {
                return@register
            }
            if (tabSettings.position == TabSettings.Position.TOP) {
                it.maxHeight -= CHAT_TAB_HEIGHT
            }
        }
    }

    private fun insideWindow(chatWindow: ChatWindow, x: Double, y: Double): Boolean {
        val renderer = chatWindow.renderer
        val startX = renderer.getUpdatedX()
        val endX = startX + renderer.getUpdatedWidthValue()
        var startY = renderer.getUpdatedY() - renderer.getTotalLineHeight(true)
        var endY = renderer.getUpdatedY()
        val tabSettings = chatWindow.tabSettings
        if (!tabSettings.hideTabs) {
            when (tabSettings.position) {
                TabSettings.Position.TOP -> startY -= CHAT_TAB_HEIGHT + CHAT_TAB_Y_OFFSET
                TabSettings.Position.BOTTOM -> endY += CHAT_TAB_HEIGHT + CHAT_TAB_Y_OFFSET
            }
        }
        return startX < x && x < endX && startY < y && y < endY
    }

    fun selectWindow(newWindow: ChatWindow) {
        val oldWindow = ChatManager.selectedWindow
        if (oldWindow == newWindow) {
            return
        }
        oldWindow.tabSettings.selectedTab.resetFilter()
        Config.values.chatWindows.remove(newWindow)
        Config.values.chatWindows.add(newWindow)
        EventBus.post(WindowSwitchEvent(oldWindow, newWindow))
    }

    fun renderAll(guiGraphics: GuiGraphicsExtractor, font: Font, guiTicks: Int, mouseX: Int, mouseY: Int, displayMode: ChatComponent.DisplayMode, changeCursorOnInsertions: Boolean) {
        EventBus.post(RenderWindowsPreEvent(guiGraphics))
        val chatGraphicsAccess = if (displayMode.foreground) {
            ChatComponent.DrawingFocusedGraphicsAccess(guiGraphics, font, mouseX, mouseY, changeCursorOnInsertions)
        } else {
            ChatComponent.DrawingBackgroundGraphicsAccess(guiGraphics)
        }
        Config.values.chatWindows.forEachIndexed { index, it ->
            if (it.generalSettings.disabled) {
                return@forEachIndexed
            }
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                it.renderer.render(it, guiGraphics, chatGraphicsAccess, guiTicks, displayMode)
            }
        }
        EventBus.post(RenderWindowsPostEvent(guiGraphics))
    }

}

data class WindowSwitchEvent(
    val oldWindow: ChatWindow,
    val newWindow: ChatWindow,
)

data class RenderWindowsPreEvent(val guiGraphics: GuiGraphicsExtractor)

data class RenderWindowsPostEvent(val guiGraphics: GuiGraphicsExtractor)
