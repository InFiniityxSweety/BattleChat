package com.ebicep.chatplus.config

import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.platform.PlatformServices
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object ConfigScreen {

    var open = false

    fun handleOpenScreen() {
        if (open) {
            open = false
            openConfigScreen()
        }
    }

    private fun openConfigScreen() {
        val screen = getConfigScreen()
        Minecraft.getInstance().setScreen(screen)
    }

    @JvmStatic
    fun getConfigScreen(previousScreen: Screen? = null): Screen {
        return PlatformServices.get().getConfigScreen(previousScreen)
    }

    @JvmStatic
    fun getTabEditorScreen(previousScreen: Screen? = null, chatTab: ChatTab): Screen {
        return PlatformServices.get().getTabEditorScreen(previousScreen, chatTab)
    }

    @JvmStatic
    fun getWindowEditorScreen(previousScreen: Screen? = null, chatWindow: ChatWindow): Screen {
        return PlatformServices.get().getWindowEditorScreen(previousScreen, chatWindow)
    }

}