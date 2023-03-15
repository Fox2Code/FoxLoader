package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public class DevelopmentModeTransformer implements PreClassTransformer {
    /**
     * While mixins already inject these interfaces, we must use a transformer to simplify the life of developers.
     */
    private static final HashMap<String, String> interfacesFromMixin = new HashMap<>();

    static {
        interfacesFromMixin.put("net.minecraft.src.game.block.Block",
                "com.fox2code.foxloader.registry.RegisteredBlock");
        interfacesFromMixin.put("net.minecraft.src.game.item.Item",
                "com.fox2code.foxloader.registry.RegisteredItem");
        interfacesFromMixin.put("net.minecraft.src.game.item.ItemStack",
                "com.fox2code.foxloader.registry.RegisteredItemStack");
        interfacesFromMixin.put("net.minecraft.src.client.player.EntityPlayerSP",
                "com.fox2code.foxloader.network.NetworkPlayer");
        interfacesFromMixin.put("net.minecraft.src.game.entity.player.EntityPlayerMP",
                "com.fox2code.foxloader.network.NetworkPlayer");
    }

    @Override
    public void transform(ClassNode classNode, String className) {
        if (!className.startsWith("net.minecraft.")) return;
        String registeredInterface = interfacesFromMixin.get(className);
        if (registeredInterface != null) {
            classNode.interfaces.add(registeredInterface.replace('.', '/'));
        }
    }
}
