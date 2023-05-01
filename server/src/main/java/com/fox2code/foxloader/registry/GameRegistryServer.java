package com.fox2code.foxloader.registry;

import static com.fox2code.foxloader.loader.ServerMod.*;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.server.network.NetworkPlayerImpl;
import com.fox2code.foxloader.server.registry.RegisteredBlockImpl;
import net.minecraft.src.game.block.*;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemBlock;
import net.minecraft.src.game.item.ItemBlockSlab;
import net.minecraft.src.game.recipe.*;
import net.minecraft.src.server.playergui.StringTranslate;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.Objects;

public class GameRegistryServer extends GameRegistry {
    public static final GameRegistryServer INSTANCE = new GameRegistryServer();
    // Common entries start
    public static final int[] chanceToEncourageFire = new int[MAXIMUM_BLOCK_ID];
    public static final int[] abilityToCatchFire = new int[MAXIMUM_BLOCK_ID];
    public static final EnumReflectTranslator<BuiltInMaterial, Material> MATERIAL =
            new EnumReflectTranslator<>(BuiltInMaterial.class, Material.class);
    public static final EnumReflectTranslator<BuiltInStepSounds, StepSound> STEP_SOUND = // Server doesn't contain all the sounds
            new EnumReflectTranslator<>(BuiltInStepSounds.class, StepSound.class, Block.class, BuiltInStepSounds.POWDER);
    // Common entries end
    private static byte[] serverHello;

    public static void initialize() {
        // The check is actually for initializing both Item and Block
        if (Block.blocksList[0].blockID != Item.itemsList[0].itemID) {
            throw new IllegalStateException("Air block is not air?");
        }
    }

    public static void freeze() {
        if (!ModLoader.areAllModsLoaded())
            throw new IllegalArgumentException("Mods didn't finished to load!");
        // Compile server hello into a byte array for memory and performance efficiency.
        serverHello = ModLoader.Internal.compileServerHello(
                new ServerHello(GameRegistry.registryEntries));
        final Block stoneBlock = Block.blocksList[0];
        for (int i = 0; i < Block.blocksList.length; i++) {
            if (Block.blocksList[i] == null) {
                Block.blocksList[i] = stoneBlock;
            }
        }
        final Item stoneItem = Item.itemsList[0];
        for (int i = 0; i < Item.itemsList.length; i++) {
            if (Item.itemsList[i] == null) {
                Item.itemsList[i] = stoneItem;
            }
        }
    }

    private int nextBlockId = INITIAL_BLOCK_ID;
    private int nextItemId = INITIAL_ITEM_ID;

    private GameRegistryServer() {}

    // START Common code //
    @Override
    public int getMaxBlockId() {
        return nextBlockId - 1;
    }

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
            case CUSTOM:
                try {
                    Constructor<? extends Block> constructor = blockBuilder.
                            gameBlockSource.asSubclass(Block.class).getConstructor(int.class);
                    block = constructor.newInstance(blockId);
                    if (block.blockID != blockId) {
                        throw new RuntimeException("Block didn't ended up with id it was given to " +
                                "(given " + blockId + " got " + block.blockID + ")");
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Custom block must accept an id as a parameter", e);
                }
                break;
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
                block = new BlockFalling(blockId, 0, material);
                break;
            case SLAB:
                EnumSlab slabType = EnumSlab.BRICK;
                for (EnumSlab enumSlabCandidate : EnumSlab.values()) {
                    if (enumSlabCandidate.hasBottomSide && enumSlabCandidate.hasTopSide &&
                            enumSlabCandidate.material == material) {
                        slabType = enumSlabCandidate;
                        break;
                    }
                }

                block = new BlockSlab(blockId, !primary, slabType);

                selfNotify = true;
                break;
            case STAIRS:
                block = new BlockStairs(blockId, Objects.requireNonNull(blockSource, "blockSource")) {};
                selfNotify = true;
                break;
        }
        if (selfNotify) {
            Block.requiresSelfNotify[blockId] = true;
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
        if (itemBuilder.gameItemSource != null) {
            Class<? extends Item> itemCls = itemBuilder.gameItemSource.asSubclass(Item.class);
            if (block != null) {
                try {
                    item = itemCls.getConstructor(Block.class, int.class).newInstance(block, itemId);
                } catch (NoSuchMethodException runtimeException) {
                    try {
                        item = itemCls.getConstructor(int.class).newInstance(itemId);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Unable t initialize item", e);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Unable t initialize item", e);
                }
            } else {
                try {
                    item = itemCls.getConstructor(int.class).newInstance(itemId);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Unable t initialize item", e);
                }
            }
            if (item.itemID != itemId) {
                throw new RuntimeException("Item didn't ended up with id it was given to " +
                        "(given " + itemId + " got " + item.itemID + ")");
            }
        } else if (blockPrimary != null &&
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
        FurnaceRecipes.instance.addSmelting(input.getRegisteredItemId(), toItemStack(output));
    }

    @Override
    public void addBlastFurnaceRecipe(RegisteredItem input, RegisteredItemStack output) {
        BlastFurnaceRecipes.instance.addSmelting(input.getRegisteredItemId(), toItemStack(output));
    }

    @Override
    public void addFreezerRecipe(RegisteredItem input, RegisteredItemStack output) {
        RefridgifreezerRecipes.instance.addSmelting(input.getRegisteredItemId(), toItemStack(output));
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

    public void sendRegistryData(EntityPlayerMP networkPlayer) {
        ((NetworkPlayerImpl) networkPlayer).sendNetworkDataRaw(
                ModLoader.FOX_LOADER_MOD_ID, serverHello);
    }
}
