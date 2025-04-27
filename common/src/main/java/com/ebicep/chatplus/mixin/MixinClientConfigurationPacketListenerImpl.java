package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.chatwindows.ChatWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public class MixinClientConfigurationPacketListenerImpl {

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void chatPlus$init(
            Minecraft minecraft,
            Connection connection,
            CommonListenerCookie commonListenerCookie,
            CallbackInfo ci
    ) {
        if (commonListenerCookie.serverData() != null) {
            String ip = commonListenerCookie.serverData().ip;
            for (@NotNull ChatWindow chatWindow : Config.INSTANCE.getValues().getChatWindows()) {
                chatWindow.getRenderer().updateCachedDimension();
                chatWindow.getTabSettings().updateTabSettings(ip);
            }
        }
    }

}
