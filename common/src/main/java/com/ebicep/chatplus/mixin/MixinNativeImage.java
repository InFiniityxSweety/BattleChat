package com.ebicep.chatplus.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NativeImage.class)
public abstract class MixinNativeImage implements IMixinNativeImage {
}
