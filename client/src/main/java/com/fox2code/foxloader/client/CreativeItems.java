package com.fox2code.foxloader.client;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.ContainerCreative;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CreativeItems {
    private static ArrayList<ItemStack> moddedItemStacksTmp = new ArrayList<>();

    public static void addToCreativeInventory(ItemStack itemStack) {
        boolean a = false;
        if (moddedItemStacksTmp != null) {
            moddedItemStacksTmp.add(itemStack);
        } else {
            Internal.computedModdedStacks.add(itemStack);
            a = true;
        }
        ModLoader.getModLoaderLogger().log(Level.FINE,
                "Added item to creative tab -> " + itemStack.itemID + " " + a);
    }

    public static List<ItemStack> getCreativeItems() {
        if (Internal.computedModdedStacks == null) {
            EntityPlayer entityPlayer = Minecraft.theMinecraft.thePlayer;
            if (entityPlayer == null) {
                Entity entity = Minecraft.theMinecraft.renderViewEntity;
                if (entity instanceof EntityPlayer) {
                    entityPlayer = (EntityPlayer) entity;
                }
            }
            if (entityPlayer != null) {
                new ContainerCreative(entityPlayer);
            } else {
                try { // Should work as fine to just reach the code we need.
                    new ContainerCreative(null);
                } catch (NullPointerException ignored) {}
            }
        }
        return Internal.computedModdedStacks;
    }

    public static class Internal {
        public static List<ItemStack> computedModdedStacks;

        public static void markLoadFinished(List<ItemStack> itemStacks) {
            if (moddedItemStacksTmp != null) {
                itemStacks.addAll(moddedItemStacksTmp);
                moddedItemStacksTmp = null;
                computedModdedStacks = new ArrayList<>(itemStacks);
            }
        }
    }
}
