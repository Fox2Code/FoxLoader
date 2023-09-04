package com.fox2code.foxloader.loader.mixin;

import com.fox2code.foxloader.loader.ModLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MixinTestModePlugin implements IMixinConfigPlugin {
    private static final HashSet<String> testModeMixins = new HashSet<>(
            Arrays.asList("MixinEntity", "MixinEntityPlayerMP",
                    "MixinMinecraftServer", "MixinStringTranslate"));

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!ModLoader.TEST_MODE) return true;
        int i = mixinClassName.lastIndexOf('.');
        return testModeMixins.contains(
                mixinClassName.substring(i + 1));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
