package com.fox2code.foxloader.client.renderer;

import com.fox2code.foxloader.launcher.utils.BufferedImageHelper;
import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.packet.ServerDynamicTexture;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.renderer.block.Texture;
import net.minecraft.src.client.renderer.block.TextureStitched;
import net.minecraft.src.client.renderer.block.icon.Icon;
import net.minecraft.src.client.renderer.block.icon.IconRegister;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;

import java.util.HashMap;
import java.util.function.Function;

public class TextureDynamic extends TextureStitched implements Icon {
    // This is a very dirty hack, but nothing else works.
    private static final boolean USE_DIRECT_TEXTURE_HACK = true;
    public static final int DYN_TEX_WIDTH = 16;
    public static final int DYN_TEX_HEIGHT = 16;

    private static boolean canRegister = true;
    private static final HashMap<String, DynamicDataHolder> holderCache = new HashMap<>();
    private static final Function<String, DynamicDataHolder> holderProvider = DynamicDataHolder::new;
    private static final DynamicDataHolder[] serverDynHolders =
            new DynamicDataHolder[ServerDynamicTexture.SERVER_DYN_MAX_ID];

    static {
        for (int i = 0; i < ServerDynamicTexture.SERVER_DYN_MAX_ID; i++) {
            DynamicDataHolder dynamicDataHolder = new DynamicDataHolder("server_dyn" + i);
            holderCache.put("server_dyn" + i, dynamicDataHolder);
            serverDynHolders[i] = dynamicDataHolder;
        }
    }

    private final DynamicDataHolder dynamicDataHolder;
    private int modCount;

    public static boolean isDynamicTexName(String name) {
        return name.contains("server_dyn") || name.contains("dynamic_");
    }

    public static DynamicDataHolder registerDynamicDataHolder(String textureId) {
        if (!isDynamicTexName(textureId))
            throw new IllegalArgumentException("Invalid dynamic texture id: " + textureId);
        if (canRegister) return holderCache.computeIfAbsent(textureId, holderProvider);
        DynamicDataHolder dynamicDataHolder = holderCache.get(textureId);
        if (dynamicDataHolder == null)
            throw new IllegalStateException("Dynamic textures already registered.");
        return dynamicDataHolder;
    }

    public TextureDynamic(String par1) {
        super(par1);
        this.modCount = 0;
        this.dynamicDataHolder = holderCache.computeIfAbsent(par1, holderProvider);
    }

    @Override
    public float getMinU() {
        if (USE_DIRECT_TEXTURE_HACK) {
            this.bindCachedTexture();
            return 0;
        }
        return super.getMinU();
    }

    @Override
    public float getMaxU() {
        return USE_DIRECT_TEXTURE_HACK ? 1F : super.getMaxU();
    }

    @Override
    public float getMinV() {
        return USE_DIRECT_TEXTURE_HACK ? 0 : super.getMinV();
    }

    @Override
    public float getMaxV() {
        return USE_DIRECT_TEXTURE_HACK ? 1F : super.getMaxV();
    }

    @Override
    public void updateAnimation() {
        this.dynamicDataHolder.updateTexture(this, this.textureSheet, this.originX, this.originY);
    }

    public void setRenderingData(int[] renderingData) {
        this.dynamicDataHolder.setRenderingData(renderingData);
    }

    public int[] getRenderingData() {
        return this.dynamicDataHolder.getRenderingData();
    }

    public DynamicDataHolder getDynamicDataHolder() {
        return this.dynamicDataHolder;
    }

    public void bindCachedTexture() {
        // Bind via RenderEngine for compatibility reasons.
        Minecraft.getInstance().renderEngine.bindTexture(
                this.dynamicDataHolder.getCachedTexture().getGlTextureId());
    }

    public static final class DynamicDataHolder {
        private static final int[] RENDERING_DATA_DEFAULT = new int[256];
        static { RENDERING_DATA_DEFAULT[0] = 0xFF000000; }
        private Texture cachedTexture;
        private int[] renderingData = RENDERING_DATA_DEFAULT;
        private int modCount = 1;
        private final String id;

        private DynamicDataHolder(String id) {
            if (!isDynamicTexName(id))
                throw new IllegalArgumentException("Invalid dynamic texture id: " + id);
            this.id = id;
        }

        public synchronized void setRenderingData(int[] renderingData) {
            if (renderingData == null) renderingData = RENDERING_DATA_DEFAULT;
            this.renderingData = renderingData;
            this.modCount++;
            if (this.modCount == 0)
                this.modCount++;
            if (USE_DIRECT_TEXTURE_HACK) {
                Texture texture = this.cachedTexture;
                this.cachedTexture = null;
                if (texture != null) {
                    Minecraft.getInstance().renderEngine
                            .deleteTexture(texture.getGlTextureId());
                }
                this.getCachedTexture();
            } else {
                Texture cachedTexture = this.cachedTexture;
                if (cachedTexture != null) {
                    ((TextureExt) cachedTexture).pushDynamicData(
                            0, 0, this.renderingData);
                }
            }
        }

        public int[] getRenderingData() {
            int[] renderingData = this.renderingData;
            return renderingData == RENDERING_DATA_DEFAULT ? null : renderingData;
        }

        synchronized void updateTexture(TextureDynamic textureDynamic, Texture textureSheet,
                                        int originX, int originY) {
            if (textureDynamic.modCount != this.modCount) {
                ((TextureExt) textureSheet).pushDynamicData(
                        originX, originY, this.renderingData);
                textureDynamic.modCount = this.modCount;
                ModLoader.getModLoaderLogger().info( // Log to let client know of server updates
                            "Updated dynamic texture: " + textureDynamic.getIconName());

            }
        }

        public Texture getCachedTexture() {
            Texture texture = this.cachedTexture;
            if (texture != null) return texture;
            if (USE_DIRECT_TEXTURE_HACK) {
                return this.cachedTexture = new Texture(this.id,
                        2, 16, 16, 10496, 6408, 9728, 9728,
                        BufferedImageHelper.toBufferedImage(this.renderingData));
            } else {
                texture = new Texture(this.id, 2, 16, 16, 10496, 6408, 9728, 9728, null);
                ((TextureExt) texture).pushDynamicData(0, 0, this.renderingData);
                return this.cachedTexture = texture;
            }
        }
    }

    public static class Hooks {
        private static Item[] ITEM_CACHE;

        public static DynamicDataHolder getServerDynTex(int id) {
            return serverDynHolders[id];
        }

        public static void registerDynamic(TextureMapExt textureMapExt) {
            canRegister = false; // Don't allow register after initialization.
            // holderCache.keySet().forEach(textureMapExt::registerTextureDynamic);
        }

        public static ItemStack toRenderItemStack(ItemStack itemStack) {
            if (itemStack == null) return null;
            RegisteredItemStack registeredItemStack = ClientMod.toRegisteredItemStack(itemStack);
            int dynTexture = registeredItemStack.getRegisteredDynamicTextureId();
            if (dynTexture < 0 || dynTexture >= ServerDynamicTexture.SERVER_DYN_MAX_ID) return itemStack;
            return ((TexCacheItemStackRender) registeredItemStack).getRenderItemCache(getItemCache()[dynTexture]);
        }

        public static void ensureItemCache() {
            getItemCache();
        }

        private static Item[] getItemCache() {
            if (ITEM_CACHE != null) return ITEM_CACHE;
            ITEM_CACHE = new Item[ServerDynamicTexture.SERVER_DYN_MAX_ID];

            for (int i = 0; i < ServerDynamicTexture.SERVER_DYN_MAX_ID; i++) {
                ITEM_CACHE[i] = new DynamicTexItem(i);
            }

            return ITEM_CACHE;
        }

        public interface TexCacheItemStackRender {
            ItemStack getRenderItemCache(Item item);
        }
    }

    private static class DynamicTexItem extends Item {
        protected DynamicTexItem(int id) {
            super(GameRegistry.INITIAL_ITEM_ID + id
                    - ServerDynamicTexture.SERVER_DYN_MAX_ID
                    - GameRegistry.PARAM_ITEM_ID_DIFF);
            this.setItemName("server_dyn" + id);
        }

        @Override
        public void registerIcons(IconRegister register) {
            final String id = this.itemName.replace("item.", "");
            if (register instanceof TextureMapExt) {
                ((TextureMapExt)register).registerTextureDynamic(id);
            }
            this.itemIcon = register.registerIcon(id);
            if (!(this.itemIcon instanceof TextureDynamic)) {
                throw new InternalError("WTF???");
            }
        }

        @Override
        public Icon getIconFromDamage(int damageMetadata) {
            if (this.itemIcon != null && ModLoader.isOnGameThread()) {
                ((TextureDynamic) this.itemIcon).updateAnimation();
            }
            return this.itemIcon;
        }

        @Override
        public int getColor(ItemStack itemstack) {
            if (USE_DIRECT_TEXTURE_HACK && ModLoader.isOnGameThread()) {
                ((TextureDynamic) this.itemIcon).bindCachedTexture();
            }
            return super.getColor(itemstack);
        }
    }
}
