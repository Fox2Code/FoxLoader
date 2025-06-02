package com.fox2code.foxloader.client;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.client.util.KeyBinding;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public final class KeyBindingAPI {
    private static final ArrayList<KeyBinding> registeredKeyBindings = new ArrayList<>();
    private static boolean loaded;

    private KeyBindingAPI() { throw new AssertionError(); }

    public static void registerKeyBinding(@NotNull KeyBinding keyBinding) {
        if (loaded) throw new IllegalStateException("Options are already loaded");
        registeredKeyBindings.add(keyBinding);
    }

    public static class Internal {
        public static KeyBinding[] inject(KeyBinding[] keyBindings) {
            if (!ModLoader.areAllModsLoaded())
                throw new IllegalStateException("Mods didn't finished to load!");
            loaded = true;
            if (registeredKeyBindings.isEmpty())
                return keyBindings;
            KeyBinding[] newKeyBindings = Arrays.copyOf(keyBindings,
                    keyBindings.length + registeredKeyBindings.size());
            System.arraycopy(registeredKeyBindings.toArray(new KeyBinding[0]), 0,
                    newKeyBindings, keyBindings.length, registeredKeyBindings.size());
            return newKeyBindings;
        }
    }
}