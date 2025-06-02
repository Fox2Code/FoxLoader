package com.fox2code.foxloader.container;

import net.minecraft.client.gui.GuiContainer;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.block.tileentity.TileEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used on {@link Container} to define a {@link GuiContainer} to use.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerGuiAuto {
    Class<? extends GuiContainer> value();
    Class<? extends TileEntity> tileEntity() default TileEntity.class;
}
