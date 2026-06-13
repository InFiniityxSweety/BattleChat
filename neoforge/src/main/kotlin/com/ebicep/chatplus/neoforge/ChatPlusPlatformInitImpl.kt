package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.neoforge.ConfigDirectoryImpl
import com.ebicep.chatplus.config.neoforge.ConfigScreenImpl
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.platform.PlatformServicesProvider
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.GameShuttingDownEvent
import net.neoforged.neoforge.event.level.LevelEvent

class ChatPlusPlatformInitImpl : PlatformServicesProvider {

    private val tickListeners = mutableListOf<() -> Unit>()
    private val renderListeners = mutableListOf<(GuiGraphicsExtractor, Float) -> Unit>()
    private val startedListeners = mutableListOf<() -> Unit>()
    private val stoppingListeners = mutableListOf<() -> Unit>()
    private val levelLoadListeners = mutableListOf<() -> Unit>()
    private var tickHooked = false
    private var renderHooked = false
    private var lifecycleHooked = false

    override fun platformInit() {
        ChatPlus.init()
        NeoForgePlatformHooks.modContainer.registerExtensionPoint(IConfigScreenFactory::class.java) {
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
        NeoForge.EVENT_BUS.addListener<ClientTickEvent.Post> {
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

    override fun registerClientStarted(listener: () -> Unit) {
        startedListeners.add(listener)
        ensureLifecycleHooks()
    }

    override fun registerClientStopping(listener: () -> Unit) {
        stoppingListeners.add(listener)
        ensureLifecycleHooks()
    }

    override fun registerClientLevelLoad(listener: () -> Unit) {
        levelLoadListeners.add(listener)
        ensureLifecycleHooks()
    }

    private fun ensureLifecycleHooks() {
        if (lifecycleHooked) {
            return
        }
        lifecycleHooked = true
        NeoForgePlatformHooks.modEventBus.addListener(FMLClientSetupEvent::class.java) { event ->
            event.enqueueWork {
                startedListeners.forEach { it() }
            }
        }
        NeoForge.EVENT_BUS.addListener<GameShuttingDownEvent> {
            stoppingListeners.forEach { it() }
        }
        NeoForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> {
            levelLoadListeners.forEach { it() }
        }
        NeoForge.EVENT_BUS.addListener<LevelEvent.Load> { event ->
            if (event.level.isClientSide) {
                levelLoadListeners.forEach { it() }
            }
        }
    }
}
