package net.momirealms.craftengine.core.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class IdList<T> implements IndexedIterable<T> {
    private int nextId;
    private final Reference2IntMap<T> idMap;
    private final List<T> list;

    public IdList() {
        this(512);
    }

    public IdList(int initialSize) {
        this.list = Lists.newArrayListWithExpectedSize(initialSize);
        this.idMap = new Reference2IntOpenHashMap<>(initialSize);
        this.idMap.defaultReturnValue(-1);
    }

    public void set(T value, int id) {
        this.idMap.put(value, id);

        while(this.list.size() <= id) {
            this.list.add(null);
        }

        this.list.set(id, value);
        if (this.nextId <= id) {
            this.nextId = id + 1;
        }

    }

    public void add(T value) {
        this.set(value, this.nextId);
    }

    public int getRawId(T value) {
        return this.idMap.getInt(value);
    }

    @Nullable
    public final T get(int index) {
        return index >= 0 && index < this.list.size() ? this.list.get(index) : null;
    }

    public Iterator<T> iterator() {
        return Iterators.filter(this.list.iterator(), Objects::nonNull);
    }

    public boolean containsKey(int index) {
        return this.get(index) != null;
    }

    public int size() {
        return this.idMap.size();
    }
}
