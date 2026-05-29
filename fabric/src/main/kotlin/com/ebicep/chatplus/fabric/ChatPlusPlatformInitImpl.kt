package com.ebicep.chatplus.fabric

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.fabric.ConfigDirectoryImpl
import com.ebicep.chatplus.config.fabric.ConfigScreenImpl
import com.ebicep.chatplus.events.fabric.ClientCommandRegistration
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.platform.PlatformServicesProvider
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.PlatformKeyEvent
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

class ChatPlusPlatformInitImpl : PlatformServicesProvider {

    private val tickListeners = mutableListOf<() -> Unit>()
    private val keyListeners = mutableListOf<(Minecraft, Int, PlatformKeyEvent) -> EventResult>()
    private val hudListeners = mutableListOf<(GuiGraphicsExtractor, Float) -> Unit>()
    private var tickHooked = false
    private var hudHooked = false
    private val keyStates = BooleanArray(GLFW.GLFW_KEY_LAST + 1)

    override fun platformInit() {
        ChatPlus.init()
        ClientCommandRegistration.registerCommands()
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
        ensureTickHook()
    }

    override fun registerRenderHud(listener: (GuiGraphicsExtractor, Float) -> Unit) {
        hudListeners.add(listener)
        ensureHudHook()
    }

    override fun registerKeyPressed(listener: (Minecraft, Int, PlatformKeyEvent) -> EventResult) {
        keyListeners.add(listener)
        ensureTickHook()
    }

    private fun ensureTickHook() {
        if (tickHooked) {
            return
        }
        tickHooked = true
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            tickListeners.forEach { it() }
            if (keyListeners.isNotEmpty()) {
                pollKeys(client)
            }
        }
    }

    private fun ensureHudHook() {
        if (hudHooked) {
            return
        }
        hudHooked = true
        // Fabric HUD layers require unique identifiers, so use one element and fan out to listeners.
        val hudElement: HudElement = HudElement { graphics, deltaTracker ->
            val tickDelta = deltaTracker.gameTimeDeltaTicks
            hudListeners.forEach { it(graphics, tickDelta) }
        }
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("chatplus", "hud"),
            hudElement
        )
    }

    private fun pollKeys(client: Minecraft) {
        val window = client.window
        val handle = window.handle()
        for (key in 0..GLFW.GLFW_KEY_LAST) {
            val down = if (key in 0..7) {
                GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS
            } else if (key < GLFW.GLFW_KEY_SPACE) {
                // Skip invalid key codes that GLFW will reject.
                continue
            } else {
                InputConstants.isKeyDown(window, key)
            }
            if (down == keyStates[key]) {
                continue
            }
            keyStates[key] = down
            val action = if (down) GLFW.GLFW_PRESS else GLFW.GLFW_RELEASE
            val event = PlatformKeyEvent(key, getModifiers(client))
            dispatchKeyEvent(client, action, event)
        }
    }

    private fun dispatchKeyEvent(client: Minecraft, action: Int, event: PlatformKeyEvent) {
        for (listener in keyListeners) {
            if (listener(client, action, event).interrupt) {
                break
            }
        }
    }

    private fun getModifiers(client: Minecraft): Int {
        var modifiers = 0
        if (client.hasAltDown()) {
            modifiers = modifiers or 1
        }
        if (client.hasControlDown()) {
            modifiers = modifiers or 2
        }
        if (client.hasShiftDown()) {
            modifiers = modifiers or 4
        }
        return modifiers
    }
}