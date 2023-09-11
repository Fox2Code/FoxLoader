package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.mixin.MixinBootstrapService;
import com.fox2code.foxloader.loader.mixin.MixinService;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.util.HashSet;

/**
 * We must put mixin specific code in a separate instance because of class loading order.
 */
final class ModLoaderMixin {
    private static final HashSet<String> activeConfigurations = new HashSet<>();

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
        FoxLauncher.getFoxClassLoader().addClassTransformers((bytes, className) ->
                mixinTransformer.transformClassBytes(className, className, bytes));
        MixinExtrasBootstrap.init();
    }

    static boolean addMixinConfigurationSafe(String modId, String mixin) {
        return addMixinConfigurationSafe(modId, mixin, true);
    }

    static boolean addMixinConfigurationSafe(String modId, String mixin, boolean explicit) {
        if (mixin != null && !activeConfigurations.contains(mixin)) {
            if (FoxLauncher.getFoxClassLoader().getResource(mixin) != null) {
                activeConfigurations.add(mixin);
                Mixins.addConfiguration(mixin);
                System.out.println("Loaded mixin: " + mixin);
                // Used for spark compatibility
                Mixins.getConfigs().stream().filter(config1 ->
                        config1.getName().equals(mixin)).findFirst().ifPresent(config ->
                        config.getConfig().decorate("foxLoader.modId", modId));
                return true;
            } else if (explicit) {
                System.out.println("Explicitly defined mixin doesn't exist: \"" + mixin + "\"");
            }
        }
        return false;
    }
}
