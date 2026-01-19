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
package com.fox2code.foxloader.dev.compatibility

import com.fox2code.foxloader.dev.CompatibilityModule
import com.fox2code.foxloader.dev.FoxLoaderConfig
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.specs.Spec

final class ShadowCompatibility extends CompatibilityModule {
    public static CompatibilityModule INSTANCE = new ShadowCompatibility()

    private ShadowCompatibility() {
        super("shadow")
    }

    @Override
    void onApplyOnConfig(Project project, FoxLoaderConfig config) {
        config.useJarTask(project.tasks.named('shadowJar', ShadowJar))
        project.tasks.jar.enabled = false
        project.tasks.assemble.dependsOn("shadowJar")
        project.tasks.named('shadowJar', ShadowJar) {
            archiveClassifier.set("")
        }
    }

    @Override
    void onLateApply(Project project, FoxLoaderConfig config) {
        final HashSet<String> dependenciesToExclude = new HashSet<>()
        appendDependenciesNamesToExclude(config, dependenciesToExclude)
        project.tasks.named('shadowJar', ShadowJar) {
            dependencies {
                exclude(new Spec<? super ResolvedDependency>() {
                    @Override
                    boolean isSatisfiedBy(Object o) {
                        return o instanceof ResolvedDependency &&
                                isSatisfiedBy(o as ResolvedDependency)
                    }

                    boolean isSatisfiedBy(ResolvedDependency resolvedDependency) {
                        String dependencyName = resolvedDependency.getName()
                        return dependenciesToExclude.contains(dependencyName) ||
                                // Exclude Mixins
                                dependencyName.startsWith("net.fabricmc:sponge-mixin:") ||
                                dependencyName.startsWith("org.spongepowered:mixin:") ||
                                // Exclude Junit
                                dependencyName.startsWith("junit:junit:") ||
                                dependencyName.startsWith("org.junit.jupiter:") ||
                                dependencyName.startsWith("org.junit.platform:") ||
                                // Exclude LWJGL2
                                dependencyName.startsWith("net.java.jutils:") ||
                                dependencyName.startsWith("net.java.jinput:") ||
                                dependencyName.startsWith("org.lwjgl.lwjgl:") ||
                                // Exclude LWJGL3
                                dependencyName.startsWith("org.lwjgl:")
                    }
                })
            }
        }
    }
}
