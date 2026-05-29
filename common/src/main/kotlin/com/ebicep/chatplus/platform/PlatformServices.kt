package com.ebicep.chatplus.platform

import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.PlatformKeyEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import java.nio.file.Path
import java.util.ServiceLoader

interface PlatformServicesProvider {
    fun platformInit()
    fun getConfigDirectory(): Path
    fun getConfigScreen(previousScreen: Screen? = null): Screen
    fun getTabEditorScreen(previousScreen: Screen? = null, chatTab: ChatTab): Screen
    fun getWindowEditorScreen(previousScreen: Screen? = null, chatWindow: ChatWindow): Screen
    fun registerClientTickPost(listener: () -> Unit)
    fun registerRenderHud(listener: (GuiGraphicsExtractor, Float) -> Unit)
    fun registerKeyPressed(listener: (Minecraft, Int, PlatformKeyEvent) -> EventResult)
}

object PlatformServices {
    private val provider: PlatformServicesProvider by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { loadProvider() }

    fun get(): PlatformServicesProvider = provider

    private fun loadProvider(): PlatformServicesProvider {
        val classLoader = PlatformServicesProvider::class.java.classLoader
        val isFabric = isClassPresent("net.fabricmc.loader.api.FabricLoader", classLoader)
        val isNeoForge = isClassPresent("net.neoforged.fml.ModLoadingContext", classLoader) ||
            isClassPresent("net.neoforged.neoforge.common.NeoForge", classLoader)

        val preferred = when {
            isFabric -> listOf(
                "com.ebicep.chatplus.fabric.ChatPlusPlatformInitImpl",
                "com.ebicep.chatplus.neoforge.ChatPlusPlatformInitImpl"
            )
            isNeoForge -> listOf(
                "com.ebicep.chatplus.neoforge.ChatPlusPlatformInitImpl",
                "com.ebicep.chatplus.fabric.ChatPlusPlatformInitImpl"
            )
            else -> listOf(
                "com.ebicep.chatplus.fabric.ChatPlusPlatformInitImpl",
                "com.ebicep.chatplus.neoforge.ChatPlusPlatformInitImpl"
            )
        }

        preferred.forEach { className ->
            tryCreateProvider(className, classLoader)?.let { return it }
        }

        val loader = ServiceLoader.load(PlatformServicesProvider::class.java, classLoader)
        val iterator = loader.iterator()
        while (iterator.hasNext()) {
            try {
                return iterator.next()
            } catch (_: Throwable) {
                // ignore and continue
            }
        }
        error("No PlatformServicesProvider found. Ensure the platform module registers a provider.")
    }

    private fun tryCreateProvider(className: String, classLoader: ClassLoader): PlatformServicesProvider? {
        return try {
            val clazz = Class.forName(className, true, classLoader)
            clazz.getDeclaredConstructor().newInstance() as PlatformServicesProvider
        } catch (_: Throwable) {
            null
        }
    }

    private fun isClassPresent(className: String, classLoader: ClassLoader): Boolean {
        return try {
            Class.forName(className, false, classLoader)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
