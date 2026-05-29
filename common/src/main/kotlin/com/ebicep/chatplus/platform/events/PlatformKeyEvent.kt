package com.ebicep.chatplus.platform.events

class PlatformKeyEvent(private val key: Int, private val modifiers: Int) {
    fun key(): Int = key
    fun modifiers(): Int = modifiers
}
