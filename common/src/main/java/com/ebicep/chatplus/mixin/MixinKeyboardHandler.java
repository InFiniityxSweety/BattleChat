package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.features.DeleteMessages;
import com.ebicep.chatplus.platform.events.EventResult;
import com.ebicep.chatplus.platform.events.PlatformKeyEvent;
import com.ebicep.chatplus.platform.events.client.ClientRawInputEvent;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void chatplus$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        Window window = Minecraft.getInstance().getWindow();
        if (handle != window.handle()) {
            return;
        }
        EventResult result = ClientRawInputEvent.dispatch(
                Minecraft.getInstance(),
                action,
                new PlatformKeyEvent(event.key(), event.modifiers())
        );
        if (result.getInterrupt()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"))
    private void handleDebugKeys(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        DeleteMessages.INSTANCE.f3D();
    }

}
