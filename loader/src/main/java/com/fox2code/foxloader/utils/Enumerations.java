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
