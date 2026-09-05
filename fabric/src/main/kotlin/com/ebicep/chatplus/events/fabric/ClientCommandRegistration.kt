package com.ebicep.chatplus.events.fabric

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component

object ClientCommandRegistration {

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource>, _: CommandBuildContext? ->
            // BattleChat public command names.
            dispatcher.register(createCommand("battlechat"))
            dispatcher.register(createCommand("bc"))

            // Keep the original ChatPlus aliases for imported configs / muscle memory.
            dispatcher.register(createCommand("chatplus"))
            dispatcher.register(createCommand("cp"))
        })
    }

    private fun createCommand(commandName: String): LiteralArgumentBuilder<FabricClientCommandSource?>? =
        ClientCommands.literal(commandName)
            .then(
                ClientCommands.literal("clear")
                    .executes {
                        ChatManager.globalSelectedTab.clear()
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                ClientCommands.literal("hide")
                    .executes {
                        Config.values.hideChatEnabled = !Config.values.hideChatEnabled
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                ClientCommands.literal("tab")
                    .then(
                        ClientCommands.literal("delete")
                            .executes {
                                val selectedWindow = ChatManager.selectedWindow
                                val globalSelectedTab = ChatManager.globalSelectedTab
                                selectedWindow.tabSettings.removeTab(globalSelectedTab)
                                Command.SINGLE_SUCCESS
                            }
                    )
                    .then(
                        ClientCommands.literal("clone")
                            .executes {
                                val selectedWindow = ChatManager.selectedWindow
                                val globalSelectedTab = ChatManager.globalSelectedTab
                                selectedWindow.tabSettings.cloneTab(globalSelectedTab)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
            .then(
                ClientCommands.literal("debug")
                    .executes {
                        Debug.debug = !Debug.debug
                        ChatPlus.sendMessage(
                            Component.literal("Debug ${if (Debug.debug) "Enabled" else "Disabled"}")
                                .withStyle(if (Debug.debug) ChatFormatting.GREEN else ChatFormatting.RED)
                        )
                        Command.SINGLE_SUCCESS
                    }
            )
            .then(
                ClientCommands.literal("test")
                    .executes {
//                        ChatPlus.doTest()
                        Command.SINGLE_SUCCESS
                    }
            )
            .executes {
                ConfigScreen.open = true
                Command.SINGLE_SUCCESS
            }
}
