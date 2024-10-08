package com.fox2code.foxloader.client.helpers;

import com.fox2code.foxloader.client.WorldProviderCustom;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.block.BlockPortal;
import net.minecraft.src.game.level.WorldProvider;

import java.lang.reflect.InvocationTargetException;

public class BlockPortalHelper {
    public static BlockPortal withProvider(BlockPortal blockPortal, WorldProviderCustom worldProvider, String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (BlockPortal) blockPortal.getClass().getMethod("withProvider").invoke(blockPortal,worldProvider,name);
    }
}
