package com.ebicep.chatplus.features.chattabs

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MessageSignature

/**
 * Classifies incoming chat before it is routed into tabs.
 *
 * Modern Minecraft gives us a GuiMessageSource and (for signed player chat) a
 * MessageSignature. Legacy servers reached through ViaVersion can lose that
 * distinction, so a conservative player-name/chat-shape fallback is used only
 * when native metadata does not already identify player chat.
 */
object MessageClassifier {

    enum class Kind {
        PLAYER,
        SERVER,
    }

    data class Classification(
        val kind: Kind,
        val confidence: Int,
        val reason: String,
    )

    private val playerToken = Regex("[A-Za-z0-9_]{3,16}")
    private val levelPrefix = Regex("\\[\\d{1,4}]\\s*")
    private val explicitChatDelimiter = Regex("^\\s*(?::|»|›|>)\\s*")
    private val angleChatSuffix = Regex("^\\s*>\\s*.+")

    private val obviousSystemContinuation = listOf(
        " joined",
        " left",
        " disconnected",
        " connected",
        " was ",
        " has ",
        " died",
        " fell",
        " burned",
        " drowned",
        " suffocated",
        " blew up",
        " hit the ground",
        " tried to swim",
        " went up in flames",
        " was slain",
        " was shot",
        " was killed",
    )

    fun classify(
        component: Component,
        source: GuiMessageSource,
        signature: MessageSignature?,
    ): Classification {
        if (signature != null) {
            return Classification(Kind.PLAYER, 100, "message has a player chat signature")
        }

        val sourceName = source.toString().uppercase()
        if (sourceName.contains("PLAYER")) {
            return Classification(Kind.PLAYER, 100, "Minecraft GuiMessageSource is player chat")
        }

        val legacy = classifyLegacy(component.string)
        if (legacy != null) {
            return legacy
        }

        return if (sourceName.contains("SYSTEM")) {
            Classification(Kind.SERVER, 100, "Minecraft GuiMessageSource is system and no legacy player-chat shape matched")
        } else {
            Classification(Kind.SERVER, 80, "no player-chat metadata or conservative legacy match")
        }
    }

    private fun classifyLegacy(rawText: String): Classification? {
        val text = ChatFormatting.stripFormatting(rawText)?.trim().orEmpty()
        if (text.isEmpty()) {
            return null
        }

        val connection = Minecraft.getInstance().connection ?: return null
        var matchedPlayer: String? = null
        var playerStart = -1

        playerToken.findAll(text).forEach { match ->
            if (matchedPlayer != null) {
                return@forEach
            }
            if (connection.getPlayerInfo(match.value) != null) {
                matchedPlayer = match.value
                playerStart = match.range.first
            }
        }

        val name = matchedPlayer ?: return null
        val afterName = text.substring(playerStart + name.length)
        val afterLower = afterName.lowercase()

        if (afterName.isBlank()) {
            return null
        }

        if (obviousSystemContinuation.any { afterLower.startsWith(it) }) {
            return null
        }

        if (explicitChatDelimiter.containsMatchIn(afterName)) {
            return Classification(Kind.PLAYER, 92, "legacy message contains online player '$name' followed by a chat delimiter")
        }

        val beforeName = text.substring(0, playerStart)
        if (beforeName.trimEnd().endsWith("<") && angleChatSuffix.containsMatchIn(afterName)) {
            return Classification(Kind.PLAYER, 92, "legacy <player> chat format matched for '$name'")
        }

        // BattleCraft and many legacy networks prepend a numeric level before a
        // rank/name. This also covers extra clan/queue tags after the name.
        if (levelPrefix.containsMatchIn(beforeName) && afterName.trim().length >= 2) {
            return Classification(Kind.PLAYER, 88, "legacy ranked chat contains level prefix and online player '$name'")
        }

        return null
    }
}
