/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.entity;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.mojang.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.skin.SkinHelper;
import net.minecraft.client.renderer.particle.EntityFXPickup;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.other.EntityItem;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.entity.projectile.EntityThrownArrow;
import net.minecraft.common.networking.Packet18Animation;
import net.minecraft.common.networking.Packet22Collect;
import net.minecraft.common.networking.Packet41UpdateFishingRod;
import net.minecraft.common.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.EntityTracker;

/**
 * Fake player to use player specific methods from mods.
 */
public abstract class EntityFakePlayer extends EntityPlayer {
    public int gamemode = 0;

    public EntityFakePlayer(World world) {
        this(world, null);
    }

    public EntityFakePlayer(World world, String username) {
        super(world);
        this.username = username == null ? "[FakePlayer]" : username;
    }

    @Override
    public void updateSkin() {
        String skinUsername;
        if (FoxLauncher.isClient() && (skinUsername = this.getSkinUsername()) != null) {
            String originalUsername = this.username;
            this.username = skinUsername;
            SkinHelper.applySkin(this);
            this.username = originalUsername;
        }
    }

    @Override
    public void readEntityFromNBT(CompoundTag nbt) {
        super.readEntityFromNBT(nbt);
        this.gamemode = nbt.getInteger("Gamemode");
    }

    @Override
    public void writeEntityToNBT(CompoundTag bt) {
        super.writeEntityToNBT(bt);
        bt.setInteger("Gamemode", this.gamemode);
    }

    @Override
    public void onItemPickup(Entity entity, int arg2) {
        if (this.worldObj.isRemote) {
            // Replicate EntityOtherPlayerMP behaviour
            super.onItemPickup(entity, arg2);
            return;
        }

        if (FoxLauncher.isClient()) {
            // Replicate EntityPlayerSP behaviour
            Minecraft mc = Minecraft.getInstance();
            mc.effectRenderer.addEffect(new EntityFXPickup(mc.theWorld, entity, this, -0.5F));
            return;
        }

        // Replicate EntityPlayerMP behaviour
        if (!entity.isDead) {
            EntityTracker tracker = MinecraftServer.getInstance().getEntityTracker(this.dimension);
            if (entity instanceof EntityItem || entity instanceof EntityThrownArrow) {
                tracker.sendPacketToTrackedPlayers(entity, new Packet22Collect(entity.entityId, this.entityId));
            }
        }

        super.onItemPickup(entity, arg2);
        this.currentContainer.updateInventory();
    }

    @Override
    public void swingItem() {
        this.swingProgressInt = -1;
        this.isSwinging = true;
        if (FoxLauncher.isServer()) {
            MinecraftServer.getInstance().getEntityTracker(this.dimension)
                    .sendPacketToTrackedPlayers(this, new Packet18Animation(this, 1));
        }
    }

    @Override
    public void func_6420_o() {}

    @Override
    public boolean teleportToDim(int dimension, double x, double y, double z, float yaw, float pitch) {
        // Cross dimension teleport not supported.
        if (this.dimension != dimension) {
            return false;
        }
        this.setPositionAndRotation(x, y, z, yaw, pitch);
        return true;
    }

    @Override
    public boolean changeGamemode(int gamemode) {
        if (this.worldObj.isRemote) {
            return super.changeGamemode(gamemode);
        }
        if (this.gamemode == gamemode) {
            return false;
        }
        if (gamemode == 0) {
            this.capabilities.allowFlying = false;
            this.capabilities.isFlying = false;
            this.capabilities.isCreativeMode = false;
            this.capabilities.disableDamage = false;
        } else {
            this.capabilities.allowFlying = true;
            this.capabilities.isCreativeMode = true;
            this.capabilities.disableDamage = true;
        }
        this.gamemode = gamemode;
        return true;
    }

    @Override
    public void updateFishingRod() {
        super.updateFishingRod();
        if (FoxLauncher.isServer()) {
            MinecraftServer.getInstance().configManager.sendPacketToAllPlayers(
                    new Packet41UpdateFishingRod(this.entityId));
        }
    }

    @Override
    public int getPlayerDataGamemode() {
        return this.gamemode;
    }

    public String getSkinUsername() {
        return null;
    }
}
