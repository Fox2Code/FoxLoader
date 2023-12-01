package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.ResourceReloadingHelper;
import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.renderer.block.Texture;
import net.minecraft.src.client.renderer.block.TextureManager;
import net.minecraft.src.client.renderer.block.TextureMap;
import net.minecraft.src.client.renderer.block.icon.IconRegister;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

@Mixin(TextureMap.class)
public class MixinTextureMap {
    @Redirect(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/block/Block;registerIcons(Lnet/minecraft/src/client/renderer/block/icon/IconRegister;)V"))
    public void registerBlockIconsProxy(Block instance, IconRegister iconRegister) {
        try {
            instance.registerIcons(iconRegister);
        } catch (RuntimeException e) {
            ModLoader.getModLoaderLogger().log(Level.SEVERE, "Failed to registerIcons", e);
            ResourceReloadingHelper.notifyResourceError();
        }
    }

    @Redirect(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/item/Item;registerIcons(Lnet/minecraft/src/client/renderer/block/icon/IconRegister;)V"))
    public void registerItemIconsProxy(Item instance, IconRegister iconRegister) {
        try {
            instance.registerIcons(iconRegister);
        } catch (RuntimeException e) {
            ModLoader.getModLoaderLogger().log(Level.SEVERE, "Failed to registerIcons", e);
            ResourceReloadingHelper.notifyResourceError();
        }
    }

    @Redirect(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/client/renderer/block/TextureManager;createTexture(Ljava/lang/String;)Ljava/util/List;"))
    public List<Texture> createTextureRedirect(TextureManager instance, String name) {
        if (name == null || name.isEmpty())
            return Collections.emptyList();
        try { // Fix crash on Minecraft launcher.
            return instance.createTexture(name);
        } catch (NullPointerException e) {
            System.out.println(
                    "TextureManager.createTexture encountered an NullPointerException when trying to read file " +
                            name + ". Ignoring.");
            return Collections.emptyList();
        }
    }
}
