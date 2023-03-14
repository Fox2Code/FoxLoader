package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredBlock;
import com.fox2code.foxloader.registry.RegisteredItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.fox2code.foxloader.server.registry.RegisteredBlockImpl;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class MixinBlock implements RegisteredBlock, RegisteredBlockImpl {
    @Shadow
    public abstract int getBlockID();
    @Shadow @Final
    public int blockID;

    @Shadow protected abstract Block setHardness(float hardness);
    @Shadow protected abstract Block setResistance(float resistance);

    @Override
    public RegisteredItem asRegisteredItem() {
        return (RegisteredItem) Item.itemsList[this.getBlockID()];
    }

    @Override
    public RegisteredItemStack newRegisteredItemStack() {
        return (RegisteredItemStack) (Object) new ItemStack((Block) (Object) this);
    }

    @Override
    public int getRegisteredBlockId() {
        return this.blockID;
    }

    @Override
    public void setRegisteredHardness(float hardness) {
        this.setHardness(hardness);
    }

    @Override
    public void setRegisteredResistance(float resistance) {
        this.setResistance(resistance);
    }
}
