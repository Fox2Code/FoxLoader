package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.game.level.chunk.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(SaveHandler.class)
public interface AcessorSaveHandlers {
    @Accessor
    File getSaveDirectory();
}
