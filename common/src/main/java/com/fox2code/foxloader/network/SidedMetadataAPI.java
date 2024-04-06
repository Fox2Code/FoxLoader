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
    public static final String KEY_VISIBLE_SERVER_NAME = "visible_server_name";
    public static final String KEY_SERVER_BUTTON_NAME = "server_button_name";
    public static final String KEY_SERVER_BUTTON_LINK = "server_button_link";
    private static final HashMap<String, String> selfMetadata = new HashMap<>();
    private static final Map<String, String> publicSelfMetaData = Collections.unmodifiableMap(selfMetadata);
    private static final ArrayList<Runnable> onActiveMetadataChangedHandlers = new ArrayList<>();
    private static Map<String, String> activeMetaData = null;

    static {
        selfMetadata.put(KEY_REINDEV_VERSION, BuildConfig.REINDEV_VERSION);
        selfMetadata.put(KEY_FOXLOADER_VERSION, BuildConfig.FOXLOADER_VERSION);
    }

    private SidedMetadataAPI() { throw new AssertionError(); }

    @NotNull
    public static Map<String, String> getSelfMetadata() {
        return publicSelfMetaData;
    }
    @NotNull
    public static Map<String, String> getActiveMetadata() {
        Map<String, String> active = SidedMetadataAPI.activeMetaData;
        return active == null ? publicSelfMetaData : active;
    }

    public static void putSelfMetadata(@NotNull String key, @Nullable String value) {
        Objects.requireNonNull(key, "key");
        if (KEY_REINDEV_VERSION.equals(key) || KEY_FOXLOADER_VERSION.equals(key)) {
            // As we may use these fields in the future for compatibility, don't allow change
            throw new IllegalArgumentException("Cannot change privileged key");
        }
        if (GameRegistry.getInstance() != null && GameRegistry.getInstance().isFrozen()) {
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
            if (FoxLauncher.isClient() &&  SidedMetadataAPI.activeMetaData != activeSelfMetaData) {
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
