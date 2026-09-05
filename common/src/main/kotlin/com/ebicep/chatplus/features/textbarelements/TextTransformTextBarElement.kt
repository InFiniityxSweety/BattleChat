package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.TextTransform
import com.ebicep.chatplus.features.TextTransformMode
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_DISPLAY_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component

/** Compact `✨ Normal` selector shown next to the chat input. */
class TextTransformTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    companion object {
        private const val ACTIVE_COLOR = 0xFF55FF55.toInt()
        private const val ITEM_HEIGHT = 12

        var selectorShow: Boolean = false
        private var hoveredMode: TextTransformMode? = null

        init {
            EventBus.register<ChatScreenCloseEvent> {
                selectorShow = false
                hoveredMode = null
            }
        }
    }

    override fun getWidth(): Int {
        val font = Minecraft.getInstance().font
        return TextTransformMode.entries.maxOf { font.width("✨ ${it.displayName}") }
    }

    override fun getText(): String {
        return "✨ ${TextTransform.mode.displayName}"
    }

    override fun onClick(button: Int) {
        when (button) {
            0 -> selectorShow = !selectorShow
            1 -> {
                TextTransform.setMode(TextTransformMode.NORMAL)
                selectorShow = false
            }
        }
    }

    override fun onClickEvent(event: ChatScreenMouseClickedEvent) {
        if (!selectorShow || event.button != 0) {
            return
        }

        val selected = hoveredMode ?: return
        TextTransform.setMode(selected)
        selectorShow = false
        hoveredMode = null
        event.returnFunction = true
    }

    override fun onHover(guiGraphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int) {
        guiGraphics.setTooltipForNextFrame(
            chatPlusScreen.font,
            Component.literal("Outgoing text style · Left click: choose · Right click: Normal"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(
        guiGraphics: GuiGraphicsExtractor,
        currentX: Int,
        currentY: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, -1)

        if (TextTransform.mode != TextTransformMode.NORMAL) {
            renderOutline(guiGraphics, currentX, currentY, ACTIVE_COLOR)
        }

        if (!selectorShow) {
            hoveredMode = null
            return
        }

        val font = Minecraft.getInstance().font
        val modes = TextTransformMode.entries
        val menuWidth = maxOf(
            getPaddedWidth(),
            modes.maxOf { font.width(it.displayName) + TextBarElements.PADDING * 2 }
        )
        val menuX = currentX + getPaddedWidth() - menuWidth
        val menuHeight = modes.size * ITEM_HEIGHT
        val renderAbove = currentY >= chatPlusScreen.height / 2
        val menuY = if (renderAbove) currentY - menuHeight - 1 else currentY + EDIT_BOX_DISPLAY_HEIGHT + 1

        hoveredMode = null
        modes.forEachIndexed { index, mode ->
            val y = menuY + index * ITEM_HEIGHT
            val hovering = mouseX in menuX until (menuX + menuWidth) && mouseY in y until (y + ITEM_HEIGHT)
            if (hovering) {
                hoveredMode = mode
            }

            guiGraphics.fill(
                menuX,
                y,
                menuX + menuWidth,
                y + ITEM_HEIGHT,
                if (hovering) 0xAA333333.toInt() else 0xCC000000.toInt()
            )
            guiGraphics.text(
                font,
                mode.displayName,
                menuX + TextBarElements.PADDING,
                y + 2,
                if (mode == TextTransform.mode) ACTIVE_COLOR else -1
            )
        }
    }
}
