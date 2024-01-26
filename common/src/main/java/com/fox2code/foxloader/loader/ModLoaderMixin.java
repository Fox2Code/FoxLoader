package com.fox2code.foxloader.loader;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.fox2code.foxloader.launcher.ClassGenerator;
import com.fox2code.foxloader.launcher.ClassTransformer;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.mixin.MixinBootstrapService;
import com.fox2code.foxloader.loader.mixin.MixinService;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * We must put mixin specific code in a separate instance because of class loading order.
 */
final class ModLoaderMixin {
    private static final HashMap<String, String> activeConfigurations = new HashMap<>();
    private static final HashMap<String, String> mixinPackages = new HashMap<>();

    static void initializeMixin(boolean client) {
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
        MixinEnvironment.getCurrentEnvironment().setSide(client ?
                MixinEnvironment.Side.CLIENT : MixinEnvironment.Side.SERVER);
        MixinBootstrap.getPlatform().inject();
        IMixinTransformer mixinTransformer = // Inject mixin transformer into class loader.
                (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        FoxLoaderMixinWrapper foxLoaderMixinWrapper = new FoxLoaderMixinWrapper(mixinTransformer);
        FoxLauncher.getFoxClassLoader().addClassTransformers(foxLoaderMixinWrapper);
        FoxLauncher.getFoxClassLoader().addClassGenerator(foxLoaderMixinWrapper);
        MixinExtrasBootstrap.init();
        MixinSquaredBootstrap.init();
    }

    static boolean addMixinConfigurationSafe(String modId, String mixin) {
        return addMixinConfigurationSafe(modId, mixin, true);
    }

    /**
     * @param modId the modId of the mod trying to register that mixin
     * @param mixin the mixin config file (Ending in {@code .json})
     * @param explicit if the file was explicitly defined by the mod
     * @return if caller should try a fallback name if available
     */
    static boolean addMixinConfigurationSafe(String modId, String mixin, boolean explicit) {
        if (mixin == null || modId == null) return true;
        String oldModId = activeConfigurations.get(mixin);
        if (oldModId == null) {
            String mixinPackage = null;
            try (InputStream resource = FoxLauncher
                    .getFoxClassLoader().getResourceAsStream(mixin)) {
                if (resource != null) {
                    JsonObject jsonObject = ModLoader.gson.fromJson(
                            new InputStreamReader(resource, StandardCharsets.UTF_8), JsonObject.class);
                    if (jsonObject.has("package")) {
                        mixinPackage = jsonObject.get("package").getAsString();
                    }
                } else if (explicit) {
                    System.out.println("Explicitly defined mixin doesn't exist: \"" + mixin + "\"");
                    return true;
                }
            } catch (Exception e) {
                ModLoader.getModLoaderLogger().log(Level.WARNING, "Failed to read mixin config", e);
                return false;
            }
            if (mixinPackage != null) {
                String oldMixin = mixinPackages.putIfAbsent(mixinPackage, mixin);
                if (oldMixin != null) {
                    ModLoader.getModLoaderLogger().log(Level.SEVERE, "Mixin " + mixin +
                            " use same mixin package as " + oldMixin);
                    ModLoader.getModLoaderLogger().log(Level.SEVERE,
                            "As this causes issues, " + mixin + " has been disabled!");
                    return false;
                }
                activeConfigurations.put(mixin, modId);
                Mixins.addConfiguration(mixin);
                System.out.println("Loaded mixin: " + mixin);
                // Used for spark compatibility
                Mixins.getConfigs().stream().filter(config1 ->
                        config1.getName().equals(mixin)).findFirst().ifPresent(config ->
                        config.getConfig().decorate("foxLoader.modId", modId));
                return false;
            } else if (explicit) {
                ModLoader.getModLoaderLogger().log(Level.WARNING,
                        "Explicitly defined mixin is invalid: \"" + mixin + "\"");
            }
        } else if (!modId.equals(oldModId)) {
            ModLoader.getModLoaderLogger().log(Level.WARNING,
                    "The mixin \"" + mixin + "\" has been defined twice by " + oldModId + " and " + modId);
        }
        return false;
    }

    private static final class FoxLoaderMixinWrapper implements ClassGenerator, ClassTransformer {
        private final IMixinTransformer mixinTransformer;

        private FoxLoaderMixinWrapper(IMixinTransformer mixinTransformer) {
            this.mixinTransformer = mixinTransformer;
        }

        @Override
        public byte[] generate(String className) {
            return this.mixinTransformer.transformClassBytes(className, className, null);
        }

        @Override
        public byte[] transform(byte[] bytes, String className) {
            return this.mixinTransformer.transformClassBytes(className, className, bytes);
        }
    }
}
