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
package com.fox2code.foxloader.dev

import com.fox2code.foxloader.dependencies.DependencyHelper
import com.fox2code.foxloader.dev.compatibility.KotlinCompatibility
import com.fox2code.foxloader.dev.compatibility.ShadowCompatibility
import com.fox2code.foxloader.launcher.BuildConfig
import com.fox2code.foxloader.patching.PatchBridge
import com.fox2code.foxloader.utils.Platform
import com.fox2code.foxloader.utils.SourceUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.gradle.api.*
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.objectweb.asm.Type

import javax.swing.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.text.Normalizer

class GradlePlugin implements Plugin<Project> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create()
    private static final Object processLock = new Object()
    static {
        DependencyHelper.DependencyImpl.install(DevDependencyImpl.INSTANCE)
    }
    private File foxLoaderCache
    private File foxLoaderData
    private File mcLibraryRoot

    @Override
    void apply(Project project) {
        final int javaVersionInt = Integer.parseInt(String.valueOf(
                project.getProperties().getOrDefault("foxloader.java-version", "21")))
        if (javaVersionInt < DevConstants.MIN_JAVA_VERSION) {
            throw new IllegalArgumentException(
                    "Java version was set to ${javaVersionInt} but minimum is " + DevConstants.MIN_JAVA_VERSION)
        } else if (javaVersionInt > DevConstants.MAX_JAVA_VERSION) {
            throw new IllegalArgumentException(
                    "Java version was set to ${javaVersionInt} but maximum is " + DevConstants.MAX_JAVA_VERSION)
        }
        final JavaVersion javaVersion = JavaVersion.toVersion(javaVersionInt)
        foxLoaderCache = new File(project.gradle.getGradleUserHomeDir(), "fox-loader")
        foxLoaderData = new File(foxLoaderCache, "data.json")
        mcLibraryRoot = new File(Platform.getAppDir("minecraft"), "libraries")
        DependencyHelper.setMCLibraryRoot(this.mcLibraryRoot)
        project.apply([plugin: 'java-library'])
        project.apply([plugin: 'maven-publish'])
        project.apply([plugin: 'eclipse'])
        project.apply([plugin: 'idea'])
        project.eclipse.classpath.downloadSources = true
        project.idea.module.downloadSources = true
        project.repositories {
            maven {
                url foxLoaderCache.toURI().toString()
            }
            mavenCentral()
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
                content {
                    includeGroup "maven.modrinth"
                }
            }
            maven {
                name = 'Sponge'
                url 'https://repo.spongepowered.org/maven'
                content {
                    includeGroup "org.spongepowered"
                }
            }
            maven {
                name = 'Fabric'
                url 'https://maven.fabricmc.net/'
                content {
                    includeGroup "net.fabricmc"
                }
            }
            maven {
                name = 'Unascribed'
                url "https://repo.unascribed.com"
                content {
                    includeGroup "com.unascribed"
                }
            }
            maven {
                name = 'Sleeping Town'
                url 'https://repo.sleeping.town/'
                content {
                    includeGroup 'blue.endless'
                }
                if (javaVersionInt < 21) {
                    metadataSources {
                        artifact()
                    }
                }
            }
            maven {
                url 'https://cdn.fox2code.com/maven'
            }
            maven {
                url 'https://jitpack.io/'
            }
        }
        project.java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(javaVersionInt)
            }

            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion

            withSourcesJar()
            withJavadocJar()
        }
        project.configurations {
            sourcePreDownload
            sourcePreDownload.transitive = false
            sourcePreDownload.canBeResolved = true
        }
        project.tasks.javadoc.failOnError false
        // Support "@reason" javadoc Mixin tag mandated by Minecraft-Dev Intellij plugin
        project.tasks.javadoc.options.tags = [ "reason" ]
        project.tasks.javadoc.options.addStringOption('Xdoclint:none', '-quiet')
        project.tasks.register("runClient", JavaExec) {
            group = "FoxLoader"
            description = "Run ReIndev client from gradle"
        }.get().dependsOn(project.getTasks().named("assemble"))
        project.tasks.register("runServer", JavaExec) {
            group = "FoxLoader"
            description = "Run ReIndev server from gradle"
        }.get().dependsOn(project.getTasks().named("assemble"))
        JsonObject foxLoaderJsonData = readData()
        project.tasks.register("changeDefaultUsername", Task) {
            group = "FoxLoader"
            description = "Change the default username used for FoxLoader"
        }.get().doLast {
            JsonElement jsonElement = foxLoaderJsonData.get("username")
            String username
            if (jsonElement == null) {
                username = Normalizer.normalize(System.getProperty("user.name"),
                        Normalizer.Form.NFD).replaceAll("[^a-zA-Z0-9_]+","")
            } else {
                username = jsonElement.asString
            }
            username = JOptionPane.showInputDialog("Default username = ???", username)
            if (username == null || username.isEmpty()) {
                foxLoaderJsonData.remove("username")
            } else {
                foxLoaderJsonData.addProperty("username", username)
            }
            saveData(foxLoaderJsonData)
        }
        project.extensions.create("foxloader", FoxLoaderConfig)
        File buildDir = project.layout.buildDirectory.get().asFile
        File gitIgnore = new File(buildDir, ".gitignore")
        if (buildDir.exists() && !gitIgnore.exists()) {
            Files.write(gitIgnore.toPath(), "*".getBytes(StandardCharsets.UTF_8))
        }
        File runDir = new File(project.projectDir, "run").getAbsoluteFile()
        if (runDir.exists() || runDir.mkdirs()) {
            File gitIgnoreRun = new File(runDir, ".gitignore")
            if (!gitIgnoreRun.exists()) {
                Files.write(gitIgnoreRun.toPath(), "*".getBytes(StandardCharsets.UTF_8))
            }
        }
        project.afterEvaluate {
            project.tasks.withType(JavaCompile.class).configureEach {
                options.compilerArgs += '-g'
                options.encoding = 'UTF-8'
            }
            FoxLoaderConfig config = ((FoxLoaderConfig) project.extensions.getByName("foxloader"))
            if ((!config.disableKotlinCompatibility) &&
                    project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
                config.applyCompatibilityModule(KotlinCompatibility.INSTANCE)
            }
            if ((!config.disableShadowCompatibility) &&
                    project.pluginManager.hasPlugin("com.gradleup.shadow")) {
                config.applyCompatibilityModule(ShadowCompatibility.INSTANCE)
            }
            for (CompatibilityModule compatibilityModule : config.compatibilityModules.values()) {
                compatibilityModule.onApplyOnConfig(project, config)
            }
            config.configImmutable = true
            for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
                if (dependency == DependencyHelper.jvmDowngraderCore ||
                        dependency == DependencyHelper.jvmDowngraderJavaAPI) {
                    continue
                }
                addDependencyToProject(project, dependency)
            }
            for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependenciesModernJava) {
                if (javaVersionInt >= dependency.javaSupport) {
                    addDependencyToProject(project, dependency)
                }
            }
            for (String dependencyBundle : config.usedDependencyBundlesList) {
                for (DependencyHelper.Dependency dependency : DependencyHelper.getDependencyBundle(dependencyBundle)) {
                    addDependencyToProject(project, dependency)
                }
            }
            project.configurations.configureEach {
                exclude group: 'org.spongepowered', module: 'mixin'
            }
            process(project, foxLoaderCache, config)
            String foxLoaderVersion = BuildConfig.FOXLOADER_VERSION
            if (config.foxLoaderLibVersionOverride != null) {
                foxLoaderVersion = config.foxLoaderLibVersionOverride
            }
            project.dependencies {
                runtimeOnly(BuildConfig.SPARK_DEPENDENCY)
            }
            if (config.localTesting) {
                project.dependencies {
                    implementation("com.github.Fox2Code.FoxLoader:loader:1.0")
                }
            } else {
                project.dependencies {
                    implementation("com.fox2code.FoxLoader:loader:${foxLoaderVersion}")
                    sourcePreDownload("com.fox2code.FoxLoader:loader:${foxLoaderVersion}:sources")
                }
            }
            if (config.useLWJGLX) {
                project.configurations.configureEach {
                    exclude group: 'org.lwjgl.lwjgl', module: 'lwjgl'
                    exclude group: 'org.lwjgl.lwjgl', module: 'lwjgl_util'
                    exclude group: 'org.lwjgl.lwjgl', module: 'lwjgl-platform'
                }

                String lwjglNatives

                switch (OperatingSystem.current()) {
                    case OperatingSystem.LINUX:
                        def osArch = System.getProperty("os.arch")
                        lwjglNatives = osArch.startsWith("arm") || osArch.startsWith("aarch64")
                                ? "natives-linux-${osArch.contains("64") || osArch.startsWith("armv8") ? "arm64" : "arm32"}"
                                : "natives-linux"
                        break
                    case OperatingSystem.MAC_OS:
                        lwjglNatives = "natives-macos"
                        break
                    case OperatingSystem.WINDOWS:
                        def osArch = System.getProperty("os.arch")
                        lwjglNatives = osArch.contains("64")
                                ? "natives-windows${osArch.startsWith("aarch64") ? "-arm64" : ""}"
                                : "natives-windows-x86"
                        break
                }

                project.dependencies {
                    implementation(platform("org.lwjgl:lwjgl-bom:${config.LWJGLXLWJGLVersion}"))
                    implementation("com.fox2code:lwjglx:${config.LWJGLXVersion}")
                    runtimeOnly "org.lwjgl:lwjgl::$lwjglNatives"
                    runtimeOnly "org.lwjgl:lwjgl-glfw::$lwjglNatives"
                    runtimeOnly "org.lwjgl:lwjgl-openal::$lwjglNatives"
                    runtimeOnly "org.lwjgl:lwjgl-opengl::$lwjglNatives"
                }
            } else {
                project.dependencies {
                    implementation("org.lwjgl.lwjgl:lwjgl:2.9.1")
                    implementation("org.lwjgl.lwjgl:lwjgl_util:2.9.1")
                    implementation("org.lwjgl.lwjgl:lwjgl-platform:2.9.1")
                }
            }
            project.dependencies {
                implementation("net.java.jinput:jinput:2.0.5")
            }
            Objects.requireNonNull(config.modId, "The mod id cannot be null!")
            if (config.modVersion == null && project.version != null) {
                config.modVersion = project.version.toString()
            }
            Jar jarTask = config.getJarTaskToUse(project)
            jarTask.manifest {
                attributes 'For-FoxLoader-Version': BuildConfig.FOXLOADER_VERSION
                attributes 'For-ReIndev-Version': BuildConfig.REINDEV_VERSION
                if (!config.usedDependencyBundles.isEmpty()) {
                    attributes 'Request-FoxLoader-Dependency-Bundles': config.getUsedDependencyBundles()
                }
                attributes 'ModId': config.modId
                if (config.modMain != null &&
                        !config.modMain.isEmpty()) {
                    attributes 'ModMain': config.modMain
                }
                if (config.modName != null &&
                        !config.modName.isEmpty()) {
                    attributes 'ModName': config.modName
                }
                if (config.modVersion != null &&
                        !config.modVersion.isEmpty()) {
                    attributes 'ModVersion': config.modVersion
                }
                if (config.modDesc != null) {
                    attributes 'ModDesc': config.modDesc
                            .replace("\t", "    ")
                            .replace('\n', '\t')
                }
                if (config.modAuthors != null) {
                    attributes 'ModAuthors': config.modAuthors
                }
                if (config.modIcon != null) {
                    attributes 'ModIcon': config.modIcon
                }
                if (config.modEnvironment != null &&
                        !config.modEnvironment.isEmpty()) {
                    attributes 'ModEnvironment': config.modEnvironment
                }
                if (config.modWebsite != null &&
                        !config.modWebsite.isEmpty()) {
                    attributes 'ModWebsite': config.modWebsite
                }
                if (config.modClassTransformer != null &&
                        !config.modClassTransformer.isEmpty()) {
                    attributes 'ModClassTransformer': config.modClassTransformer
                }
                if (config.modLoadingPlugin != null &&
                        !config.modLoadingPlugin.isEmpty()) {
                    attributes 'ModLoadingPlugin': config.modLoadingPlugin
                }
                if (config.unofficial) {
                    attributes 'Unofficial': 'true'
                }
            }
            File mod = jarTask.getArchiveFile().get().getAsFile()
            JsonElement savedUsername = foxLoaderJsonData.get("username")
            String username = config.username
            if (savedUsername != null) {
                username = savedUsername.getAsString()
            }
            username = username.trim()
            if (username.size() < 4) {
                username = "Player" + new Random().nextInt(1000)
            }
            final JavaToolchainService toolchain = project.extensions.getByType(JavaToolchainService.class)
            final String java8executable = toolchain.launcherFor {
                it.languageVersion.set(JavaLanguageVersion.of(8))
            }.get().executablePath.asFile.absolutePath
            String dumpClass = config.dumpClass
            JavaExec runClient = project.getTasks().named("runClient").get() as JavaExec
            runClient.executable = java8executable
            runClient.classpath = project.sourceSets.main.runtimeClasspath
            runClient.mainClass.set("com.fox2code.foxloader.launcher.ClientMain")
            runClient.systemProperty("foxloader.inject-mod", mod.getAbsolutePath())
            runClient.systemProperty("foxloader.dev-mode", "true")
            if (dumpClass != null && !dumpClass.isEmpty()) {
                runClient.systemProperty("foxloader.dump-class", dumpClass)
            }
            if (config.localTesting) {
                runClient.systemProperty("foxloader.ignore-cache", "true")
            }
            runClient.workingDir = runDir
            runClient.args(username, "-", "--gameDir", runDir.getPath())
            JavaExec runServer = project.getTasks().named("runServer").get() as JavaExec
            runServer.executable = java8executable
            runServer.classpath = project.sourceSets.main.runtimeClasspath
            runServer.mainClass.set("com.fox2code.foxloader.launcher.ServerMain")
            runServer.systemProperty("foxloader.inject-mod", mod.getAbsolutePath())
            runServer.systemProperty("foxloader.dev-mode", "true")
            if (dumpClass != null && !dumpClass.isEmpty()) {
                runServer.systemProperty("foxloader.dump-class", dumpClass)
            }
            if (config.localTesting) {
                runServer.systemProperty("foxloader.ignore-cache", "true")
            }
            runServer.workingDir = runDir
            if (config.decompileSources) {
                project.configurations.sourcePreDownload.resolve()
            }
            if (project.rootProject == project &&
                    config.modVersion != null && !config.modVersion.isEmpty()) {
                File root = project.rootProject.rootDir
                File gitConfig = new File(root, ".git" + File.separator + "config")
                if (gitConfig.exists()) {
                    try (BufferedReader bufferedReader =
                            new BufferedReader(new FileReader(gitConfig))) {
                        String line
                        String url = null
                        boolean check = false
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line == "[remote \"origin\"]") check = true
                            else if (check) {
                                line = line.trim()
                                if (line.startsWith("url = ")) {
                                    url = line.substring(6)
                                    break
                                } else if (line.startsWith("[")) {
                                    break
                                }
                            }
                        }
                        if (url != null) {
                            if (url.endsWith(".git")) {
                                url = url.substring(0, url.length() - 4)
                            }
                            if (url.startsWith("git@github.com:")) {
                                addJitPackPublishTask(project, config,
                                        JitPackService.GITHUB, url.substring(15))
                            } else if (url.startsWith("https://github.com/")) {
                                addJitPackPublishTask(project, config,
                                        JitPackService.GITHUB, url.substring(19))
                            } else if (url.startsWith("git@gitlab.com:")) {
                                addJitPackPublishTask(project, config,
                                        JitPackService.GITLAB, url.substring(15))
                            } else if (url.startsWith("https://gitlab.com/")) {
                                addJitPackPublishTask(project, config,
                                        JitPackService.GITLAB, url.substring(19))
                            }
                        }
                    }
                }
            }
            for (CompatibilityModule compatibilityModule : config.compatibilityModules.values()) {
                compatibilityModule.onLateApply(project, config)
            }
        }
    }

    static void addDependencyToProject(Project project, DependencyHelper.Dependency dependency) {
        project.dependencies {
            implementation(dependency.name)
            if (!DependencyHelper.skipDevSources(dependency)) {
                sourcePreDownload(dependency.name + ":sources")
            }
        }
    }

    static void addJitPackPublishTask(Project project, FoxLoaderConfig config, JitPackService service, String path) {
        int i = path.indexOf('/')
        if (i == -1) return
        if (path == "Fox2Code/FoxLoaderExampleMod" && "examplemod" != config.modId) {
            return // Ignore example mod URL if mod id does not match
        }
        String owner = path.substring(0, i)
        String repo = path.substring(i + 1)
        Jar jar = config.getJarTaskToUse(project)
        String modWebsite = config.modWebsite
        if (modWebsite == null) {
            modWebsite = service.websitePrefix + path
            jar.manifest.attributes.put('ModWebsite', modWebsite)
        }
        if (config.modAuthors == null) {
            config.modAuthors = path.substring(0, i)
        }
        if (!config.addJitPackCIPublish) {
            return
        }
        jar.manifest.attributes.put('ModJitPack',
                service.jitpackPrefix + owner + ":" + repo)
        if (System.getenv("JITPACK") == null) return
        (project.getExtensions().getByName("publishing") as PublishingExtension).publications.register(
                "release", MavenPublication, new Action<MavenPublication>() {
            @Override
            void execute(MavenPublication mavenPublication) {
                mavenPublication.from(project.components.java as SoftwareComponent)
                mavenPublication.groupId = service.jitpackPrefix + owner
                mavenPublication.artifactId = repo
                mavenPublication.version = '1.0' // JitPack only work well with "1.0" as version
                mavenPublication.pom.url = modWebsite
                mavenPublication.pom.properties = [
                        "foxloader.version": BuildConfig.FOXLOADER_VERSION,
                        "reindev.version"  : BuildConfig.REINDEV_VERSION,
                        "mod.version"      : config.modVersion,
                ]
            }
        })
    }

    void process(Project project, File foxLoaderCache, FoxLoaderConfig config) {
        synchronized (processLock) {
            final String version = BuildConfig.REINDEV_VERSION
            File jar = DependencyHelper.loadDependencyAsFile(
                    DependencyHelper.reIndevDependencySlim,
                    new File(project.rootDir, BuildConfig.SLIM_JAR_NAME))
            if (jar == null || !jar.exists()) {
                throw new IOException("Provided file \"" + jar.getAbsolutePath() + "\" does not exists!")
            }
            final String versionFox = version + "-fl_" + BuildConfig.FOXLOADER_VERSION
            File jarFox = new File(foxLoaderCache,
                    "net/silveros/reindev-fl/" + versionFox +
                            "/reindev-fl-" + versionFox + ".jar")
            if (config.forceReload && jarFox.exists()) jarFox.delete()
            File pomFox = new File(foxLoaderCache,
                    "net/silveros/reindev-fl/" + versionFox +
                            "/reindev-fl-" + versionFox + ".pom")
            injectPom(pomFox, "net.silveros", "reindev-fl", versionFox)
            if (!jarFox.exists()) {
                System.out.println("Patching ReIndev")
                PatchBridge.patchComputeFrames(jar, jarFox)
            }
            if (config.decompileSources) {
                decompile(project, foxLoaderCache, config, versionFox, jarFox)
            }
            project.dependencies {
                implementation("net.silveros:reindev-fl:${versionFox}")
            }
        }
    }

    private void decompile(Project project, File foxLoaderCache, FoxLoaderConfig config,
                                      String versionFox, File jarFox) {
        File unpickedJarFox = new File(foxLoaderCache,
                "net/silveros/" + "reindev-fl" + "/" + versionFox + "/" +
                        "reindev-fl" + "-" + versionFox + "-unpicked.jar")
        File sourcesJarFox = new File(foxLoaderCache,
                "net/silveros/" + "reindev-fl" + "/" + versionFox + "/" +
                        "reindev-fl" + "-" + versionFox + "-sources.jar")
        if (config.forceReload && sourcesJarFox.exists()) sourcesJarFox.delete()
            if (!sourcesJarFox.exists()) {
                if (unpickedJarFox.exists()) unpickedJarFox.delete()
                System.out.println("Unpicking ReIndev references for source")
                PatchBridge.patch(jarFox, unpickedJarFox, true)
                try {
                    System.out.println("Decompiling patched ReIndev")
                    decompileExec(project, unpickedJarFox, sourcesJarFox)
                    if (!sourcesJarFox.exists()) {
                        throw new NoSuchFileException("Failed to decompile patched ReIndev")
                    }
                } catch (Throwable throwable) {Throwable root = throwable
                    while (root.getCause() != null)
                        root = root.getCause()
                    root.initCause(UserMessage.FAIL_DECOMPILE)
                    if (throwable instanceof OutOfMemoryError ||
                            throwable instanceof RuntimeException) {
                        throwable.printStackTrace()
                    } else throw throwable
                }
            }
    }

    private void decompileExec(Project project, File unpickedJarFox, File sourcesJarFox) {
        // Equivalent of: new FoxLoaderDecompiler(unpickedJarFox, sourcesJarFox).decompile()
        final JavaToolchainService toolchain = project.extensions.getByType(JavaToolchainService.class)
        FoxJavaExec foxJavaExec = new FoxJavaExec(toolchain.launcherFor {
            it.languageVersion.set(JavaLanguageVersion.of(DependencyHelper.vineFlower.javaSupport))
        }.get().executablePath.asFile)
        foxJavaExec.addFile(SourceUtil.getSourceFile(Type.class))
        foxJavaExec.addFile(SourceUtil.getSourceFile(GradlePlugin.class))
        foxJavaExec.addFile(DependencyHelper.loadDependencyAsFile(DependencyHelper.vineFlower))
        foxJavaExec.setMainClass("com.fox2code.foxloader.decompiler.FoxLoaderDecompiler")
        foxJavaExec.exec(this.mcLibraryRoot.getAbsolutePath(),
                unpickedJarFox.getAbsolutePath(), sourcesJarFox.getAbsolutePath())
    }

    static void injectPom(File file,String group,String id,String ver) {
        File parent = file.getParentFile()
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Failed to create cache dir, is file-system read-only?")
        Files.write(file.toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\n" +
                "         xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>"+group+"</groupId>\n" +
                "    <artifactId>"+id+"</artifactId>\n" +
                "    <version>"+ver+"</version>\n" +
                "    <packaging>jar</packaging>\n" +
                "    <dependencies>\n" +
                "    </dependencies>\n" +
                "    <properties>\n" +
                "        <maven.compiler.source>1.8</maven.compiler.source>\n" +
                "        <maven.compiler.target>1.8</maven.compiler.target>\n" +
                "    </properties>\n" +
                "</project>").getBytes(StandardCharsets.UTF_8))
    }


    JsonObject readData() {
        if (foxLoaderData.exists()) try {
            return gson.fromJson(new String(
                    Files.readAllBytes(foxLoaderData.toPath()), StandardCharsets.UTF_8), JsonObject)
        } catch (Throwable ignored) {}
        return new JsonObject()
    }

    void saveData(JsonObject jsonObject) {
        Files.write(foxLoaderData.toPath(), gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8))
    }
}
