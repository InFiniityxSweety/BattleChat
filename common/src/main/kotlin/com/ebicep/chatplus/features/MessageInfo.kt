package com.ebicep.chatplus.features

import com.ebicep.chatplus.MOD_COLOR
import com.ebicep.chatplus.MOD_NAME
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.MessageClassifier
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component

/**
 * Lightweight classifier diagnostics for BattleChat.
 *
 * Ctrl + right click a visible chat line to inspect exactly why it was routed
 * as PLAYER or SERVER. Normal right-click selection remains untouched.
 */
object MessageInfo {

    init {
        EventBus.register<ChatScreenMouseClickedEvent>({ 200 }) { event ->
            if (event.button != 1 || !Minecraft.getInstance().hasControlDown()) {
                return@register
            }

            val selectedTab = ChatManager.globalSelectedTab
            val line = selectedTab.getHoveredOverMessageLine(event.mouseX, event.mouseY) ?: return@register
            val linked = line.linkedMessage
            val guiMessage = linked.guiMessage
            val classification = MessageClassifier.classify(
                guiMessage.content,
                guiMessage.source,
                guiMessage.signature
            )

            val kindColor = when (classification.kind) {
                MessageClassifier.Kind.PLAYER -> ChatFormatting.GREEN
                MessageClassifier.Kind.SERVER -> ChatFormatting.RED
            }
            val sourceText = guiMessage.source.toString()
            val senderText = linked.senderUUID?.toString() ?: "unknown"
            val signedText = if (guiMessage.signature != null) "yes" else "no"
            val rawText = guiMessage.content.string.replace('\n', ' ').take(220)

            // Add diagnostics directly to the tab in which the user requested them.
            // Sending them as normal system chat would route them into the Server tab,
            // which is confusing when inspecting a player message in the Chat tab.
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("$MOD_NAME > ").withColor(MOD_COLOR)
                    .append(Component.literal("Message Info").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            )
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("Type: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(classification.kind.name).withStyle(kindColor))
                    .append(Component.literal(" · Confidence: ${classification.confidence}%").withStyle(ChatFormatting.GRAY))
            )
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("Reason: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(classification.reason).withStyle(ChatFormatting.WHITE))
            )
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("Minecraft source: $sourceText · Signed: $signedText")
                    .withStyle(ChatFormatting.GRAY)
            )
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("Sender UUID: $senderText")
                    .withStyle(ChatFormatting.GRAY)
            )
            showInTab(
                selectedTab,
                guiMessage.source,
                Component.literal("Text: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(rawText).withStyle(ChatFormatting.WHITE))
            )

            event.returnFunction = true
        }
    }

    private fun showInTab(tab: ChatTab, source: GuiMessageSource, component: Component) {
        tab.addNewMessage(
            AddNewMessageEvent(
                component.copy(),
                component,
                null,
                null,
                Minecraft.getInstance().gui.hud.guiTicks,
                source,
                GuiMessageTag.system(),
                false
            )
        )
    }
}
