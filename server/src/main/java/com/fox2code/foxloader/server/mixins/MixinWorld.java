package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ServerMod;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.*;
import net.minecraft.src.game.block.tileentity.TileEntity;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.other.EntityItem;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.level.IChunkProvider;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ConcurrentModificationException;
import java.util.List;

@Mixin(World.class)
public abstract class MixinWorld implements RegisteredWorld {
    @Shadow public List<EntityPlayer> playerEntities;
    @Shadow public List<TileEntity> loadedTileEntityList;
    @Shadow public List<Entity> loadedEntityList;

    @Shadow public abstract int getBlockId(int x, int y, int z);
    @Shadow public abstract int getBlockMetadata(int xCoord, int yCoord, int zCoord);
    @Shadow public abstract boolean setBlockAndMetadataWithNotify(int xCoord, int yCoord, int zCoord, int block, int metadata);
    @Shadow public abstract boolean entityJoinedWorld(Entity entity);

    @Override
    public boolean hasRegisteredControl() {
        return true;
    }

    @Override
    public int getRegisteredBlockId(int xCoord, int yCoord, int zCoord) {
        return this.getBlockId(xCoord, yCoord, zCoord);
    }

    @Override
    public int getRegisteredBlockMetadata(int xCoord, int yCoord, int zCoord) {
        return this.getBlockMetadata(xCoord, yCoord, zCoord);
    }

    @Override
    public void setRegisteredBlockAndMetadataWithNotify(int xCoord, int yCoord, int zCoord, int block, int metadata) {
        this.setBlockAndMetadataWithNotify(xCoord, yCoord, zCoord, block, metadata);
    }

    @Override
    public void forceSetRegisteredBlockAndMetadataWithNotify(int xCoord, int yCoord, int zCoord, int block, int metadata) {
        this.setBlockAndMetadataWithNotify(xCoord, yCoord, zCoord, block, metadata);
    }

    @Override
    public RegisteredEntityItem spawnRegisteredEntityItem(double x, double y, double z, RegisteredItemStack registeredItemStack) {
        EntityItem entityItem = new EntityItem((World) (Object) this,
                x, y, z, ServerMod.toItemStack(registeredItemStack));
        if (!this.entityJoinedWorld(entityItem)) {
            return null;
        }
        return (RegisteredEntityItem) entityItem;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends RegisteredEntity> getRegisteredEntities() {
        return (List<? extends RegisteredEntity>) (Object) this.loadedEntityList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends RegisteredTileEntity> getRegisteredTileEntities() {
        return (List<? extends RegisteredTileEntity>) (Object) this.loadedTileEntityList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends NetworkPlayer> getRegisteredNetworkPlayers() {
        return (List<? extends NetworkPlayer>) (Object) this.playerEntities;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/level/IChunkProvider;unload100OldestChunks()Z"))
    public boolean hotfix_asyncKick(IChunkProvider instance) {
        try {
            return instance.unload100OldestChunks();
        } catch (ConcurrentModificationException e) {
            return false;
        }
    }
}
