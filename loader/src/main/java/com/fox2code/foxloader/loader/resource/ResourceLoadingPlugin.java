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
package com.fox2code.foxloader.loader.resource;

import com.fox2code.foxloader.launcher.FoxClassLoader;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.LoadingPlugin;
import com.fox2code.foxloader.loader.Mod;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.utils.io.IOUtils;
import com.fox2code.foxloader.utils.io.URLUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loading plugin to load resource packs from the mods folder.
 */
public final class ResourceLoadingPlugin extends LoadingPlugin {
    public static final ResourceLoadingPlugin RESOURCE_LOADING_PLUGIN = new ResourceLoadingPlugin();

    private ResourceLoadingPlugin() {
        super("resource");
    }

    @Override
    public @Nullable ModInfo getModInfo(@NotNull File mod, @Nullable String jarPath) throws Exception {
        if (jarPath != null || !mod.getName().endsWith(".zip")) {
            return null;
        }
        String description;
        URL packIcon = null;
        String sha256;
        try (ZipFile zipFile = new ZipFile(mod)) {
            Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
            while (zipEntryEnumeration.hasMoreElements()) {
                String entryName = zipEntryEnumeration.nextElement().getName();
                if (entryName.startsWith("META-INF/") || entryName.endsWith(".mixins.json") ||
                        entryName.endsWith(".class") || entryName.endsWith(".class/")) {
                    return null; // Skip potential java mods.
                }
            }
            ZipEntry descriptionEntry;
            if ((descriptionEntry = zipFile.getEntry("pack.txt")) == null) {
                return null;
            }
            description = new String(IOUtils.readAllBytes(
                    zipFile.getInputStream(descriptionEntry)),
                    StandardCharsets.UTF_8);
            if (zipFile.getEntry("pack.png") != null) {
                packIcon = URLUtils.getEntryURLOf(mod, "pack.png");
            }
            sha256 = IOUtils.toHex(IOUtils.sha256Of(mod));
        } catch (IOException ioe) {
            return null;
        }
        final String packId = "resource_" + sha256.substring(0, 16);
        final String iconPath = "assets/" + packId + "/icon.png";
        if (packIcon != null) {
            // Inject resource pack icon as a resource
            FoxClassLoader foxClassLoader = FoxLauncher.getFoxClassLoader();
            foxClassLoader.injectResource(iconPath, packIcon);
            if (foxClassLoader.getResource(iconPath) == null) {
                packIcon = null;
            }
        }
        return new ResourcePackInfo(mod, packId, mod.getName(),
                description, packIcon == null ? null : iconPath);
    }

    @Override
    public void preLoadModContainer(@NotNull ModContainer modContainer) throws Exception {

    }

    @Override
    public @Nullable Mod loadModContainer(@NotNull ModContainer modContainer) throws Exception {
        return null;
    }
}
