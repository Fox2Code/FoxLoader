package com.fox2code.foxloader.loader.mixin;

import com.fox2code.foxloader.launcher.FoxLauncher;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;

public class MixinBootstrapService implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "FoxMixinService";
    }

    @Override
    public String getServiceClassName() {
        return "com.fox2code.foxloader.loader.mixin.MixinService";
    }

    @Override
    public void bootstrap() {
        if (FoxLauncher.getFoxClassLoader() != this.getClass().getClassLoader()) {
            throw new ServiceInitialisationException(this.getName() + " is not available");
        }
    }
}
