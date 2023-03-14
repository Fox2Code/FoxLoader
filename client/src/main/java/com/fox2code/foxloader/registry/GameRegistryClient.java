package com.fox2code.foxloader.registry;

import static com.fox2code.foxloader.loader.ClientMod.*;

import com.fox2code.foxloader.client.CreativeItems;
import com.fox2code.foxloader.client.registry.RegisteredBlockImpl;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.packet.ServerHello;
import net.minecraft.src.client.gui.StringTranslate;
import net.minecraft.src.game.block.*;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemBlock;
import net.minecraft.src.game.item.ItemBlockSlab;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.recipe.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class GameRegistryClient extends GameRegistry {
    public static final GameRegistryClient INSTANCE = new GameRegistryClient();
    // Common entries start
    public static final int[] chanceToEncourageFire = new int[MAXIMUM_BLOCK_ID];
    public static final int[] abilityToCatchFire = new int[MAXIMUM_BLOCK_ID];
    public static final EnumReflectTranslator<BuiltInMaterial, Material> MATERIAL =
            new EnumReflectTranslator<>(BuiltInMaterial.class, Material.class);
    public static final EnumReflectTranslator<BuiltInStepSounds, StepSound> STEP_SOUND =
            new EnumReflectTranslator<>(BuiltInStepSounds.class, StepSound.class, Block.class);
    // Common entries end
    public static final short[] itemIdMappingIn = new short[MAXIMUM_ITEM_ID];
    public static final short[] itemIdMappingOut = new short[MAXIMUM_ITEM_ID];
    public static final short[] blockIdMappingIn = new short[MAXIMUM_BLOCK_ID];
    public static final short[] blockIdMappingOut = new short[MAXIMUM_BLOCK_ID];
    public static final String[] itemIdMappingInNames = new String[MAXIMUM_ITEM_ID];
    private static MappingState idMappingState = MappingState.CLIENT;
    private enum MappingState {
        CLIENT, SERVER, CUSTOM
    }

    static {
        for (short i = 0; i < MAXIMUM_ITEM_ID; i++) {
            itemIdMappingIn[i] = i;
            itemIdMappingOut[i] = i;
        }
        for (short i = 0; i < MAXIMUM_BLOCK_ID; i++) {
            blockIdMappingIn[i] = i;
            blockIdMappingOut[i] = i;
        }
    }

    public static void initialize() {
        // The check is actually for initializing both Item and Block
        if (Block.blocksList[0].blockID != Item.itemsList[0].itemID) {
            throw new IllegalStateException("Air block is not air?");
        }
    }

    public static void freeze() {
        if (!ModLoader.areAllModsLoaded())
            throw new IllegalArgumentException("Mods didn't finished to load!");
        final Block stoneBlock = Block.blocksList[1];
        for (int i = 0; i < Block.blocksList.length; i++) {
            if (Block.blocksList[i] == null) {
                Block.blocksList[i] = stoneBlock;
            }
        }
        final Item airItem = Item.itemsList[0];
        for (int i = 0; i < Item.itemsList.length; i++) {
            if (Item.itemsList[i] == null) {
                Item.itemsList[i] = airItem;
            }
        }
    }

    private int nextBlockId = INITIAL_BLOCK_ID;
    private int nextItemId = INITIAL_ITEM_ID;

    private GameRegistryClient() {}

    // START Common code //
    @Override
    public RegisteredItem getRegisteredItem(int id) {
        return (RegisteredItem) Item.itemsList[id];
    }

    @Override
    public RegisteredBlock getRegisteredBlock(int id) {
        return (RegisteredBlock) Block.blocksList[id];
    }

    @Override
    public int generateNewBlockId(String name, int fallbackId) {
        if (registryEntries.containsKey(name)) {
            throw new RuntimeException("Duplicate item/block string id: " + name);
        }
        if (fallbackId < 0 || fallbackId > 255) {
            throw new IllegalArgumentException("Invalid fallback id: " + fallbackId);
        }
        int blockId = nextBlockId++;
        if (blockId > MAXIMUM_BLOCK_ID) {
            throw new RuntimeException("Maximum block count registered! (Too many mods?)");
        }
        registryEntries.put(name, new RegistryEntry(
                (short) convertBlockIdToItemId(blockId), (short) fallbackId, name,
                StringTranslate.getInstance().translateKey("tile." + name.replace(':', '.'))));
        return blockId;
    }

    @Override
    public int generateNewItemId(String name, int fallbackId) {
        if (registryEntries.containsKey(name)) {
            throw new RuntimeException("Duplicate item/block string id: " + name);
        }
        if (fallbackId < 0 || fallbackId > 255) {
            throw new IllegalArgumentException("Invalid fallback id: " + fallbackId);
        }
        int itemId = nextItemId++;
        if (itemId > MAXIMUM_ITEM_ID) {
            throw new RuntimeException("Maximum block count registered! (Too many mods?)");
        }
        registryEntries.put(name, new RegistryEntry((short) itemId, (short) fallbackId, name,
                StringTranslate.getInstance().translateKey("item." + name.replace(':', '.'))));
        return itemId;
    }

    @Override
    public RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder, int fallbackId) {
        validateRegistryName(name);
        if (blockBuilder == null) blockBuilder = DEFAULT_BLOCK_BUILDER;
        String secondaryExt = blockBuilder.builtInBlockType.secRegistryExt;
        Block mainBlock = this.registerNewBlock0(name, blockBuilder, fallbackId, true);
        Block secondaryBlock = null;
        if (secondaryExt != null) {
            secondaryBlock = this.registerNewBlock0(name + secondaryExt, blockBuilder, fallbackId, false);
        }
        this.registerNewItem0(name, blockBuilder.itemBuilder, mainBlock, secondaryBlock, -1, true);
        if (secondaryExt != null) {
            this.registerNewItem0(name + secondaryExt, blockBuilder.itemBuilder, mainBlock, secondaryBlock, -1, false);
        }
        return (RegisteredBlock) mainBlock;
    }

    public Block registerNewBlock0(String name, BlockBuilder blockBuilder, int fallbackId, boolean primary) {
        int blockId = generateNewBlockId(name, fallbackId);
        Block block;
        Material material = MATERIAL.translate(blockBuilder.builtInMaterial);
        String blockName = blockBuilder.blockName == null ? name : blockBuilder.blockName;
        Block blockSource = (Block) blockBuilder.blockSource;
        String blockType = blockBuilder.blockType == null ?
                (blockSource != null ? blockSource.getBlockName() : blockName) : blockBuilder.blockType;
        boolean selfNotify = false;
        switch (blockBuilder.builtInBlockType) {
            default:
                throw new IllegalArgumentException("Invalid block type " + blockBuilder.builtInBlockType);
            case BLOCK:
                block = new Block(blockId, material) {};
                break;
            case GLASS:
                block = new BlockGlass(blockId) {};
                break;
            case WORKBENCH:
                block = new BlockWorkbench(blockId) {};
                break;
            case FALLING:
                block = new BlockFalling(blockId, material);
                break;
            case SLAB:
                block = new BlockSlab(blockId, !primary, blockType, material);
                selfNotify = true;
                break;
            case STAIRS:
                block = new BlockStairs(blockId, Objects.requireNonNull(blockSource, "blockSource")) {};
                selfNotify = true;
                break;
        }
        if (selfNotify) {
            Block.selfNotify[blockId] = true;
        }
        block.stepSound = STEP_SOUND.translate(blockBuilder.builtInStepSounds);
        if (blockBuilder.blockHardness != 0f) {
            ((RegisteredBlockImpl) block).setRegisteredHardness(blockBuilder.blockHardness);
        }
        if (blockBuilder.blockResistance != 0f) {
            ((RegisteredBlockImpl) block).setRegisteredResistance(blockBuilder.blockResistance);
        }
        if (blockBuilder.blockBurnType != 0 && blockBuilder.blockBurnTime != 0) {
            block.setBurnTime(blockBuilder.blockBurnTime, blockBuilder.blockBurnType);
        }
        if (blockBuilder.chanceToEncourageFire != 0) {
            chanceToEncourageFire[blockId] = blockBuilder.chanceToEncourageFire;
        }
        if (blockBuilder.abilityToCatchFire != 0) {
            abilityToCatchFire[blockId] = blockBuilder.abilityToCatchFire;
        }
        if (blockBuilder.tooltipColor != 0) {
            block.setTooltipColor(blockBuilder.tooltipColor);
        }
        block.setBlockName(blockBuilder.blockName == null ?
                name.replace(':', '.') : blockBuilder.blockName);
        return block;
    }

    @Override
    public RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder, int fallbackId) {
        validateRegistryName(name);
        return this.registerNewItem0(name, itemBuilder, null, null, this.generateNewItemId(name, fallbackId), true);
    }

    private RegisteredItem registerNewItem0(String name, ItemBuilder itemBuilder,
                                            Block blockPrimary, Block blockSecondary,
                                            int itemId, boolean primary) {
        if (itemBuilder == null) itemBuilder = DEFAULT_ITEM_BUILDER;
        Item item;
        Block block = primary ? blockPrimary : blockSecondary;
        if (block != null) {
            itemId = convertBlockIdToItemId(block.blockID);
        }
        final int pItemId = itemId - PARAM_ITEM_ID_DIFF;
        if (blockPrimary != null &&
                blockSecondary != null) {
            item = new ItemBlockSlab(pItemId, block.blockID, blockPrimary, blockSecondary, !primary);
        } else if (block != null) {
            item = new ItemBlock(pItemId, block.blockID) {};
        } else {
            item = new Item(pItemId) {};
        }
        item.setMaxStackSize(itemBuilder.maxStackSize);
        Item containerItem = (Item) itemBuilder.containerItem;
        if (containerItem != null) {
            item.setContainerItem(containerItem);
        }
        if (block == null) {
            item.setItemName(itemBuilder.itemName == null ?
                    name.replace(':', '.') : itemBuilder.itemName);
        }
        if (itemBuilder.itemBurnType != 0 && itemBuilder.itemBurnTime != 0) {
            item.setBurnTime(itemBuilder.itemBurnTime, itemBuilder.itemBurnType);
        }
        if (itemBuilder.tooltipColor != 0) {
            item.setTooltipColor(itemBuilder.tooltipColor);
        }
        if (!itemBuilder.hideFromCreativeInventory) {
            CreativeItems.addToCreativeInventory(new ItemStack(item));
        }
        return (RegisteredItem) item;
    }

    private static boolean recipeFrozen = false;

    @Override
    public void registerRecipe(RegisteredItemStack result, Object... recipe) {
        if (recipeFrozen) throw new IllegalArgumentException("Too late to register recipes!");
        CraftingManager.getInstance().addRecipe(toItemStack(result), recipe);
    }

    @Override
    public void registerShapelessRecipe(RegisteredItemStack result, Ingredient... ingredients) {
        if (recipeFrozen) throw new IllegalArgumentException("Too late to register recipes!");
        CraftingManager.getInstance().addShapelessRecipe(toItemStack(result), (Object[]) ingredients);
    }

    @Override
    public void addFurnaceRecipe(RegisteredItem input, RegisteredItemStack output) {
        FurnaceRecipes.smelting().addSmelting(input.getRegisteredItemId(), toItemStack(output));
    }

    @Override
    public void addBlastFurnaceRecipe(RegisteredItem input, RegisteredItemStack output) {
        BlastFurnaceRecipes.smelting().addSmelting(input.getRegisteredItemId(), toItemStack(output));
    }

    @Override
    public void addFreezerRecipe(RegisteredItem input, RegisteredItemStack output) {
        RefridgifreezerRecipes.smelting().addSmelting(input.getRegisteredItemId(), toItemStack(output));
    }

    public static void freezeRecipes() {
        if (recipeFrozen) return;
        recipeFrozen = true;
        CraftingManager.getInstance().getRecipeList().sort(new RecipeSorter());
    }

    private static class RecipeSorter implements Comparator<Object> {
        public int compareRecipes(IRecipe var1, IRecipe var2) {
            if (var1 instanceof ShapelessRecipes && var2 instanceof ShapedRecipes) {
                return 1;
            } else if (var2 instanceof ShapelessRecipes && var1 instanceof ShapedRecipes) {
                return -1;
            } else if (var2.getRecipeSize() < var1.getRecipeSize()) {
                return -1;
            } else {
                return var2.getRecipeSize() > var1.getRecipeSize() ? 1 : 0;
            }
        }

        public int compare(Object var1, Object var2) {
            return this.compareRecipes((IRecipe)var1, (IRecipe)var2);
        }
    }
    // END Common code //

    public static void resetMappings(boolean singlePlayer) {
        if (singlePlayer) {
            if (idMappingState == MappingState.CLIENT) return;
            idMappingState = MappingState.CLIENT;
            for (short i = INITIAL_ITEM_ID; i < MAXIMUM_ITEM_ID; i++) {
                itemIdMappingIn[i] = i;
                itemIdMappingOut[i] = i;
            }
            for (short i = (short) INITIAL_TRANSLATED_BLOCK_ID; i < MAXIMUM_TRANSLATED_BLOCK_ID; i++) {
                itemIdMappingIn[i] = i;
                itemIdMappingOut[i] = i;
            }
            for (short i = INITIAL_BLOCK_ID; i < MAXIMUM_BLOCK_ID; i++) {
                blockIdMappingIn[i] = i;
                blockIdMappingOut[i] = i;
            }
        } else {
            if (idMappingState == MappingState.SERVER) return;
            idMappingState = MappingState.SERVER;
            for (int i = INITIAL_TRANSLATED_BLOCK_ID; i < MAXIMUM_TRANSLATED_BLOCK_ID; i++) {
                itemIdMappingIn[i] = DEFAULT_FALLBACK_BLOCK_ID;
                itemIdMappingOut[i] = DEFAULT_FALLBACK_BLOCK_ID;
            }
            for (int i = INITIAL_BLOCK_ID; i < MAXIMUM_BLOCK_ID; i++) {
                blockIdMappingIn[i] = DEFAULT_FALLBACK_BLOCK_ID;
                blockIdMappingOut[i] = DEFAULT_FALLBACK_BLOCK_ID;
            }
            for (int i = INITIAL_ITEM_ID; i < MAXIMUM_ITEM_ID; i++) {
                itemIdMappingIn[i] = DEFAULT_FALLBACK_ITEM_ID;
                itemIdMappingOut[i] = DEFAULT_FALLBACK_ITEM_ID;
            }
        }
        Arrays.fill(itemIdMappingInNames, null);
    }

    public static void initializeMappings(ServerHello serverHello) {
        if (idMappingState != MappingState.SERVER) {
            resetMappings(false);
        }
        if (serverHello.registryEntries.isEmpty()) {
            return;
        }
        idMappingState = MappingState.CUSTOM;
        for (RegistryEntry registryEntry : serverHello.registryEntries.values()) {
            final short remoteId = registryEntry.realId;
            if (isLoaderReservedItemId(remoteId)) {
                RegistryEntry local = registryEntries.get(
                        itemIdMappingInNames[remoteId] = registryEntry.name);
                if (local == null) {
                    itemIdMappingIn[remoteId] = registryEntry.fallbackId;
                    if (remoteId >= INITIAL_TRANSLATED_BLOCK_ID &&
                            remoteId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                        blockIdMappingIn[convertItemIdToBlockId(remoteId)] = registryEntry.fallbackId;
                    }
                } else {
                    itemIdMappingIn[remoteId] = local.realId;
                    itemIdMappingOut[local.realId] = remoteId;
                    if (remoteId >= INITIAL_TRANSLATED_BLOCK_ID &&
                            remoteId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                        if (local.realId >= INITIAL_TRANSLATED_BLOCK_ID &&
                                local.realId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                            final short remoteBlockId = (short) convertItemIdToBlockId(remoteId);
                            final short localBlockId = (short) convertItemIdToBlockId(local.realId);
                            blockIdMappingIn[remoteBlockId] = localBlockId;
                            blockIdMappingOut[localBlockId] = remoteBlockId;
                        } else {
                            // We should never reach here, but let still "support" this extreme case.
                            blockIdMappingIn[convertItemIdToBlockId(remoteId)] = registryEntry.fallbackId;
                        }
                    }
                }
            }
        }
    }
}
