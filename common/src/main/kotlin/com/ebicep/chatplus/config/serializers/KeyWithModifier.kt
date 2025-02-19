package com.ebicep.chatplus.config.serializers

import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
) {

    fun isDown(): Boolean {
        val keyDown = key.isDown()
        val modifierDown = modifier == 0.toShort() ||
                modifier == 1.toShort() && Screen.hasAltDown() ||
                modifier == 2.toShort() && Screen.hasControlDown() ||
                modifier == 4.toShort() && Screen.hasShiftDown()
        return keyDown && modifierDown
    }

    fun isDown(keyCode: Int, modifier: Int): Boolean {
        return key.value != -1 && key.value == keyCode && (this.modifier == 0.toShort() || this.modifier == modifier.toShort())
    }

    fun getDisplayName(parentheses: Boolean): Component {
        if (key.value == -1) {
            return Component.empty()
        }
        val modifierDisplayName = when (modifier) {
            1.toShort() -> "Alt"
            2.toShort() -> "Ctrl"
            4.toShort() -> "Shift"
            else -> ""
        }
        val keyDisplayName = key.displayName
        val component = Component.empty()
        if (parentheses) {
            component.append(Component.literal(" ("))
        }
        component.append(Component.literal(modifierDisplayName))
        if (modifierDisplayName.isNotEmpty()) {
            component.append(Component.literal(" + "))
        }
        component.append(keyDisplayName)
        if (parentheses) {
            component.append(Component.literal(")"))
        }
        return component
    }

}