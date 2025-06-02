package com.fox2code.foxloader.server;

import net.minecraft.common.networking.Packet53BlockChange;
import net.minecraft.common.util.math.MathHelper;
import net.minecraft.server.entity.player.EntityPlayerMP;

public final class NetworkBlockUpdateHelper {
    private NetworkBlockUpdateHelper() {}

    /**
     * Send client side block updates from the server in case a block interaction has been cancelled.
     *
     * @param entityPlayerMP the player
     * @param x modified block x
     * @param y modified block y
     * @param z modified block z
     * @param facing the facing direction or -1 if cancelled event had no facing direction
     */
    public static void sendBlockUpdate(EntityPlayerMP entityPlayerMP, int x, int y, int z, int facing) {
        entityPlayerMP.playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x, y, z, entityPlayerMP.worldObj)
        );
        int rot = MathHelper.floor_double((entityPlayerMP.rotationYaw * 4.0D / 360.0D) + 0.5D) & 3;
        byte xoffs = 0;
        byte zoffs = 0;
        int ignoreFacing;
        switch (rot) {
            case 0:
                zoffs = 1;
                ignoreFacing = 3;
                break;
            case 1:
                xoffs = -1;
                ignoreFacing = 4;
                break;
            case 2:
                zoffs = -1;
                ignoreFacing = 2;
                break;
            case 3:
                xoffs = 1;
                ignoreFacing = 5;
                break;
            default:
                assert false;
                return;
        }
        entityPlayerMP.playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x + xoffs, y, z + zoffs, entityPlayerMP.worldObj));
        if (facing == -1 || facing == ignoreFacing) {
            return;
        }
        switch(facing) {
            case 0:
                --y;
                break;
            case 1:
                ++y;
                break;
            case 2:
                --z;
                break;
            case 3:
                ++z;
                break;
            case 4:
                --x;
                break;
            case 5:
                ++x;
                break;
            default:
                assert false;
                return;
        }
        entityPlayerMP.playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x, y, z, entityPlayerMP.worldObj));
    }
}
