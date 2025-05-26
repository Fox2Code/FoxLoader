package com.fox2code.foxloader.client.registry;

import com.fox2code.foxloader.utils.BlockFace;
import net.minecraft.src.client.renderer.block.icon.Icon;

public interface RegisteredBlockImpl {
    void setRegisteredHardness(float hardness);

    void setRegisteredResistance(float resistance);

    // BlockFace overloads

    void setIcon(Icon icon, BlockFace blockFace, int metadata);

    Icon getIcon(BlockFace blockFace);

    Icon getIcon(BlockFace blockFace, int metadata);

    // Similar names that should not be used

    default void setTexture(Icon icon, BlockFace blockFace, int metadata) {
        setIcon(icon, blockFace, metadata);
    }

    default Icon getBlockTextureFromSide(BlockFace blockFace) {
        return this.getIcon(blockFace);
    }

    default Icon getTexture(BlockFace blockFace, int metadata) {
        return this.getIcon(blockFace, metadata);
    }
}
