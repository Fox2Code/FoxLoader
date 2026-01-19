/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.patching.mixin;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.loader.java.JavaModInfo;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.moulberry.mixinconstraints.util.MixinHacks;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;

public final class MixinModLoader {
    private static final HashMap<String, String> activeConfigurations = new HashMap<>();
    private static final HashMap<String, String> mixinPackages = new HashMap<>();
    static final IContainerHandle exposedPrimaryContainerHandle =
            new ModContainerHandle(JavaModInfo.FOX_LOADER_MOD_INFO);
    static final Collection<IContainerHandle> exposedContainerHandles =
            Collections.singleton(exposedPrimaryContainerHandle);
    private static boolean preInitialized = false, initialized = false;

    private MixinModLoader() {}

    public static IMixinTransformer initializeMixin(boolean client) {
        if (preInitialized) throw new IllegalStateException("Duplicate call to initializeMixin");
        System.setProperty("mixin.bootstrapService", MixinBootstrapService.class.getName());
        System.setProperty("mixin.service", MixinService.class.getName());
        MixinBootstrap.init();
        MixinEnvironment.getCurrentEnvironment()
                .setOption(MixinEnvironment.Option.DISABLE_REFMAP, true);
        MixinEnvironment.getCurrentEnvironment()
                .setOption(MixinEnvironment.Option.DEBUG_INJECTORS, true);
        MixinEnvironment.getCurrentEnvironment()
                .setOption(MixinEnvironment.Option.DEBUG_VERBOSE, true);
        for (MixinEnvironment.Phase phase : new MixinEnvironment.Phase[]{
                MixinEnvironment.Phase.PREINIT, MixinEnvironment.Phase.INIT, MixinEnvironment.Phase.DEFAULT}) {
            MixinEnvironment.getEnvironment(phase).setSide(client ?
                    MixinEnvironment.Side.CLIENT : MixinEnvironment.Side.SERVER);
        }
        MixinBootstrap.getPlatform().inject();
        IMixinTransformer mixinTransformer = // Inject mixin transformer into class loader.
                (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        MixinExtrasBootstrap.init();
        MixinSquaredBootstrap.init();
        preInitialized = true;
        return mixinTransformer;
    }

    public static void notifyInitialized() {
        if (initialized) throw new IllegalStateException("Duplicate call to notifyInitialized!");
        if (!preInitialized) throw new IllegalStateException("Mixins are not pre initialized!");
        System.setProperty("mixinconstaints.abstraction", MixinConstraintsImpl.class.getName());
        MixinHacks.registerMixinExtension(new MixinConstraintsExtension());
        initialized = true;
        ((MixinService) org.spongepowered.asm.service.MixinService.getService()).onStartup();
        if (MixinEnvironment.getEnvironment(MixinEnvironment.Phase.DEFAULT) != MixinEnvironment.getCurrentEnvironment())
            throw new Error("Mixin phase mismatch");
    }

    /**
     * @param modId the modId of the mod trying to register that mixin
     * @param mixin the mixin config file (Ending in {@code .json})
     * @return if caller should try a fallback name if available
     */
    public static boolean addMixinConfigurationSafe(String modId, String mixin) {
        return addMixinConfigurationSafe(modId, mixin, true, null);
    }

    /**
     * @param modId the modId of the mod trying to register that mixin
     * @param mixin the mixin config file (Ending in {@code .json})
     * @param explicit if the file was explicitly defined by the mod
     * @return if caller should try a fallback name if available
     */
    public static boolean addMixinConfigurationSafe(String modId, String mixin, boolean explicit) {
        return addMixinConfigurationSafe(modId, mixin, explicit, null);
    }

    /**
     * @param modId the modId of the mod trying to register that mixin
     * @param mixin the mixin config file (Ending in {@code .json})
     * @param explicit if the file was explicitly defined by the mod
     * @param mixinConfigHandler to handle the registered config
     * @return if caller should try a fallback name if available
     */
    public static boolean addMixinConfigurationSafe(
            String modId, String mixin, boolean explicit, MixinConfigHandler mixinConfigHandler) {
        if (!initialized) {
            throw new IllegalStateException("Trying to use Mixin service before it has been initialized");
        }
        if (mixin == null || modId == null) return true;
        String oldModId = activeConfigurations.get(mixin);
        if (oldModId == null) {
            String mixinPackage = null, mixinPlugin = null;
            boolean hasMixins = false;
            try (InputStream resource = FoxLauncher
                    .getFoxClassLoader().getResourceAsStream(mixin)) {
                if (resource != null) {
                    JsonObject jsonObject = ModLoaderInit.gson.fromJson(
                            new InputStreamReader(resource, StandardCharsets.UTF_8), JsonObject.class);
                    if (jsonObject.has("package")) {
                        mixinPackage = jsonObject.get("package").getAsString();
                    }
                    if (jsonObject.has("plugin")) {
                        mixinPlugin = jsonObject.get("plugin").getAsString();
                        hasMixins = true;
                    }
                    if (jsonObject.has("mixins") && !hasMixins) {
                        hasMixins = !jsonObject.get("mixins").getAsJsonArray().isEmpty();
                    }
                } else if (explicit) {
                    System.out.println("Explicitly defined mixin doesn't exist: \"" + mixin + "\"");
                    return true;
                }
            } catch (Exception e) {
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Failed to read mixin config", e);
                return false;
            }
            if (mixinPackage != null) {
                if ("com.moulberry.mixinconstraints.ConstraintsMixinPlugin".equals(mixinPlugin)) {
                    ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Mixin " + mixin +
                            " set MixinConstraints as mixin plugin, but this isn't necessary.");
                }
                if (!mixinPackage.endsWith(".")) {
                    mixinPackage += ".";
                }
                String oldMixin = mixinPackages.putIfAbsent(mixinPackage, mixin);
                if (oldMixin != null) {
                    ModLoaderInit.getModLoaderLogger().log(Level.SEVERE, "Mixin " + mixin +
                            " use same mixin package as " + oldMixin);
                    ModLoaderInit.getModLoaderLogger().log(Level.SEVERE,
                            "As this causes issues, " + mixin + " has been disabled!");
                    return false;
                }
                activeConfigurations.put(mixin, modId);
                ModContainer modContainer =
                        ModLoaderInit.getModContainer(modId);
                Mixins.addConfiguration(
                        mixin, modContainer != null ?
                        modContainer.getModInfo() : null);
                if (hasMixins) {
                    ModLoaderInit.Internal.markAddMixin(modContainer);
                }
                System.out.println("Loaded mixin: " + mixin);
                // Used for spark compatibility
                Mixins.getConfigs().stream().filter(config1 ->
                        config1.getName().equals(mixin)).findFirst().ifPresent(config -> {
                    config.getConfig().decorate(FabricUtil.KEY_MOD_ID, modId);
                    config.getConfig().decorate("foxLoader.modId", modId);
                });
                if (mixinConfigHandler != null) {
                    mixinConfigHandler.onConfigAdded(modId, mixin, mixinPackage);
                }
                return false;
            } else if (explicit) {
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                        "Explicitly defined mixin is invalid: \"" + mixin + "\"");
            }
        } else if (!modId.equals(oldModId)) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "The mixin \"" + mixin + "\" has been defined twice by " + oldModId + " and " + modId);
        }
        return false;
    }

    private static class ModContainerHandle extends ContainerHandleURI {
        private final ModInfo modInfo;

        public ModContainerHandle(ModInfo modInfo) {
            super(modInfo.toURI());
            this.modInfo = modInfo;
        }

        @Override
        public String getDescription() {
            return this.modInfo.description;
        }

        @Override
        public String getId() {
            return this.modInfo.id;
        }

        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public File getFile() {
            return this.modInfo.file;
        }
    }

    @FunctionalInterface
    public interface MixinConfigHandler {
        void onConfigAdded(String modId, String mixinConfig, String mixinPackageName);
    }
}
