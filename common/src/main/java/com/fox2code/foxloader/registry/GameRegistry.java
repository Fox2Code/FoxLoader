package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.loader.ModLoader;

import java.util.*;

public abstract class GameRegistry {
    static final HashMap<String, RegistryEntry> registryEntries = new HashMap<>();
    static final BlockBuilder DEFAULT_BLOCK_BUILDER = new BlockBuilder();
    static final ItemBuilder DEFAULT_ITEM_BUILDER = new ItemBuilder();
    private static GameRegistry gameRegistry;
    public static final int PARAM_ITEM_ID_DIFF = 256;
    public static final int INITIAL_BLOCK_ID = 360;
    public static final int MAXIMUM_BLOCK_ID = 1024; // Hard max: 1258
    public static final int INITIAL_ITEM_ID = 2048;
    public static final int MAXIMUM_ITEM_ID = 4096; // Hard max: 31999
    // Block ids but translated to item ids
    public static final int INITIAL_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(INITIAL_BLOCK_ID);
    public static final int MAXIMUM_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(MAXIMUM_BLOCK_ID);
    // The default fallback id for blocks is stone.
    public static final int DEFAULT_FALLBACK_BLOCK_ID = 1;
    // The default fallback id for items is planks.
    public static final int DEFAULT_FALLBACK_ITEM_ID = 5;

    public static GameRegistry getInstance() {
        return gameRegistry;
    }

    private static final ThreadLocal<int[]> blockIntArrayLocal =
            ThreadLocal.withInitial(() -> new int[gameRegistry.getMaxBlockId() + 1]);

    /**
     * @return array to be temporary used in block calculations.
     */
    public static int[] getTemporaryBlockIntArray() {
        if (gameRegistry.isFrozen()) {
            int[] array = blockIntArrayLocal.get();
            Arrays.fill(array, 0);
            return array;
        }
        return new int[gameRegistry.getMaxBlockId() + 1];
    }

    /**
     * This is instanced by the mod loaded and should just be a static interface to interact with the game.
     */
    GameRegistry() {
        if (gameRegistry != null)
            throw new IllegalStateException("Only one registry can exists at a time");
        gameRegistry = this;
    }

    /**
     * @return a registered modded item with the corresponding registry name
     */
    public RegisteredItem getRegisteredItem(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        return registryEntry == null ? null :
                this.getRegisteredItem(registryEntry.realId);
    }

    /**
     * @return a registered modded item with the corresponding registry name
     */
    public RegisteredBlock getRegisteredBlock(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        return registryEntry == null ? null :
                this.getRegisteredBlock(
                        convertItemIdToBlockId(registryEntry.realId));
    }

    /**
     * @return list of registered modded entries
     */
    public static Collection<RegistryEntry> getRegistryEntries() {
        return Collections.unmodifiableCollection(registryEntries.values());
    }

    /**
     * @return maximum expected block id
     */
    public abstract int getMaxBlockId();

    /**
     * @return a registered item with the corresponding id
     */
    public abstract RegisteredItem getRegisteredItem(int id);

    /**
     * @return a registered block with the corresponding id
     */
    public abstract RegisteredBlock getRegisteredBlock(int id);

    /**
     * @param translationKey translation key to use
     * @return translated component
     */
    public abstract String translateKey(String translationKey);

    /**
     * @param translationKey translation key to use
     * @param args translation arguments to use
     * @return translated formatted component
     */
    public abstract String translateKeyFormat(String translationKey, String... args);

    /**
     * Only use this if you know what you are doing.
     */
    public abstract int generateNewBlockId(String name, int fallbackId);

    /**
     * Only use this if you know what you are doing.
     */
    public abstract int generateNewItemId(String name, int fallbackId);

    /**
     * Register a new block into the game
     */
    public final RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder) {
        return this.registerNewBlock(name, blockBuilder, DEFAULT_FALLBACK_BLOCK_ID);
    }

    public abstract RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder, int fallbackId);

    /**
     * Register a new item into the game
     */
    public final RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder) {
        return this.registerNewItem(name, itemBuilder, DEFAULT_FALLBACK_ITEM_ID);
    }

    public abstract RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder, int fallbackId);

    public abstract void registerRecipe(RegisteredItemStack result, Object... recipe);

    public abstract void registerShapelessRecipe(RegisteredItemStack result, Ingredient... ingredients);

    public abstract void addFurnaceRecipe(RegisteredItem input, RegisteredItemStack output);

    public abstract void addBlastFurnaceRecipe(RegisteredItem input, RegisteredItemStack output);

    public abstract void addFreezerRecipe(RegisteredItem input, RegisteredItemStack output);

    public boolean isFrozen() {
        return ModLoader.areAllModsLoaded();
    }

    public static int convertBlockIdToItemId(int blockID) {
        return blockID > 255 ? blockID + 744 : blockID;
    }

    public static int convertItemIdToBlockId(int itemId) {
        return itemId > MAXIMUM_TRANSLATED_BLOCK_ID ? -1 : // -1 means no block equivalent.
                itemId > 255 ? itemId < 1000 ? -1 : itemId - 744 : itemId;
    }

    public static void validateRegistryName(String name) {
        if (name.indexOf(':') == -1) {
            throw new IllegalArgumentException("Please add your mod id in the registry name, ex \"modid:item\")");
        }
        if (name.indexOf('\0') != -1) {
            throw new IllegalArgumentException("Null bytes are not supported in registry identifiers");
        }
    }

    /**
     * @param itemId the item id
     * @return if the id is reserved for mod loader use
     */
    public static boolean isLoaderReservedItemId(int itemId) {
        return (itemId >= INITIAL_TRANSLATED_BLOCK_ID &&
                itemId < MAXIMUM_TRANSLATED_BLOCK_ID) ||
                (itemId >= INITIAL_ITEM_ID && itemId < MAXIMUM_ITEM_ID);
    }

    public interface Ingredient {}

    public enum BuiltInMaterial implements EnumReflectTranslator.ReflectEnum {
        AIR, GRASS("grass", "grassMaterial"), GROUND, WOOD, ROCK,
        IRON, WATER, LAVA, LEAVES, PLANTS, SPONGE, CLOTH, FIRE,
        SAND, CIRCUITS, GLASS, TNT, CORAL, ICE, SNOW, BUILT_SNOW("builtSnow"),
        CACTUS, CLAY, PUMPKIN, PORTAL, CAKE("cakeMaterial"),
        WEB("web", "cobweb"), PISTON, CHAIR, QUICKSAND, ASH,
        MOVEABLE_CIRCUIT("moveableCircuit"), LIGHT_BLOCK("lightBlock"),
        SUGAR_CANE("sugarCane"), HONEYCOMB, OBSIDIAN, MAGMA,
        POTION_FIRE("potionfire");

        private final String[] reflectNames;

        BuiltInMaterial() {
            this.reflectNames = new String[]{name().toLowerCase(Locale.ROOT)};
        }

        BuiltInMaterial(String... reflectNames) {
            this.reflectNames = reflectNames;
        }

        @Override
        public String[] getReflectNames() {
            return this.reflectNames;
        }
    }

    public enum BuiltInStepSounds implements EnumReflectTranslator.ReflectEnum {
        POWDER("soundPowder", "soundPowderFootstep", /* Why? */ "soundUnused"),
        WOOD("soundWood", "soundWoodFootstep"),
        GRAVEL("soundGravel", "soundGravelFootstep"),
        GRASS("soundGrass", "soundGrassFootstep"),
        STONE("soundStone", "soundStoneFootstep"),
        METAL("soundMetal", "soundMetalFootstep"),
        GLASS("soundGlass", "soundGlassFootstep"),
        CLOTH("soundCloth", "soundClothFootstep"),
        SAND("soundSand", "soundSandFootstep"),
        BUSH("soundBush", "soundBushFootstep"),
        SNOW("soundSnow", "soundSnowFootstep"),
        SLIME("soundSlime", "soundSlimeFootstep");
        private final String[] reflectNames;

        BuiltInStepSounds(String... reflectNames) {
            this.reflectNames = reflectNames;
        }

        @Override
        public String[] getReflectNames() {
            return this.reflectNames;
        }
    }

    public enum BuiltInBlockType {
        CUSTOM, BLOCK, GLASS, WORKBENCH, FALLING, SLAB("_full"), STAIRS;

        public final String secRegistryExt;

        BuiltInBlockType(String secRegistryExt) {
            this.secRegistryExt = secRegistryExt;
        }

        BuiltInBlockType() {
            this.secRegistryExt = null;
        }
    }
}
