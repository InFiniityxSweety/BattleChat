package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.MessageClassifier
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
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

            ChatPlus.sendMessage(
                Component.literal("Message Info")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            )
            ChatPlus.sendMessage(
                Component.literal("Type: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(classification.kind.name).withStyle(kindColor))
                    .append(Component.literal(" · Confidence: ${classification.confidence}%").withStyle(ChatFormatting.GRAY))
            )
            ChatPlus.sendMessage(
                Component.literal("Reason: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(classification.reason).withStyle(ChatFormatting.WHITE))
            )
            ChatPlus.sendMessage(
                Component.literal("Minecraft source: $sourceText · Signed: $signedText")
                    .withStyle(ChatFormatting.GRAY)
            )
            ChatPlus.sendMessage(
                Component.literal("Sender UUID: $senderText")
                    .withStyle(ChatFormatting.GRAY)
            )
            ChatPlus.sendMessage(
                Component.literal("Text: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(rawText).withStyle(ChatFormatting.WHITE))
            )

            event.returnFunction = true
        }
    }
}
