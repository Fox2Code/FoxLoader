package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureMapExt;
import com.fox2code.foxloader.client.renderer.TextureDynamic;
import net.minecraft.src.client.renderer.Rect2i;
import net.minecraft.src.client.renderer.block.Texture;
import net.minecraft.src.client.renderer.block.TextureManager;
import net.minecraft.src.client.renderer.block.TextureMap;
import net.minecraft.src.client.renderer.block.TextureStitched;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(TextureMap.class)
public class MixinTextureMap implements TextureMapExt {
    @Shadow @Final private Map<String, TextureStitched> textureStitchedMap;
    @Shadow private BufferedImage missingImage;
    @Shadow @Final private List<TextureStitched> listTextureStiched;
    @Unique private Texture missingTexture;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initBlankTexture(int type, String filename, String path, BufferedImage image, CallbackInfo ci) {
        this.missingTexture = TextureManager.instance().makeTexture("missingno",
                2, this.missingImage.getWidth(), this.missingImage.getHeight(),
                10496, GL11.GL_RGBA, 9728, 9728, false, this.missingImage);
    }

    @Inject(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/client/renderer/block/TextureManager;createStitcher(Ljava/lang/String;)Lnet/minecraft/src/client/renderer/block/icon/Stitcher;"))
    public void injectDynamic(CallbackInfo ci) {
        TextureDynamic.Hooks.registerDynamic(this);
    }

    @Redirect(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/client/renderer/block/TextureManager;makeTexture(Ljava/lang/String;IIIIIIIZLjava/awt/image/BufferedImage;)Lnet/minecraft/src/client/renderer/block/Texture;",
    ordinal = 0))
    public Texture makeMissingTextureRedirect(
            TextureManager instance, String filename, int type, int w, int h,
            int depth, int format, int minfilter, int magfilter, boolean par9, BufferedImage image) {
        return this.missingTexture;
    }

    @Redirect(method = "refreshTextures", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/client/renderer/block/TextureManager;createTexture(Ljava/lang/String;)Ljava/util/List;"))
    public List<Texture> createTextureRedirect(TextureManager instance, String name) {
        if (name == null || name.isEmpty())
            return Collections.emptyList();
        if (TextureDynamic.isDynamicTexName(name)) {
            String stubName = name.replace("item.", "").replace("tile.", "").replace(".name", "");
            int lastSeparator = Math.max(stubName.lastIndexOf('/'), stubName.lastIndexOf('\\'));
            int lastDot = stubName.lastIndexOf('.');
            stubName = lastDot < lastSeparator ? stubName.substring(lastSeparator + 1) :
                    stubName.substring(lastSeparator + 1, lastDot);
            return Collections.singletonList(TextureDynamic.registerDynamicDataHolder(stubName).getCachedTexture());
        }
        try { // Fix crash on Minecraft launcher.
            return instance.createTexture(name);
        } catch (NullPointerException e) {
            System.out.println("TextureManager.createTexture encountered an " +
                    "NullPointerException when trying to read file " + name + ". Ignoring.");
            return Collections.emptyList();
        }
    }

    @Inject(method = "refreshTextures", at = @At("RETURN"))
    public void makeTextureDynamicUpdate(CallbackInfo ci) {
        this.textureStitchedMap.values().forEach(textureStitched -> {
            if (textureStitched instanceof TextureDynamic) {
                this.listTextureStiched.add(textureStitched);
            }
        });
    }

    @Override
    public void registerTextureDynamic(String s) {
        TextureStitched texture = this.textureStitchedMap.get(s);
        if (texture != null && !(texture instanceof TextureDynamic)) {
            throw new IllegalArgumentException("Texture already registered as static!");
        }
        TextureDynamic textureDynamic = (TextureDynamic) texture;
        if (textureDynamic == null) {
            textureDynamic = new TextureDynamic(s);
            this.textureStitchedMap.put(s, textureDynamic);
        }
    }
}
