package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredBlock;
import com.fox2code.foxloader.registry.RegisteredTileEntity;
import com.fox2code.foxloader.registry.RegisteredWorld;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.block.tileentity.TileEntity;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntity.class)
public abstract class MixinTileEntity implements RegisteredTileEntity {
    @Shadow public World worldObj;
    @Shadow public int xCoord;
    @Shadow public int yCoord;
    @Shadow public int zCoord;

    @Shadow public abstract Block getBlockType();

    @Override
    public RegisteredWorld getCurrentRegisteredWorld() {
        return (RegisteredWorld) this.worldObj;
    }

    @Override
    public int getRegisteredX() {
        return this.xCoord;
    }

    @Override
    public int getRegisteredY() {
        return this.yCoord;
    }

    @Override
    public int getRegisteredZ() {
        return this.zCoord;
    }

    @Override
    public RegisteredBlock getRegisteredBlock() {
        return (RegisteredBlock) this.getBlockType();
    }
}
