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
package com.fox2code.foxloader.utils;

import java.util.*;
import java.util.function.Consumer;

public final class Enumerations {
    private Enumerations() {}

    public static <E> Enumeration<E> empty() {
        return Collections.emptyEnumeration();
    }

    public static <E> Enumeration<E> singleton(E e) {
        return e == null ? new Null<>() : new Singleton<>(e);
    }

    public static <E> Enumeration<E> optional(E e) {
        return e == null ? empty() : singleton(e);
    }

    private static abstract class IteratorEnumeration<E> implements Enumeration<E>, Iterator<E> {
        @Override
        public abstract boolean hasMoreElements();

        @Override
        public abstract E nextElement();

        @Override
        public final boolean hasNext() {
            return false;
        }

        @Override
        public final E next() {
            return null;
        }

        @Override
        public final void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (hasMoreElements()) {
                action.accept(nextElement());
            }
        }

        // Implement java9+ helper
        public final Iterator<E> asIterator() {
            return this;
        }
    }

    private static final class Singleton<E> extends IteratorEnumeration<E> {
        private E element;

        private Singleton(E element) {
            this.element = element;
        }

        @Override
        public boolean hasMoreElements() {
            return this.element != null;
        }

        @Override
        public E nextElement() {
            E element = this.element;
            this.element = null;
            if (element == null) {
                throw new NoSuchElementException();
            }
            return element;
        }
    }

    private static final class Null<E> extends IteratorEnumeration<E> {
        private boolean hasMoreElements;

        private Null() {
            this.hasMoreElements = true;
        }

        @Override
        public boolean hasMoreElements() {
            return this.hasMoreElements;
        }

        @Override
        public E nextElement() {
            if (!this.hasMoreElements) {
                throw new NoSuchElementException();
            }
            this.hasMoreElements = false;
            return null;
        }
    }
}
