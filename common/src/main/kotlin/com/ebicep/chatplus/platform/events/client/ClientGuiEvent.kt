package com.ebicep.chatplus.platform.events.client

import com.ebicep.chatplus.platform.PlatformServices
import net.minecraft.client.gui.GuiGraphicsExtractor

object ClientGuiEvent {
    object RENDER_HUD {
        fun register(listener: (GuiGraphicsExtractor, Float) -> Unit) {
            PlatformServices.get().registerRenderHud(listener)
        }
    }
}
