package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.GameRegistry;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.level.chunk.ChunkBlockMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkBlockMap.class)
public class MixinChunkBlockMap {
    /**
     * @author Fox2Cdoe
     */
    @Overwrite
    public static void cleanupInvalidBlocks(short[] bmap) {
        if (bmap == null) return;
        final int len = bmap.length;
        for(int biter = 0; biter < len; ++biter) {
            final int block = bmap[biter] & '\uffff';
            if (block >= GameRegistry.MAXIMUM_BLOCK_ID ||
                    Block.blocksList[block] == null) {
                bmap[biter] = 0;
            }
        }
    }
}
