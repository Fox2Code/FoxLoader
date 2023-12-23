package com.fox2code.foxloader.launcher.utils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class Enumerations {
    public static <E> Enumeration<E> empty() {
        return Collections.emptyEnumeration();
    }

    public static <E> Enumeration<E> singleton(E e) {
        return e == null ? new Null<>() : new Singleton<>(e);
    }

    public static <E> Enumeration<E> optional(E e) {
        return e == null ? empty() : singleton(e);
    }

    private static final class Singleton<E> implements Enumeration<E> {
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

    private static final class Null<E> implements Enumeration<E> {
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
