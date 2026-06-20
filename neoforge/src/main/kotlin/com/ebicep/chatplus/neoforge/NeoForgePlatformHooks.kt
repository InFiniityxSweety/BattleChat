package com.ebicep.chatplus.neoforge

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer

object NeoForgePlatformHooks {
    lateinit var modContainer: ModContainer
    lateinit var modEventBus: IEventBus
}
