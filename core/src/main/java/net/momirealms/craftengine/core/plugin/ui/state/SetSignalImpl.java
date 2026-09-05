package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class SetSignalImpl<E> extends CollectionSignal<Set<E>> implements SetSignal<E> {
    private final Set<E> delegate;
    private final ElementHooks<Function<? super E, ? extends E>> adding = new ElementHooks<>(); // 存入之前的钩子
    private final ElementHooks<Consumer<? super E>> removing = new ElementHooks<>();            // 移除之后的钩子

    SetSignalImpl(Set<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    @NotNull
    public Subscription beforeAdd(@NotNull Function<? super E, ? extends E> hook) {
        return this.adding.register(hook);
    }

    @Override
    @NotNull
    public Subscription afterRemove(@NotNull Consumer<? super E> hook) {
        return this.removing.register(hook);
    }

    @Override
    public Set<E> get() {
        return this;
    }

    // 依次经过每个存活钩子, 没挂过钩子时只读一次标志.
    private E adding(E element) {
        if (!this.adding.active()) return element;
        E result = element;
        for (HookReference<Function<? super E, ? extends E>> reference : this.adding.live()) {
            Function<? super E, ? extends E> hook = reference.get();
            if (hook != null) {
                result = hook.apply(result);
            }
        }
        return result;
    }

    private void removed(E element) {
        if (!this.removing.active()) return;
        for (HookReference<Consumer<? super E>> reference : this.removing.live()) {
            Consumer<? super E> hook = reference.get();
            if (hook != null) {
                hook.accept(element);
            }
        }
    }

    // 移除已经落地, 钩子失败时仍在 finally 中通知
    private void removedThenChanged(E element) {
        try {
            this.removed(element);
        } finally {
            this.changed();
        }
    }

    // 一批移除逐个执行钩子, 最后只通知一次
    private void allRemovedThenChanged(@Nullable List<E> doomed) {
        try {
            if (doomed != null) {
                for (int i = 0; i < doomed.size(); i++) {
                    this.removed(doomed.get(i));
                }
            }
        } finally {
            this.changed();
        }
    }

    // 读取

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return this.delegate.toArray();
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        return this.delegate.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return this.delegate.toArray(generator);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.delegate.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return this.delegate.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return this.delegate.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return this.delegate.parallelStream();
    }

    // 放入

    @Override
    public boolean add(E e) {
        if (!this.adding.active()) {
            boolean added = this.delegate.add(e);
            if (added) {
                this.changed();
            }
            return added;
        }
        // 原元素已经存在时不执行放入钩子
        if (this.delegate.contains(e)) return false;
        boolean added = this.delegate.add(this.adding(e));
        if (added) {
            this.changed();
        }
        return added;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        if (c.isEmpty()) return false;
        Collection<? extends E> stored = c;
        if (this.adding.active()) {
            // 先计算全部钩子结果再批量写入, 写时复制 delegate 只复制一次
            List<E> fresh = new ArrayList<>();
            Set<E> freshElements = new HashSet<>();
            for (E element : c) {
                if (!this.delegate.contains(element) && freshElements.add(element)) {
                    fresh.add(this.adding(element));
                }
            }
            stored = fresh;
        }
        boolean added = this.delegate.addAll(stored);
        if (added) {
            this.changed();
        }
        return added;
    }

    // 移除

    @Override
    public boolean remove(Object o) {
        boolean removed = this.delegate.remove(o);
        if (removed) {
            @SuppressWarnings("unchecked") E element = (E) o;
            this.removedThenChanged(element);
        }
        return removed;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        Collection<?> lookup = !this.removing.active() ? c : lookupOf(c);
        return this.removeMatching(lookup::contains, () -> this.delegate.removeAll(c));
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Collection<?> lookup = !this.removing.active() ? c : lookupOf(c);
        return this.removeMatching(element -> !lookup.contains(element), () -> this.delegate.retainAll(c));
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return this.removeMatching(filter, () -> this.delegate.removeIf(filter));
    }

    // 有钩子时先记录命中元素, 再让 delegate 批量删除. 写时复制迭代器不支持逐个删除.
    private boolean removeMatching(Predicate<? super E> matching, BooleanSupplier bulkRemove) {
        List<E> doomed = null;
        if (this.removing.active()) {
            doomed = new ArrayList<>();
            for (E element : this.delegate) {
                if (matching.test(element)) {
                    doomed.add(element);
                }
            }
        }
        boolean removed = bulkRemove.getAsBoolean();
        if (!removed) return false;
        this.allRemovedThenChanged(doomed);
        return true;
    }

    @Override
    public void clear() {
        if (this.delegate.isEmpty()) return;
        List<E> doomed = !this.removing.active() ? null : new ArrayList<>(this.delegate);
        this.delegate.clear();
        this.allRemovedThenChanged(doomed);
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return new MutatingIterator(this.delegate.iterator());
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    // 迭代器删除写回同一个包装器
    private final class MutatingIterator implements Iterator<E> {
        private final Iterator<E> it;
        @Nullable private E last;   // 最近一次 next 给出的元素, remove 的对象

        private MutatingIterator(Iterator<E> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return this.it.hasNext();
        }

        @Override
        public E next() {
            return this.last = this.it.next();
        }

        @Override
        public void remove() {
            this.it.remove();
            SetSignalImpl.this.removedThenChanged(this.last);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            // 经 next 更新 last, 后续 remove 才能拿到对应元素
            while (this.it.hasNext()) {
                action.accept(this.next());
            }
        }
    }
}
