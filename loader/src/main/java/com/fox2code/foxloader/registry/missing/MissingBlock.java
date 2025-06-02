package com.fox2code.foxloader.registry.missing;

import net.minecraft.common.block.Block;
import net.minecraft.common.block.data.Materials;
import net.minecraft.common.block.sound.StepSounds;
import net.minecraft.common.item.block.ItemBlock;

public class MissingBlock extends Block {
    public MissingBlock(String name) {
        super(name, Materials.ROCK);
        this.addDescription(new MissingItemDesc());
        this.setBlockUnbreakable();
        this.setResistance(6000000.0F);
        this.setSound(StepSounds.SOUND_STONE);
        this.disableStats();
        this.hideFromCreativeMenu();
    }

    @Override
    protected void allocateTextures() {
        super.allocateTextures();
    }

    @Override
    protected ItemBlock initializeItemBlock() {
        return new MissingItemBlock(this);
    }
}
