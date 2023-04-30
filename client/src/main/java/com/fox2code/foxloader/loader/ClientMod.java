package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.src.game.item.ItemStack;

public interface ClientMod extends Mod.SidedMod {
    static Minecraft getGameInstance() {
        return Minecraft.getInstance();
    }

    static NetworkPlayer getLocalNetworkPlayer() {
        return (NetworkPlayer) getGameInstance().thePlayer;
    }

    @SuppressWarnings("DataFlowIssue")
    static ItemStack toItemStack(RegisteredItemStack registeredItemStack) {
        return (ItemStack) (Object) registeredItemStack;
    }

    static RegisteredItemStack toRegisteredItemStack(ItemStack registeredItemStack) {
        return (RegisteredItemStack) (Object) registeredItemStack;
    }


}
