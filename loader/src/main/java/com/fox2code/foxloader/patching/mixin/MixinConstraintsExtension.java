package com.fox2code.foxloader.patching.mixin;

import com.moulberry.mixinconstraints.mixin.MixinTransformer;
import com.moulberry.mixinconstraints.util.MixinHacks;
import com.moulberry.mixinconstraints.util.Pair;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

class MixinConstraintsExtension implements IExtension {
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void preApply(ITargetClassContext context) {
        for (Pair<IMixinInfo, ClassNode> pair : MixinHacks.getMixinsFor(context)) {
            MixinTransformer.transform(pair.first(), pair.second());
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {

    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {

    }
}
