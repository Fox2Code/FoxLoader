/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.client.CreativeTab;
import com.fox2code.foxloader.energy.FoxPowerCableTileEntity;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.loader.packet.LoaderNetworkManager;
import com.fox2code.foxloader.loader.packet.ServerNoRegistry;
import com.fox2code.foxloader.loader.packet.ServerRegistry;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import com.fox2code.foxloader.patching.PatchConstants;
import com.fox2code.foxloader.registry.missing.MissingBlock;
import com.fox2code.foxloader.registry.missing.MissingItem;
import com.fox2code.foxloader.registry.missing.MissingItemBlock;
import net.minecraft.common.block.Block;
import net.minecraft.common.block.Blocks;
import net.minecraft.common.block.tileentity.TileEntity;
import net.minecraft.common.item.Item;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.item.Items;
import net.minecraft.common.item.block.ItemBlock;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.common.networking.Packet250PluginMessage;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public final class GameRegistry {
    private static final int BLOCK_ID_DIFF = PatchConstants.BLOCK_ID_DIFF;
    public static final int INITIAL_BLOCK_ID = PatchConstants.INITIAL_BLOCK_ID;
    public static final int MODIFIED_BLOCK_LIMIT = PatchConstants.MODIFIED_BLOCK_LIMIT;
    public static final int MAXIMUM_BLOCK_ID = MODIFIED_BLOCK_LIMIT - 1;
    public static final int INITIAL_ITEM_ID = 8192;
    public static final int MAXIMUM_ITEM_ID = PatchConstants.MAXIMUM_ITEM_ID;
    // Block ids but translated to item ids
    public static final int INITIAL_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(INITIAL_BLOCK_ID);
    public static final int MAXIMUM_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(MAXIMUM_BLOCK_ID);
    private static final int INITIAL_ITEM_BLOCK_ID = 4096;
    private static final int MAXIMUM_ITEM_BLOCK_ID = 8191;
    public static final int INITIAL_ENTITY_ID = 256;
    private static final int REGISTRY_FILE_VER = 0;

    public static final short[] itemIdMappingIn = new short[MAXIMUM_ITEM_ID];
    public static final short[] itemIdMappingOut = new short[MAXIMUM_ITEM_ID];
    public static final short[] blockIdMappingIn = new short[MODIFIED_BLOCK_LIMIT];
    public static final short[] blockIdMappingOut = new short[MODIFIED_BLOCK_LIMIT];
    public static final String[] itemIdMappingInNames = new String[MAXIMUM_ITEM_ID];
    public static final String[] itemIdMappingLocalNames = new String[MAXIMUM_ITEM_ID];
    public static boolean doNetworkRemap = false;
    private static final HashSet<String> registeredEntries = new HashSet<>();
    private static final Set<String> registeredEntriesTypeNamesIdsRaw = Collections.unmodifiableSet(registeredEntries);
    private static final HashSet<String> registeredEntriesVanilla = new HashSet<>();
    private static final Set<String> registeredEntriesVanillaRaw = Collections.unmodifiableSet(registeredEntriesVanilla);
    private static final HashSet<String> registeredEntriesVanillaFL = new HashSet<>();
    private static final Set<String> registeredEntriesVanillaFLRaw = Collections.unmodifiableSet(registeredEntriesVanillaFL);
    private static Set<String> registeredEntriesTypeNamesIds = registeredEntriesTypeNamesIdsRaw;
    private static final LinkedHashMap<String, RegistryEntry> registryEntries = new LinkedHashMap<>();
    public static final RegistryEntry[] registryEntriesItemIds =
            new RegistryEntry[MAXIMUM_ITEM_ID - INITIAL_TRANSLATED_BLOCK_ID];
    static final ModContainer REINDEV_MOD_CONTAINER = ModLoaderInit.getModContainer("reindev");
    static final ModContainer FOXLOADER_MOD_CONTAINER = ModLoaderInit.getModContainer("foxloader");
    private static final File FL_CONFIG = new File(ModLoader.getConfigFolder(), "foxloader");
    private static final File REGISTRY_FILE = new File(FL_CONFIG, "registry.dat");
    private static final File REGISTRY_FILE_BAK = new File(FL_CONFIG, "registry.dat.bak");
    private static MappingState idMappingState = MappingState.LOCAL;
    static boolean initialized = false, frozen = false;
    private static Block FALLBACK_BLOCK = null;
    private static Item FALLBACK_ITEM = null;
    private static short FALLBACK_ITEM_BLOCK_ID = 0;

    private GameRegistry() { throw new AssertionError(); }

    /**
     * @param name the registry id of the item.
     * @return a registered modded item with the corresponding registry name
     */
    public static Item getRegisteredItem(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        return registryEntry == null ? null : Items.ITEMS_LIST[registryEntry.realId];
    }

    /**
     * @param name the registry id of the block.
     * @return a registered modded item with the corresponding registry name
     */
    public static Block getRegisteredBlock(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        if (registryEntry == null) return null;
        int blockId = convertItemIdToBlockId(registryEntry.realId);
        return blockId < 0 ? null : Blocks.BLOCKS_LIST[blockId];
    }

    /**
     * @return list of registered modded entries
     */
    public static Collection<RegistryEntry> getRegistryEntries() {
        return Collections.unmodifiableCollection(registryEntries.values());
    }

    /**
     * @param name registry id name
     * @throws IllegalArgumentException if name is not a valid registry id
     */
    public static void validateRegistryName(String name) throws IllegalArgumentException {
        if (name.indexOf('\0') != -1 || name.indexOf(' ') != -1) {
            throw new IllegalArgumentException("Null bytes and spaces are not supported in registry identifiers");
        }
        if (name.indexOf(':') == -1) {
            throw new IllegalArgumentException("Please add your mod id in the registry name, ex \"modid:item\"" +
                    " (Got: \"" + name + "\")");
        }
        if (name.endsWith("tile") || name.endsWith("item")) {
            // This makes ReIndev texture engine glitch out.
            throw new IllegalArgumentException("Ids cannot ends with \"tile\" or \"item\" due to engine limitation" +
                    " (Got: \"" + name + "\")");
        }
    }

    /**
     * @param itemId the item id
     * @return if the id is reserved for mod loader use
     */
    public static boolean isLoaderReservedItemId(int itemId) {
        return (itemId >= INITIAL_TRANSLATED_BLOCK_ID &&
                itemId <= MAXIMUM_TRANSLATED_BLOCK_ID) ||
                (itemId >= INITIAL_ITEM_ID && itemId <= MAXIMUM_ITEM_ID);
    }

    /**
     * @param itemId the item id
     * @return if the id is reserved for mod loader and a block
     */
    public static boolean isLoaderReservedBlockItemId(int itemId) {
        return (itemId >= INITIAL_TRANSLATED_BLOCK_ID &&
                itemId < MAXIMUM_TRANSLATED_BLOCK_ID);
    }

    /**
     * @param blockId the block id
     * @return if the id is reserved for mod loader
     */
    public static boolean isLoaderReservedBlockId(int blockId) {
        return (blockId >= INITIAL_BLOCK_ID &&
                blockId < MAXIMUM_BLOCK_ID);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    private enum MappingState {
        LOCAL, VANILLA, VANILLA_FL, REMOTE
    }

    static {
        // Always start with sanity check
        if (INITIAL_TRANSLATED_BLOCK_ID != INITIAL_ITEM_BLOCK_ID ||
                MAXIMUM_TRANSLATED_BLOCK_ID != MAXIMUM_ITEM_BLOCK_ID) {
            throw new AssertionError("Got (" + INITIAL_TRANSLATED_BLOCK_ID +
                    ", " + MAXIMUM_TRANSLATED_BLOCK_ID + ")");
        }
        if (!isLoaderReservedItemId(INITIAL_TRANSLATED_BLOCK_ID)) {
            throw new AssertionError("Initial ItemBlock ID is not a reserved ItemID");
        }
        int tmp1, tmp2;
        if ((tmp2 = convertItemIdToBlockId((tmp1 = convertBlockIdToItemId(INITIAL_BLOCK_ID)))) != INITIAL_BLOCK_ID) {
            throw new AssertionError("Got (" + INITIAL_BLOCK_ID + " > " + tmp1 + ">" + tmp2 + ")");
        }

        for (short i = 0; i < MAXIMUM_ITEM_ID; i++) {
            itemIdMappingIn[i] = i;
            itemIdMappingOut[i] = i;
        }
        for (short i = 0; i < MAXIMUM_BLOCK_ID; i++) {
            blockIdMappingIn[i] = i;
            blockIdMappingOut[i] = i;
        }
        Arrays.fill(itemIdMappingInNames, null);
        Arrays.fill(itemIdMappingLocalNames, null);
    }

    public static void initialize() {
        if (initialized)
            throw new IllegalStateException("GameRegistry already initialized");
        // The check is actually for initializing both Item and Block
        if (Blocks.BLOCKS_LIST[0].blockID != Items.ITEMS_LIST[0].itemID) {
            throw new IllegalStateException("Air block is not air?");
        }
        for(int i = 0; i < INITIAL_TRANSLATED_BLOCK_ID; ++i) {
            Item item;
            if ((item = Items.ITEMS_LIST[i]) != null) {
                String itemName = item.getItemName();
                if (itemName.startsWith("item.") ||
                        (itemName.startsWith("tile.") && item.isItemBlock())) {
                    itemName = itemName.substring(5);
                }
                registeredEntriesVanilla.add(itemName);
                String itemNameFL = "reindev:" + itemName;
                registeredEntriesVanillaFL.add(itemNameFL);
                registeredEntries.add(itemNameFL);
            }
        }
        initialized = true;
        registryEntries.clear();
        if (REGISTRY_FILE.exists()) {
            int initialBlockID = INITIAL_BLOCK_ID - 1;
            int initialItemID = INITIAL_ITEM_ID - 1;
            int initialEntityID = INITIAL_ENTITY_ID - 1;
            try (DataInputStream dataInputStream = new DataInputStream(
                    Files.newInputStream(REGISTRY_FILE.toPath()))) {
                int version = dataInputStream.readUnsignedShort();
                if (version > REGISTRY_FILE_VER) {
                    throw new RuntimeException("Version of \"registry.dat\" is too new: " + version);
                }
                int entryCount = dataInputStream.readUnsignedShort();
                while (entryCount-- > 0) {
                    RegistryEntry registryEntry = new RegistryEntry(dataInputStream);
                    int realId = registryEntry.realId;
                    if (!isLoaderReservedItemId(realId))
                        throw new IOException("Invalid non-loader id: " + realId);
                    registryEntries.put(registryEntry.name, registryEntry);
                    registryEntriesItemIds[registryEntry.realId - INITIAL_TRANSLATED_BLOCK_ID] = registryEntry;
                    if (isItemBlock(realId)) {
                        initialBlockID = Math.max(initialBlockID, convertItemIdToBlockId(realId));
                    } else {
                        initialItemID = Math.max(initialItemID, realId);
                    }
                }
                entryCount = dataInputStream.readUnsignedShort();
                while (entryCount-- > 0) {
                    RegistryEntry registryEntry = new RegistryEntry(dataInputStream);
                    int realId = registryEntry.realId;
                    if (realId < INITIAL_ENTITY_ID)
                        throw new IOException("Invalid non-loader id: " + realId);
                    EntityRegistry.entityEntries.put(registryEntry.name, registryEntry);
                    registryEntriesItemIds[registryEntry.realId - INITIAL_TRANSLATED_BLOCK_ID] = registryEntry;
                    initialEntityID = Math.max(initialEntityID, realId);
                }
                // Allow to load new game content without conflicting with item ids
                Internal.initializeCustomInitialIds(initialBlockID + 1, initialItemID + 1, initialEntityID + 1);
            } catch (IOException ioe) {
                registryEntries.clear();
                if (!REGISTRY_FILE_BAK.exists()) {
                    if (!REGISTRY_FILE.renameTo(REGISTRY_FILE_BAK)) {
                        ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                                "Failed to backup current registry file");
                    }
                }
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                        "Skipping reading registry data due to corrupted file", ioe);
            }
        }
        FOXLOADER_MOD_CONTAINER.runInContext(() -> {
            // Register missing item stuff
            FALLBACK_BLOCK = new MissingBlock("block_missing");
            FALLBACK_ITEM = new MissingItem("item_missing");
            FALLBACK_ITEM_BLOCK_ID = (short) FALLBACK_BLOCK.getItemID();
            registerNewTileEntityType("cable", FoxPowerCableTileEntity.class);
            registeredEntriesVanillaFL.add("foxloader:block_missing");
            registeredEntriesVanillaFL.add("foxloader:item_missing");
        });
    }

    public static void freeze() {
        if (!ModLoader.areAllModsLoaded())
            throw new IllegalStateException("Mods didn't finished to load!");
        if (frozen)
            throw new IllegalStateException("GameRegistry already frozen");
        frozen = true;

        for (int i = INITIAL_BLOCK_ID; i < MAXIMUM_BLOCK_ID; i++) {
            Block registeredBlock = Blocks.BLOCKS_LIST[i];
            if (registeredBlock != null) {
                if (!Block.internalInitializeItemBlock(registeredBlock)) {
                    throw new RuntimeException("Item id mismatch for " +
                            registeredBlock.getClass().getName());
                }
            }
        }
        for (int i = INITIAL_TRANSLATED_BLOCK_ID; i < MAXIMUM_ITEM_ID; i++) {
            Item item = Items.ITEMS_LIST[i];
            CreativeTab creativeTab;
            if (item != null && (creativeTab = item.getRegisterFLTab()) != null) {
                creativeTab.addToCreativeTab(item);
            }
        }
        for (int i = 0; i < Blocks.BLOCKS_LIST.length; i++) {
            if (Blocks.BLOCKS_LIST[i] == null) {
                Blocks.BLOCKS_LIST[i] = FALLBACK_BLOCK;
            }
        }
        final Item FALLBACK_ITEM_BLOCK = Items.ITEMS_LIST[FALLBACK_ITEM_BLOCK_ID];
        Objects.requireNonNull(FALLBACK_ITEM_BLOCK, "FALLBACK_ITEM_BLOCK");
        for (int i = 0; i < Items.ITEMS_LIST.length; i++) {
            if (Items.ITEMS_LIST[i] == null) {
                Items.ITEMS_LIST[i] = isItemBlock(i) ? FALLBACK_ITEM_BLOCK : FALLBACK_ITEM;
            }
        }
        for (RegistryEntry registryEntry : registryEntries.values()) {
            itemIdMappingLocalNames[registryEntry.realId] = registryEntry.name;
        }
        Internal.resetMappings(true);
        if (Internal.hasCustomRegistryData) {
            Internal.serverRegistryData = LoaderNetworkManager.compileServerPacketData(
                    new ServerRegistry(registryEntries, EntityRegistry.entityEntries,
                            new HashMap<>(SidedMetadataAPI.getSelfMetadata())), 1);
        } else {
            Internal.serverRegistryData = LoaderNetworkManager.compileServerPacketData(
                    new ServerNoRegistry(new HashMap<>(SidedMetadataAPI.getSelfMetadata())), 1);
        }
        if (Internal.hasNewRegistryData) {
            if (!(FL_CONFIG.exists() || FL_CONFIG.mkdirs()))
                throw new RuntimeException("Cannot create FoxLoader config file");
            // Save registry data to keep id consistency.
            try (DataOutputStream dataOutputStream = new DataOutputStream(
                    Files.newOutputStream(REGISTRY_FILE.toPath()))) {
                dataOutputStream.writeShort(REGISTRY_FILE_VER);
                RegistryEntry.writeEntries(dataOutputStream, registryEntries);
                RegistryEntry.writeEntries(dataOutputStream, EntityRegistry.entityEntries);
            } catch (IOException ioe) {
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Failed to write new game data");
            }
        }
    }

    public static int convertBlockIdToItemId(int blockID) {
        return blockID >= INITIAL_BLOCK_ID ? blockID + BLOCK_ID_DIFF :
                blockID > 255 ? blockID + 744 : blockID;
    }

    public static int convertItemIdToBlockId(int itemId) {
        return itemId > MAXIMUM_TRANSLATED_BLOCK_ID ? -1 : // -1 means no block equivalent.
                itemId >= INITIAL_TRANSLATED_BLOCK_ID ? itemId - BLOCK_ID_DIFF :
                itemId > 255 ? itemId < 1000 ? -1 : itemId - 744 : itemId;
    }

    public static boolean isItemBlock(int itemId) {
        return itemId <= MAXIMUM_TRANSLATED_BLOCK_ID &&
                (itemId >= INITIAL_TRANSLATED_BLOCK_ID || (itemId <= 255 || (itemId >= 1000)));
    }

    public static boolean isMissingItemStack(ItemStack itemStack) {
        int itemId = itemStack.getItemID();
        if (itemId < 0 || itemId >= Items.ITEMS_LIST.length) {
            return true;
        }
        Item item = Items.ITEMS_LIST[itemId];
        return item == null || item instanceof MissingItem ||
                item instanceof MissingItemBlock;
    }

    public static void registerNewTileEntityType(String name, Class<? extends TileEntity> tileEntityClass) {
        ModContainer modContainer = ModContainer.getActiveModContainer();
        if (modContainer == null) throw new IllegalStateException("No mod container active");
        name = modContainer.getModId() + ":" + name;
        validateRegistryName(name);
        TileEntity.addMapping(tileEntityClass, name);
    }

    public static ModContainer getRegisteringMod(Item item) {
        if (!isLoaderReservedItemId(item.itemID)) {
            return REINDEV_MOD_CONTAINER;
        }
        if (item == FALLBACK_ITEM ||
                item.itemID == FALLBACK_ITEM_BLOCK_ID) {
            return FOXLOADER_MOD_CONTAINER;
        }
        RegistryEntry registryEntry = registryEntriesItemIds[item.itemID - INITIAL_TRANSLATED_BLOCK_ID];
        if (registryEntry == null) {
            throw new IllegalArgumentException("How did you do that?");
        }
        return ModLoaderInit.getModContainer(registryEntry.name.substring(0, registryEntry.name.indexOf(':')));
    }

    public static ModContainer getRegisteringMod(Block block) {
        if (!isLoaderReservedBlockId(block.blockID)) {
            return REINDEV_MOD_CONTAINER;
        }
        if (block == FALLBACK_BLOCK) {
            return FOXLOADER_MOD_CONTAINER;
        }
        RegistryEntry registryEntry = registryEntriesItemIds[
                convertBlockIdToItemId(block.blockID) - INITIAL_TRANSLATED_BLOCK_ID];
        if (registryEntry == null) {
            throw new IllegalArgumentException("How did you do that?");
        }
        return ModLoaderInit.getModContainer(registryEntry.name.substring(0, registryEntry.name.indexOf(':')));
    }

    public static Set<String> getCommandCompletionItemIDs() {
        if (registeredEntriesTypeNamesIds == null || registeredEntriesTypeNamesIds.isEmpty()) {
            throw new IllegalStateException("Not initialized yet!");
        }
        return registeredEntriesTypeNamesIds;
    }

    public static final class Internal {
        private static int nextBlockId = INITIAL_BLOCK_ID;
        private static int nextItemId = INITIAL_ITEM_ID;
        private static byte[] serverRegistryData;
        static boolean hasCustomRegistryData = false;
        static boolean hasNewRegistryData = false;
        // Allow compatibility mods to allow the use of vanilla constructors.
        private static int registerBlockIDCompat = 0;
        private static int registerItemIDCompat = 0;
        public static boolean allowVanillaRegisterOverride = false;

        static void initializeCustomInitialIds(int initialNextBlockId, int initialNextItemId, int initialNextEntityId) {
            if (frozen || hasCustomRegistryData)
                throw new IllegalStateException("Some game data already has been registered!");
            nextBlockId = initialNextBlockId;
            nextItemId = initialNextItemId;
            EntityRegistry.Internal.nextEntityId = initialNextEntityId;
        }

        public static int generateBlockId(Block block, String id) {
            if (!initialized) throw new IllegalStateException("GameRegistry not initialized");
            if (frozen) throw new IllegalStateException("GameRegistry frozen");
            ModContainer modContainer = ModContainer.getActiveModContainer();
            if (modContainer == null) throw new IllegalStateException("No mod container active");
            if (modContainer != FOXLOADER_MOD_CONTAINER) {
                modContainer.markAddGameContent();
                hasCustomRegistryData = true;
            }
            final int blockId = nextBlockId;
            final int itemId = convertBlockIdToItemId(blockId);
            int existingId = addRegistryEntry(itemId, modContainer.getModId() + ":" + id, true);
            block.setBlockName(modContainer.getModId() + "." + id);
            if (existingId == -1) {
                nextBlockId++;
            } else {
                return convertItemIdToBlockId(existingId);
            }
            return blockId;
        }

        public static int generateItemId(Item item, String id) {
            if (!initialized) throw new IllegalStateException("GameRegistry not initialized");
            if (frozen) throw new IllegalStateException("GameRegistry frozen");
            ModContainer modContainer = ModContainer.getActiveModContainer();
            if (modContainer == null) throw new IllegalStateException("No mod container active");
            if (modContainer != FOXLOADER_MOD_CONTAINER) {
                modContainer.markAddGameContent();
                hasCustomRegistryData = true;
            }
            final int itemId = nextItemId;
            int existingId = addRegistryEntry(itemId, modContainer.getModId() + ":" + id, false);
            item.setItemName(modContainer.getModId() + "." + id);
            if (existingId == -1) {
                nextItemId++;
            } else {
                return existingId;
            }
            return itemId;
        }

        private static int addRegistryEntry(int id, String fullId, boolean forBlock) {
            allowVanillaRegisterOverride = false;
            validateRegistryName(fullId);
            if (!registeredEntries.add(fullId)) {
                throw new IllegalArgumentException("Duplicate id: " + fullId);
            }
            RegistryEntry existingRegistryEntry = registryEntries.get(fullId);
            if (existingRegistryEntry != null) {
                int realId = existingRegistryEntry.realId;
                if (forBlock != isItemBlock(realId)) {
                    throw new IllegalArgumentException(
                            "Incompatible saved registry for: " + fullId + " (" + realId + ")");
                }
                return realId;
            }
            RegistryEntry registryEntry = new RegistryEntry((short) id, fullId);
            registryEntries.put(registryEntry.name, registryEntry);
            registryEntriesItemIds[registryEntry.realId - INITIAL_TRANSLATED_BLOCK_ID] = registryEntry;
            hasNewRegistryData = true;
            return -1;
        }

        public static void resetMappings(boolean asLocalMapping) {
            EntityRegistry.Internal.resetEntityMappings(asLocalMapping);
            doNetworkRemap = false;
            if (asLocalMapping) {
                if (idMappingState == MappingState.LOCAL) return;
                registeredEntriesTypeNamesIds = registeredEntriesTypeNamesIdsRaw;
                idMappingState = MappingState.LOCAL;
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
                registeredEntriesTypeNamesIds = registeredEntriesVanillaRaw;
                if (idMappingState == MappingState.VANILLA) return;
                if (idMappingState == MappingState.VANILLA_FL) {
                    idMappingState = MappingState.VANILLA;
                    return;
                }
                idMappingState = MappingState.VANILLA;
                for (int i = INITIAL_TRANSLATED_BLOCK_ID; i < MAXIMUM_TRANSLATED_BLOCK_ID; i++) {
                    itemIdMappingIn[i] = FALLBACK_ITEM_BLOCK_ID;
                    itemIdMappingOut[i] = FALLBACK_ITEM_BLOCK_ID;
                }
                for (int i = INITIAL_BLOCK_ID; i < MAXIMUM_BLOCK_ID; i++) {
                    blockIdMappingIn[i] = (short) FALLBACK_BLOCK.blockID;
                    blockIdMappingOut[i] = (short) FALLBACK_BLOCK.blockID;
                }
                for (int i = INITIAL_ITEM_ID; i < MAXIMUM_ITEM_ID; i++) {
                    itemIdMappingIn[i] = (short) FALLBACK_ITEM.itemID;
                    itemIdMappingOut[i] = (short) FALLBACK_ITEM.itemID;
                }
            }
            Arrays.fill(itemIdMappingInNames, null);
        }

        public static void resetMappingsAsVanillaFL() {
            resetMappings(false);
            registeredEntriesTypeNamesIds = registeredEntriesVanillaFLRaw;
            idMappingState = MappingState.VANILLA_FL;
        }

        public static void initializeRemoteMappings(ServerRegistry serverRegistry) {
            if (serverHelloIdenticalToLocal(serverRegistry)) {
                resetMappings(true);
                return;
            }
            initializeMappingsEx(serverRegistry, false);
            doNetworkRemap = true;
        }

        public static ServerRegistry initializeWorldMappings(ServerRegistry serverRegistry) {
            if (serverRegistry == null) {
                resetMappings(true);
                return new ServerRegistry(
                        new HashMap<>(registryEntries),
                        new HashMap<>(EntityRegistry.entityEntries),
                        new HashMap<>(SidedMetadataAPI.getSelfMetadata()));
            }
            if (serverHelloIdenticalToLocal(serverRegistry)) {
                resetMappings(true);
                return null;
            }
            ServerRegistry newServerRegistry = initializeMappingsEx(serverRegistry, true);
            if (serverHelloIdenticalToLocal(newServerRegistry)) {
                resetMappings(true);
                newServerRegistry.registryEntries.clear();
                newServerRegistry.registryEntries.putAll(registryEntries);
            }
            return newServerRegistry;
        }

        private static boolean serverHelloIdenticalToLocal(ServerRegistry serverRegistry) {
            if (serverRegistry == null ||
                    serverRegistry.registryEntries.size() != registryEntries.size() ||
                    serverRegistry.entityEntries.size() != EntityRegistry.entityEntries.size()) {
                return false;
            }
            for (RegistryEntry remoteRegistryEntry : serverRegistry.registryEntries.values()) {
                RegistryEntry localRegistryEntry = registryEntries.get(remoteRegistryEntry.name);
                if (!Objects.equals(remoteRegistryEntry, localRegistryEntry)) return false;
            }
            for (RegistryEntry remoteEntityEntry : serverRegistry.entityEntries.values()) {
                RegistryEntry localRegistryEntry = EntityRegistry.entityEntries.get(remoteEntityEntry.name);
                if (!Objects.equals(remoteEntityEntry, localRegistryEntry)) return false;
            }
            return true;
        }

        private static ServerRegistry initializeMappingsEx(ServerRegistry serverRegistry, boolean localWorld) {
            if (idMappingState != MappingState.VANILLA) {
                resetMappings(false);
            }
            if (serverRegistry.registryEntries.isEmpty()) {
                throw new IllegalArgumentException("Corrupted registry packet");
            }
            if (serverRegistry.metadata != null && !serverRegistry.metadata.isEmpty()) {
                SidedMetadataAPI.Internal.setActiveMetaData(
                        Collections.unmodifiableMap(serverRegistry.metadata));
            }
            HashSet<String> serverRegisteredEntries = new HashSet<>(registeredEntriesVanillaFL);
            serverRegisteredEntries.addAll(serverRegistry.registryEntries.keySet());
            registeredEntriesTypeNamesIds = Collections.unmodifiableSet(serverRegisteredEntries);
            idMappingState = MappingState.REMOTE;
            boolean allowMissingRegistryKeys =
                    SidedMetadataAPI.getBoolean(SidedMetadataAPI.KEY_ALLOW_MISSING_REGISTRY_KEYS);
            LinkedHashMap<String, RegistryEntry> toInjectRegistryEntries =
                    localWorld ? new LinkedHashMap<>(registryEntries) : null;
            int nextRemoteItemId = INITIAL_ITEM_ID - 1, nextRemoteItemBlockId = INITIAL_TRANSLATED_BLOCK_ID - 1;
            for (RegistryEntry registryEntry : serverRegistry.registryEntries.values()) {
                final short remoteId = registryEntry.realId;
                if (isLoaderReservedItemId(remoteId)) {
                    RegistryEntry local = registryEntries.get(
                            itemIdMappingInNames[remoteId] = registryEntry.name);
                    if (local == null) {
                        if (!allowMissingRegistryKeys) {
                            throw new IllegalArgumentException(
                                    "Missing client side registry key: " + registryEntry.name);
                        }
                        if (remoteId >= INITIAL_TRANSLATED_BLOCK_ID &&
                                remoteId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                            nextRemoteItemBlockId = Math.max(nextRemoteItemBlockId, remoteId);
                            itemIdMappingIn[remoteId] = FALLBACK_ITEM_BLOCK_ID;
                            blockIdMappingIn[convertItemIdToBlockId(remoteId)] =  (short) FALLBACK_BLOCK.blockID;
                        } else {
                            nextRemoteItemId = Math.max(nextRemoteItemId, remoteId);
                            itemIdMappingIn[remoteId] = (short) FALLBACK_ITEM.itemID;
                        }
                    } else {
                        if (toInjectRegistryEntries != null) {
                            toInjectRegistryEntries.remove(local.name);
                        }
                        itemIdMappingIn[remoteId] = local.realId;
                        itemIdMappingOut[local.realId] = remoteId;
                        if (remoteId >= INITIAL_TRANSLATED_BLOCK_ID &&
                                remoteId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                            if (local.realId >= INITIAL_TRANSLATED_BLOCK_ID &&
                                    local.realId < MAXIMUM_TRANSLATED_BLOCK_ID) {
                                nextRemoteItemBlockId = Math.max(nextRemoteItemBlockId, remoteId);
                                final short remoteBlockId = (short) convertItemIdToBlockId(remoteId);
                                final short localBlockId = (short) convertItemIdToBlockId(local.realId);
                                blockIdMappingIn[remoteBlockId] = localBlockId;
                                blockIdMappingOut[localBlockId] = remoteBlockId;
                            } else {
                                // We should never reach here, but let still "support" this extreme case.
                                blockIdMappingIn[convertItemIdToBlockId(remoteId)] = (short) FALLBACK_BLOCK.blockID;
                                ModLoaderInit.getModLoaderLogger().warning("Out of scope block registry id?");
                            }
                        } else {
                            nextRemoteItemId = Math.max(nextRemoteItemId, remoteId);
                        }
                    }
                }
            }
            ServerRegistry newServerRegistry =
                    EntityRegistry.Internal.initializeEntityMappingsEx(serverRegistry, localWorld);
            if (toInjectRegistryEntries == null ||
                    toInjectRegistryEntries.isEmpty()) {
                return newServerRegistry;
            }
            nextRemoteItemId++;
            nextRemoteItemBlockId++;
            if (newServerRegistry == null) {
                newServerRegistry = new ServerRegistry(
                        new HashMap<>(serverRegistry.registryEntries),
                        new HashMap<>(serverRegistry.entityEntries),
                        new HashMap<>(SidedMetadataAPI.getSelfMetadata()));
            }
            for (RegistryEntry registryEntry : toInjectRegistryEntries.values()) {
                if (isLoaderReservedBlockItemId(registryEntry.realId)) {
                    nextRemoteItemBlockId++;
                    newServerRegistry.registryEntries.put(registryEntry.name,
                            new RegistryEntry((short) nextRemoteItemBlockId, registryEntry.name));
                    final short remoteBlockId = (short) convertItemIdToBlockId(nextRemoteItemBlockId);
                    final short localBlockId = (short) convertItemIdToBlockId(registryEntry.realId);
                    blockIdMappingIn[remoteBlockId] = localBlockId;
                    blockIdMappingOut[localBlockId] = remoteBlockId;
                    itemIdMappingIn[nextRemoteItemBlockId] = registryEntry.realId;
                    itemIdMappingOut[registryEntry.realId] = (short) nextRemoteItemBlockId;
                } else {
                    nextRemoteItemId++;
                    newServerRegistry.registryEntries.put(registryEntry.name,
                            new RegistryEntry((short) nextRemoteItemId, registryEntry.name));
                    itemIdMappingIn[nextRemoteItemBlockId] = registryEntry.realId;
                    itemIdMappingOut[registryEntry.realId] = (short) nextRemoteItemId;
                }
            }
            return newServerRegistry;
        }

        public static String strRegIdToItemStrId(String id) {
            if (id.startsWith("reindev:")) {
                String name = id.substring(8);

                for(int i = 0; i < INITIAL_TRANSLATED_BLOCK_ID; ++i) {
                    Item item;
                    if ((item = Items.ITEMS_LIST[i]) != null && item != FALLBACK_ITEM &&
                            (item.getItemName().replace("item.", "").equals(name) ||
                                    item.getItemName().equals(name) || item.isItemBlock() &&
                                    item.getItemName().replace("tile.", "").equals(name))) {
                        return "" + item.itemID;
                    }
                }
            } else if (id.indexOf(':') != -1) {
                RegistryEntry registryEntry = registryEntries.get(id);
                if (registryEntry != null) return "" + registryEntry.realId;
            }
            return id;
        }

        public static String strRegIdToBlockStrId(String id) {
            if (id.startsWith("reindev:")) {
                String name = id.substring(8);

                for(int i = 0; i < PatchConstants.ORIGINAL_BLOCK_LIMIT; ++i) {
                    Block block;
                    if ((block = Blocks.BLOCKS_LIST[i]) != null && block != FALLBACK_BLOCK &&
                            (block.getBlockName().replace("tile.", "").equals(name) ||
                                    block.getBlockName().equals(name))) {
                        return "" + block.blockID;
                    }
                }
            } else if (id.indexOf(':') != -1) {
                RegistryEntry registryEntry = registryEntries.get(id);
                if (registryEntry != null) {
                    int blockId = convertItemIdToBlockId(registryEntry.realId);
                    if (blockId != -1) return "" + blockId;
                }
            }
            return id;
        }

        public static boolean isIncompatibleClient(NetworkManager networkManager) {
            return hasCustomRegistryData && !networkManager.hasFoxLoader();
        }

        public static void sendRegistryData(NetworkManager networkManager) {
            networkManager.addToSendQueue(new Packet250PluginMessage("foxloader", serverRegistryData));
            if (FoxLauncher.DEVELOPING_FOXLOADER) {
                ModLoaderInit.getModLoaderLogger().info("Sending registry data");
            }
        }

        public static void checkVanillaConstructorItem(Item item, int id) {
            if (initialized) {
                int itemIdCompat = registerItemIDCompat;
                int realItemId = 256 + id;
                registerItemIDCompat = 0;
                if (itemIdCompat != 0 && itemIdCompat == realItemId) {
                    return;
                }
                if (isLoaderReservedBlockItemId(realItemId) &&item instanceof ItemBlock) {
                    return;
                }
                if (allowVanillaRegisterOverride && !isLoaderReservedItemId(realItemId)) {
                    return;
                }
                throw new IllegalStateException("Cannot use vanilla constructor in mods! (ID: " + realItemId + ")");
            }
        }

        public static void checkVanillaConstructorBlock(Block block, int id) {
            if (initialized) {
                int blockIdCompat = registerBlockIDCompat;
                registerBlockIDCompat = 0;
                if (blockIdCompat != 0 && blockIdCompat == id) {
                    return;
                }
                if (allowVanillaRegisterOverride && !isLoaderReservedBlockId(id)) {
                    return;
                }
                throw new IllegalStateException("Cannot use vanilla constructor in mods! (ID: " + id + ")");
            }
        }

        // For use by compatibility mods
        public static int addRegistryEntryForCompat(String fullId, boolean forBlock) {
            validateRegistryName(fullId);
            if (frozen) {
                throw new IllegalStateException("Registry is frozen");
            }
            hasCustomRegistryData = true;
            int index = fullId.indexOf(':');
            ModContainer modContainer = ModLoaderInit.getModContainer(fullId.substring(0, index));
            if (modContainer != null) {
                modContainer.markAddGameContent();
            }
            final int blockId;
            final int itemId;
            if (forBlock) {
                blockId = nextBlockId;
                itemId = convertBlockIdToItemId(blockId);
            } else {
                blockId = 0;
                itemId = nextItemId;
            }
            int existingId = addRegistryEntry(itemId, fullId, forBlock);
            if (existingId == -1) {
                if (forBlock) {
                    nextBlockId++;
                } else {
                    nextItemId++;
                }
                registerBlockIDCompat = blockId;
                registerItemIDCompat = itemId;
                return forBlock ? blockId : itemId;
            } else {
                int existingBlockId = forBlock ? convertItemIdToBlockId(existingId) : 0;
                registerBlockIDCompat = existingBlockId;
                registerItemIDCompat = existingId;
                return forBlock ? existingBlockId : existingId;
            }
        }

        public static String state() {
            return idMappingState.name();
        }
    }
}
