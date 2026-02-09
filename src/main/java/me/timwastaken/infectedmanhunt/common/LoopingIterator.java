package me.timwastaken.infectedmanhunt.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LoopingIterator<T> implements Iterator<T> {
    private final Iterable<T> collection;
    private Iterator<T> currentIterator;

    public LoopingIterator(Iterable<T> collection) {
        this.collection = collection;
        this.currentIterator = collection.iterator();
    }

    @Override
    public boolean hasNext() {
        // Always returns true unless the collection is empty
        return currentIterator.hasNext() || collection.iterator().hasNext();
    }

    @Override
    public T next() {
        // If we reached the end, reset to the start
        if (!currentIterator.hasNext()) {
            currentIterator = collection.iterator();
        }

        // Check if collection is actually empty
        if (!currentIterator.hasNext()) {
            throw new NoSuchElementException("Collection is empty");
        }

        return currentIterator.next();
    }

    @Override
    public void remove() {
        currentIterator.remove();
    }
}