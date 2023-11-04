package com.fox2code.foxloader.launcher.utils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Faster implementation of {@link ThreadLocal} made to work better at low threads counts.
 */
public class FastThreadLocal<T> extends ThreadLocal<T> {
    private static final FastThreadLocalCache<?> NO_CACHE = new FastThreadLocalCache<>(null);
    // Java cache value per thread to improve performance.
    // Usually we would want to use volatile for fields accessed from multiple threads.
    // but for this, not using volatile is on purpose and works in our favor.
    private FastThreadLocalCache<T> fastThreadLocalCache;
    private final boolean withCustomSupplier;

    @SuppressWarnings("unchecked")
    private static <T>  FastThreadLocalCache<T> noCache() {
        return (FastThreadLocalCache<T>) NO_CACHE;
    }

    public static <S> FastThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedFastThreadLocal<>(supplier);
    }

    public FastThreadLocal() {
        boolean customSupplier = true;
        try { // Assume custom supplier if this fails
            customSupplier = this.getClass().getMethod("initialValue")
                    .getDeclaringClass() != Thread.class;
        } catch (NoSuchMethodException ignored) {}
        this.withCustomSupplier = customSupplier;
        this.fastThreadLocalCache = customSupplier ? noCache() :
                new FastThreadLocalCache<>(Thread.currentThread());
    }

    @Override
    public final T get() {
        final Thread thread = Thread.currentThread();
        final FastThreadLocalCache<T> fastThreadLocalCache = this.fastThreadLocalCache;
        if (fastThreadLocalCache.thread == thread)
            return fastThreadLocalCache.value;
        return (this.fastThreadLocalCache = new FastThreadLocalCache<>(thread, super.get())).value;
    }

    @Override
    public final void set(T value) {
        final Thread thread = Thread.currentThread();
        final FastThreadLocalCache<T> fastThreadLocalCache = this.fastThreadLocalCache;
        if (fastThreadLocalCache.thread == thread)
            fastThreadLocalCache.value = value;
        if (value == null && !this.withCustomSupplier)
            super.remove();
        else
            super.set(value);
    }

    @Override
    @SuppressWarnings("ThreadLocalSetWithNull")
    public final void remove() {
        if (this.withCustomSupplier) {
            // Since we use volatile for speed
            // and do not lock fastThreadLocalCache
            // we cannot remove cache conditionally
            // for thread safety reason
            this.fastThreadLocalCache = noCache();
            super.remove();
        } else this.set(null);
    }

    private static class FastThreadLocalCache<T> {
        private final Thread thread;
        private T value;

        private FastThreadLocalCache(Thread thread) {
            this.thread = thread;
        }

        private FastThreadLocalCache(Thread thread, T value) {
            this.thread = thread;
            this.value = value;
        }
    }

    static final class SuppliedFastThreadLocal<T> extends FastThreadLocal<T> {
        private final Supplier<? extends T> supplier;

        SuppliedFastThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return Objects.requireNonNull(supplier.get(), "Initial value cannot be null");
        }
    }
}
