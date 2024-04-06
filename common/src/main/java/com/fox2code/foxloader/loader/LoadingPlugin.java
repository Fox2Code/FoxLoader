package com.fox2code.foxloader.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * A plugin only in memory while FoxLoader is loading mods
 */
public abstract class LoadingPlugin {
    boolean mayLoadNewMods;
    boolean privileged;

    public LoadingPlugin() {
        this.privileged = false;
    }

    /**
     * Note: For libraries it's preferred to inject them directly into the class loader.
     *
     * @param file mod that wasn't successfully loaded via FoxLoader
     * @return ModContainerProperties of the load mod or {@code null}
     */
    public Collection<ModContainerProperties> tryInitJavaModProperties(@NotNull File file) { return null; }

    /**
     * Called when all mods are loaded before preload
     */
    public void beforePreLoading() {}

    /**
     * @return tell if the loading plugin may load new mods
     */
    public boolean mayLoadNewMods() {
        return true;
    }

    public String getProperty(
            @NotNull ModContainer modContainer,
            @NotNull ModContainerProperty modContainerProperty) {
        return modContainerProperty.get(modContainer);
    }

    public void setProperty(
            @NotNull ModContainer modContainer,
            @NotNull ModContainerProperty modContainerProperty,
            @Nullable String value) {
        if (!this.privileged) throw new IllegalStateException("LoadingPlugin is not privileged");
        modContainerProperty.set(this, modContainer, value);
    }

    public enum ModContainerProperty {
        PRE_PATCH(m -> m.prePatch, (m, v) -> m.prePatch = v),
        COMMON_MOD_CLS(m -> m.commonModCls, (m, v) -> m.commonModCls = v),
        COMMON_MIXIN_CLS(m -> m.commonMixins, (m, v) -> m.commonMixins = v),
        CLIENT_MOD_CLS(m -> m.clientModCls, (m, v) -> m.clientModCls = v),
        CLIENT_MIXIN_CLS(m -> m.clientMixins, (m, v) -> m.clientMixins = v),
        SERVER_MOD_CLS(m -> m.serverModCls, (m, v) -> m.serverModCls = v),
        SERVER_MIXIN_CLS(m -> m.serverMixins, (m, v) -> m.serverMixins = v),
        LOADING_PLUGIN(m -> m.loadingPlugin, (m, v) -> {
            throw new IllegalStateException("LoadingPlugin cannot edit LOADING_PLUGIN value");
        });

        private final Function<ModContainer, String> getter;
        private final BiConsumer<ModContainer, String> setter;

        ModContainerProperty(Function<ModContainer, String> getter, BiConsumer<ModContainer, String> setter) {
            this.getter = getter; this.setter = setter;
        }

        String get(ModContainer modContainer) {
            return this.getter.apply(modContainer);
        }
        void set(LoadingPlugin loadingPlugin, ModContainer modContainer, String value) {
            if (!loadingPlugin.privileged) throw new NullPointerException();
            this.setter.accept(modContainer, value);
        }
    }

    /**
     * Used to define mod properties of mods loaded by LoadingPlugin
     */
    public static final class ModContainerProperties {
        private final File file;
        private final String id;
        private final String name;
        private final String version;
        private final String description;
        private final boolean unofficial;
        private final boolean addToClassLoader;
        private final Consumer<ModContainer> modContainerConsumer;
        final String absPath;
        final URL urlPath;

        public ModContainerProperties(
                @NotNull File file,@NotNull String id,@Nullable String name,@Nullable String version,
                @Nullable String description, boolean unofficial) {
            this(file, id, name, version, description, unofficial, file.getName().endsWith(".jar"), null);
        }

        public ModContainerProperties(
                @NotNull File file,@NotNull String id,@Nullable String name,@Nullable String version,
                @Nullable String description, boolean unofficial, boolean addToClassLoader) {
            this(file, id, name, version, description, unofficial, addToClassLoader, null);
        }

        public ModContainerProperties(
                @NotNull File file,@NotNull String id,@Nullable String name,@Nullable String version,
                @Nullable String description, boolean unofficial, boolean addToClassLoader,
                @Nullable Consumer<ModContainer> modContainerConsumer) {
            this.file = Objects.requireNonNull(file, "file");
            this.id = Objects.requireNonNull(id, "id");
            if (ModLoader.isReservedModId(id)) {
                throw new IllegalArgumentException("Reserved mod id!");
            }
            this.name = name != null && !name.isEmpty() ? name :
                    id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
            this.version = version != null && !version.isEmpty() ? version : "1.0";
            this.description = description != null ? description : "...";
            this.unofficial = unofficial;
            this.addToClassLoader = addToClassLoader;
            this.modContainerConsumer = modContainerConsumer;
            this.absPath = this.file.getAbsolutePath();
            try {
                this.urlPath = this.file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The file is not a valid file", e);
            }
        }

        @NotNull
        public File getFile() {
            return this.file;
        }

        @NotNull
        public String getId() {
            return this.id;
        }

        public boolean isAddToClassLoader() {
            return this.addToClassLoader;
        }

        ModContainer makeModContainer() {
            ModContainer modContainer = new ModContainer(
                    this.file, this.id, this.name, this.version,
                    this.description, null, this.unofficial);
            if (this.modContainerConsumer != null) {
                try {
                    this.modContainerConsumer.accept(modContainer);
                } catch (Throwable t) {
                    ModLoader.getModLoaderLogger().log(Level.WARNING,
                            "Failed to execute handler on " + this.id, t);
                }
            }
            return modContainer;
        }
    }
}
