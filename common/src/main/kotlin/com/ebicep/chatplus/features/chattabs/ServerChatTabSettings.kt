package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import com.mojang.brigadier.suggestion.Suggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MessageSignature

@Serializable
open class ServerChatTabSettings : MessageFilterFormatted {

    var serverPattern = MessageFilter("")
    var name: String = ""
        set(value) {
            field = value
            if (::chatTab.isInitialized) {
                chatTab.width = -1
            }
        }

    var autoSend: String = ""
    var autoPrefix: String = ""

    // BattleChat: optional semantic source filter applied before regex matching.
    // ANY preserves upstream ChatPlus behaviour and existing configs.
    var messageSourceMode: MessageSourceMode = MessageSourceMode.ANY

    // priority of tab, when adding messages, tabs are sorted by priority first
    // if a message got added to a tab then any other tab with a lower priority will not get the message
    var priority: Int = 0

    // if true then priority will be ignored when deciding to "skip" this tab
    var alwaysAdd: Boolean = false

    // if true then tab loop will break if message is added to this tab, overrides alwaysAdds
    var skipOthers: Boolean = false
    var commandsOverrideAutoPrefix: Boolean = true
    var suggestionsPatterns: MutableList<ServerChatTabCommandSuggestion> = mutableListOf()
    var notificationSettings: ServerChatTabNotificationSettings = ServerChatTabNotificationSettings()

    @Transient
    lateinit var chatTab: ChatTab

    constructor() : super("(?s).*")

    constructor(pattern: String) : super(pattern)

    constructor(pattern: String, formatted: Boolean) : super(pattern, formatted)

    fun matchesSource(classification: MessageClassifier.Classification): Boolean {
        return when (messageSourceMode) {
            MessageSourceMode.ANY -> true
            MessageSourceMode.PLAYER -> classification.kind == MessageClassifier.Kind.PLAYER
            MessageSourceMode.SERVER -> classification.kind == MessageClassifier.Kind.SERVER
        }
    }

    fun matchesSource(component: Component, source: GuiMessageSource, signature: MessageSignature?): Boolean {
        return matchesSource(MessageClassifier.classify(component, source, signature))
    }

    fun applyBattleChatNameDefault() {
        if (messageSourceMode != MessageSourceMode.ANY) {
            return
        }
        when (name.trim().lowercase()) {
            "chat", "player", "players", "spieler" -> messageSourceMode = MessageSourceMode.PLAYER
            "server", "system" -> messageSourceMode = MessageSourceMode.SERVER
        }
    }

    fun clone(): ServerChatTabSettings {
        return ServerChatTabSettings(
            pattern, formatted
        ).also {
            it.serverPattern = MessageFilter(this.serverPattern.pattern)
            it.name = this.name
            it.autoSend = this.autoSend
            it.autoPrefix = this.autoPrefix
            it.messageSourceMode = this.messageSourceMode
            it.priority = this.priority
            it.alwaysAdd = this.alwaysAdd
            it.skipOthers = this.skipOthers
            it.commandsOverrideAutoPrefix = this.commandsOverrideAutoPrefix
            it.suggestionsPatterns = this.suggestionsPatterns.map { it.clone() }.toMutableList()
            it.notificationSettings = this.notificationSettings.clone()
        }
    }

    fun modifyCommandSuggestions(input: String, suggestions: MutableList<Suggestion>) {
        for (pattern in suggestionsPatterns) {
            if (!pattern.commandMatcher.matches(input)) {
                continue
            }
            when (pattern.mode) {
                ServerChatTabCommandSuggestion.SuggestionMode.FILTER -> {
                    suggestions.removeIf { !pattern.suggestionMatcher.matches(it.text) }
                }

                ServerChatTabCommandSuggestion.SuggestionMode.SORT -> {
                    val matchCache: MutableMap<Suggestion, Boolean> = HashMap<Suggestion, Boolean>()
                    for (s in suggestions) {
                        matchCache.put(s, pattern.suggestionMatcher.matches(s.text))
                    }
                    suggestions.sortWith(compareBy { !matchCache[it]!! })
                }
            }
            break
        }
    }

}

@Serializable
enum class MessageSourceMode {
    ANY,
    PLAYER,
    SERVER,
}

@Serializable
data class ServerChatTabNotificationSettings(
    var disableNotifications: Boolean = false,
    var notificationMatch: MessageFilterFormatted = MessageFilterFormatted("(?s).*"),
) {
    fun clone(): ServerChatTabNotificationSettings {
        return ServerChatTabNotificationSettings(
            this.disableNotifications,
            MessageFilterFormatted(
                this.notificationMatch.pattern,
                this.notificationMatch.formatted
            )
        )
    }
}

@Serializable
data class ServerChatTabCommandSuggestion(
    val commandMatcher: MessageFilter = MessageFilter(""),
    val suggestionMatcher: MessageFilter = MessageFilter(""),
    var mode: SuggestionMode = SuggestionMode.FILTER,
) {
    fun clone(): ServerChatTabCommandSuggestion {
        return ServerChatTabCommandSuggestion(
            MessageFilter(
                this.commandMatcher.pattern,
            ),
            MessageFilter(
                this.suggestionMatcher.pattern,
            )
        )
    }

    fun updateRegex() {
        commandMatcher.updateRegex()
        suggestionMatcher.updateRegex()
    }

    @Serializable
    enum class SuggestionMode(val key: String) : EnumTranslatableName {
        FILTER("chatPlus.chatWindow.tabSettings.chatTabs.suggestionsPatterns.suggestionMode.filter"),
        SORT("chatPlus.chatWindow.tabSettings.chatTabs.suggestionsPatterns.suggestionMode.sort"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }
}
