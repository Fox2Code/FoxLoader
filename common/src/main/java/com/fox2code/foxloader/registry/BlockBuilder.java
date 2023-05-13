package com.fox2code.foxloader.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public final class BlockBuilder {
    public Class<? extends RegisteredBlock> gameBlockSource;
    @Nullable
    public ItemBuilder itemBuilder;
    @NotNull
    public GameRegistry.BuiltInMaterial builtInMaterial =
            GameRegistry.BuiltInMaterial.ROCK;
    @NotNull
    public GameRegistry.BuiltInStepSounds builtInStepSounds =
            GameRegistry.BuiltInStepSounds.POWDER;
    @NotNull
    public GameRegistry.BuiltInBlockType builtInBlockType =
            GameRegistry.BuiltInBlockType.BLOCK;
    @Nullable
    public String blockName;
    @Nullable
    public String blockType;
    @Nullable
    public RegisteredBlock blockSource;
    public float blockHardness;
    public float blockResistance;
    public int blockBurnTime;
    public byte blockBurnType;
    public int chanceToEncourageFire;
    public int abilityToCatchFire;
    public int tooltipColor;
    public byte effectiveToolBit;

    public BlockBuilder() {}

    public BlockBuilder setGameBlockSource(Class<? extends RegisteredBlock> gameBlockSource) {
        this.builtInBlockType = GameRegistry.BuiltInBlockType.CUSTOM;
        this.gameBlockSource = gameBlockSource;
        return this;
    }

    public BlockBuilder setItemBlock(@Nullable ItemBuilder itemBuilder) {
        this.itemBuilder = itemBuilder;
        return this;
    }

    public BlockBuilder setBlockType(@NotNull GameRegistry.BuiltInBlockType builtInBlockType) {
        this.builtInBlockType = builtInBlockType;
        this.gameBlockSource = null;
        return this;
    }

    public BlockBuilder setBlockMaterial(@NotNull GameRegistry.BuiltInMaterial builtInMaterial) {
        this.builtInMaterial = builtInMaterial;
        return this;
    }

    public BlockBuilder setBlockStepSounds(@NotNull GameRegistry.BuiltInStepSounds builtInStepSounds) {
        this.builtInStepSounds = builtInStepSounds;
        return this;
    }

    public BlockBuilder setBlockName(@Nullable String blockName) {
        this.blockName = blockName;
        return this;
    }

    public BlockBuilder setBlockType(@Nullable String blockType) {
        this.blockType = blockType;
        return this;
    }

    public BlockBuilder setBlockSource(@Nullable RegisteredBlock blockSource) {
        this.blockSource = blockSource;
        return this;
    }

    public BlockBuilder setBlockHardness(float blockHardness) {
        this.blockHardness = blockHardness;
        return this;
    }

    public BlockBuilder setBlockResistance(float blockResistance) {
        this.blockResistance = blockResistance;
        return this;
    }

    public BlockBuilder setBurnTime(int num) {
        this.blockBurnTime = num;
        this.blockBurnType = 1;
        return this;
    }

    public BlockBuilder setFreezeTime(int num) {
        this.blockBurnTime = num;
        this.blockBurnType = 2;
        return this;
    }

    public BlockBuilder setBurnRate(
            int chanceToEncourageFire, int abilityToCatchFire) {
        this.chanceToEncourageFire = chanceToEncourageFire;
        this.abilityToCatchFire = abilityToCatchFire;
        return this;
    }

    public BlockBuilder setTooltipColor(int color) {
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        this.tooltipColor = color;
        return this;
    }

    public BlockBuilder setTooltipColor(Color color) {
        return this.setTooltipColor(color.getRGB());
    }

    public BlockBuilder setEffectiveTool(RegisteredToolType registeredToolType) {
        this.effectiveToolBit |= (byte)(this.effectiveToolBit | 1 << registeredToolType.ordinal());
        return this;
    }

    public BlockBuilder setEffectiveTools(RegisteredToolType... registeredToolTypes) {
        for (RegisteredToolType registeredToolType : registeredToolTypes) {
            this.setEffectiveTool(registeredToolType);
        }
        return this;
    }
}
