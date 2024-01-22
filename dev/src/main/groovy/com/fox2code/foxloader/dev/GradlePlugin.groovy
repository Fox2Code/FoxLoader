package com.fox2code.foxloader.dev

import com.fox2code.foxloader.launcher.BuildConfig
import com.fox2code.foxloader.launcher.DependencyHelper
import com.fox2code.foxloader.loader.PreLoader
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.internal.os.OperatingSystem

import javax.swing.JOptionPane
import java.nio.charset.StandardCharsets
import java.nio.file.ClosedFileSystemException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.text.Normalizer
import java.util.jar.JarFile

class GradlePlugin implements Plugin<Project> {
    private static FoxLoaderDecompilerHelper decompilerHelper
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create()
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
                url 'https://cdn.fox2code.com/maven'
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
        // Support "@reason" javadoc Mixin tag mandated by Minecraft-Dev Intellij plugin
        project.javadoc.options.tags = [ "reason" ]
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
            if (config.decompileSources && decompilerHelper == null) {
                decompilerHelper = new FoxLoaderDecompilerHelper()
            }
            project.tasks.jar {
                from(project.sourceSets.client.output)
                from(project.sourceSets.server.output)
            }
            for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
                if (dependency == DependencyHelper.jFallback) continue
                project.dependencies {
                    implementation(dependency.name)
                }
            }
            project.configurations.all {
                exclude group: 'org.spongepowered', module: 'mixin'
            }
            process(project, foxLoaderCache, config)
            String foxLoaderVersion = config.localTesting ?
                    "1.0" : BuildConfig.FOXLOADER_VERSION // Full release will be 1.0.0 to avoid conflicts.
            if (config.foxLoaderLibVersionOverride != null) {
                foxLoaderVersion = config.foxLoaderLibVersionOverride
            }
            final String foxLoaderGroupId = config.useLegacyFoxLoaderGroupId ?
                    "com.github.Fox2Code.FoxLoader" : "com.fox2code.FoxLoader"
            project.dependencies {
                runtimeOnly(BuildConfig.SPARK_DEPENDENCY)
                implementation("${foxLoaderGroupId}:common:${foxLoaderVersion}")
                clientImplementation("${foxLoaderGroupId}:client:${foxLoaderVersion}")
                serverImplementation("${foxLoaderGroupId}:server:${foxLoaderVersion}")
            }
            if (!config.useLegacyFoxLoaderGroupId) {
                project.configurations.all {
                    exclude group: 'com.github.Fox2Code.FoxLoader', module: 'common'
                    exclude group: 'com.github.Fox2Code.FoxLoader', module: 'client'
                    exclude group: 'com.github.Fox2Code.FoxLoader', module: 'server'
                }
            }
            if (config.useLWJGLX) {
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
                    clientImplementation(platform("org.lwjgl:lwjgl-bom:${config.LWJGLXLWJGLVersion}"))
                    clientImplementation("com.github.Fox2Code:lwjglx:${config.LWJGLXVersion}")
                    clientRuntimeOnly "org.lwjgl:lwjgl::$lwjglNatives"
                    clientRuntimeOnly "org.lwjgl:lwjgl-glfw::$lwjglNatives"
                    clientRuntimeOnly "org.lwjgl:lwjgl-openal::$lwjglNatives"
                    clientRuntimeOnly "org.lwjgl:lwjgl-opengl::$lwjglNatives"
                }
            } else {
                project.dependencies {
                    clientImplementation("net.java.jinput:jinput:2.0.5")
                    clientImplementation("org.lwjgl.lwjgl:lwjgl:2.9.1")
                    clientImplementation("org.lwjgl.lwjgl:lwjgl_util:2.9.1")
                    clientImplementation("org.lwjgl.lwjgl:lwjgl-platform:2.9.1")
                }
            }
            Objects.requireNonNull(config.modId, "The mod id cannot be null!")
            if (config.modVersion == null && project.version != null) {
                config.modVersion = project.version.toString()
            }
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
                if (config.modVersion != null &&
                        !config.modVersion.isEmpty()) {
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
                if (config.unofficial) {
                    attributes 'Unofficial': 'true'
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
            if (config.localTesting) {
                runClient.systemProperty("foxloader.ignore-cache", "true")
            }
            runClient.workingDir = runDir
            runClient.args(username, "-", "--gameDir", runDir.getPath())
            JavaExec runServer = project.getTasks().getByName("runServer") as JavaExec
            runServer.classpath = project.sourceSets.server.runtimeClasspath
            runServer.mainClass.set("com.fox2code.foxloader.launcher.ServerMain")
            runServer.systemProperty("foxloader.inject-mod", mod.getAbsolutePath())
            runServer.systemProperty("foxloader.dev-mode", "true")
            if (config.localTesting) {
                runServer.systemProperty("foxloader.ignore-cache", "true")
            }
            runServer.workingDir = runDir
            if (config.addJitPackCIPublish && project.rootProject == project &&
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
                                addJitPackPublishTask(project, config, url.substring(15))
                            } else if (url.startsWith("https://github.com/")) {
                                addJitPackPublishTask(project, config, url.substring(19))
                            }
                        }
                    }
                }
            }
        }
    }

    static void addJitPackPublishTask(Project project, FoxLoaderConfig config, String path) {
        int i = path.indexOf('/')
        if (i == -1) return
        String owner = path.substring(0, i)
        String repo = path.substring(i + 1)
        Jar jar = (project.getTasks().getByName("jar") as Jar)
        String modWebsite = config.modWebsite
        if (modWebsite == null) {
            modWebsite = "https://github.com/" + path
            jar.manifest.attributes.put('ModWebsite', modWebsite)
        }
        jar.manifest.attributes.put('ModJitPack',
                "com.github." + owner + ":" + repo)
        if (System.getenv("JITPACK") == null) return
        (project.getExtensions().getByName("publishing") as PublishingExtension).publications.register(
                "release", MavenPublication, new Action<MavenPublication>() {
            @Override
            void execute(MavenPublication mavenPublication) {
                mavenPublication.from(project.components.java as SoftwareComponent)
                mavenPublication.groupId = "com.github." + owner
                mavenPublication.artifactId = repo
                mavenPublication.version = '1.0' // JitPack only work with "1.0" as version
                mavenPublication.pom.url = modWebsite
                mavenPublication.pom.properties = [
                        "foxloader.version": BuildConfig.FOXLOADER_VERSION,
                        "reindev.version"  : BuildConfig.REINDEV_VERSION,
                        "mod.version"      : config.modVersion,
                ]
            }
        })
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
        final String versionFox = version + "-rfl_" +
                BuildConfig.FOXLOADER_TRANSFORMER_VERSION
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
        if (config.decompileSources && decompilerHelper != null) {
            File unpickedJarFox = new File(foxLoaderCache,
                    "net/silveros/" + sideName + "/" + versionFox + "/" +
                            sideName + "-" + versionFox + "-unpicked.jar")
            File sourcesJarFox = new File(foxLoaderCache,
                    "net/silveros/" + sideName + "/" + versionFox + "/" +
                            sideName + "-" + versionFox + "-sources.jar")
            if (config.forceReload && sourcesJarFox.exists()) sourcesJarFox.delete()
            if (!jarFileExists(sourcesJarFox)) {
                closeJarFileSystem(unpickedJarFox)
                if (unpickedJarFox.exists()) unpickedJarFox.delete()
                System.out.println("Unpicking ReIndev " + logSideName + " references for source")
                PreLoader.patchDevReIndevForSource(jarFox, unpickedJarFox)
                try {
                    System.out.println("Decompiling patched ReIndev " + logSideName)
                    decompilerHelper.decompile(unpickedJarFox, sourcesJarFox, client)
                } catch (OutOfMemoryError oom) {
                    boolean deleteFailed = false
                    try {
                        closeJarFileSystem(unpickedJarFox)
                        closeJarFileSystem(sourcesJarFox)
                    } finally {
                        if (!sourcesJarFox.delete()) {
                            sourcesJarFox.deleteOnExit()
                            deleteFailed = true
                        }
                    }
                    Throwable root = oom
                    while (root.getCause() != null)
                        root = root.getCause()

                    root.initCause(deleteFailed ? // If delete failed, restart
                            UserMessage.UNRECOVERABLE_STATE_DECOMPILE :
                            client ? UserMessage.FAIL_DECOMPILE_CLIENT :
                                    UserMessage.FAIL_DECOMPILE_SERVER)
                    if (deleteFailed) throw oom // If delete failed, we can't recover
                    oom.printStackTrace()
                } catch (Throwable throwable) {
                    boolean deleteFailed = false
                    try {
                        closeJarFileSystem(unpickedJarFox)
                        closeJarFileSystem(sourcesJarFox)
                    } finally {
                        if (!unpickedJarFox.delete()) {
                            unpickedJarFox.deleteOnExit()
                            deleteFailed = true
                        }
                        if (!sourcesJarFox.delete()) {
                            sourcesJarFox.deleteOnExit()
                            deleteFailed = true
                        }
                    }
                    Throwable root = throwable
                    while (root.getCause() != null)
                        root = root.getCause()
                    root.initCause(deleteFailed ? // If delete failed, restart
                            UserMessage.UNRECOVERABLE_STATE_DECOMPILE :
                            client ? UserMessage.FAIL_DECOMPILE_CLIENT :
                                    UserMessage.FAIL_DECOMPILE_SERVER)
                    if (deleteFailed) {
                        terminateProcess()
                    }
                    throw throwable
                }
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

    static void closeJarFileSystem(File file) {
        URI uri = new URI("jar:file", null, file.toURI().getPath(), null)
        FileSystem fileSystem = null
        try {
            fileSystem = FileSystems.getFileSystem(uri)
            if (fileSystem != null) fileSystem.close()
        } catch (Exception ignored) {}
        if (fileSystem != null) {
            try {
                Files.exists(fileSystem.getPath("META-INF/MANIFEST.MF"))
            } catch (ClosedFileSystemException e) {
                terminateProcess()
                Throwable root = e
                while (root.getCause() != null) root = root.getCause()
                if (!(root instanceof UserMessage)) {
                    root.initCause(UserMessage.UNRECOVERABLE_STATE_DECOMPILE)
                }
                throw e
            }
        }
    }

    static boolean jarFileExists(File file) {
        if (!file.exists()) return false
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.manifest != null
        } catch (Exception ignored) {
            return false
        }
    }

    static void terminateProcess() {
        new Thread("FoxLoader - Termination Thread") {
            @Override
            void run() {
                try {
                    sleep(500L)
                } catch (Exception ignored) {}
                System.exit(-1)
            }
        }.start()
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

    static class FoxLoaderConfig {
        boolean decompileSources = // Only decompile sources if we have no CI to not waste server time
                System.getenv("CI") == null && System.getenv("JITPACK") == null
        boolean includeClient = true
        boolean includeServer = true
        boolean addJitPackCIPublish = true
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
        // Fox testing only
        public String foxLoaderLibVersionOverride
        public boolean localTesting = false
        public boolean forceReload = false
        public boolean unofficial = false
        public boolean useLWJGLX = false
        public boolean useLegacyFoxLoaderGroupId
        public String LWJGLXVersion = "0.21"
        public String LWJGLXLWJGLVersion = "3.3.1"
    }
}
