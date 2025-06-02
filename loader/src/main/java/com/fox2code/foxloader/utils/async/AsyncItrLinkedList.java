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

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * A variant of LinkedList that support concurrent modifications on iterator.
 *
 * @see LinkedList
 * @param <E> the type of elements held in this collection
 */
public class AsyncItrLinkedList<E> extends LinkedList<E> {
    @NotNull @Override
    public ListIterator<E> listIterator(int index) {
        return new UnsafeAsyncLinkedListItr(super.listIterator(index));
    }

    private class UnsafeAsyncLinkedListItr implements ListIterator<E> {
        private ListIterator<E> listIterator;
        private int modCount;
        private E lastRet;
        private int size;

        private UnsafeAsyncLinkedListItr(ListIterator<E> listIterator) {
            this.listIterator = listIterator;
            this.modCount = AsyncItrLinkedList.this.modCount;
            this.size = AsyncItrLinkedList.this.size();
        }

        @Override
        public boolean hasNext() {
            return this.listIterator.hasNext();
        }

        @Override
        public E next() {
            if (!this.listIterator.hasNext())
                throw new NoSuchElementException();
            this.fixComodification();
            return this.lastRet = this.listIterator.next();
        }

        @Override
        public boolean hasPrevious() {
            return this.listIterator.hasPrevious();
        }

        @Override
        public E previous() {
            if (!this.listIterator.hasPrevious())
                throw new NoSuchElementException();
            this.fixComodification();
            return this.listIterator.previous();
        }

        @Override
        public int nextIndex() {
            return this.listIterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return this.listIterator.previousIndex();
        }

        @Override
        public void remove() {
            this.fixComodification();
            this.listIterator.remove();
            this.addModCount();
        }

        @Override
        public void set(E e) {
            this.fixComodification();
            this.listIterator.set(e);
        }

        @Override
        public void add(E e) {
            this.fixComodification();
            this.listIterator.add(e);
            this.addModCount();
        }

        final void addModCount() {
            this.modCount++;
            this.size = AsyncItrLinkedList.this.size();
        }

        final void fixComodification() {
            if (AsyncItrLinkedList.this.modCount != this.modCount) {
                // UnsafeAsyncLinkedList.this.modCount = this.modCount;
                int indexDiff = AsyncItrLinkedList.this.size() - this.size;
                int index = Math.max(0, this.listIterator.previousIndex() + indexDiff);
                this.listIterator = AsyncItrLinkedList.super.listIterator(index);
                if (this.listIterator.next() != this.lastRet) this.listIterator.previous();
            }
        }
    }
}
