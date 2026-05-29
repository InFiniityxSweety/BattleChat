package com.ebicep.chatplus.platform.events.client

import com.ebicep.chatplus.platform.PlatformServices
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.PlatformKeyEvent
import net.minecraft.client.Minecraft

object ClientRawInputEvent {
    object KEY_PRESSED {
        fun register(listener: (Minecraft, Int, PlatformKeyEvent) -> EventResult) {
            PlatformServices.get().registerKeyPressed(listener)
        }
    }
}
