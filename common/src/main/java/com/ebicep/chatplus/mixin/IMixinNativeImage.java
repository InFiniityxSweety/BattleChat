package com.ebicep.chatplus.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface IMixinNativeImage {

    @Accessor("size")
    long size();

    @Invoker("writeToChannel")
    boolean callWriteToChannel(WritableByteChannel writableByteChannel);

}