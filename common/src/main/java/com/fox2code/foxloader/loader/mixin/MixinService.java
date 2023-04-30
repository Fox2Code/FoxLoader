package com.fox2code.foxloader.loader.mixin;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.PreLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterJava;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

public class MixinService extends MixinServiceAbstract implements IMixinService,
        IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    private final ContainerHandleVirtual containerHandleVirtual = new ContainerHandleVirtual("foxloader");

    public MixinService() {
        if (FoxLauncher.getFoxClassLoader() != this.getClass().getClassLoader()) {
            throw new Error("WTF?! MixinService is loaded in the wrong context?");
        }
    }

    @Override
    public String getName() {
        return "FoxMixinService";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return containerHandleVirtual;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_8;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return FoxLauncher.getFoxClassLoader().getResourceAsStream(name);
    }

    @Override
    public URL[] getClassPath() {
        return FoxLauncher.getFoxClassLoader().getURLs();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return FoxLauncher.getFoxClassLoader().loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, FoxLauncher.getFoxClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, FoxLauncher.getFoxClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, false);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        ClassNode classNode = new ClassNode();
        InputStream is = FoxLauncher.getFoxClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (is == null) {
            System.err.println("Failed to find class \"" + name + "\" for mixin");
            throw new ClassNotFoundException(name);
        }
        new ClassReader(is).accept(classNode, 0);
        // Always apply PreLoader
        PreLoader.patchForMixin(classNode, name);
        return classNode;
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return null;
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return null;
    }

    @Override
    public void addTransformerExclusion(String name) {
        FoxLauncher.getFoxClassLoader().addTransformerExclusion(name);
    }

    @Override
    public void registerInvalidClass(String className) {
        //Invalid classes are not implemented in this context
    }

    @Override
    public boolean isClassLoaded(String className) {
        return FoxLauncher.getFoxClassLoader().isClassLoaded(className);
    }

    @Override
    public String getClassRestrictions(String className) {
        return FoxLauncher.getFoxClassLoader().isTransformExclude(className) ?
                "PACKAGE_TRANSFORMER_EXCLUSION" : "";
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal internal) {
        super.offer(internal);
        if (internal instanceof IMixinTransformerFactory) {
            MixinEnvironment.getCurrentEnvironment().setActiveTransformer(
                    ((IMixinTransformerFactory) internal).createTransformer());

        }
    }

    protected ILogger createLogger(final String name) {
        return new LoggerAdapterJava(name) {
            @Override
            public void catching(Throwable t) {
                this.log(Level.WARN, "Catching " + t.getClass().getName() + ": " + t.getMessage(), t);
            }
        };
    }
}
