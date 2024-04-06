package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.network.SidedMetadataAPI;
import net.minecraft.src.client.gui.GuiConnecting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(GuiConnecting.class)
public class MixinGuiConnecting {
    @Inject(method = "initGui", at = @At("HEAD"))
    public void onInitGui(CallbackInfo ci) {
        SidedMetadataAPI.Internal.setActiveMetaData(Collections.emptyMap());
    }
}
