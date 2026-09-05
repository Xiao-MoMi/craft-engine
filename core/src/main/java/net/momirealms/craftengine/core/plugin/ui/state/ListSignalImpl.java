package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

final class ListSignalImpl<E> extends CollectionSignal<List<E>> implements ListSignal<E> {
    private final List<E> delegate;
    private final Facade root;
    private final ElementHooks<Function<? super E, ? extends E>> adding = new ElementHooks<>(); // 存入之前的钩子
    private final ElementHooks<Consumer<? super E>> removing = new ElementHooks<>();            // 移除之后的钩子

    ListSignalImpl(List<E> delegate) {
        this.delegate = delegate;
        this.root = new Facade(delegate);
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
    public List<E> get() {
        return this;
    }

    @Override
    public int size() {
        return this.root.size();
    }

    @Override
    public boolean isEmpty() {
        return this.root.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.root.contains(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.root.containsAll(c);
    }

    @Override
    public E get(int index) {
        return this.root.get(index);
    }

    @Override
    public E getFirst() {
        return this.root.getFirst();
    }

    @Override
    public E getLast() {
        return this.root.getLast();
    }

    @Override
    public int indexOf(Object o) {
        return this.root.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.root.lastIndexOf(o);
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return this.root.toArray();
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        return this.root.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return this.root.toArray(generator);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.root.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return this.root.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return this.root.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return this.root.parallelStream();
    }

    @Override
    public boolean add(E e) {
        return this.root.add(e);
    }

    @Override
    public void add(int index, E element) {
        this.root.add(index, element);
    }

    @Override
    public void addFirst(E e) {
        this.root.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        this.root.addLast(e);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return this.root.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        return this.root.addAll(index, c);
    }

    @Override
    public E set(int index, E element) {
        return this.root.set(index, element);
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<E> operator) {
        this.root.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super E> c) {
        this.root.sort(c);
    }

    @Override
    public boolean remove(Object o) {
        return this.root.remove(o);
    }

    @Override
    public E remove(int index) {
        return this.root.remove(index);
    }

    @Override
    public E removeFirst() {
        return this.root.removeFirst();
    }

    @Override
    public E removeLast() {
        return this.root.removeLast();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.root.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.root.retainAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return this.root.removeIf(filter);
    }

    @Override
    public void clear() {
        this.root.clear();
    }

    @Override
    @NotNull
    public List<E> subList(int fromIndex, int toIndex) {
        return this.root.subList(fromIndex, toIndex);
    }

    @Override
    public List<E> reversed() {
        return this.root.reversed();
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return this.root.iterator();
    }

    @Override
    @NotNull
    public ListIterator<E> listIterator() {
        return this.root.listIterator();
    }

    @Override
    @NotNull
    public ListIterator<E> listIterator(int index) {
        return this.root.listIterator(index);
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    // 根 List 与 subList/reversed 视图共用的写入门面, 所有变更写回同一个包装器
    private final class Facade implements List<E> {
        private final List<E> target;

        private Facade(List<E> target) {
            this.target = target;
        }

        // 依次经过每个存活钩子, 没挂过钩子时只读一次标志.
        private E adding(E element) {
            ElementHooks<Function<? super E, ? extends E>> hooks = ListSignalImpl.this.adding;
            if (!hooks.active()) return element;
            E result = element;
            for (HookReference<Function<? super E, ? extends E>> reference : hooks.live()) {
                Function<? super E, ? extends E> hook = reference.get();
                if (hook != null) {
                    result = hook.apply(result);
                }
            }
            return result;
        }

        // 两类钩子都没挂时写路径可以整段走原生实现
        private boolean hooked() {
            return ListSignalImpl.this.adding.active() || ListSignalImpl.this.removing.active();
        }

        private void removed(E element) {
            ElementHooks<Consumer<? super E>> hooks = ListSignalImpl.this.removing;
            if (!hooks.active()) return;
            for (HookReference<Consumer<? super E>> reference : hooks.live()) {
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
                ListSignalImpl.this.changed();
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
                ListSignalImpl.this.changed();
            }
        }

        // 读取

        @Override
        public int size() {
            return this.target.size();
        }

        @Override
        public boolean isEmpty() {
            return this.target.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.target.contains(o);
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return this.target.containsAll(c);
        }

        @Override
        public E get(int index) {
            return this.target.get(index);
        }

        @Override
        public E getFirst() {
            return this.target.getFirst();
        }

        @Override
        public E getLast() {
            return this.target.getLast();
        }

        @Override
        public int indexOf(Object o) {
            return this.target.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return this.target.lastIndexOf(o);
        }

        @Override
        @NotNull
        public Object[] toArray() {
            return this.target.toArray();
        }

        @Override
        @NotNull
        public <T> T[] toArray(@NotNull T[] a) {
            return this.target.toArray(a);
        }

        @Override
        public <T> T[] toArray(IntFunction<T[]> generator) {
            return this.target.toArray(generator);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            this.target.forEach(action);
        }

        @Override
        public Spliterator<E> spliterator() {
            return this.target.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return this.target.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return this.target.parallelStream();
        }

        // 放入

        @Override
        public boolean add(E e) {
            boolean added = this.target.add(this.adding(e));
            if (added) {
                ListSignalImpl.this.changed();
            }
            return added;
        }

        @Override
        public void add(int index, E element) {
            this.target.add(index, this.adding(element));
            ListSignalImpl.this.changed();
        }

        @Override
        public void addFirst(E e) {
            this.target.addFirst(this.adding(e));
            ListSignalImpl.this.changed();
        }

        @Override
        public void addLast(E e) {
            this.target.addLast(this.adding(e));
            ListSignalImpl.this.changed();
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends E> c) {
            boolean added = this.target.addAll(this.allAdding(c));
            if (added) {
                ListSignalImpl.this.changed();
            }
            return added;
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends E> c) {
            boolean added = this.target.addAll(index, this.allAdding(c));
            if (added) {
                ListSignalImpl.this.changed();
            }
            return added;
        }

        // 先计算全部钩子结果再批量写入, 写时复制 delegate 只复制一次
        private Collection<? extends E> allAdding(Collection<? extends E> c) {
            if (!ListSignalImpl.this.adding.active() || c.isEmpty()) return c;
            List<E> stored = new ArrayList<>(c.size());
            for (E element : c) {
                stored.add(this.adding(element));
            }
            return stored;
        }

        // 替换

        @Override
        public E set(int index, E element) {
            if (!this.hooked()) {
                E old = this.target.set(index, element);
                if (old != element) {
                    ListSignalImpl.this.changed();
                }
                return old;
            }
            // 先摘旧值再放新值, 让按位置维护的旁表保持对应
            E old = this.target.get(index);
            if (old == element) return old;
            this.removed(old);
            this.target.set(index, this.adding(element));
            ListSignalImpl.this.changed();
            return old;
        }

        @Override
        public void replaceAll(@NotNull UnaryOperator<E> operator) {
            if (this.target.isEmpty()) return;
            if (!this.hooked()) {
                boolean[] changed = new boolean[1];
                this.target.replaceAll(old -> {
                    E replacement = operator.apply(old);
                    changed[0] |= replacement != old;
                    return replacement;
                });
                if (changed[0]) {
                    ListSignalImpl.this.changed();
                }
                return;
            }
            // 先计算全部替换结果再批量写回, 写时复制 delegate 只复制一次
            List<E> results = new ArrayList<>(this.target.size());
            boolean changed = false;
            for (E old : this.target) {
                E replacement = operator.apply(old);
                if (replacement != old) {
                    changed = true;
                    this.removed(old);
                    replacement = this.adding(replacement);
                }
                results.add(replacement);
            }
            if (!changed) return;
            Iterator<E> next = results.iterator();
            this.target.replaceAll(ignored -> next.next());
            ListSignalImpl.this.changed();
        }

        @Override
        public void sort(Comparator<? super E> c) {
            if (this.target.size() < 2) return;
            this.target.sort(c);
            ListSignalImpl.this.changed();
        }

        // 移除

        @Override
        public boolean remove(Object o) {
            boolean removed = this.target.remove(o);
            if (removed) {
                @SuppressWarnings("unchecked") E element = (E) o;
                this.removedThenChanged(element);
            }
            return removed;
        }

        @Override
        public E remove(int index) {
            E old = this.target.remove(index);
            this.removedThenChanged(old);
            return old;
        }

        @Override
        public E removeFirst() {
            E old = this.target.removeFirst();
            this.removedThenChanged(old);
            return old;
        }

        @Override
        public E removeLast() {
            E old = this.target.removeLast();
            this.removedThenChanged(old);
            return old;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            // 没有移除钩子时沿用 delegate 的原始查找方式
            Collection<?> lookup = ListSignalImpl.this.removing == null ? c : lookupOf(c);
            return this.removeMatching(lookup::contains, () -> this.target.removeAll(c));
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            Collection<?> lookup = ListSignalImpl.this.removing == null ? c : lookupOf(c);
            return this.removeMatching(element -> !lookup.contains(element), () -> this.target.retainAll(c));
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return this.removeMatching(filter, () -> this.target.removeIf(filter));
        }

        // 有钩子时先记录命中元素, 再让 target 批量删除. 写时复制迭代器不支持逐个删除.
        private boolean removeMatching(Predicate<? super E> matching, BooleanSupplier bulkRemove) {
            List<E> doomed = null;
            if (ListSignalImpl.this.removing != null) {
                doomed = new ArrayList<>();
                for (E element : this.target) {
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
            if (this.target.isEmpty()) return;
            List<E> doomed = ListSignalImpl.this.removing == null ? null : new ArrayList<>(this.target);
            this.target.clear();
            this.allRemovedThenChanged(doomed);
        }

        // 视图与迭代器

        @Override
        @NotNull
        public List<E> subList(int fromIndex, int toIndex) {
            return new Facade(this.target.subList(fromIndex, toIndex));
        }

        @Override
        public List<E> reversed() {
            return new Facade(this.target.reversed());
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return new MutatingIterator(this.target.listIterator());
        }

        @Override
        @NotNull
        public ListIterator<E> listIterator() {
            return new MutatingIterator(this.target.listIterator());
        }

        @Override
        @NotNull
        public ListIterator<E> listIterator(int index) {
            return new MutatingIterator(this.target.listIterator(index));
        }

        @Override
        public boolean equals(Object o) {
            return o == this || this.target.equals(o);
        }

        @Override
        public int hashCode() {
            return this.target.hashCode();
        }

        @Override
        public String toString() {
            return this.target.toString();
        }

        // 迭代器变更写回同一个包装器, 每个动作通知一次
        private final class MutatingIterator implements ListIterator<E> {
            private final ListIterator<E> it;
            @Nullable private E last;   // 最近一次 next / previous 给出的元素, set 与 remove 的对象
            private boolean canModify;

            private MutatingIterator(ListIterator<E> it) {
                this.it = it;
            }

            @Override
            public boolean hasNext() {
                return this.it.hasNext();
            }

            @Override
            public E next() {
                E next = this.it.next();
                this.last = next;
                this.canModify = true;
                return next;
            }

            @Override
            public boolean hasPrevious() {
                return this.it.hasPrevious();
            }

            @Override
            public E previous() {
                E previous = this.it.previous();
                this.last = previous;
                this.canModify = true;
                return previous;
            }

            @Override
            public int nextIndex() {
                return this.it.nextIndex();
            }

            @Override
            public int previousIndex() {
                return this.it.previousIndex();
            }

            @Override
            public void remove() {
                this.it.remove();
                this.canModify = false;
                Facade.this.removedThenChanged(this.last);
            }

            @Override
            public void set(E e) {
                if (!this.canModify) {
                    throw new IllegalStateException();
                }
                if (this.last == e) return;
                Facade.this.removed(this.last);
                E stored = Facade.this.adding(e);
                this.it.set(stored);
                this.last = stored;
                ListSignalImpl.this.changed();
            }

            @Override
            public void add(E e) {
                this.it.add(Facade.this.adding(e));
                this.canModify = false;
                ListSignalImpl.this.changed();
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
}
