package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.neoforge.ConfigDirectoryImpl
import com.ebicep.chatplus.config.neoforge.ConfigScreenImpl
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.platform.PlatformServicesProvider
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.PlatformKeyEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

class ChatPlusPlatformInitImpl : PlatformServicesProvider {

    private val tickListeners = mutableListOf<() -> Unit>()
    private val keyListeners = mutableListOf<(Minecraft, Int, PlatformKeyEvent) -> EventResult>()
    private val renderListeners = mutableListOf<(GuiGraphicsExtractor, Float) -> Unit>()
    private var tickHooked = false
    private var keyHooked = false
    private var renderHooked = false

    override fun platformInit() {
        ChatPlus.init()
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: ModContainer, screen: Screen ->
                ConfigScreen.getConfigScreen(screen)
            }
        }
    }

    override fun getConfigDirectory() = ConfigDirectoryImpl.getConfigDirectory()

    override fun getConfigScreen(previousScreen: Screen?): Screen {
        return ConfigScreenImpl.getConfigScreen(previousScreen)
    }

    override fun getTabEditorScreen(previousScreen: Screen?, chatTab: ChatTab): Screen {
        return ConfigScreenImpl.getTabEditorScreen(previousScreen, chatTab)
    }

    override fun getWindowEditorScreen(previousScreen: Screen?, chatWindow: ChatWindow): Screen {
        return ConfigScreenImpl.getWindowEditorScreen(previousScreen, chatWindow)
    }

    override fun registerClientTickPost(listener: () -> Unit) {
        tickListeners.add(listener)
        if (tickHooked) {
            return
        }
        tickHooked = true
        NeoForge.EVENT_BUS.addListener<ClientTickEvent.Post> { event ->
            tickListeners.forEach { it() }
        }
    }

    override fun registerRenderHud(listener: (GuiGraphicsExtractor, Float) -> Unit) {
        renderListeners.add(listener)
        if (renderHooked) {
            return
        }
        renderHooked = true
        NeoForge.EVENT_BUS.addListener<RenderGuiEvent.Post> { event ->
            val guiGraphics = event.guiGraphics
            val partialTick = event.partialTick.gameTimeDeltaTicks
            renderListeners.forEach { it(guiGraphics, partialTick) }
        }
    }

    override fun registerKeyPressed(listener: (Minecraft, Int, PlatformKeyEvent) -> EventResult) {
        keyListeners.add(listener)
        if (keyHooked) {
            return
        }
        keyHooked = true
        NeoForge.EVENT_BUS.addListener<InputEvent.Key> { event ->
            dispatchKeyEvent(event)
        }
    }

    private fun dispatchKeyEvent(event: InputEvent.Key): EventResult {
        val client = Minecraft.getInstance()
        val action = event.action
        val keyEvent = PlatformKeyEvent(event.key, event.modifiers)
        var finalResult = EventResult.pass()
        for (listener in keyListeners) {
            finalResult = listener(client, action, keyEvent)
            if (finalResult.interrupt) {
                break
            }
        }
        return finalResult
    }
}