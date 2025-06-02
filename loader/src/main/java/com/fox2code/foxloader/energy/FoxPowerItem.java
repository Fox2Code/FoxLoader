package com.fox2code.foxloader.energy;

import net.minecraft.common.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface FoxPowerItem {
    long getMaxFoxPowerStorage(ItemStack itemStack);

    long getStoredFoxPower(ItemStack itemStack);

    long getFoxPowerStorageStep(ItemStack itemStack);

    long getFoxPowerStorageMaxOutput(ItemStack itemStack);

    long getFoxPowerStorageMaxInput(ItemStack itemStack);

    long drainFoxPower(ItemStack itemStack, long amount);

    long sendFoxPower(ItemStack itemStack, long amount);

    @NotNull
    FoxPowerType getFoxPowerType();
}
