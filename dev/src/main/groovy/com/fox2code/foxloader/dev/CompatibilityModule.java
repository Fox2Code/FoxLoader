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
package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Collection;

public abstract class CompatibilityModule {
    private final String id;

    public CompatibilityModule(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void onApplyOnConfig(Project project, FoxLoaderConfig config) {}

    public void onLateApply(Project project, FoxLoaderConfig config) {}

    public static ArrayList<String> getDependenciesNamesToExclude(FoxLoaderConfig config) {
        ArrayList<String> dependenciesToExclude = new ArrayList<>();
        appendDependenciesNamesToExclude(config, dependenciesToExclude);
        return dependenciesToExclude;
    }

    public static void appendDependenciesNamesToExclude(FoxLoaderConfig config, Collection<String> collection) {
        collection.add("org.lwjgl.lwjgl:lwjgl:2.9.1");
        collection.add("org.lwjgl.lwjgl:lwjgl_util:2.9.1");
        collection.add("org.lwjgl.lwjgl:lwjgl-platform:2.9.1");
        if (config.useLWJGLX) {
            collection.add("com.fox2code:lwjglx:" + config.LWJGLXVersion);
        }
        String foxLoaderVersion = BuildConfig.FOXLOADER_VERSION;
        if (config.foxLoaderLibVersionOverride != null) {
            foxLoaderVersion = config.foxLoaderLibVersionOverride;
        }
        collection.add("com.fox2code.FoxLoader:loader:" + foxLoaderVersion);
        for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
            collection.add(dependency.name);
        }
        for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependenciesModernJava) {
            collection.add(dependency.name);
        }
        for (String bundle : config.usedDependencyBundlesList) {
            for (DependencyHelper.Dependency dependency : DependencyHelper.getDependencyBundle(bundle)) {
                collection.add(dependency.name);
            }
        }
    }
}
