package com.fox2code.foxloader.client.mixins;

import net.minecraft.src.client.packets.CompressedStreamTools;
import net.minecraft.src.game.level.World;
import net.minecraft.src.game.level.chunk.Chunk;
import net.minecraft.src.game.level.chunk.ChunkLoader;
import net.minecraft.src.game.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static net.minecraft.src.game.level.chunk.ChunkLoader.loadChunkIntoWorldFromCompound;

@Mixin(ChunkLoader.class)
public abstract class MixinChunkLoader {
    @Shadow
    private File saveDir;
    @Shadow
    private boolean createIfNecessary;

    @Overwrite
    private File chunkFileForXZ(int arg1, int arg2) {
        String var3 = "c." + Integer.toString(arg1, 36) + "." + Integer.toString(arg2, 36) + ".dat";
        String var4 = Integer.toString(arg1 & 63, 36);
        String var5 = Integer.toString(arg2 & 63, 36);
        File file = new File(this.saveDir, var4);
        if (!file.exists()) {
            if (!this.createIfNecessary) {
                return null;
            }

            file.mkdir();
        }

        file = new File(file, var5);
        if (!file.exists()) {
            if (!this.createIfNecessary) {
                return null;
            }

            file.mkdir();
        }

        file = new File(file, var3);
        return !file.exists() && !this.createIfNecessary ? null : file;
    }

    @Overwrite
    public Chunk loadChunk(World world, int x, int y, int z) throws IOException {
        File file = this.chunkFileForXZ(x, z);
        if (file != null && file.exists()) {
            try {

                FileInputStream fileInputStream = new FileInputStream(file);
                NBTTagCompound nBTTagCompound = CompressedStreamTools.func_1138_a(fileInputStream);
                if (!nBTTagCompound.hasKey("Level")) {
                    System.out.println("Chunk file at " + x + "," + z + " is missing level data, skipping");
                    return null;
                }

                if (!nBTTagCompound.getCompoundTag("Level").hasKey("Blocks")) {
                    System.out.println("Chunk file at " + x + "," + z + " is missing block data, skipping");
                    return null;
                }

                Chunk chunk = loadChunkIntoWorldFromCompound(world, nBTTagCompound.getCompoundTag("Level"));
                if (!chunk.isAtLocation(x, y, z)) {
                    System.out
                            .println(
                                    "Chunk file at "
                                            + x
                                            + ","
                                            + y
                                            + ","
                                            + z
                                            + " is in the wrong location; relocating. (Expected "
                                            + x
                                            + ", "
                                            + y
                                            + ","
                                            + z
                                            + ", got "
                                            + chunk.xPosition
                                            + ", "
                                            + chunk.yPosition
                                            + ", "
                                            + chunk.zPosition
                                            + ")"
                            );
                    nBTTagCompound.setInteger("xPos", x);
                    nBTTagCompound.setInteger("zPos", z);
                    chunk = loadChunkIntoWorldFromCompound(world, nBTTagCompound.getCompoundTag("Level"));
                }

                chunk.fixBlockList();
                return chunk;
            } catch (Exception var9) {
                var9.printStackTrace();
            }
        }

        return null;

        }

}
