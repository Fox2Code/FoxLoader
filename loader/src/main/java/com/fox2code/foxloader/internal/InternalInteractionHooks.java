package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.interaction.*;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.util.math.Vec3D;
import net.minecraft.common.world.World;

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
        return playerUseItemOnAirEvent.isCancelled();
    }

    public static boolean sendPlayerUseItemOnBlockEvent(
            EntityPlayer player, World world, ItemStack itemstack,
            int x, int y, int z, int facing, float xVec, float yVec, float zVec) {
        if (PLAYER_USE_ITEM_ON_BLOCK_EVENT.isEmpty()) return false;
        PlayerUseItemOnBlockEvent playerUseItemOnBlockEvent = new PlayerUseItemOnBlockEvent(
                player, itemstack, x, y, z, facing, xVec, yVec, zVec);
        PLAYER_USE_ITEM_ON_BLOCK_EVENT.callEvent(playerUseItemOnBlockEvent);
        return playerUseItemOnBlockEvent.isCancelled();
    }

    public static boolean sendPlayerUseItemOnEntityEvent(EntityPlayer player, Entity target) {
        if (PLAYER_USE_ITEM_ON_ENTITY_EVENT.isEmpty()) return false;
        PlayerUseItemOnEntityEvent playerUseItemOnEntityEvent =
                new PlayerUseItemOnEntityEvent(player, player.inventory.getCurrentItem(), target);
        PLAYER_USE_ITEM_ON_ENTITY_EVENT.callEvent(playerUseItemOnEntityEvent);
        return playerUseItemOnEntityEvent.isCancelled();
    }

    public static boolean sendPlayerAttackEntityEvent(EntityPlayer player, Entity target) {
        if (PLAYER_ATTACK_ENTITY_EVENT.isEmpty()) return false;
        PlayerAttackEntityEvent playerAttackEntityEvent =
                new PlayerAttackEntityEvent(player, player.inventory.getCurrentItem(), target);
        PLAYER_ATTACK_ENTITY_EVENT.callEvent(playerAttackEntityEvent);
        return playerAttackEntityEvent.isCancelled();
    }
}
