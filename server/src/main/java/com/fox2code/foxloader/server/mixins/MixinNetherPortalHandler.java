package com.fox2code.foxloader.server.mixins;

import net.minecraft.src.game.MathHelper;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.level.NetherPortalHandler;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetherPortalHandler.class)
public class MixinNetherPortalHandler {
    /**
     * Backport 2.9 NetherPortalHandler to 2.8.1
     *
     * @author Fox2Code
     * @reason Just a hotfix
     * @param entity entity
     * @param world world
     * @return boolean
     */
    @Overwrite
    public boolean useExistingPortal(World world, Entity entity) {
        short size = 0;
        if (world.worldProvider != null) {
            size = (short) (world.worldProvider.isHellWorld ? 16 : 128);
        }
        byte ysize = 64;
        double tol = -1.0D;
        int clsx = 0;
        int clsy = 0;
        int clsz = 0;
        int xpos = MathHelper.floor_double(entity.posX);
        int ypos = MathHelper.floor_double(entity.posY);
        int zpos = MathHelper.floor_double(entity.posZ);
        double cposy;
        for (int xiter = xpos - size; xiter <= xpos + size; ++xiter) {
            double cposx = (double) xiter + 0.5D - entity.posX;

            for (int ziter = zpos - size; ziter <= zpos + size; ++ziter) {
                double cposz = (double) ziter + 0.5D - entity.posZ;

                for (int yiter = ypos + ysize; yiter > ypos - ysize; yiter -= 3) {
                    if (world.getBlockId(xiter, yiter, ziter) == Block.portal.blockID) {
                        while (world.getBlockId(xiter, yiter - 1, ziter) == Block.portal.blockID) {
                            --yiter;
                        }

                        cposy = (double) yiter + 0.5D - entity.posY;
                        double distance = cposx * cposx + cposy * cposy + cposz * cposz;
                        if (tol < 0.0D || distance < tol) {
                            tol = distance;
                            clsx = xiter;
                            clsy = yiter;
                            clsz = ziter;
                            break;
                        }
                    }
                }
            }
        }

        if (tol >= 0.0D) {
            double closestx = (double) clsx + 0.5D;
            double closesty = (double) clsy + 0.5D;
            cposy = (double) clsz + 0.5D;//this variable shall now be nknown as closestz
            if (world.getBlockId(clsx - 1, clsy, clsz) == Block.portal.blockID) {
                closestx -= 0.5D;
            }

            if (world.getBlockId(clsx + 1, clsy, clsz) == Block.portal.blockID) {
                closestx += 0.5D;
            }

            if (world.getBlockId(clsx, clsy, clsz - 1) == Block.portal.blockID) {
                cposy -= 0.5D;
            }

            if (world.getBlockId(clsx, clsy, clsz + 1) == Block.portal.blockID) {
                cposy += 0.5D;
            }

            entity.setLocationAndAngles(closestx, closesty, cposy, entity.rotationYaw, 0.0F);
            entity.motionX = entity.motionY = entity.motionZ = 0.0D;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Backport 2.9 NetherPortalHandler to 2.8.1
     *
     * @author Fox2Code
     * @reason Just a hotfix
     * @param entity entity
     * @param world world
     * @return boolean
     */
    @Overwrite
    public boolean makeNewPortal(World world, Entity entity) {
        byte size = 16;
        double distance = -1.0D;
        int cenx = MathHelper.floor_double(entity.posX);
        int ceny = MathHelper.floor_double(entity.posY);
        int cenz = MathHelper.floor_double(entity.posZ);
        int closestx = cenx;
        int closesty = ceny;
        int closestz = cenz;
        int rvidfk = 0;
        int rval = world.rand.nextInt(4);

        int xiter;
        double cposx;
        int ziter;
        double cposz;
        int yiter;
        int rvtr;
        int rvtrodd;
        int rvtreven;
        int acx;
        int acz;
        int acy;
        int srx;
        int sry;
        double cposy;
        double cdist;
        int fcount = 0;
        int ccount = 0;
        for (xiter = cenx - size; xiter <= cenx + size; ++xiter) {//preliminary scan for good terrain
            cposx = (double) xiter + 0.5D - entity.posX;

            for (ziter = cenz - size; ziter <= cenz + size; ++ziter) {
                cposz = (double) ziter + 0.5D - entity.posZ;

                reattemptPreliminary:
                for (yiter = ceny; yiter <= ceny + size; yiter++) {
                    if (fcount > 100) {
                        break;
                    }
                    if (world.isAirBlock(xiter, yiter, ziter)) {
                        while (yiter > ceny - size - 16 && world.isAirBlock(xiter, yiter - 1, ziter)) {
                            --yiter;
                            fcount++;
                        }

                        for (rvtr = rval; rvtr < rval + 4; ++rvtr) {
                            rvtrodd = rvtr % 2;
                            rvtreven = 1 - rvtrodd;
                            if (rvtr % 4 >= 2) {
                                rvtrodd = -rvtrodd;
                                rvtreven = -rvtreven;
                            }
                            for (acx = 0; acx < 4; ++acx) {
                                for (acz = 0; acz < 4; ++acz) {
                                    for (acy = -1; acy < 4; ++acy) {
                                        srx = xiter + (acz - 1) * rvtrodd + acx * rvtreven;
                                        sry = yiter + acy;
                                        int srz = ziter + (acz - 1) * rvtreven - acx * rvtrodd;
                                        if ((acy < 0 && !world.getBlockMaterial(srx, sry, srz)
                                                .isSolid()) || (acy > -1 && !world.isAirBlock(srx, sry, srz))) {
                                            fcount++;
                                            //if (fcount<10) {
                                            continue reattemptPreliminary;
                                            //}
                                        }
                                    }
                                }
                            }
                            ccount++;
                            cposy = (double) yiter + 0.5D - entity.posY;
                            cdist = cposx * cposx + cposy * cposy + cposz * cposz;
                            if (distance < 0.0D || cdist < distance) {
                                distance = cdist;
                                closestx = xiter;
                                closesty = yiter;
                                closestz = ziter;
                                rvidfk = rvtr % 4;
                            }
                        }
                    } else {
                        fcount++;
                        break;
                    }
                }
            }
        }
        if (fcount > 100 && ccount == 0) {
            distance = -1.0D;
        }

        int locx = closestx;
        int locy = closesty;
        ziter = closestz;
        int rvidfkodd = rvidfk % 2;
        int rvidfkeven = rvidfkodd ^ 1;
        if (rvidfk % 4 >= 2) {
            rvidfkodd = -rvidfkodd;
            rvidfkeven = -rvidfkeven;
        }
        int md = rvidfkeven + 1;

        boolean obi;
        if (distance < 0.0D) {
            locx = cenx;
            ziter = cenz;
            if (closesty < 70) {
                closesty = 70;
            }

            locy = closesty;

            for (yiter = -1; yiter <= 1; ++yiter) {
                for (rvtr = 1; rvtr < 3; ++rvtr) {
                    for (rvtrodd = -1; rvtrodd < 3; ++rvtrodd) {
                        rvtreven = locx + (rvtr - 1) * rvidfkodd + yiter * rvidfkeven;
                        acx = locy + rvtrodd;
                        acz = ziter + (rvtr - 1) * rvidfkeven - yiter * rvidfkodd;
                        obi = rvtrodd < 0;
                        world.setBlockWithNotify(rvtreven, acx, acz, obi ? Block.obsidian.blockID : 0);
                    }
                }
            }
        }

        for (yiter = 0; yiter < 4; ++yiter) {
            world.editingBlocks = true;

            for (rvtr = 0; rvtr < 4; ++rvtr) {
                for (rvtrodd = -1; rvtrodd < 4; ++rvtrodd) {
                    rvtreven = locx + (rvtr - 1) * rvidfkodd;
                    acx = locy + rvtrodd;
                    acz = ziter + (rvtr - 1) * rvidfkeven;
                    obi = rvtr == 0 || rvtr == 3 || rvtrodd == -1 || rvtrodd == 3;
                    if (obi) {
                        world.setBlockWithNotify(rvtreven, acx, acz, Block.obsidian.blockID);
                    } else {
                        world.setBlockAndMetadataWithNotify(rvtreven, acx, acz, Block.portal.blockID, md);
                    }
                }
            }

            world.editingBlocks = false;

            for (rvtr = 0; rvtr < 4; ++rvtr) {
                for (rvtrodd = -1; rvtrodd < 4; ++rvtrodd) {
                    rvtreven = locx + (rvtr - 1) * rvidfkodd;
                    acx = locy + rvtrodd;
                    acz = ziter + (rvtr - 1) * rvidfkeven;
                    world.notifyBlocksOfNeighborChange(rvtreven, acx, acz, world.getBlockId(rvtreven, acx, acz));
                }
            }
        }

        return true;
    }
}
