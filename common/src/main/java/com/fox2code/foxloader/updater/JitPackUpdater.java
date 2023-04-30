package com.fox2code.foxloader.updater;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.utils.NetUtils;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class JitPackUpdater extends AbstractUpdater {
    protected final String jitPackUrlRoot;
    protected final String jitPackArtifactId;
    protected final String jitPackExt;
    protected final String jitPackManifestUrl;
    protected final String jitPackModVersionTag;
    protected String latestJitPackVersion;

    public JitPackUpdater(ModContainer modContainer) {
        this(modContainer, "mod.version");
    }

    protected JitPackUpdater(ModContainer modContainer, String modVersionTag) {
        super(modContainer);
        final String jitPack = modContainer.jitpack;
        int i = jitPack.indexOf(':');
        int i2 = jitPack.indexOf(':', i + 1);
        if (i2 == -1) i2 = jitPack.length();
        this.jitPackUrlRoot = "https://www.jitpack.io/" +
                jitPack.substring(0, i2).replace('.', '/').replace(':', '/');
        this.jitPackArtifactId = modContainer.jitpack.substring(i + 1, i2).replace(':', '-');
        this.jitPackExt = i2 == jitPack.length() ? "" : "-" +
                modContainer.jitpack.substring(i2 + 1).replace(':', '-');
        this.jitPackManifestUrl = this.jitPackUrlRoot + "/maven-metadata.xml";
        this.jitPackModVersionTag = modVersionTag;
    }

    // https://www.jitpack.io/com/github/Fox2Code/FoxLoader/final/0.3.0/final-0.3.0.pom
    // https://www.jitpack.io/com/github/Fox2Code/FoxLoader/final/maven-metadata.xml
    // com.github.Fox2Code.FoxLoader:final

    protected final String getUrlForLatestJar() {
        return getUrlForVersionAndExt(this.latestJitPackVersion, ".jar");
    }

    protected final String getUrlForVersionAndExt(String version, String ext) {
        return this.jitPackUrlRoot + "/" + version + "/" +
                this.jitPackArtifactId + "-" + version + ext;
    }

    @Nullable
    @Override
    protected String findLatestVersion() throws IOException {
        String manifest = NetUtils.downloadAsString(this.jitPackManifestUrl);
        String latestVersion = getTagValue(manifest, "release");
        if (latestVersion == null || latestVersion.equals(modContainer.version)) {
            return null;
        }
        manifest = NetUtils.downloadAsString(getUrlForVersionAndExt(latestVersion, ".pom"));
        String reIndevVersion = getTagValue(manifest, "reindev.version");
        if (ModLoader.checkSemVerMismatch(BuildConfig.REINDEV_VERSION, reIndevVersion)) {
            reIndevVersion = getTagValue(manifest, "reindev.version.allowFrom");
            if (ModLoader.checkSemVerMismatch(BuildConfig.REINDEV_VERSION, reIndevVersion)) {
                return null;
            }
        }
        String latestModVersionReal = getTagValue(manifest, this.jitPackModVersionTag);
        if (latestModVersionReal == null || latestModVersionReal.equals(modContainer.version)) {
            return null;
        }
        Semver semver = Semver.coerce(latestModVersionReal);
        if (semver != null && !semver.isGreaterThan(modContainer.semver)) {
            return null;
        }
        this.latestJitPackVersion = latestVersion;
        return latestModVersionReal;
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
        File destination = new File(ModLoader.mods,
                modContainer.name.replace(" ", "") +
                        "-" + this.getLatestVersion() + ".jar");
        if (destination.exists()) return; // ???
        try (OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
            NetUtils.downloadTo(this.getUrlForLatestJar(), outputStream);
        } catch (IOException ioException) {
            if (!destination.delete())
                destination.deleteOnExit();
            throw ioException;
        }
        this.modContainer.file.deleteOnExit();
    }
}
