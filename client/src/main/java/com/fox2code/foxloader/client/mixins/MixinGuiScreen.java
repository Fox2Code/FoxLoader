package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.config.NoConfigObject;
import net.minecraft.src.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiScreen.class)
public class MixinGuiScreen implements NoConfigObject {
}
