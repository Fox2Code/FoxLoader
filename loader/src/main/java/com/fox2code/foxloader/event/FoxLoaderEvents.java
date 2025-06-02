package com.fox2code.foxloader.event;

import com.fox2code.foxevents.Event;
import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxevents.FoxEvents;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

public final class FoxLoaderEvents extends FoxEvents {
    public static final FoxLoaderEvents INSTANCE = new FoxLoaderEvents();

    static {
        try {
            FoxEvents.setFoxEvents(INSTANCE);
        } catch (Exception e) {
            FoxEvents.Unsafe.setFoxEventsUnsafe(INSTANCE);
        }
    }

    private boolean warnEventFailure;

    private FoxLoaderEvents() {
        this.warnEventFailure = FoxLauncher.DEVELOPING_FOXLOADER || FoxLauncher.DEV_MODE || FoxLauncher.isClient();
    }

    @Override
    public void registerEvents(@NotNull Object handler) {
        String className = handler instanceof Class ? ((Class<?>) handler).getName() : handler.getClass().getName();
        ModContainer modContainer = ModContainer.getActiveModContainer();
        long currentTime = System.currentTimeMillis();
        for (EventCallback eventCallback : this.getEventCallbacks(handler)) {
            this.registerEventCallback(eventCallback.withValidator(
                    new DynamicValidator(null, className, modContainer, currentTime)));
        }
    }

    @Override
    public void registerEvents(@NotNull Object handler, @Nullable BooleanSupplier validator) {
        String className = handler instanceof Class ? ((Class<?>) handler).getName() : handler.getClass().getName();
        ModContainer modContainer = ModContainer.getActiveModContainer();
        long currentTime = System.currentTimeMillis();
        for (EventCallback eventCallback : this.getEventCallbacks(handler, validator)) {
            this.registerEventCallback(eventCallback.withValidator(
                    new DynamicValidator(validator, className, modContainer, currentTime)));
        }
    }

    public CallbackList registerEventsWithList(@NotNull Object handler) {
        String className = handler instanceof Class ? ((Class<?>) handler).getName() : handler.getClass().getName();
        ModContainer modContainer = ModContainer.getActiveModContainer();
        long currentTime = System.currentTimeMillis();
        ArrayList<EventCallback> eventCallbacks = this.getEventCallbacks(handler);
        for (EventCallback eventCallback : eventCallbacks) {
            this.registerEventCallback(eventCallback.withValidator(
                    new DynamicValidator(null, className, modContainer, currentTime)));
        }
        return new CallbackList(eventCallbacks);
    }

    @Override
    public void unregisterEvents(@NotNull Object handler) {
        this.unregisterEventsForClassLoader(handler.getClass().getClassLoader(), handler);
    }

    @Override
    protected void onEventError(@NotNull Event event, @NotNull EventCallback eventCallback, @NotNull Throwable throwable) {
        DynamicValidator dynamicValidator = (DynamicValidator) eventCallback.validator;
        long currentTime = System.currentTimeMillis();
        boolean disable = currentTime - dynamicValidator.lastFailure < 200L;
        ModLoaderInit.getModLoaderLogger().log(disable ? Level.SEVERE : Level.WARNING,
                "Failed to pass " + event.getClass().getName() + " to " + dynamicValidator.className, throwable);
        if (disable) {
            ModLoaderInit.getModLoaderLogger().log(Level.SEVERE,
                    "Disabling failing event handler for performance reason");
            ModLoader.broadcastMessageToPrivileged( // This is probably an emergency worthy of notice
                    "An EventHandler has been disabled due to repeated failures, check logs for more info.");
            dynamicValidator.allowRunningEvent = false;
            this.invalidateCallbackValidators();
        } else if (this.warnEventFailure) {
            this.warnEventFailure = false;
            ModLoader.broadcastMessageToPrivileged( // Send one notice client side when an event handler failed
                    "An EventHandler has failed, please check logs for more info.");
        }
        dynamicValidator.lastFailure = currentTime;
        ModContainer modContainer = dynamicValidator.modContainer;
        if (modContainer != null) modContainer.onEventError(event, eventCallback, throwable, disable);
    }

    @Override
    protected void onUnsafeAccess(String method) {
        if (!"getEventHolderMapUnsafe".equals(method)) {
            throw new SecurityException("Operation not allowed");
        }
    }

    private static class DynamicValidator implements BooleanSupplier {
        private final BooleanSupplier parent;
        private final String className;
        private final ModContainer modContainer;
        private boolean allowRunningEvent = true;
        private long lastFailure;

        private DynamicValidator(BooleanSupplier parent, String className, ModContainer modContainer, long lastFailure) {
            this.parent = parent;
            this.className = className;
            this.modContainer = modContainer;
            this.lastFailure = lastFailure;
        }

        @Override
        public boolean getAsBoolean() {
            return this.allowRunningEvent && (this.parent == null || this.parent.getAsBoolean());
        }
    }

    public static class CallbackList {
        private final ArrayList<EventCallback> eventCallbacks;

        private CallbackList(ArrayList<EventCallback> eventCallbacks) {
            this.eventCallbacks = eventCallbacks;
        }

        public boolean hasEvent(Class<? extends Event> e) {
            for (EventCallback eventCallback : this.eventCallbacks) {
                if (eventCallback.eventHolder.peekEvent() == e) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasEventRegistered(Class<? extends Event> e) {
            for (EventCallback eventCallback : this.eventCallbacks) {
                if (eventCallback.eventHolder.peekEvent() == e && eventCallback.isRegistered()) {
                    return true;
                }
            }
            return false;
        }

        public void unregisterEvent(Class<? extends Event> e) {
            for (EventCallback eventCallback : this.eventCallbacks) {
                if (eventCallback.eventHolder.peekEvent() == e) {
                    FoxLoaderEvents.INSTANCE.unregisterEventCallback(eventCallback);
                }
            }
        }

        public void registerEvent(Class<? extends Event> e) {
            for (EventCallback eventCallback : this.eventCallbacks) {
                if (eventCallback.eventHolder.peekEvent() == e) {
                    FoxLoaderEvents.INSTANCE.registerEventCallback(eventCallback);
                }
            }
        }

        public void unregisterAllEvents() {
            for (EventCallback eventCallback : this.eventCallbacks) {
                FoxLoaderEvents.INSTANCE.unregisterEventCallback(eventCallback);
            }
        }

        public void registerAllEvents() {
            for (EventCallback eventCallback : this.eventCallbacks) {
                FoxLoaderEvents.INSTANCE.registerEventCallback(eventCallback);
            }
        }

        public int getCallbackCount() {
            return this.eventCallbacks.size();
        }
    }
}
