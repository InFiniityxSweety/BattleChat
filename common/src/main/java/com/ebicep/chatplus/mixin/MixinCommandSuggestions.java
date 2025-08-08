package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.features.chattabs.ServerChatTabSettings;
import com.ebicep.chatplus.hud.ChatManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestions {

    @Inject(
            method = "showSuggestions",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/suggestion/Suggestions;isEmpty()Z"
            )
    )
    private void showSuggestions(boolean bl, CallbackInfo ci, @Local Suggestions suggestions) {
        ServerChatTabSettings currentSettings = ChatManager.INSTANCE.getGlobalSelectedTab().getCurrentSettings();
        suggestions.getList().removeIf(suggestion -> !currentSettings.getCommandsSuggestionsPattern().matches(suggestion.getText()));
        // TODO only commands
    }

}
