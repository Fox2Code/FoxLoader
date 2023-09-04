package com.fox2code.foxloader.client.gui;

import net.minecraft.src.client.gui.Container;

/**
 * Tells FoxLoader that the container is a wrapped container.
 * <p>
 * If your container has client side only inventory slots please use {@link InventoryClientOnly} too
 */
public interface ContainerWrapped {
    Container getParentContainer();

     static Container getNetworkContainer(Container container) {
         int antiSoftLock = 0;
         while (container instanceof ContainerWrapped) {
             container = ((ContainerWrapped) container).getParentContainer();
             if (antiSoftLock++>100) return null;
         }
         return container;
     }
}
