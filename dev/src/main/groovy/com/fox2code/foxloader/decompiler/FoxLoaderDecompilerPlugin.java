package com.fox2code.foxloader.decompiler;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.main.plugins.PluginContext;

public class FoxLoaderDecompilerPlugin implements Plugin {
    @Override
    public String id() {
        return "FoxLoader";
    }

    @Override
    public String description() {
        return "FoxLoader Decompiler plugin";
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        if (PluginContext.getCurrentContext().getVariableRenamer() != FoxVariableNamingFactory.INSTANCE) {
            throw new RuntimeException("FoxLoader Variable renaming factory was not selected!");
        }
    }

    @Override
    public @Nullable IVariableNamingFactory getRenamingFactory() {
        return FoxVariableNamingFactory.INSTANCE;
    }
}
