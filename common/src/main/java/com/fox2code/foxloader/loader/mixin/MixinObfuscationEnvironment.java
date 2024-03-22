package com.fox2code.foxloader.loader.mixin;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationEnvironment;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import java.io.File;

public class MixinObfuscationEnvironment extends ObfuscationEnvironment {
    public MixinObfuscationEnvironment(ObfuscationType type) {
        super(type);
    }

    @Override
    protected IMappingProvider getMappingProvider(Messager messager, Filer filer) {
        return new IMappingProvider() {
            @Override public void clear() {}
            @Override public boolean isEmpty() { return false; }
            @Override public void read(File file) {}
            @Override public MappingMethod getMethodMapping(MappingMethod mappingMethod) { return mappingMethod.copy(); }
            @Override public MappingField getFieldMapping(MappingField mappingField) { return mappingField.copy(); }
            @Override public String getClassMapping(String s) { return s; }
            @Override public String getPackageMapping(String s) { return s; }
        };
    }

    @Override
    protected IMappingWriter getMappingWriter(Messager messager, Filer filer) {
        return (s, obfuscationType, mappingSet, mappingSet1) -> {};
    }

    @Override
    public MappingField getObfField(MappingField field) {
        return field;
    }

    @Override
    public MappingField getObfField(ITargetSelectorRemappable field) {
        return field == null ? null : field.asFieldMapping();
    }

    @Override
    public MappingField getObfField(MappingField field, boolean lazyRemap) {
        return field;
    }

    @Override
    public MappingMethod getObfMethod(MappingMethod method) {
        return method;
    }

    @Override
    public MappingMethod getObfMethod(ITargetSelectorRemappable method) {
        return method == null ? null : method.asMethodMapping();
    }

    @Override
    public MappingMethod getObfMethod(MappingMethod method, boolean lazyRemap) {
        return method;
    }

    @Override
    public String getObfClass(String className) {
        return className;
    }
}
