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
package com.fox2code.foxloader.network;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.registry.GameRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SidedMetadataAPI {
    public static final String KEY_REINDEV_VERSION = "reindev_version";
    public static final String KEY_FOXLOADER_VERSION = "foxloader_version";
    public static final String KEY_FOXBUCKET_VERSION = "foxbucket_version";
    public static final String KEY_ALLOW_MISSING_REGISTRY_KEYS = "allow_missing_registry_keys";
    private static final HashMap<String, String> selfMetadata = new HashMap<>();
    private static final Map<String, String> publicSelfMetaData = Collections.unmodifiableMap(selfMetadata);
    private static final ArrayList<Runnable> onActiveMetadataChangedHandlers = new ArrayList<>();
    private static Map<String, String> activeMetaData = null;

    static {
        selfMetadata.put(KEY_REINDEV_VERSION, BuildConfig.REINDEV_VERSION);
        selfMetadata.put(KEY_FOXLOADER_VERSION, BuildConfig.FOXLOADER_VERSION);
        selfMetadata.put(KEY_ALLOW_MISSING_REGISTRY_KEYS, "true");
    }

    private SidedMetadataAPI() { throw new AssertionError(); }

    @NotNull public static Map<String, String> getSelfMetadata() {
        return publicSelfMetaData;
    }

    @NotNull public static Map<String, String> getActiveMetadata() {
        Map<String, String> active = SidedMetadataAPI.activeMetaData;
        return active == null ? publicSelfMetaData : active;
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getActiveMetadata().get(key));
    }

    public static void putSelfMetadata(@NotNull String key, @Nullable String value) {
        Objects.requireNonNull(key, "key");
        if (KEY_REINDEV_VERSION.equals(key) || KEY_FOXLOADER_VERSION.equals(key) || KEY_FOXBUCKET_VERSION.equals(key)) {
            // As we may use these fields in the future for compatibility, don't allow change
            throw new IllegalArgumentException("Cannot change privileged key");
        }
        if (GameRegistry.isFrozen()) {
            // We don't allow editing self metadata after registry freeze cause server hello packet is
            // precompiled at that point and cannot be edited anymore, will probably be changed in the future
            throw new IllegalStateException("Cannot change metadata after game registry has been frozen");
        }
        if (value == null) {
            selfMetadata.remove(key);
        } else {
            selfMetadata.put(key, value);
        }
    }

    public static class Internal {
        public static void setActiveMetaData(@Nullable Map<String, String> activeSelfMetaData) {
            if (FoxLauncher.isClient() && SidedMetadataAPI.activeMetaData != activeSelfMetaData) {
                SidedMetadataAPI.activeMetaData = activeSelfMetaData;
                for (Runnable runnable : onActiveMetadataChangedHandlers) {
                    runnable.run();
                }
            }
        }

        public static void addHandler(Runnable runnable) {
            onActiveMetadataChangedHandlers.add(runnable);
        }
    }
}
