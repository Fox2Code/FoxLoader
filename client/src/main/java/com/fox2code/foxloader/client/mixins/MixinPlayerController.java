package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.src.client.player.PlayerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerController.class)
public abstract class MixinPlayerController implements NetworkPlayer.NetworkPlayerController {
    @Shadow public abstract boolean isInCreativeMode();

    @Override
    public boolean hasCreativeModeRegistered() {
        return this.isInCreativeMode();
    }

    @Override
    public boolean hasSelection() {
        return false;
    }

    public int getMinX() {
        return 0;
    }

    public int getMaxX() {
        return 0;
    }

    public int getMinY() {
        return 0;
    }

    public int getMaxY() {
        return 0;
    }

    public int getMinZ() {
        return 0;
    }

    public int getMaxZ() {
        return 0;
    }
}
