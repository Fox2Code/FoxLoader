package com.fox2code.foxloader.updater;

import com.fox2code.flexver.FlexVer;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.utils.io.NetUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class MavenUpdater extends AbstractUpdater {
    protected final String mavenUrlRoot;
    protected final String mavenArtifactId;
    protected final String mavenExt;
    protected final String mavenManifestUrl;
    protected final String mavenModVersionTag;
    protected String latestMavenVersion;

    public MavenUpdater(ModContainer modContainer, String repository, String dependencyBase) {
        this(modContainer, repository, dependencyBase, "mod.version");
    }

    protected MavenUpdater(ModContainer modContainer, String repository, String dependencyBase, String modVersionTag) {
        super(modContainer);
        int i = dependencyBase.indexOf(':');
        int i2 = dependencyBase.indexOf(':', i + 1);
        if (i2 == -1) i2 = dependencyBase.length();
        this.mavenUrlRoot = repository + // "https://www.jitpack.io/"
                dependencyBase.substring(0, i2).replace('.', '/').replace(':', '/');
        this.mavenArtifactId = dependencyBase.substring(i + 1, i2).replace(':', '-');
        this.mavenExt = i2 == dependencyBase.length() ? "" : "-" +
                dependencyBase.substring(i2 + 1).replace(':', '-');
        this.mavenManifestUrl = this.mavenUrlRoot + "/maven-metadata.xml";
        this.mavenModVersionTag = modVersionTag;
    }

    protected final String getUrlForLatestJar() {
        String version = this.latestMavenVersion == null ?
                this.latestVersion : this.latestMavenVersion;
        return this.getUrlForVersionAndExt(version, ".jar");
    }

    protected final String getUrlForVersionAndExt(String version, String ext) {
        return this.mavenUrlRoot + "/" + version + "/" +
                this.mavenArtifactId + "-" + version + ext;
    }

    @Override
    protected @Nullable String findLatestVersion() throws IOException {
        String latestVersion = this.findLatestMavenRelease();
        String manifest = NetUtils.downloadAsString(getUrlForVersionAndExt(latestVersion, ".pom"));
        String reIndevVersion = getTagValue(manifest, "reindev.version");
        if (reIndevVersionPatternMismatch(reIndevVersion)) {
            reIndevVersion = getTagValue(manifest, "reindev.version.allowFrom");
            if (reIndevVersionPatternMismatch(reIndevVersion)) {
                return null;
            }
        }
        String latestModVersionReal = getTagValue(manifest, this.mavenModVersionTag);
        if (latestModVersionReal == null || latestModVersionReal.equals(modContainer.getModInfo().version)) {
            return null;
        }
        FlexVer semver = FlexVer.parse(latestModVersionReal);
        if (semver != null && !semver.isGreater(modContainer.getModInfo().flexver)) {
            return null;
        }
        this.latestMavenVersion = latestVersion;
        return latestModVersionReal;
    }

    protected String findLatestMavenRelease() throws IOException {
        String manifest = NetUtils.downloadAsString(this.mavenManifestUrl);
        return getTagValue(manifest, "release");
    }

    @Nullable
    public static String getTagValue(String manifest, String tag) {
        int start = manifest.indexOf("<" + tag + ">");
        if (start == -1) return null;
        start += tag.length() + 2;
        int end = manifest.indexOf("</" + tag + ">", start);
        if (end == -1 || start == end) return null;
        return manifest.substring(start, end);
    }

    @Override
    protected void doUpdate() throws IOException {
        ModInfo modInfo = this.modContainer.getModInfo();
        File destination = new File(ModLoader.getModsFolder(),
                modInfo.name.replace(" ", "") +
                        "-" + this.getLatestVersion() + ".jar");
        if (destination.exists()) return; // ???
        try (OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
            NetUtils.downloadTo(this.getUrlForLatestJar(), outputStream);
        } catch (IOException ioException) {
            if (!destination.delete())
                destination.deleteOnExit();
            throw ioException;
        }
        if (modInfo.jarPath == null) {
            modInfo.file.deleteOnExit();
        }
    }
}
