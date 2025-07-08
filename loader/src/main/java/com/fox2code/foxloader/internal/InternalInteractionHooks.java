/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
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
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.interaction.*;
import com.fox2code.foxloader.event.inventory.PlayerClickSlotEvent;
import com.fox2code.foxloader.event.inventory.PlayerDropItemEvent;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.block.container.Slot;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.entity.player.InventoryPlayer;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.networking.Packet103SetSlot;
import net.minecraft.common.util.math.Vec3D;
import net.minecraft.common.world.World;
import net.minecraft.server.entity.player.EntityPlayerMP;

/**
 * Internal hook for player network event and interactions.
 *
 * @see net.minecraft.common.entity.player.EntityPlayer
 * @see net.minecraft.client.player.PlayerController
 * @see net.minecraft.server.entity.player.PlayerController
 */
public final class InternalInteractionHooks {
    private static final EventHolder<PlayerStartBreakBlockEvent> PLAYER_START_BREAK_BLOCK_EVENT =
            EventHolder.getHolderFromEvent(PlayerStartBreakBlockEvent.class);
    private static final EventHolder<PlayerBreakBlockEvent> PLAYER_BREAK_BLOCK_EVENT =
            EventHolder.getHolderFromEvent(PlayerBreakBlockEvent.class);
    private static final EventHolder<PlayerUseItemOnBlockEvent> PLAYER_USE_ITEM_ON_BLOCK_EVENT =
            EventHolder.getHolderFromEvent(PlayerUseItemOnBlockEvent.class);
    private static final EventHolder<PlayerUseItemOnAirEvent> PLAYER_USE_ITEM_ON_AIR_EVENT =
            EventHolder.getHolderFromEvent(PlayerUseItemOnAirEvent.class);
    private static final EventHolder<PlayerUseItemOnEntityEvent> PLAYER_USE_ITEM_ON_ENTITY_EVENT =
            EventHolder.getHolderFromEvent(PlayerUseItemOnEntityEvent.class);
    private static final EventHolder<PlayerAttackEntityEvent> PLAYER_ATTACK_ENTITY_EVENT =
            EventHolder.getHolderFromEvent(PlayerAttackEntityEvent.class);
    private static final EventHolder<PlayerClickSlotEvent> PLAYER_CLICK_SLOT_EVENT =
            EventHolder.getHolderFromEvent(PlayerClickSlotEvent.class);
    private static final EventHolder<PlayerDropItemEvent> PLAYER_DROP_ITEM_EVENT =
            EventHolder.getHolderFromEvent(PlayerDropItemEvent.class);

    public static boolean sendPlayerStartBreakBlockEvent(
            EntityPlayer entityPlayer, int x, int y, int z, int facing) {
        if (PLAYER_START_BREAK_BLOCK_EVENT.isEmpty()) return false;
        PlayerStartBreakBlockEvent playerStartBreakBlockEvent = new PlayerStartBreakBlockEvent(
                entityPlayer, x, y, z, facing, entityPlayer.inventory.getCurrentItem());
        PLAYER_START_BREAK_BLOCK_EVENT.callEvent(playerStartBreakBlockEvent);
        return playerStartBreakBlockEvent.isCancelled();
    }

    public static boolean sendPlayerBreakBlockEvent(
            EntityPlayer entityPlayer, int x, int y, int z) {
        if (PLAYER_BREAK_BLOCK_EVENT.isEmpty()) return false;
        PlayerBreakBlockEvent playerStartBreakBlockEvent = new PlayerBreakBlockEvent(
                entityPlayer, x, y, z, entityPlayer.inventory.getCurrentItem());
        PLAYER_BREAK_BLOCK_EVENT.callEvent(playerStartBreakBlockEvent);
        updateSlotItemIfCancelled(playerStartBreakBlockEvent);
        return playerStartBreakBlockEvent.isCancelled();
    }

    public static boolean sendPlayerUseItemOnBlockEvent(
            EntityPlayer player, World world, ItemStack itemstack,
            int x, int y, int z, int facing, Vec3D vec3d) {
        return sendPlayerUseItemOnBlockEvent(player, world, itemstack, x, y, z, facing,
                (float) vec3d.xCoord, (float) vec3d.yCoord, (float) vec3d.zCoord);
    }

    public static boolean sendPlayerUseItemOnAirEvent(
            EntityPlayer player, World world, ItemStack itemStack) {
        if (PLAYER_USE_ITEM_ON_AIR_EVENT.isEmpty()) return false;
        PlayerUseItemOnAirEvent playerUseItemOnAirEvent =
                new PlayerUseItemOnAirEvent(player, itemStack);
        PLAYER_USE_ITEM_ON_AIR_EVENT.callEvent(playerUseItemOnAirEvent);
        updateSlotItemIfCancelled(playerUseItemOnAirEvent);
        return playerUseItemOnAirEvent.isCancelled();
    }

    public static boolean sendPlayerUseItemOnBlockEvent(
            EntityPlayer player, World world, ItemStack itemstack,
            int x, int y, int z, int facing, float xVec, float yVec, float zVec) {
        if (PLAYER_USE_ITEM_ON_BLOCK_EVENT.isEmpty()) return false;
        PlayerUseItemOnBlockEvent playerUseItemOnBlockEvent = new PlayerUseItemOnBlockEvent(
                player, itemstack, x, y, z, facing, xVec, yVec, zVec);
        PLAYER_USE_ITEM_ON_BLOCK_EVENT.callEvent(playerUseItemOnBlockEvent);
        updateSlotItemIfCancelled(playerUseItemOnBlockEvent);
        return playerUseItemOnBlockEvent.isCancelled();
    }

    public static boolean sendPlayerUseItemOnEntityEvent(EntityPlayer player, Entity target) {
        if (PLAYER_USE_ITEM_ON_ENTITY_EVENT.isEmpty()) return false;
        PlayerUseItemOnEntityEvent playerUseItemOnEntityEvent =
                new PlayerUseItemOnEntityEvent(player, player.inventory.getCurrentItem(), target);
        PLAYER_USE_ITEM_ON_ENTITY_EVENT.callEvent(playerUseItemOnEntityEvent);
        updateSlotItemIfCancelled(playerUseItemOnEntityEvent);
        return playerUseItemOnEntityEvent.isCancelled();
    }

    public static boolean sendPlayerAttackEntityEvent(EntityPlayer player, Entity target) {
        if (PLAYER_ATTACK_ENTITY_EVENT.isEmpty()) return false;
        PlayerAttackEntityEvent playerAttackEntityEvent =
                new PlayerAttackEntityEvent(player, player.inventory.getCurrentItem(), target);
        PLAYER_ATTACK_ENTITY_EVENT.callEvent(playerAttackEntityEvent);
        player.setSprinting(!playerAttackEntityEvent.isCancelled() &&
                playerAttackEntityEvent.getUseExtraKnockback());
        return playerAttackEntityEvent.isCancelled();
    }

    public static boolean onHandleClickSlot(Container container, int slotId, int mouseButton,
                                            int transferType, EntityPlayer player) {
        if (!PLAYER_CLICK_SLOT_EVENT.isEmpty()) {
            PlayerClickSlotEvent playerClickSlotEvent = new PlayerClickSlotEvent(
                    container, slotId, mouseButton, transferType, player);
            PLAYER_CLICK_SLOT_EVENT.callEvent(playerClickSlotEvent);
            if (playerClickSlotEvent.isCancelled()) {
                return true;
            }
        }
        // Handle dropping manually there.
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) {
            return false;
        }
        if ((transferType == 0 || transferType == 1) && (mouseButton == 0 || mouseButton == 1) &&
                slotId == -999 && player.inventory.getCursorStack() != null) {
            return mouseButton == 0 ? onDropCursorItemStack(player) : onDropCursorItem(player);
        }
        Slot slot;
        ItemStack slotStack;
        if (transferType == 3 && (mouseButton == 0 || mouseButton == 1) && slotId >= 0 &&
                slotId < container.slots.size() && (slot = container.getSlot(slotId)) != null &&
                (slotStack = slot.getStack()) != null) {
            PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                    player, slotStack, slotId, mouseButton == 0 ? 64 : 1, false, false);
            PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
            updateSlotItemIfCancelled(playerDropItemEvent);
            return playerDropItemEvent.isCancelled();
        }
        return false;
    }

    public static boolean onDropCurrentItem(EntityPlayer player) {
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) return false;
        ItemStack currentItem = player.inventory.getCurrentItem();
        if (currentItem == null) return false;
        PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                player, currentItem, player.inventory.currentItem, 1, false, true);
        PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
        updateSlotItemIfCancelled(playerDropItemEvent);
        return playerDropItemEvent.isCancelled();
    }

    public static boolean onDropCurrentItemStack(EntityPlayer player) {
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) return false;
        ItemStack currentItem = player.inventory.getCurrentItem();
        if (currentItem == null) return false;
        PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                player, currentItem, player.inventory.currentItem, 64, false, true);
        PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
        updateSlotItemIfCancelled(playerDropItemEvent);
        return playerDropItemEvent.isCancelled();
    }

    public static boolean onDropCursorItem(EntityPlayer player) {
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) return false;
        ItemStack cursorItem = player.inventory.getCursorStack();
        if (cursorItem == null) return false;
        PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                player, cursorItem, -999, 1, false, false);
        PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
        updateSlotItemIfCancelled(playerDropItemEvent);
        return playerDropItemEvent.isCancelled();
    }

    public static boolean onDropCursorItemStack(EntityPlayer player) {
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) return false;
        ItemStack cursorItem = player.inventory.getCursorStack();
        if (cursorItem == null) return false;
        PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                player, cursorItem, -999, 64, false, false);
        PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
        updateSlotItemIfCancelled(playerDropItemEvent);
        return playerDropItemEvent.isCancelled();
    }

    public static boolean onDropCreativeItemStack(EntityPlayer player, ItemStack itemStack) {
        if (PLAYER_DROP_ITEM_EVENT.isEmpty()) return false;
        if (itemStack == null) return false;
        PlayerDropItemEvent playerDropItemEvent = new PlayerDropItemEvent(
                player, itemStack, -999, 64, true, false);
        PLAYER_DROP_ITEM_EVENT.callEvent(playerDropItemEvent);
        return playerDropItemEvent.isCancelled();
    }

    private static void updateSlotItemIfCancelled(PlayerUseItemEvent playerUseItemEvent) {
        if (playerUseItemEvent.isCancelled() && playerUseItemEvent.getHeldItem() != null) {
            updateHotbarSlotItem(playerUseItemEvent.getEntityPlayer(), playerUseItemEvent.getHeldItem(),
                    playerUseItemEvent.getEntityPlayer().inventory.currentItem);
        }
    }

    private static void updateSlotItemIfCancelled(PlayerBreakBlockEvent playerBreakBlockEvent) {
        if (playerBreakBlockEvent.isCancelled()) {
            InventoryPlayer inventoryPlayer = playerBreakBlockEvent.getEntitySource().inventory;
            updateHotbarSlotItem(playerBreakBlockEvent.getEntitySource(),
                    inventoryPlayer.getCurrentItem(), inventoryPlayer.currentItem);
        }
    }

    private static void updateSlotItemIfCancelled(PlayerDropItemEvent playerDropItemEvent) {
        if (playerDropItemEvent.isCancelled()) {
            if (playerDropItemEvent.isHotbar()) {
                updateHotbarSlotItem(playerDropItemEvent.getEntityPlayer(),
                        playerDropItemEvent.getItemToDrop(), playerDropItemEvent.getSlotId());
            } else {
                updateSlotItem(playerDropItemEvent.getEntityPlayer(),
                        playerDropItemEvent.getItemToDrop(), playerDropItemEvent.getSlotId());
            }
        }
    }

    private static void updateSlotItem(EntityPlayer entityPlayer, ItemStack itemStack, int slotId) {
        if (entityPlayer instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entityPlayer).playerNetServerHandler.sendPacket(slotId == -999 ?
                    new Packet103SetSlot(-1, -1, entityPlayer.inventory.getCursorStack()) :
                    new Packet103SetSlot(((EntityPlayerMP) entityPlayer).currentWindowId, slotId, itemStack));
        }
    }

    private static void updateHotbarSlotItem(EntityPlayer entityPlayer, ItemStack itemStack, int slotId) {
        if (entityPlayer instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entityPlayer).playerNetServerHandler.sendPacket(slotId == -999 ?
                    new Packet103SetSlot(-1, -1, entityPlayer.inventory.getCursorStack()) :
                    new Packet103SetSlot(0, slotId < 36 ? 36 + slotId : slotId, itemStack));
        }
    }
}
