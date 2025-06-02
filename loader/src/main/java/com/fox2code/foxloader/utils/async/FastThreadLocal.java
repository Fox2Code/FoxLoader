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
package com.fox2code.foxloader.utils.async;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Faster implementation of {@link ThreadLocal} made to work better at low thread count.
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
            // for thread safety reasons
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
