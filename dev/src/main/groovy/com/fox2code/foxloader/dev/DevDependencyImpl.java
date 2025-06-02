package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.dependencies.DependencyHelper;

import java.net.URL;

public final class DevDependencyImpl extends DependencyHelper.DependencyImpl {
    public static final DevDependencyImpl INSTANCE = new DevDependencyImpl();

    private DevDependencyImpl() {}

    @Override
    public boolean hasClass(String cls) {
        // Very simple but should be good enough for our use case in dev plugin.
        return DevDependencyImpl.class.getClassLoader()
                .getResource(cls.replace('.', '/') + ".class") != null;
    }

    @Override
    public boolean isDev() {
        return true;
    }

    @Override
    public URL getClassResource(String className) {
        return DevDependencyImpl.class.getClassLoader()
                .getResource(className.replace('.', '/') + ".class");
    }
}
