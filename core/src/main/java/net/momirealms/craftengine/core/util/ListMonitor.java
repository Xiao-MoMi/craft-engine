package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

public class ListMonitor<T> implements List<T> {

    private final List<T> list;
    private final Consumer<T> addConsumer;
    private final Consumer<Object> removeConsumer;

    public ListMonitor(List<T> list, Consumer<T> addConsumer, Consumer<Object> removeConsumer) {
        for (T key : list) {
            addConsumer.accept(key);
        }
        this.list = list;
        this.addConsumer = addConsumer;
        this.removeConsumer = removeConsumer;
    }

    public List<T> list() {
        return list;
    }

    public Consumer<T> addConsumer() {
        return addConsumer;
    }

    public Consumer<Object> removeConsumer() {
        return removeConsumer;
    }

    @Override
    public synchronized boolean add(T t) {
        addConsumer.accept(t);
        return list.add(t);
    }

    @Override
    public synchronized boolean addAll(@NotNull Collection<? extends T> c) {
        for (T element : c) {
            addConsumer.accept(element);
        }
        return list.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, @NotNull Collection<? extends T> c) {
        for (T element : c) {
            addConsumer.accept(element);
        }
        return list.addAll(index, c);
    }

    @Override
    public synchronized void add(int index, T element) {
        addConsumer.accept(element);
        list.add(index, element);
    }

    @Override
    public synchronized boolean remove(Object o) {
        removeConsumer.accept(o);
        return list.remove(o);
    }

    @Override
    public synchronized boolean removeAll(@NotNull Collection<?> collection) {
        for (Object o : collection) {
            removeConsumer.accept(o);
        }
        return list.removeAll(collection);
    }

    @Override
    public synchronized void clear() {
        for (T element : list) {
            removeConsumer.accept(element);
        }
        list.clear();
    }

    @Override
    public synchronized int size() {
        return list.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return list.contains(o);
    }

    @NotNull
    @Override
    public synchronized Iterator<T> iterator() {
        return list.iterator();
    }

    @NotNull
    @Override
    public synchronized Object[] toArray() {
        return list.toArray();
    }

    @NotNull
    @Override
    public synchronized <E> E[] toArray(@NotNull E[] a) {
        return list.toArray(a);
    }

    @NotNull
    @Override
    public synchronized List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @SuppressWarnings("all")
    @Override
    public synchronized boolean containsAll(@NotNull Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public synchronized boolean retainAll(@NotNull Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public synchronized T get(int index) {
        return list.get(index);
    }

    @Override
    public synchronized T set(int index, T element) {
        return list.set(index, element);
    }

    @Override
    public synchronized T remove(int index) {
        return list.remove(index);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @NotNull
    @Override
    public synchronized ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @NotNull
    @Override
    public synchronized ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }
}
