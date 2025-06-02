package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.packet.ServerRegistry;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.EntityList;
import net.minecraft.common.world.World;

import java.lang.reflect.Array;
import java.util.*;

public final class EntityRegistry {
    public static final int INITIAL_ENTITY_ID = 256;
    private static final int MAX_ENTITY_ID = 1024;
    private static final int[] networkMappingIn = new int[MAX_ENTITY_ID];
    private static final HashSet<String> registeredEntities = new HashSet<>();
    private static final Set<String> registryEntitiesTypeNamesIdsRaw = Collections.unmodifiableSet(registeredEntities);
    private static Set<String> registryEntitiesTypeNamesIds = registryEntitiesTypeNamesIdsRaw;
    static final LinkedHashMap<String, RegistryEntry> entityEntries = new LinkedHashMap<>();

    public static void registerEntityClass(Class<? extends Entity> entity, String name) {
        ModContainer modContainer = ModContainer.getActiveModContainer();
        if (modContainer == null) throw new IllegalStateException("No mod container active");
        int id = EntityRegistry.Internal.registerEntityId(modContainer.getModId() + ":" + name);
        EntityList.addMapping(entity, name, id);
    }

    public static Entity createEntityRemote(int id, World world) {
        return EntityList.createEntity(networkMappingIn[id], world);
    }

    public static final class Internal {
        static int nextEntityId = INITIAL_ENTITY_ID;

        static int registerEntityId(String fullId) {
            if (!GameRegistry.initialized) throw new IllegalStateException("GameRegistry not initialized");
            if (GameRegistry.frozen) throw new IllegalStateException("GameRegistry frozen");
            ModContainer modContainer = ModContainer.getActiveModContainer();
            if (modContainer == null) throw new IllegalStateException("No mod container active");
            if (modContainer != GameRegistry.FOXLOADER_MOD_CONTAINER) {
                modContainer.markAddGameContent();
                GameRegistry.Internal.hasCustomRegistryData = true;
            }
            final int entityId = nextEntityId;
            int existingId = addEntityEntry(entityId, fullId);
            if (existingId == -1) {
                nextEntityId++;
            } else {
                return existingId;
            }
            return entityId;
        }

        private static int addEntityEntry(int id, String fullId) {
            GameRegistry.validateRegistryName(fullId);
            if (!EntityRegistry.registeredEntities.add(fullId)) {
                throw new IllegalArgumentException("Duplicate id: " + fullId);
            }
            RegistryEntry existingRegistryEntry = EntityRegistry.entityEntries.get(fullId);
            if (existingRegistryEntry != null) {
                return existingRegistryEntry.realId;
            }
            RegistryEntry registryEntry = new RegistryEntry((short) id, fullId);
            EntityRegistry.entityEntries.put(registryEntry.name, registryEntry);
            GameRegistry.Internal.hasNewRegistryData = true;
            return -1;
        }

        static void resetEntityMappings(boolean asLocalMapping) {
            if (asLocalMapping) {
                registryEntitiesTypeNamesIds = registryEntitiesTypeNamesIdsRaw;
                for (int i = 0; i < MAX_ENTITY_ID; i++) {
                    networkMappingIn[i] = i;
                }
            } else {
                registryEntitiesTypeNamesIds = Collections.emptySet();
                Arrays.fill(networkMappingIn, -1);
                for (int i = 0; i < INITIAL_ENTITY_ID; i++) {
                    networkMappingIn[i] = i;
                }
            }
        }

        static ServerRegistry initializeEntityMappingsEx(ServerRegistry serverRegistry, boolean localWorld) {
            if (!localWorld) {
                registryEntitiesTypeNamesIds =
                        Collections.unmodifiableSet(serverRegistry.registryEntries.keySet());
            }
            LinkedHashMap<String, RegistryEntry> toInjectEntityEntries =
                    localWorld ? new LinkedHashMap<>(serverRegistry.entityEntries) : null;
            int nextRemoteEntityId = INITIAL_ENTITY_ID - 1;
            for (RegistryEntry registryEntry : serverRegistry.registryEntries.values()) {
                final short remoteId = registryEntry.realId;
                RegistryEntry local = entityEntries.get(registryEntry.name);
                if (local == null) {
                    nextRemoteEntityId = Math.max(nextRemoteEntityId, remoteId);
                } else {
                    if (toInjectEntityEntries != null) {
                        toInjectEntityEntries.remove(local.name);
                    }
                    networkMappingIn[remoteId] = local.realId;
                }
            }
            if (toInjectEntityEntries == null ||
                    toInjectEntityEntries.isEmpty()) {
                return null;
            }
            nextRemoteEntityId++;
            ServerRegistry newServerRegistry = new ServerRegistry(
                    new HashMap<>(serverRegistry.registryEntries),
                    new HashMap<>(serverRegistry.entityEntries),
                    new HashMap<>(SidedMetadataAPI.getSelfMetadata()));
            for (RegistryEntry registryEntry : toInjectEntityEntries.values()) {
                if (registryEntry.realId >= INITIAL_ENTITY_ID) {
                nextRemoteEntityId++;
                newServerRegistry.registryEntries.put(registryEntry.name,
                        new RegistryEntry((short) nextRemoteEntityId, registryEntry.name));
                }
            }
            return newServerRegistry;
        }
    }
}
