package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.EmojiPicker
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_DISPLAY_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import kotlin.math.ceil

/**
 * Compact emoji picker opened from the 😀 button beside the chat input.
 * Left-click inserts at the current cursor position; right-click toggles favorite.
 */
class EmojiPickerTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    companion object {
        private const val CELL = 18
        private const val COLS = 8
        private const val PANEL_PADDING = 4
        private const val SEARCH_HEIGHT = 14
        private const val CATEGORY_HEIGHT = 18
        private const val MAX_RESULTS = 40
        private const val ACTIVE_COLOR = 0xFF55FF55.toInt()

        var selectorShow = false

        init {
            EventBus.register<ChatScreenCloseEvent> {
                selectorShow = false
            }
        }
    }

    private lateinit var searchBox: EditBox
    private var selectedCategory = EmojiPicker.Category.RECENT
    private var hoveredCategory: EmojiPicker.Category? = null
    private var hoveredEmoji: EmojiPicker.Emoji? = null
    private var ignoreNextClickEvent = false
    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0

    override fun init() {
        searchBox = EditBox(
            chatPlusScreen.minecraft!!.fontFilterFishy,
            -1000,
            -1000,
            COLS * CELL,
            SEARCH_HEIGHT,
            Component.literal("Search emoji")
        )
        searchBox.setMaxLength(32)
        searchBox.isBordered = true
        searchBox.setCanLoseFocus(true)
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callAddWidget(searchBox)
    }

    override fun getWidth(): Int = Minecraft.getInstance().font.width("😀") + 4

    override fun getText(): String = "😀"

    override fun onClick(button: Int) {
        if (button != 0) return
        ignoreNextClickEvent = true
        if (selectorShow) closePicker() else openPicker()
    }

    private fun openPicker() {
        selectorShow = true
        hoveredCategory = null
        hoveredEmoji = null
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callSetInitialFocus(searchBox)
    }

    private fun closePicker() {
        selectorShow = false
        hoveredCategory = null
        hoveredEmoji = null
        searchBox.isFocused = false
        searchBox.x = -1000
        searchBox.y = -1000
        chatPlusScreen as IMixinScreen
        chatPlusScreen as IMixinChatScreen
        chatPlusScreen.callSetInitialFocus(chatPlusScreen.input)
    }

    override fun onClickEvent(event: ChatScreenMouseClickedEvent) {
        if (ignoreNextClickEvent) {
            ignoreNextClickEvent = false
            return
        }
        if (!selectorShow) return

        hoveredCategory?.let { category ->
            if (event.button == 0) {
                selectedCategory = category
                searchBox.value = ""
                event.returnFunction = true
                return
            }
        }

        hoveredEmoji?.let { emoji ->
            when (event.button) {
                0 -> {
                    chatPlusScreen as IMixinChatScreen
                    chatPlusScreen.input?.insertText(emoji.value)
                    EmojiPicker.use(emoji.value)
                    event.returnFunction = true
                    chatPlusScreen as IMixinScreen
                    chatPlusScreen.callSetInitialFocus(chatPlusScreen.input)
                    return
                }
                1 -> {
                    EmojiPicker.toggleFavorite(emoji.value)
                    event.returnFunction = true
                    return
                }
            }
        }

        if (event.mouseX in panelX.toDouble()..(panelX + panelWidth).toDouble() &&
            event.mouseY in panelY.toDouble()..(panelY + panelHeight).toDouble()
        ) {
            event.returnFunction = searchBox.mouseClicked(event.mouseButtonEvent, false)
            return
        }

        closePicker()
    }

    override fun onHover(guiGraphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int) {
        guiGraphics.setTooltipForNextFrame(
            chatPlusScreen.font,
            Component.literal("Emoji picker"),
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
        if (selectorShow) renderOutline(guiGraphics, currentX, currentY, ACTIVE_COLOR)
        if (!selectorShow) return

        val font = Minecraft.getInstance().font
        val entries = EmojiPicker.emojis(selectedCategory, searchBox.value).take(MAX_RESULTS)
        val rows = maxOf(1, ceil(entries.size / COLS.toDouble()).toInt())
        panelWidth = COLS * CELL + PANEL_PADDING * 2
        panelHeight = PANEL_PADDING + SEARCH_HEIGHT + 2 + CATEGORY_HEIGHT + 2 + rows * CELL + PANEL_PADDING
        panelX = (currentX + getPaddedWidth() - panelWidth).coerceIn(2, maxOf(2, chatPlusScreen.width - panelWidth - 2))
        panelY = if (currentY >= chatPlusScreen.height / 2) {
            currentY - panelHeight - 2
        } else {
            currentY + EDIT_BOX_DISPLAY_HEIGHT + 2
        }

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE000000.toInt())
        guiGraphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF555555.toInt())

        searchBox.x = panelX + PANEL_PADDING
        searchBox.y = panelY + PANEL_PADDING
        searchBox.width = panelWidth - PANEL_PADDING * 2
        searchBox.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)

        val categoryY = searchBox.y + SEARCH_HEIGHT + 2
        hoveredCategory = null
        EmojiPicker.Category.entries.forEachIndexed { index, category ->
            val x = panelX + PANEL_PADDING + index * CELL
            val hovering = mouseX in x until x + CELL && mouseY in categoryY until categoryY + CATEGORY_HEIGHT
            if (hovering) hoveredCategory = category
            guiGraphics.fill(
                x,
                categoryY,
                x + CELL,
                categoryY + CATEGORY_HEIGHT,
                if (hovering) 0xAA333333.toInt() else 0x66000000
            )
            guiGraphics.centeredText(
                font,
                category.icon,
                x + CELL / 2,
                categoryY + 4,
                if (category == selectedCategory) ACTIVE_COLOR else -1
            )
            if (hovering) {
                guiGraphics.setTooltipForNextFrame(font, Component.literal(category.displayName), mouseX, mouseY)
            }
        }

        val gridY = categoryY + CATEGORY_HEIGHT + 2
        hoveredEmoji = null
        entries.forEachIndexed { index, emoji ->
            val col = index % COLS
            val row = index / COLS
            val x = panelX + PANEL_PADDING + col * CELL
            val y = gridY + row * CELL
            val hovering = mouseX in x until x + CELL && mouseY in y until y + CELL
            if (hovering) hoveredEmoji = emoji
            guiGraphics.fill(
                x,
                y,
                x + CELL,
                y + CELL,
                if (hovering) 0xAA333333.toInt() else 0x33000000
            )
            guiGraphics.centeredText(
                font,
                emoji.value,
                x + CELL / 2,
                y + 5,
                if (EmojiPicker.isFavorite(emoji.value)) 0xFFFFD966.toInt() else -1
            )
            if (hovering) {
                guiGraphics.setTooltipForNextFrame(
                    font,
                    Component.literal("${emoji.keywords} · Left: insert · Right: favorite"),
                    mouseX,
                    mouseY
                )
            }
        }

        if (entries.isEmpty()) {
            guiGraphics.centeredText(
                font,
                if (selectedCategory == EmojiPicker.Category.FAVORITES) "No favorites yet" else "No emojis found",
                panelX + panelWidth / 2,
                gridY + 5,
                0xFFAAAAAA.toInt()
            )
        }
    }
}
