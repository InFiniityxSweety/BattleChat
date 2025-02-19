package com.ebicep.chatplus.util

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object KeyUtil {

    fun InputConstants.Key.isDown(): Boolean {
        if (value == -1) return false
        val window = Minecraft.getInstance().window ?: return false
        return InputConstants.isKeyDown(window.window, value)
    }

    fun InputConstants.Key.getDisplayName(parentheses: Boolean): Component {
        if (this.value == -1) {
            return Component.empty()
        }
        val keyDisplayName = displayName
        val component = Component.empty()
        if (parentheses) {
            component.append(Component.literal(" ("))
        }
        component.append(keyDisplayName)
        if (parentheses) {
            component.append(Component.literal(")"))
        }
        return component
    }


}