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
package com.fox2code.foxloader.loader.java;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.utils.io.JarUtils;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;

public final class JavaModInfo extends ModInfo {
    private static final Attributes.Name MOD_CLASS_TRANSFORMER = new Attributes.Name("ModClassTransformer");
    private static final Attributes.Name MOD_LOADING_PLUGIN = new Attributes.Name("ModLoadingPlugin");
    private static final Attributes.Name MOD_JITPACK = new Attributes.Name("ModJitPack");
    private static final Attributes.Name MOD_MIXIN = new Attributes.Name("ModMixin");
    private static final Attributes.Name MOD_MAIN = new Attributes.Name("ModMain");
    private static final Attributes.Name FOR_FOX_LOADER_VERSION = new Attributes.Name("For-FoxLoader-Version");
    private static final Attributes.Name REQUEST_FOXLOADER_DEPENDENCY_BUNDLES =
            new Attributes.Name("Request-FoxLoader-Dependency-Bundles");
    public static final JavaModInfo FOX_LOADER_MOD_INFO;

    static {
        try {
            FOX_LOADER_MOD_INFO = new JavaModInfo(
                    FoxLauncher.foxLoaderFile, null, "foxloader", "FoxLoader", BuildConfig.FOXLOADER_VERSION,
                    "ReIndev mod loader with foxes!!!", "Fox2Code", "assets/foxloader/icon.png",
                    "any", false, Long.MAX_VALUE, "com.fox2code.foxloader.loader.ModLoader");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public final List<String> requestedDependencyBundles;
    public final String forFoxLoaderVersion;
    public final String classTransformer;
    public final String loadingPlugin;
    public final String jitPack;
    public final String mixin;
    public final String main;
    public final boolean builtin;
    final boolean explicitMixin;

    JavaModInfo(File file, String jarPath) throws IOException {
        this(file, jarPath, JarUtils.getManifest(file, jarPath).getMainAttributes());
    }

    private JavaModInfo(File file, String jarPath, Attributes mainAttributes) throws IOException {
        super(file, jarPath, mainAttributes);
        String requestedDependencyBundlesText = mainAttributes.getValue(REQUEST_FOXLOADER_DEPENDENCY_BUNDLES);
        if (requestedDependencyBundlesText == null || requestedDependencyBundlesText.isEmpty()) {
            this.requestedDependencyBundles = Collections.emptyList();
        } else {
            this.requestedDependencyBundles = Collections.unmodifiableList(
                    Arrays.asList(requestedDependencyBundlesText.split(",")));
        }
        this.forFoxLoaderVersion = mainAttributes.getValue(FOR_FOX_LOADER_VERSION);
        this.classTransformer = mainAttributes.getValue(MOD_CLASS_TRANSFORMER);
        this.loadingPlugin = mainAttributes.getValue(MOD_LOADING_PLUGIN);
        this.jitPack = mainAttributes.getValue(MOD_JITPACK);
        String modMixin = mainAttributes.getValue(MOD_MIXIN);
        this.mixin = modMixin != null ? modMixin :
                this.id + ".mixins.json";
        this.explicitMixin = modMixin != null;
        this.main = mainAttributes.getValue(MOD_MAIN);
        this.builtin = false;
    }

    JavaModInfo(File file, String jarPath, String id, String name, String version, String description, String authors, String iconPath,
                        String environment, boolean unofficial, long loadOrderPriority, String main) throws IOException {
        super(file, jarPath, id, name, version, description, authors, iconPath, environment, unofficial, loadOrderPriority);
        this.requestedDependencyBundles = Collections.emptyList();
        this.forFoxLoaderVersion = BuildConfig.FOXLOADER_VERSION;
        this.classTransformer = null;
        this.loadingPlugin = null;
        this.jitPack = null;
        if ("foxloader".equals(id)) {
            this.mixin = "foxloader.mixins.json";
            this.explicitMixin = true;
        } else {
            this.mixin = null;
            this.explicitMixin = false;
        }
        this.main = main;
        this.builtin = true;
    }

    @Override
    public final boolean isJavaArchive() {
        return true;
    }

    @Override
    public Collection<String> getRequestedDependencyBundles() {
        return this.requestedDependencyBundles;
    }
}
