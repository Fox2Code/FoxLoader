package com.fox2code.foxloader.launcher.utils;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A variant of LinkedList that support concurrent modifications on iterator.
 *
 * @see LinkedList
 * @param <E> the type of elements held in this collection
 */
public class AsyncItrLinkedList<E> extends LinkedList<E> {
    @NotNull
    @Override
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
