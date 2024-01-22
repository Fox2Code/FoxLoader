package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.launcher.DependencyHelper;
import com.fox2code.foxloader.launcher.utils.SourceUtil;
import com.fox2code.jfallback.JFallbackClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class FoxLoaderDecompilerHelper extends JFallbackClassLoader {
    private IFoxLoaderDecompilerProvider provider;
    private final Thread loadVineFlowerThread;
    private boolean loaded = false;

    public FoxLoaderDecompilerHelper() throws MalformedURLException {
        super(new URL[]{SourceUtil.getSourceFile(FoxLoaderDecompilerHelper.class).toURI().toURL()},
                FoxLoaderDecompilerHelper.class.getClassLoader());
        (this.loadVineFlowerThread = new Thread(() -> {
            File vineFlower = DependencyHelper.loadDependencyAsFile(DependencyHelper.vineFlower);
            try {
                this.addURL(vineFlower.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.loaded = true;
        }, "Load VineFlower Thread")).start();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("com.fox2code.foxloader.dev11.")) {
            Class<?> cls = this.findLoadedClass(name);
            return cls != null ? cls : this.findClass(name);
        } else {
            return super.loadClass(name, resolve);
        }
    }

    public void decompile(File source, File destination, boolean client) throws ReflectiveOperationException {
        if (this.loadVineFlowerThread.isAlive()) {
            try {
                this.loadVineFlowerThread.join(10000L);
            } catch (InterruptedException ignored) {}
        }
        if (!this.loaded) {
            throw new RuntimeException("Failed to download VineFlower");
        }
        if (this.provider == null) {
            this.provider = this.loadClass("com.fox2code.foxloader.dev11.FoxLoaderDecompilerProvider")
                    .asSubclass(IFoxLoaderDecompilerProvider.class).newInstance();
        }
        this.provider.newDecompiler(source, destination, client).decompile();
    }
}
