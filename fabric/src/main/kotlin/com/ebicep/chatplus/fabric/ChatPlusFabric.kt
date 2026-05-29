package com.ebicep.chatplus.fabric

import com.ebicep.chatplus.ChatPlusPlatformInit
import net.fabricmc.api.ModInitializer


object ChatPlusFabric : ModInitializer {

    override fun onInitialize() {
        ChatPlusPlatformInit.platformInit()
    }

}
