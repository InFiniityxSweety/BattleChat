package com.ebicep.chatplus.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphics implements IMixinGuiGraphics {


}
