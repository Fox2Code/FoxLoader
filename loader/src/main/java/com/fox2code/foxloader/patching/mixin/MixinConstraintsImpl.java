package com.fox2code.foxloader.patching.mixin;

import com.fox2code.flexver.FlexVer;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.moulberry.mixinconstraints.util.Abstractions;

public class MixinConstraintsImpl extends Abstractions {
    public MixinConstraintsImpl() {}

    @Override
    protected boolean isDevEnvironment() {
        return FoxLauncher.DEV_MODE || FoxLauncher.DEVELOPING_FOXLOADER;
    }

    @Override
    protected String getModVersion(String modId) {
        ModContainer modContainer = ModLoaderInit.getModContainer(modId);
        return modContainer == null ? null : modContainer.getModInfo().version;
    }

    @Override
    protected boolean isVersionInRange(String version, String min, String max) {
        FlexVer versionFlex = FlexVer.parse(version);
        FlexVer minFlex = FlexVer.parse(version);
        FlexVer maxFlex = FlexVer.parse(version);
        return versionFlex.isGreaterOrEqual(minFlex) &&
                maxFlex.isGreaterOrEqual(versionFlex);
    }

    @Override
    protected String getPlatformName() {
        return "FoxLoader";
    }
}
