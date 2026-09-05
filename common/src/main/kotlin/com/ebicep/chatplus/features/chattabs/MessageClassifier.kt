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

    private data class PlayerCandidate(
        val name: String,
        val start: Int,
    )

    private val playerToken = Regex("[A-Za-z0-9_]{3,16}")
    private val levelPrefix = Regex("\\[\\d{1,4}]\\s*")
    private val explicitChatDelimiter = Regex("^\\s*(?::|»|›|>)\\s*.+")
    private val angleChatSuffix = Regex("^\\s*>\\s*.+")
    private val bracketedChatSuffix = Regex("^\\s*(?:\\[[^\\]\\r\\n]{1,32}]\\s*)+.+")

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
        " won ",
        " received ",
        " earned ",
        " reached ",
    )

    fun classify(
        component: Component,
        source: GuiMessageSource,
        signature: MessageSignature?,
    ): Classification {
        if (signature != null) {
            return Classification(Kind.PLAYER, 100, "message has a player chat signature")
        }

        if (source == GuiMessageSource.PLAYER) {
            return Classification(Kind.PLAYER, 100, "Minecraft GuiMessageSource is player chat")
        }

        // ViaVersion/legacy servers commonly surface old player chat as SYSTEM.
        // Try the conservative legacy classifier before accepting SYSTEM as final.
        classifyLegacy(component.string)?.let { return it }

        return if (source == GuiMessageSource.SYSTEM) {
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
        val candidates = playerToken.findAll(text).mapNotNull { match ->
            if (connection.getPlayerInfo(match.value) == null) {
                null
            } else {
                PlayerCandidate(match.value, match.range.first)
            }
        }.toList()

        if (candidates.isEmpty()) {
            return null
        }

        // Prefer strong chat shapes over simply taking the first online player name
        // found in a server/plugin message.
        for (candidate in candidates) {
            val afterName = text.substring(candidate.start + candidate.name.length)
            if (afterName.isBlank()) {
                continue
            }

            val afterLower = afterName.lowercase()
            if (obviousSystemContinuation.any { afterLower.startsWith(it) }) {
                continue
            }

            if (explicitChatDelimiter.matches(afterName)) {
                return Classification(
                    Kind.PLAYER,
                    95,
                    "legacy message contains online player '${candidate.name}' followed by a chat delimiter"
                )
            }

            val beforeName = text.substring(0, candidate.start)
            if (beforeName.trimEnd().endsWith("<") && angleChatSuffix.matches(afterName)) {
                return Classification(Kind.PLAYER, 95, "legacy <player> chat format matched for '${candidate.name}'")
            }

            // BattleCraft MineWars currently uses e.g.
            // [6] Owner PlayerName [Epic] message
            // while other modes commonly use a ':' delimiter. Requiring a bracketed
            // suffix here avoids treating arbitrary level/rank server announcements as chat.
            if (levelPrefix.containsMatchIn(beforeName) && bracketedChatSuffix.matches(afterName)) {
                return Classification(
                    Kind.PLAYER,
                    90,
                    "legacy ranked chat contains a level prefix, online player '${candidate.name}', and a post-name tag"
                )
            }
        }

        return null
    }
}
