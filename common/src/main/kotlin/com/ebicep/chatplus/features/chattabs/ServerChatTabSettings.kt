package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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

    var autoPrefix: String = ""

    // priority of tab, when adding messages, tabs are sorted by priority first
    // if a message got added to a tab then any other tab with a lower priority will not get the message
    var priority: Int = 0

    // if true then priority will be ignored when deciding to "skip" this tab
    var alwaysAdd: Boolean = false

    // if true then tab loop will break if message is added to this tab, overrides alwaysAdds
    var skipOthers: Boolean = false
    var commandsOverrideAutoPrefix: Boolean = true

    @Transient
    lateinit var chatTab: ChatTab

    constructor() : super("(?s).*")

    constructor(pattern: String) : super(pattern)

    constructor(pattern: String, formatted: Boolean) : super(pattern, formatted)

    fun clone(): ServerChatTabSettings {
        return ServerChatTabSettings(
            pattern, formatted
        ).also {
            it.name = this.name
            it.autoPrefix = this.autoPrefix
            it.priority = this.priority
            it.alwaysAdd = this.alwaysAdd
            it.skipOthers = this.skipOthers
            it.commandsOverrideAutoPrefix = this.commandsOverrideAutoPrefix
        }
    }

}
