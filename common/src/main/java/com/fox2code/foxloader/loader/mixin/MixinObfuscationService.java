package com.fox2code.foxloader.loader.mixin;

import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MixinObfuscationService implements IObfuscationService {
    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor annotationProcessor) {
        return Collections.singletonList(new ObfuscationTypeDescriptor(
                "foxloader", null, null, MixinObfuscationEnvironment.class));
    }
}
