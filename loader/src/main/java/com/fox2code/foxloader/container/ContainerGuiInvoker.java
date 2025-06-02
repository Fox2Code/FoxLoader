package com.fox2code.foxloader.container;

import net.minecraft.common.block.container.Container;
import net.minecraft.common.world.World;

public abstract class ContainerGuiInvoker {
    public abstract void openContainer(int windowID);

    public void openContainerBlock(int windowID, World world, int x, int y, int z) {
        this.openContainer(windowID);
    }

    public void openContainerDirect(Container container, World world, int x, int y, int z) {
        this.openContainerBlock(container.windowId, world, x, y, z);
    }
}
