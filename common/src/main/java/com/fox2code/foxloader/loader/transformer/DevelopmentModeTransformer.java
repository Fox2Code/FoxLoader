package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;

public class DevelopmentModeTransformer implements PreClassTransformer {
    /**
     * While mixins already injects these interfaces, we must use a transformer to simplify the life of developers.
     */
    private static final HashMap<String, String> interfacesFromMixin = new HashMap<>();

    static {
        interfacesFromMixin.put("net.minecraft.src.game.block.Block",
                "com.fox2code.foxloader.registry.RegisteredBlock");
        interfacesFromMixin.put("net.minecraft.src.game.item.Item",
                "com.fox2code.foxloader.registry.RegisteredItem");
        interfacesFromMixin.put("net.minecraft.src.game.item.ItemStack",
                "com.fox2code.foxloader.registry.RegisteredItemStack");
        interfacesFromMixin.put("net.minecraft.src.game.entity.Entity",
                "com.fox2code.foxloader.registry.RegisteredEntity");
        interfacesFromMixin.put("net.minecraft.src.game.block.tileentity.TileEntity",
                "com.fox2code.foxloader.registry.RegisteredTileEntity");
        interfacesFromMixin.put("net.minecraft.src.client.player.EntityPlayerSP",
                "com.fox2code.foxloader.network.NetworkPlayer");
        interfacesFromMixin.put("net.minecraft.src.game.entity.player.EntityPlayerMP",
                "com.fox2code.foxloader.network.NetworkPlayer");
        interfacesFromMixin.put("net.minecraft.src.client.player.PlayerController",
                "com.fox2code.foxloader.network.NetworkPlayer$NetworkPlayerController");
        interfacesFromMixin.put("net.minecraft.src.server.player.PlayerController",
                "com.fox2code.foxloader.network.NetworkPlayer$NetworkPlayerController");
        interfacesFromMixin.put("net.minecraft.src.game.level.World",
                "com.fox2code.foxloader.registry.RegisteredWorld");
    }

    @Override
    public void transform(ClassNode classNode, String className) {
        if (!className.startsWith("net.minecraft.")) return;
        String registeredInterface = interfacesFromMixin.get(className);
        if (registeredInterface != null) {
            classNode.interfaces.add(registeredInterface.replace('.', '/'));
        }
        for (MethodNode methodNode : classNode.methods) {
            final Type[] args = Type.getArgumentTypes(methodNode.desc);
            String desc;
            if (args.length != 0 && ((desc = args[args.length - 1]
                    .getDescriptor()).equals("[Ljava/lang/Object;") ||
                    desc.equals("[Ljava/lang/String;"))) {
                methodNode.access |= ACC_VARARGS;
            }
        }
    }
}
