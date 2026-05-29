package com.ebicep.chatplus.platform.events

class EventResult private constructor(val interrupt: Boolean) {
    companion object {
        fun pass(): EventResult = EventResult(false)
        fun interruptTrue(): EventResult = EventResult(true)
    }
}
