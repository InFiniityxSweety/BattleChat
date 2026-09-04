package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.events.EventBus;
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent;
import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.ebicep.chatplus.features.chattabs.SkipNewMessageEvent;
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager;
import com.ebicep.chatplus.hud.ChatManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE / 2)
public class MixinChatComponent {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled() || (Config.INSTANCE.getValues().getShowVanillaWhenUnfocused() && !ChatManager.INSTANCE.isChatFocused())) {
            return;
        }
        graphics.pose().pushMatrix();
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("chatplus");
        ChatWindowsManager.INSTANCE.renderAll(graphics, font, ticks, mouseX, mouseY, displayMode, changeCursorOnInsertions);
        profilerFiller.pop();
        graphics.pose().popMatrix();
        ci.cancel();
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("RETURN"))
    public void addMessage(
            Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci, @Local(name = "message") GuiMessage message
    ) {
        if (!ChatPlus.INSTANCE.isEnabled() && !Config.INSTANCE.getValues().getAddMessagesIfDisabled()) {
            return;
        }
        contents = message.content();
        signature = message.signature();
        source = message.source();
        tag = message.tag();
        List<ChatTab> addMessagesTo = new ArrayList<>();

        Integer lastPriority = null;
        for (ChatTab chatTab : ChatManager.INSTANCE.getGlobalSortedTabs()) {
            int priority = chatTab.getPriority();
            boolean alwaysAdd = chatTab.getAlwaysAdd();
            if (lastPriority != null && lastPriority > priority && !alwaysAdd) {
                continue;
            }

            // BattleChat source classification runs before the existing regex filter.
            // Existing tabs remain ANY by default. Tabs named Chat/Server are upgraded
            // automatically so imported ChatPlus configs immediately gain semantic routing.
            chatTab.getCurrentSettings().applyBattleChatNameDefault();
            if (!chatTab.getCurrentSettings().matchesSource(contents, source, signature)) {
                continue;
            }

            if (chatTab.matches(contents)) {
                addMessagesTo.add(chatTab);
                if (chatTab.getSkipOthers()) {
                    break;
                }
                if (!alwaysAdd) {
                    lastPriority = priority;
                }
            }
        }
        if (!addMessagesTo.isEmpty()) {
            AddNewMessageEvent messageEvent = new AddNewMessageEvent(
                    contents.copy(),
                    contents,
                    null,
                    signature,
                    this.minecraft.gui.hud.getGuiTicks(),
                    source,
                    tag,
                    false
            );
            EventBus.INSTANCE.post(AddNewMessageEvent.class, messageEvent);
            if (messageEvent.getReturnFunction()) {
                return;
            }
            for (ChatTab chatTab : addMessagesTo) {
                chatTab.addNewMessage(messageEvent);
            }
        } else {
            SkipNewMessageEvent messageEvent = new SkipNewMessageEvent(
                    contents.copy(),
                    contents,
                    null,
                    signature,
                    this.minecraft.gui.hud.getGuiTicks(),
                    source,
                    tag
            );
            EventBus.INSTANCE.post(SkipNewMessageEvent.class, messageEvent);
        }
    }

}
