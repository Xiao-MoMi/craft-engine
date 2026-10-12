package net.momirealms.craftengine.core.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class SwapList<T extends SwapList.Indexed> extends ObjectArrayList<T> {

    public interface Indexed {
        int index();

        void index(int index);
    }

    public SwapList() {
        super();
    }

    public SwapList(int capacity) {
        super(capacity);
    }

    @Override
    public boolean add(T t) {
        t.index(this.size);
        return super.add(t);
    }

    @Override
    public T set(int index, T t) {
        T old = super.set(index, t);
        old.index(-1);
        t.index(index);
        return old;
    }

    @SuppressWarnings("unchecked")
    public void swapRemove(int index) {
        final Object[] a = this.a;
        final int last = --this.size;
        final T removed = (T) a[index];
        final T moved = (T) a[last];
        a[index] = moved;
        a[last] = null;
        removed.index(-1);
        if (index != last) {
            moved.index(index);
        }
    }

    public void swapRemove(T t) {
        int index = t.index();
        if (index == -1) {
            return;
        }
        this.swapRemove(index);
    }
}
