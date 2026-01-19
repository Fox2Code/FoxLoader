/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import java.text.Normalizer;
import java.util.*;

/**
 * Config for the FoxLoader gradle plugin
 */
public class FoxLoaderConfig {
    public FoxLoaderConfig() {}

    boolean configImmutable = false;
    boolean decompileSources = // Only decompile sources if we have no CI to not waste server time
            System.getenv("CI") == null && System.getenv("JITPACK") == null;
    boolean addJitPackCIPublish = false;
    ArrayList<String> usedDependencyBundlesList = new ArrayList<>();
    String username = Normalizer.normalize(System.getProperty("user.name"),
            Normalizer.Form.NFD).replaceAll("[^a-zA-Z0-9_]+","");
    public String modMain;
    public String modId = "null";
    public String modVersion;
    public String modName;
    public String modDesc;
    public String modAuthors;
    public String modIcon;
    public String modEnvironment;
    public String modWebsite;
    public String modClassTransformer;
    public String modLoadingPlugin;
    public boolean unofficial = false;

    public void modDesc() {
        this.checkConfigMutable();
        if (this.modDesc == null) {
            this.modDesc = "";
        } else {
            this.modDesc += "\n";
        }
    }

    public void modDesc(String text) {
        this.checkConfigMutable();
        if (this.modDesc == null) {
            this.modDesc = text;
        } else {
            this.modDesc += "\n" + text;
        }
    }

    public void useDependencyBundle(String dependencyBundle) {
        this.checkConfigMutable();
        if (!DependencyHelper.availableDependencyBundles.contains(dependencyBundle)) {
            throw new NoSuchElementException("Unknown dependency bundle ID: " + dependencyBundle);
        }
        if (!this.usedDependencyBundlesList.contains(dependencyBundle)) {
            this.usedDependencyBundlesList.add(dependencyBundle);
        }
    }

    public String getUsedDependencyBundles() {
        StringJoiner stringJoiner = new StringJoiner(",");
        for (String dependencyBundle : this.usedDependencyBundlesList) {
            stringJoiner.add(dependencyBundle);
        }
        return stringJoiner.toString();
    }

    // Compatibility support
    final LinkedHashMap<String, CompatibilityModule> compatibilityModules = new LinkedHashMap<>();
    public boolean disableKotlinCompatibility = false;
    public boolean disableShadowCompatibility = false; // -> https://github.com/GradleUp/shadow

    public void applyCompatibilityModule(CompatibilityModule compatibilityModule) {
        this.checkConfigMutable();
        if (!this.compatibilityModules.containsKey(compatibilityModule.getId())) {
            this.compatibilityModules.put(compatibilityModule.getId(), compatibilityModule);
        }
    }

    // Special jar task replacement for complex build scripts
    Jar jarTaskToUse;
    TaskProvider<? extends Task> jarTaskToUseProvider;

    public final void useJarTask(Jar jarTaskToUse) {
        this.checkConfigMutable();
        this.jarTaskToUse = jarTaskToUse;
        this.jarTaskToUseProvider = null;
    }

    public final void useJarTask(TaskProvider<? extends Task> jarTaskToUseProvider) {
        this.checkConfigMutable();
        this.jarTaskToUse = null;
        this.jarTaskToUseProvider = jarTaskToUseProvider;
    }

    final Jar getJarTaskToUse(Project project) {
        if (this.jarTaskToUse != null) {
            return this.jarTaskToUse;
        }
        if (this.jarTaskToUseProvider != null) {
            return (Jar) this.jarTaskToUseProvider.get();
        }
        return (Jar) project.getTasks().named("jar").get();
    }

    // For testing only
    public String dumpClass;
    public String foxLoaderLibVersionOverride;
    public boolean localTesting = false;
    public boolean forceReload = false;
    public boolean useLWJGLX = false;
    public String LWJGLXVersion = BuildConfig.LWJGLX_VERSION;
    public String LWJGLXLWJGLVersion = "3.3.6";

    private void checkConfigMutable() {
        if (this.configImmutable) {
            throw new IllegalStateException("Trying to modify config after it has been loaded");
        }
    }
}
