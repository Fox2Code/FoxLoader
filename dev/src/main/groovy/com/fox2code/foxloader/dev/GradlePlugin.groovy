package com.fox2code.foxloader.dev

import com.fox2code.foxloader.launcher.BuildConfig
import com.fox2code.foxloader.launcher.DependencyHelper
import com.fox2code.foxloader.loader.PreLoader
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler

import javax.swing.JOptionPane
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.text.Normalizer

class GradlePlugin implements Plugin<Project> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File foxLoaderCache
    private File foxLoaderData

    @Override
    void apply(Project project) {
        project.gradle.getGradleUserHomeDir()
        foxLoaderCache = new File(project.gradle.getGradleUserHomeDir(), "fox-loader")
        foxLoaderData = new File(foxLoaderCache, "data.json")
        project.apply([plugin: 'java-library'])
        project.apply([plugin: 'maven-publish'])
        project.apply([plugin: 'eclipse'])
        project.apply([plugin: 'idea'])
        project.compileJava.options.encoding = 'UTF-8'
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
                url 'https://repo.spongepowered.org/maven'
            }
            maven {
                url 'https://jitpack.io/'
            }
            mavenLocal()
        }
        project.java {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8

            withSourcesJar()
        }
        project.sourceSets {
            main
            client {
                compileClasspath += main.output
                runtimeClasspath += main.output
            }
            server {
                compileClasspath += main.output
                runtimeClasspath += main.output
            }
        }
        project.tasks.register("runClient", JavaExec) {
            group = "FoxLoader"
            description = "Run ReIndev client from gradle"
        }.get().dependsOn(project.getTasks().getByName("jar"))
        project.tasks.register("runServer", JavaExec) {
            group = "FoxLoader"
            description = "Run ReIndev server from gradle"
        }.get().dependsOn(project.getTasks().getByName("jar"))
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
        File gitIgnore = new File(project.buildDir, ".gitignore")
        if (project.buildDir.exists() && !gitIgnore.exists()) {
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
            FoxLoaderConfig config = ((FoxLoaderConfig) project.extensions.getByName("foxloader"))
            project.tasks.jar {
                from(project.sourceSets.client.output)
                from(project.sourceSets.server.output)
            }
            for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
                project.dependencies {
                    implementation(dependency.name)
                }
            }
            process(project, foxLoaderCache, config)
            final String foxLoaderVersion = config.localTesting ?
                    "1.0" : BuildConfig.FOXLOADER_VERSION // Full release will be 1.0.0 to avoid conflicts.
            project.dependencies {
                runtimeOnly(BuildConfig.SPARK_DEPENDENCY)
                implementation("com.github.Fox2Code.FoxLoader:common:${foxLoaderVersion}")
                clientImplementation("com.github.Fox2Code.FoxLoader:client:${foxLoaderVersion}")
                serverImplementation("com.github.Fox2Code.FoxLoader:server:${foxLoaderVersion}")
            }
            project.dependencies {
                clientImplementation("net.java.jinput:jinput:2.0.5")
                clientImplementation("org.lwjgl.lwjgl:lwjgl:2.9.1")
                clientImplementation("org.lwjgl.lwjgl:lwjgl_util:2.9.1")
                clientImplementation("org.lwjgl.lwjgl:lwjgl-platform:2.9.1")
            }
            Objects.requireNonNull(config.modId, "The mod id cannot be null!")
            (project.getTasks().getByName("jar") as Jar).manifest {
                attributes 'For-FoxLoader-Version': BuildConfig.FOXLOADER_VERSION
                attributes 'For-ReIndev-Version': BuildConfig.REINDEV_VERSION
                attributes 'ModId': config.modId
                if (config.commonMod != null) {
                    attributes 'CommonMod': config.commonMod
                }
                if (config.clientMod != null) {
                    attributes 'ClientMod': config.clientMod
                }
                if (config.serverMod != null) {
                    attributes 'ServerMod': config.serverMod
                }
                if (config.modName != null) {
                    attributes 'ModName': config.modName
                }
                if (config.modVersion != null) {
                    attributes 'ModVersion': config.modVersion
                }
                if (config.modDesc != null) {
                    attributes 'ModDesc': config.modDesc
                }
                if (config.modWebsite != null) {
                    attributes 'ModWebsite': config.modWebsite
                }
                if (config.preClassTransformer != null) {
                    attributes 'PreClassTransformer': config.preClassTransformer
                }
            }
            File mod = ((Jar) project.getTasks().getByName("jar")).getArchiveFile().get().getAsFile()
            JsonElement savedUsername = foxLoaderJsonData.get("username")
            String username = config.username
            if (savedUsername != null) {
                username = savedUsername.getAsString()
            }
            username = username.trim()
            if (username.size() < 4) {
                username = "Player" + Random.newInstance().nextInt(1000)
            }
            JavaExec runClient = project.getTasks().getByName("runClient") as JavaExec
            runClient.classpath = project.sourceSets.client.runtimeClasspath
            runClient.mainClass.set("com.fox2code.foxloader.launcher.ClientMain")
            runClient.systemProperty("foxloader.inject-mod", mod.getAbsolutePath())
            runClient.systemProperty("foxloader.dev-mode", "true")
            runClient.workingDir = runDir
            runClient.args(username, "-", "--gameDir", runDir.getPath())
            JavaExec runServer = project.getTasks().getByName("runServer") as JavaExec
            runServer.classpath = project.sourceSets.server.runtimeClasspath
            runServer.mainClass.set("com.fox2code.foxloader.launcher.ServerMain")
            runServer.systemProperty("foxloader.inject-mod", mod.getAbsolutePath())
            runServer.systemProperty("foxloader.dev-mode", "true")
            runServer.workingDir = runDir
        }
    }

    static void process(Project project, File foxLoaderCache, FoxLoaderConfig config) {
        if (config.includeClient) processSide(project, foxLoaderCache, config, true)
        if (config.includeServer) processSide(project, foxLoaderCache, config, false)
    }

    static void processSide(Project project, File foxLoaderCache, FoxLoaderConfig config, boolean client) {
        final String sideName = client ? "reindev" : "reindev-server"
        final String logSideName = client ? "client" : "server"
        final String version = BuildConfig.REINDEV_VERSION
        File jar = new File(foxLoaderCache,
                "net/silveros/" + sideName + "/" + version + "/" +
                        sideName + "-" + version + ".jar")
        File pom = new File(foxLoaderCache,
                "net/silveros/" + sideName + "/" + version + "/" +
                        sideName + "-" + version + ".pom")
        injectPom(pom, "net.silveros", sideName, version)
        DependencyHelper.loadDevDependencies(foxLoaderCache, client)
        final String versionFox = version + "-rfl_" + BuildConfig.FOXLOADER_VERSION
        File jarFox = new File(foxLoaderCache,
                "net/silveros/" + sideName + "/" + versionFox + "/" +
                        sideName + "-" + versionFox + ".jar")
        if (config.forceReload && jarFox.exists()) jarFox.delete()
        File pomFox = new File(foxLoaderCache,
                "net/silveros/" + sideName + "/" + versionFox + "/" +
                        sideName + "-" + versionFox + ".pom")
        injectPom(pomFox, "net.silveros", sideName, versionFox)
        if (!jarFox.exists()) {
            System.out.println("Patching ReIndev " + logSideName)
            PreLoader.patchReIndevForDev(jar, jarFox, client)
        }
        if (config.decompileSources) {
            File sourcesJarFox = new File(foxLoaderCache,
                    "net/silveros/" + sideName + "/" + versionFox + "/" +
                            sideName + "-" + versionFox + "-sources.jar")
            if (config.forceReload && sourcesJarFox.exists()) sourcesJarFox.delete()
            if (!sourcesJarFox.exists()) {
                System.out.println("Decompiling patched ReIndev " + logSideName)
                ConsoleDecompiler.main(new String[]{
                        "-asc=1", "-bsm=1", "-sef=1", "-jrt=1", "-nls=0", "-ind=    ",
                        jarFox.getAbsolutePath(), sourcesJarFox.getAbsolutePath()
                })
            }
        }
        if (client) {
            project.dependencies {
                clientImplementation("net.silveros:" + sideName + ":${versionFox}")
            }
        } else {
            project.dependencies {
                serverImplementation("net.silveros:" + sideName + ":${versionFox}")
            }
        }
    }

    static void injectPom(File file,String group,String id,String ver) {
        File parent = file.getParentFile()
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Failed to create cache dir, is file-system read-only?")
        Files.write(file.toPath(), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>"+group+"</groupId>\n" +
                "    <artifactId>"+id+"</artifactId>\n" +
                "    <version>"+ver+"</version>\n" +
                "    <packaging>jar</packaging>\n" +
                "    <dependencies>\n" +
                "    </dependencies>\n" +
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

    static class FoxLoaderConfig {
        boolean decompileSources = true
        boolean localTesting = false
        boolean forceReload = false
        boolean includeClient = true
        boolean includeServer = true
        String username = Normalizer.normalize(System.getProperty("user.name"),
                Normalizer.Form.NFD).replaceAll("[^a-zA-Z0-9_]+","")
        public String commonMod
        public String clientMod
        public String serverMod
        public String modId = "null"
        public String modVersion
        public String modName
        public String modDesc
        public String modWebsite
        public String preClassTransformer
    }
}
