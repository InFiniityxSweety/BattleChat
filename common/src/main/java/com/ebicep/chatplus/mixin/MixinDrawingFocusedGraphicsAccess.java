package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.accessor.IMixinChatRenderContext;
import com.ebicep.chatplus.hud.ChatRenderContext;
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent;
import net.minecraft.client.gui.components.ChatComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatComponent.DrawingFocusedGraphicsAccess.class)
public abstract class MixinDrawingFocusedGraphicsAccess implements IMixinChatRenderContext {

    @Unique
    private ChatRenderContext chatPlus$chatRenderContext;

    @Override
    public ChatRenderContext chatPlus$getChatRenderContext() {
        return this.chatPlus$chatRenderContext;
    }

    @Override
    public void chatPlus$setChatRenderContext(ChatRenderContext chatRenderContext) {
        this.chatPlus$chatRenderContext = chatRenderContext;
    }

    @ModifyArg(
            method = "handleMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"
            ),
            index = 1
    )
    private int handleMessage(int i) {
        if (chatPlus$chatRenderContext == null) {
            return i;
        }
        @NotNull ChatRenderLineTextEvent renderer = chatPlus$chatRenderContext.getRenderTextLineEvent();
        return renderer.getX();
    }

}
