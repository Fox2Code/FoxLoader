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
package com.fox2code.foxloader.selection;

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxloader.event.interaction.PlayerBreakBlockEvent;
import com.fox2code.foxloader.event.interaction.PlayerUseItemOnBlockEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.item.Items;
import net.minecraft.common.util.ChatColors;

import java.util.WeakHashMap;

public final class PlayerSelectionProviderFallback extends PlayerSelectionProvider {
    private final WeakHashMap<EntityPlayer, PlayerSelectionFallback> playerSelections;

    PlayerSelectionProviderFallback() {
        this.playerSelections = new WeakHashMap<>();
    }

    @Override
    public PlayerSelection getPlayerSelection(EntityPlayer entityPlayer) {
        if (entityPlayer.worldObj.isRemote) {
            throw new IllegalArgumentException("Cannot get PlayerSelection of a remote player.");
        }
        return this.playerSelections.computeIfAbsent(entityPlayer, PlayerSelectionFallback::new);
    }

    private PlayerSelectionFallback getPlayerSelectionImpl(EntityPlayer entityPlayer) {
        return this.playerSelections.computeIfAbsent(entityPlayer, PlayerSelectionFallback::new);
    }

    @EventHandler
    public void onBreakBlock(PlayerBreakBlockEvent playerBreakBlockEvent) {
        ItemStack held = playerBreakBlockEvent.getHeldItem();
        EntityPlayer player = playerBreakBlockEvent.getEntitySource();
        if (held == null || held.getItem() != Items.WOOD_AXE ||
                !player.capabilities.isCreativeMode || player.worldObj.isRemote) {
            return;
        }
        PlayerSelectionFallback playerSelection = this.getPlayerSelectionImpl(player);
        int x = playerBreakBlockEvent.getX();
        int y = playerBreakBlockEvent.getY();
        int z = playerBreakBlockEvent.getZ();
        playerSelection.registerSelection(x, y, z, true);
        player.addChatMessage(ChatColors.WHITE + "Pos1: [" + ChatColors.RED + x + ChatColors.WHITE + ", " +
                ChatColors.GREEN + y + ChatColors.WHITE + ", " + ChatColors.AQUA + z + ChatColors.WHITE + "]" +
                ChatColors.GRAY + " (" + playerSelection.getSelectionSize() + " blocks)");
        playerBreakBlockEvent.setCancelled(true);
    }

    @EventHandler
    public void onUseBlock(PlayerUseItemOnBlockEvent playerUseItemOnBlockEvent) {
        ItemStack held = playerUseItemOnBlockEvent.getHeldItem();
        EntityPlayer player = playerUseItemOnBlockEvent.getEntityPlayer();
        if (held == null || held.getItem() != Items.WOOD_AXE ||
                !player.capabilities.isCreativeMode || player.worldObj.isRemote) {
            return;
        }
        PlayerSelectionFallback playerSelection = this.getPlayerSelectionImpl(player);
        int x = playerUseItemOnBlockEvent.getX();
        int y = playerUseItemOnBlockEvent.getY();
        int z = playerUseItemOnBlockEvent.getZ();
        playerSelection.registerSelection(x, y, z, false);
        player.addChatMessage(ChatColors.WHITE + "Pos2: [" + ChatColors.RED + x + ChatColors.WHITE + ", " +
                ChatColors.GREEN + y + ChatColors.WHITE + ", " + ChatColors.AQUA + z + ChatColors.WHITE + "]" +
                ChatColors.GRAY + " (" + playerSelection.getSelectionSize() + " blocks)");
        playerUseItemOnBlockEvent.setCancelled(true);
    }

    private static final class PlayerSelectionFallback extends PlayerSelection {
        private int x1 = 0, x2 = 0, y1 = 0, y2 = 0, z1 = 0, z2 = 0;
        private boolean hasPrimary = false, hasSecondary = false;
        private int selectionDimension = -1;

        PlayerSelectionFallback(EntityPlayer entityPlayer) {
            super(entityPlayer);
        }

        @Override
        public boolean hasSelection() {
            EntityPlayer entityPlayer = this.getPlayer();
            if (entityPlayer == null || !entityPlayer.capabilities.isCreativeMode ||
                    entityPlayer.dimension != this.selectionDimension) {
                return false;
            }
            return this.hasPrimary && this.hasSecondary;
        }

        @Override
        public int getX1() {
            return this.x1;
        }

        @Override
        public int getX2() {
            return this.x2;
        }

        @Override
        public int getY1() {
            return this.y1;
        }

        @Override
        public int getY2() {
            return this.y2;
        }

        @Override
        public int getZ1() {
            return this.z1;
        }

        @Override
        public int getZ2() {
            return this.z2;
        }

        void registerSelection(int x, int y, int z, boolean primary) {
            EntityPlayer entityPlayer = this.getPlayer();
            if (entityPlayer == null) return;
            if (entityPlayer.dimension != this.selectionDimension) {
                this.hasPrimary = false;
                this.hasSecondary = false;
                this.selectionDimension = entityPlayer.dimension;
            }
            if (primary) {
                this.x1 = x;
                this.y1 = y;
                this.z1 = z;
                this.hasPrimary = true;
            } else {
                this.x2 = x;
                this.y2 = y;
                this.z2 = z;
                this.hasSecondary = true;
            }
        }
    }
}
