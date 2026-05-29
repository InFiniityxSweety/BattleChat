package com.ebicep.chatplus.platform.events.client

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel

object ClientLifecycleEvent {
    private val levelLoadListeners = mutableListOf<() -> Unit>()
    private val startedListeners = mutableListOf<() -> Unit>()
    private val stoppingListeners = mutableListOf<() -> Unit>()
    private var tickRegistered = false
    private var shutdownHookRegistered = false
    private var started = false
    private var lastLevel: ClientLevel? = null

    private fun ensureTickListener() {
        if (tickRegistered) {
            return
        }
        tickRegistered = true
        ClientTickEvent.CLIENT_POST.register {
            val client = Minecraft.getInstance()
            if (!started) {
                started = true
                startedListeners.forEach { it() }
            }
            val level = client.level
            if (level != null && level !== lastLevel) {
                lastLevel = level
                levelLoadListeners.forEach { it() }
            } else if (level == null) {
                lastLevel = null
            }
        }
    }

    private fun ensureShutdownHook() {
        if (shutdownHookRegistered) {
            return
        }
        shutdownHookRegistered = true
        Runtime.getRuntime().addShutdownHook(Thread {
            stoppingListeners.forEach { it() }
        })
    }

    object CLIENT_LEVEL_LOAD {
        fun register(listener: () -> Unit) {
            levelLoadListeners.add(listener)
            ensureTickListener()
        }
    }

    object CLIENT_STARTED {
        fun register(listener: () -> Unit) {
            startedListeners.add(listener)
            ensureTickListener()
        }
    }

    object CLIENT_STOPPING {
        fun register(listener: () -> Unit) {
            stoppingListeners.add(listener)
            ensureShutdownHook()
        }
    }
}
