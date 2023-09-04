package com.fox2code.foxloader.client;

import net.minecraft.src.client.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;

public class KeyBindingAPI {
    private static final ArrayList<KeyBinding> registeredKeyBindings = new ArrayList<>();
    private static boolean loaded;

    public static void registerKeyBinding(KeyBinding keyBinding) {
        if (loaded) throw new IllegalStateException("Options are already loaded");
        if (registeredKeyBindings.size() >= 8) // This limit will be fixed in future update!
            throw new IllegalStateException("Cannot register more than 8 custom key-binds!");
        registeredKeyBindings.add(keyBinding);
    }

    public static class Internal {
        public static KeyBinding[] inject(KeyBinding[] keyBindings) {
            loaded = true;
            return ArrayUtils.addAll(keyBindings, registeredKeyBindings.toArray(new KeyBinding[0]));
        }
    }
}
