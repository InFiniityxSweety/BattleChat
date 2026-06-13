package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlusPlatformInit
import com.ebicep.chatplus.MOD_ID
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(MOD_ID)
class ChatPlusForge(modContainer: ModContainer, modEventBus: IEventBus) {

    init {
        NeoForgePlatformHooks.modContainer = modContainer
        NeoForgePlatformHooks.modEventBus = modEventBus
        ChatPlusPlatformInit.platformInit()
    }
}
