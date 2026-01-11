package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.features.ScreenshotChat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @ModifyExpressionValue(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
            )
    )
    private RenderTarget getMainRenderTarget(RenderTarget original) {
        RenderTarget renderTarget = ScreenshotChat.INSTANCE.getRenderTarget();
        if (renderTarget != null) {
            return renderTarget;
        }
        return original;
    }

    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"
            )
    )
    private void processBlurEffect(GameRenderer gameRenderer) {
        RenderTarget renderTarget = ScreenshotChat.INSTANCE.getRenderTarget();
        if (renderTarget == null) {
            gameRenderer.processBlurEffect();
        }
    }

}
