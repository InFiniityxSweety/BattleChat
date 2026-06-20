package com.ebicep.chatplus.platform.events.client

import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.PlatformKeyEvent
import net.minecraft.client.Minecraft

object ClientRawInputEvent {

    private val listeners = mutableListOf<(Minecraft, Int, PlatformKeyEvent) -> EventResult>()

    object KEY_PRESSED {
        fun register(listener: (Minecraft, Int, PlatformKeyEvent) -> EventResult) {
            listeners.add(listener)
        }
    }

    @JvmStatic
    fun dispatch(minecraft: Minecraft, action: Int, keyEvent: PlatformKeyEvent): EventResult {
        for (listener in listeners) {
            val result = listener(minecraft, action, keyEvent)
            if (result.interrupt) {
                return result
            }
        }
        return EventResult.pass()
    }
}
