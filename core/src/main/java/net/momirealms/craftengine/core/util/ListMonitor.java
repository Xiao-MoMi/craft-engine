package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

public final class ListMonitor<T> implements List<T> {
    private final List<T> list;
    private final Consumer<T> addListener;
    private final Consumer<Object> removeListener;

    public ListMonitor(List<T> list, Consumer<T> addListener, Consumer<Object> removeListener) {
        this.list = list;
        this.addListener = addListener;
        this.removeListener = removeListener;
        for (T key : list) {
            this.addListener.accept(key);
        }
    }

    public List<T> list() {
        return this.list;
    }

    public Consumer<T> addConsumer() {
        return this.addListener;
    }

    public Consumer<Object> removeConsumer() {
        return this.removeListener;
    }

    @Override
    public synchronized boolean add(T t) {
        this.addListener.accept(t);
        return this.list.add(t);
    }

    @Override
    public synchronized boolean addAll(@NotNull Collection<? extends T> c) {
        for (T element : c) {
            this.addListener.accept(element);
        }
        return this.list.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, @NotNull Collection<? extends T> c) {
        boolean b = this.list.addAll(index, c);
        for (T element : c) {
            this.addListener.accept(element);
        }
        return b;
    }

    @Override
    public synchronized void add(int index, T element) {
        this.list.add(index, element);
        this.addListener.accept(element);
    }

    @Override
    public synchronized boolean remove(Object o) {
        this.removeListener.accept(o);
        return this.list.remove(o);
    }

    @Override
    public synchronized boolean removeAll(@NotNull Collection<?> collection) {
        for (Object o : collection) {
            this.removeListener.accept(o);
        }
        return this.list.removeAll(collection);
    }

    @Override
    public synchronized void clear() {
        for (T element : this.list) {
            this.removeListener.accept(element);
        }
        this.list.clear();
    }

    @Override
    public synchronized int size() {
        return this.list.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return this.list.contains(o);
    }

    @NotNull
    @Override
    public synchronized Iterator<T> iterator() {
        return this.list.iterator();
    }

    @NotNull
    @Override
    public synchronized Object @NotNull [] toArray() {
        return this.list.toArray();
    }

    @NotNull
    @Override
    public synchronized <E> E @NotNull [] toArray(@NotNull E @NotNull [] a) {
        return this.list.toArray(a);
    }

    @NotNull
    @Override
    public synchronized List<T> subList(int fromIndex, int toIndex) {
        return this.list.subList(fromIndex, toIndex);
    }

    @SuppressWarnings("all")
    @Override
    public synchronized boolean containsAll(@NotNull Collection<?> c) {
        return this.list.containsAll(c);
    }

    @Override
    public synchronized boolean retainAll(@NotNull Collection<?> c) {
        return this.list.retainAll(c);
    }

    @Override
    public synchronized T get(int index) {
        return this.list.get(index);
    }

    @Override
    public synchronized T set(int index, T element) {
        return this.list.set(index, element);
    }

    @Override
    public synchronized T remove(int index) {
        return this.list.remove(index);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return this.list.indexOf(o);
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }

    @NotNull
    @Override
    public synchronized ListIterator<T> listIterator() {
        return this.list.listIterator();
    }

    @NotNull
    @Override
    public synchronized ListIterator<T> listIterator(int index) {
        return this.list.listIterator(index);
    }
}
