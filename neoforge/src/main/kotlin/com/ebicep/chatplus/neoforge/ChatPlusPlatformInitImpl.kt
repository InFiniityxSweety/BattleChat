package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.ConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.gui.IConfigScreenFactory


object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        ChatPlus.init()
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: Minecraft, screen: Screen ->
                ConfigScreen.getConfigScreen(screen)
            }
        }
    }
}