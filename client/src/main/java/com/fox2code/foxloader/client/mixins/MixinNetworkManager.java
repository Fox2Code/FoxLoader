package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.client.packets.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @ModifyConstant(method = "processReadPackets", constant = @Constant(intValue = 100))
    private int hotfix_processReadPackets(int value) {
        return Integer.MAX_VALUE;
    }
}
