package com.fox2code.foxloader.container;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.packet.LoaderNetworkManager;
import com.fox2code.foxloader.loader.packet.ServerOpenContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.world.World;
import net.minecraft.server.entity.player.EntityPlayerMP;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

public final class ContainerManager {
    private static final HashMap<String, ContainerGuiInvoker> containerGuiInvokers = new HashMap<>();
    private static final Object containerGuiInvokersLock = new Object();

    private ContainerManager() {}

    public static void registerInvoker(Class<? extends Container> cls, ContainerGuiInvoker containerGuiInvoker) {
        cls.asSubclass(Container.class);
        Objects.requireNonNull(containerGuiInvoker);
        synchronized (containerGuiInvokersLock) {
            containerGuiInvokers.putIfAbsent(cls.getName(), containerGuiInvoker);
        }
    }

    /**
     * @param entityPlayer the player to open a container for
     * @param container the container
     * @param x the block x position
     * @param y the block y position
     * @param z the block z position
     * @return true if the player was a real player, or false if the player was a fake player.
     */
    public static boolean openFoxLoaderContainer(EntityPlayer entityPlayer, Container container, int x, int y, int z) {
        Objects.requireNonNull(entityPlayer, "entityPlayer");
        Objects.requireNonNull(container, "container");
        if (FoxLauncher.isClient()) {
            EntityPlayerSP entityPlayerSP = Minecraft.getInstance().thePlayer;
            if (entityPlayerSP == entityPlayer) {
                // Clientside we can just shortcut to using the Container object directly.
                getInvokerForClass(container.getClass())
                        .openContainerDirect(container, entityPlayerSP.worldObj, x, y, z);
                return true;
            }
            return false;
        } else {
            if (!(entityPlayer instanceof EntityPlayerMP)) {
                return false;
            }
            EntityPlayerMP entityPlayerMP = (EntityPlayerMP) entityPlayer;
            entityPlayerMP.getNextWindowId();
            int newWindowId = entityPlayerMP.currentWindowId;
            container.windowId = newWindowId;
            entityPlayerMP.currentContainer = container;
            LoaderNetworkManager.sendServerPacketData(entityPlayerMP.playerNetServerHandler.getNetworkManager(),
                    new ServerOpenContainer(container.getClass().getName(), newWindowId, x, y, z));
            // Note: onCraftGuiOpened is only called on servers-only environments.
            container.onCraftGuiOpened(entityPlayerMP);
            return true;
        }
    }

    public static @NotNull ContainerGuiInvoker getInvokerForClass(Class<? extends Container> containerClass) {
        containerClass.asSubclass(Container.class);
        String className = containerClass.getName();
        if (className.startsWith("net.minecraft.")) {
            throw new IllegalArgumentException("Can only handle modded containers!");
        }
        ContainerGuiInvoker guiInvoker = containerGuiInvokers.get(className);
        if (guiInvoker != null) {
            return guiInvoker;
        }
        synchronized (containerGuiInvokersLock) {
            guiInvoker = containerGuiInvokers.get(className);
            if (guiInvoker == null) {
                ContainerGuiAuto containerGuiAuto = containerClass.getAnnotation(ContainerGuiAuto.class);
                if (containerGuiAuto != null) {
                    guiInvoker = new ContainerGuiInvokerReflection(containerClass, containerGuiAuto);
                    containerGuiInvokers.put(className, guiInvoker);
                }
            }
        }
        if (guiInvoker == null) {
            throw new RuntimeException("Unsupported container: " + className);
        }
        return guiInvoker;
    }

    public static final class Internal {
        private Internal() {}

        public static void onReceivingContainerPacket(ServerOpenContainer serverOpenContainer) {
            ContainerGuiInvoker invoker = containerGuiInvokers.get(serverOpenContainer.containerID);
            if (invoker == null) {
                // Allow to lazily load classes to compute ContainerGuiAuto.
                Class<? extends Container> containerClass;
                try {
                    containerClass = Class.forName(serverOpenContainer.containerID,
                            false, FoxLauncher.getFoxClassLoader()).asSubclass(Container.class);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to find container class", e);
                }
                invoker = getInvokerForClass(containerClass);
            }
            World world = Objects.requireNonNull(Minecraft.getInstance().theWorld);
            if (serverOpenContainer.block) {
                invoker.openContainerBlock(serverOpenContainer.windowID, world,
                        serverOpenContainer.x, serverOpenContainer.y, serverOpenContainer.z);
            } else {
                invoker.openContainer(serverOpenContainer.windowID);
            }
        }
    }
}
