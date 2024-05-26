package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.client.gui.GuiEditSign;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiEditSign.class)
public class MixinGuiEditSign {
    @Redirect(method = "onGuiClosed", at = @At(value = "FIELD",
            target = "Lnet/minecraft/src/game/level/World;multiplayerWorld:Z"))
    public boolean hotfix_multiplayerWorld(World instance) {
        return instance != null && instance.multiplayerWorld;
    }
}
