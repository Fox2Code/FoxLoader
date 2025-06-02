/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.patching.mixin;

import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.patching.PreLoader;
import com.fox2code.foxloader.utils.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterJava;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.IConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;

public class MixinService extends MixinServiceAbstract implements IMixinService,
        IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    private IConsumer<MixinEnvironment.Phase> phaseConsumer;

    public MixinService() {
        if (FoxLauncher.getFoxClassLoader() != this.getClass().getClassLoader()) {
            throw new Error("WTF?! MixinService is loaded in the wrong context?");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        this.phaseConsumer = phaseConsumer;
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
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return MixinModLoader.exposedPrimaryContainerHandle;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return MixinModLoader.exposedContainerHandles;
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
        return this.getClassNode(name, false, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        URL url = FoxLauncher.getFoxClassLoader().getResource(name.replace('.', '/') + ".class");
        if (url == null) {
            ModLoaderInit.getModLoaderLogger().warning(
                    "Failed to find class \"" + name + "\" for mixin");
            throw new ClassNotFoundException(name);
        }
        URLConnection urlConnection = url.openConnection();
        FileInfo fileInfo = null;
        if (urlConnection instanceof JarURLConnection) {
            fileInfo = FoxLauncher.getFoxClassLoader().findFileInfoFromJarURL(
                    ((JarURLConnection) urlConnection).getJarFileURL());
        }
        ClassNode classNode = new ClassNode();
        try (InputStream is = urlConnection.getInputStream()) {
            if (is == null) {
                System.err.println("Failed to read URL for class \"" + name + "\" for mixin");
                throw new ClassNotFoundException(name);
            }
            new ClassReader(PreLoader.downgradeClassBytes(IOUtils.readAllBytes(is), name))
                    .accept(classNode, readerFlags);
        }
        classNode = PreLoader.transformClassForMixins(fileInfo, name.replace('/', '.'), classNode);
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
        if (FoxClassLoader.isFoxLoaderLaunchClass(className)) {
            return "PACKAGE_CLASSLOADER_EXCLUSION,PACKAGE_TRANSFORMER_EXCLUSION";
        }
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

    public void onStartup() {
        this.phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
    }
}
