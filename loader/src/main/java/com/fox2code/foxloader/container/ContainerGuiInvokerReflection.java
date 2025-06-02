/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.container;

import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.utils.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiContainer;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.block.tileentity.TileEntity;
import net.minecraft.common.entity.inventory.IInventory;
import net.minecraft.common.entity.player.InventoryPlayer;
import net.minecraft.common.util.math.MathHelper;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * Auto implementation using reflection, used for {@link ContainerGuiAuto} implementation.
 */
public final class ContainerGuiInvokerReflection extends ContainerGuiInvoker {
    private final Class<? extends Container> container;
    private final Class<? extends TileEntity> tileEntity;
    private final Constructor<? extends GuiContainer> type1GuiConstructor;
    private final Constructor<? extends GuiContainer> type2GuiConstructor;
    private final Constructor<? extends GuiContainer> type3GuiConstructor;
    private final Constructor<? extends GuiContainer> type4GuiConstructor;
    private final Constructor<? extends Container> type1Constructor;
    private final Constructor<? extends Container> type2Constructor;
    private final Constructor<? extends Container> type3Constructor;

    public ContainerGuiInvokerReflection(
            @NotNull Class<? extends Container> container,
            @NotNull ContainerGuiAuto containerGuiAuto) {
        this(container, containerGuiAuto.tileEntity(), containerGuiAuto.value());
    }

    public ContainerGuiInvokerReflection(
            @NotNull Class<? extends Container> container,
            @Nullable Class<? extends TileEntity> tileEntity,
            @NotNull Class<? extends GuiContainer> guiContainer) {
        container.asSubclass(Container.class);
        if (tileEntity != null) {
            tileEntity.asSubclass(TileEntity.class);
        }
        guiContainer.asSubclass(GuiContainer.class);
        this.container = container;
        this.tileEntity = tileEntity;
        // Try invoker 1, player inventory + world pos.
        this.type1GuiConstructor = ReflectionUtils.getPublicConstructorSafe(guiContainer,
                InventoryPlayer.class, World.class, int.class, int.class, int.class);
        // Try invoker 2, player inventory + tile entity.
        this.type2GuiConstructor = tileEntity == null ? null :
                ReflectionUtils.getPublicConstructorSafe(
                        guiContainer, InventoryPlayer.class, tileEntity);
        // Try invoker 3, player inventory + inventory.
        this.type3GuiConstructor = tileEntity == null ||
                !IInventory.class.isAssignableFrom(tileEntity) ? null :
                ReflectionUtils.getPublicConstructorSafeMulti(guiContainer,
                        new Class[]{InventoryPlayer.class, IInventory.class},
                        new Class[]{IInventory.class, IInventory.class});
        // Try invoker 3, original container.
        this.type4GuiConstructor = ReflectionUtils.getPublicConstructorSafe(guiContainer, container);
        if (this.type4GuiConstructor != null) {
            // Try using player inventory + world pos.
            this.type1Constructor = tileEntity == null ? null :
                    ReflectionUtils.getPublicConstructorSafe(container,
                            InventoryPlayer.class, World.class, int.class, int.class, int.class);
            // Try using orig tile entity.
            this.type2Constructor = tileEntity == null ? null :
                    ReflectionUtils.getPublicConstructorSafe(container,
                            InventoryPlayer.class, tileEntity);
            // Try using inventory + inventory.
            this.type3Constructor = tileEntity == null ||
                    !IInventory.class.isAssignableFrom(tileEntity) ? null :
                    ReflectionUtils.getPublicConstructorSafeMulti(container,
                            new Class[]{InventoryPlayer.class, IInventory.class},
                            new Class[]{IInventory.class, IInventory.class});
        } else {
            this.type1Constructor = null;
            this.type2Constructor = null;
            this.type3Constructor = null;
        }
        if (this.type1GuiConstructor == null && this.type2GuiConstructor == null &&
                this.type3GuiConstructor == null && this.type1Constructor == null &&
                this.type2Constructor == null && this.type3Constructor == null) {
            // If it doesn't work for you, I can always add new pathways, just ask me on Discord.
            throw new RuntimeException("No valid way of generation a GUI has been detected!");
        }
        if (this.type4GuiConstructor == null) {
            ModLoaderInit.getModLoaderLogger().warning("The class " + guiContainer.getName() +
                    " doesn't have a direct constructor for " + container.getName());
        }
    }

    @Override
    public void openContainer(int windowID) {
        GuiContainer guiContainer = null;
        EntityPlayerSP entityPlayerSP = Minecraft.theMinecraft.thePlayer;
        Objects.requireNonNull(entityPlayerSP, "entityPlayerSP");
        if (this.type1GuiConstructor != null) {
            guiContainer = ReflectionUtils.invokeConstructorSafe(this.type1GuiConstructor,
                    entityPlayerSP.inventory, entityPlayerSP.worldObj,
                    MathHelper.floor_double(entityPlayerSP.posX),
                    MathHelper.floor_double(entityPlayerSP.posY),
                    MathHelper.floor_double(entityPlayerSP.posZ));
        }
        if (guiContainer == null && this.type1Constructor != null) {
            Container container = ReflectionUtils.invokeConstructorSafe(this.type1Constructor,
                    entityPlayerSP.inventory, entityPlayerSP.worldObj,
                    MathHelper.floor_double(entityPlayerSP.posX),
                    MathHelper.floor_double(entityPlayerSP.posY),
                    MathHelper.floor_double(entityPlayerSP.posZ));
            if (container != null) {
                guiContainer = ReflectionUtils.invokeConstructorSafe(this.type4GuiConstructor, container);
            }
        }
        if (guiContainer == null) {
            throw new RuntimeException("Failed to make GuiContainer!");
        }
        guiContainer.inventorySlots.windowId = windowID;
        Minecraft.theMinecraft.displayGuiScreen(guiContainer);
    }

    @Override
    public void openContainerBlock(int windowID, World world, int x, int y, int z) {
        GuiContainer guiContainer = null;
        EntityPlayerSP entityPlayerSP = Minecraft.theMinecraft.thePlayer;
        Objects.requireNonNull(entityPlayerSP, "entityPlayerSP");
        TileEntity tileEntity = this.tileEntity.cast(world.getBlockTileEntityImmediate(x, y, z));
        if (this.type2GuiConstructor != null) {
            guiContainer = ReflectionUtils.invokeConstructorSafe(
                    this.type2GuiConstructor, entityPlayerSP.inventory, tileEntity);
        }
        if (guiContainer == null && this.type3GuiConstructor != null) {
            guiContainer = ReflectionUtils.invokeConstructorSafe(
                    this.type3GuiConstructor, entityPlayerSP.inventory, tileEntity);
        }
        if (guiContainer == null && this.type2Constructor != null) {
            Container container = ReflectionUtils.invokeConstructorSafe(
                    this.type2Constructor, entityPlayerSP.inventory, tileEntity);
            if (container != null) {
                guiContainer = ReflectionUtils.invokeConstructorSafe(this.type4GuiConstructor, container);
            }
        }
        if (guiContainer == null && this.type3Constructor != null) {
            Container container = ReflectionUtils.invokeConstructorSafe(
                    this.type3Constructor, entityPlayerSP.inventory, tileEntity);
            if (container != null) {
                guiContainer = ReflectionUtils.invokeConstructorSafe(this.type4GuiConstructor, container);
            }
        }
        if (guiContainer == null && this.type1GuiConstructor != null) {
            guiContainer = ReflectionUtils.invokeConstructorSafe(
                    this.type1GuiConstructor, entityPlayerSP.inventory, world, x, y, z);
        }
        if (guiContainer == null && this.type1Constructor != null) {
            Container container = ReflectionUtils.invokeConstructorSafe(
                    this.type1Constructor, entityPlayerSP.inventory, world, x, y, z);
            if (container != null) {
                guiContainer = ReflectionUtils.invokeConstructorSafe(this.type4GuiConstructor, container);
            }
        }
        if (guiContainer == null) {
            throw new RuntimeException("Failed to make GuiContainer!");
        }
        guiContainer.inventorySlots.windowId = windowID;
        Minecraft.theMinecraft.displayGuiScreen(guiContainer);
    }

    @Override
    public void openContainerDirect(Container container, World world, int x, int y, int z) {
        GuiContainer guiContainer = null;
        if (this.type4GuiConstructor != null) {
            guiContainer = ReflectionUtils.invokeConstructorSafe(
                    this.type4GuiConstructor, this.container.cast(container));
        }
        if (guiContainer == null) {
            this.openContainerBlock(container.windowId, world, x, y, z);
        } else {
            Minecraft.theMinecraft.displayGuiScreen(guiContainer);
        }
    }
}
