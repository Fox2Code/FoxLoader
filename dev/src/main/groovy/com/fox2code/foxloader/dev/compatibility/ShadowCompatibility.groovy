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
    }

    @Override
    void onLateApply(Project project, FoxLoaderConfig config) {
        project.tasks.named('shadowJar', ShadowJar) {
            archiveClassifier.set("")
            dependencies {
                for (String dependencyToExclude : getDependenciesNamesToExclude(config)) {
                    exclude(dependency(dependencyToExclude))
                }
                // LWJGLX support.
                exclude(new Spec<? super ResolvedDependency>() {
                    @Override
                    boolean isSatisfiedBy(Object o) {
                        return o instanceof ResolvedDependency &&
                                isSatisfiedBy(o as ResolvedDependency)
                    }

                    boolean isSatisfiedBy(ResolvedDependency resolvedDependency) {
                        String dependencyName = resolvedDependency.getName()
                        return dependencyName.startsWith("org.lwjgl.lwjgl:") ||
                                dependencyName.startsWith("org.lwjgl:")
                    }
                })
            }
        }
    }
}
