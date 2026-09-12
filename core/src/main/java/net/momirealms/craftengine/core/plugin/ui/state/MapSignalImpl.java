package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class MapSignalImpl<K, V> extends CollectionSignal<Map<K, V>> implements MapSignal<K, V> {
    private final Map<K, V> delegate;
    private final ElementHooks<BiFunction<? super K, ? super V, ? extends V>> putting = new ElementHooks<>(); // 存入之前的钩子
    private final ElementHooks<BiConsumer<? super K, ? super V>> removing = new ElementHooks<>();             // 移除之后的钩子

    MapSignalImpl(Map<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    @NotNull
    public Subscription beforePut(@NotNull BiFunction<? super K, ? super V, ? extends V> hook) {
        return this.putting.register(hook);
    }

    @Override
    @NotNull
    public Subscription afterRemove(@NotNull BiConsumer<? super K, ? super V> hook) {
        return this.removing.register(hook);
    }

    @Override
    public Map<K, V> get() {
        return this;
    }

    // 返回经过全部放入钩子后的值
    private V putting(K key, V value) {
        if (!this.putting.active()) return value;
        V result = value;
        for (HookReference<BiFunction<? super K, ? super V, ? extends V>> reference : this.putting.live()) {
            BiFunction<? super K, ? super V, ? extends V> hook = reference.get();
            if (hook != null) {
                result = hook.apply(key, result);
            }
        }
        return result;
    }

    private boolean hooked() {
        return this.putting.active() || this.removing.active();
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
    public boolean containsKey(Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.delegate.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return this.delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        this.delegate.forEach(action);
    }

    // 放入与替换

    @Override
    public V put(K key, V value) {
        return this.putOne(key, value);
    }

    // 等值映射保留已存实例, 有钩子时先摘旧值再放新值
    private V putOne(K key, V value) {
        V old = this.delegate.get(key);
        if (old != null && old.equals(value)) return old;
        if (!this.hooked()) {
            old = this.delegate.put(key, value);
            // 返回 null 无法区分新增 key 与覆盖 null 映射, 两种情况都保守通知
            if (old == null || !old.equals(value)) {
                this.changed();
            }
            return old;
        }
        if (old != null) {
            this.removed(key, old);
        }
        this.delegate.put(key, this.putting(key, value));
        this.changed();
        return old;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) return;
        if (!this.hooked()) {
            this.delegate.putAll(m);
            this.changed();
            return;
        }
        // 每个值单独经过钩子, 通知由 batch 合并
        this.batch(() -> {
            for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                this.putOne(entry.getKey(), entry.getValue());
            }
        });
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (this.putting == null) {
            V old = this.delegate.putIfAbsent(key, value);
            if (old == null) {
                this.changed();
            }
            return old;
        }
        V existing = this.delegate.get(key);
        if (existing != null) return existing;
        V old = this.delegate.putIfAbsent(key, this.putting(key, value));
        if (old == null) {
            this.changed();
        }
        return old;
    }

    @Override
    public V replace(K key, V value) {
        if (!this.hooked()) {
            V old = this.delegate.replace(key, value);
            if (old != null) {
                if (!old.equals(value)) {
                    this.changed();
                }
                return old;
            }
            // 返回 null 无法区分未命中与替换 null 映射, containsKey 补足判断
            if (value != null && this.delegate.containsKey(key)) {
                this.changed();
            }
            return null;
        }
        V old = this.delegate.get(key);
        // null 旧值只有在 key 确实存在时才参与替换
        if (Objects.equals(old, value) || (old == null && !this.delegate.containsKey(key))) return old;
        if (old != null) {
            this.removed(key, old);
        }
        this.delegate.replace(key, this.putting(key, value));
        this.changed();
        return old;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (!this.hooked()) {
            boolean replaced = this.delegate.replace(key, oldValue, newValue);
            if (replaced && !Objects.equals(oldValue, newValue)) {
                this.changed();
            }
            return replaced;
        }
        V current = this.delegate.get(key);
        // null 旧值按 Map 契约参与匹配, key 必须真实存在
        if (!Objects.equals(current, oldValue) || (current == null && !this.delegate.containsKey(key))) return false;
        if (Objects.equals(current, newValue)) return true;
        if (current != null) {
            this.removed(key, current);
        }
        boolean replaced = this.delegate.replace(key, current, this.putting(key, newValue));
        if (replaced) {
            this.changed();
        }
        return replaced;
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (this.delegate.isEmpty()) return;
        boolean[] changed = new boolean[1];
        this.delegate.replaceAll((key, old) -> {
            V replacement = function.apply(key, old);
            if (replacement == old) return old;
            changed[0] = true;
            return this.swapping(key, old, replacement);
        });
        if (changed[0]) {
            this.changed();
        }
    }

    // 在 delegate 重算函数中先摘旧值再计算实际存入的新值
    private V swapping(K key, @Nullable V old, V replacement) {
        if (old != null) {
            this.removed(key, old);
        }
        return replacement == null ? null : this.putting(key, replacement);
    }

    private void removed(K key, V value) {
        if (!this.removing.active()) return;
        for (HookReference<BiConsumer<? super K, ? super V>> reference : this.removing.live()) {
            BiConsumer<? super K, ? super V> hook = reference.get();
            if (hook != null) {
                hook.accept(key, value);
            }
        }
    }

    // compute 钩子留在 delegate 的重算函数内, 保留并发 map 的按 key 原子性

    @Override
    public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        boolean[] changed = new boolean[1];
        V result = this.delegate.compute(key, (k, old) -> {
            V replacement = remappingFunction.apply(k, old);
            if (replacement == old) return old;
            changed[0] = true;
            return this.swapping(k, old, replacement);
        });
        if (changed[0]) {
            this.changed();
        }
        return result;
    }

    @Override
    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        boolean[] changed = new boolean[1];
        V result = this.delegate.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(k);
            if (value == null) return null;
            V stored = this.putting(k, value);
            if (stored == null) return null;
            changed[0] = true;
            return stored;
        });
        if (changed[0]) {
            this.changed();
        }
        return result;
    }

    @Override
    public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        boolean[] changed = new boolean[1];
        V result = this.delegate.computeIfPresent(key, (k, old) -> {
            V replacement = remappingFunction.apply(k, old);
            if (replacement == old) return old;
            changed[0] = true;
            return this.swapping(k, old, replacement);
        });
        if (changed[0]) {
            this.changed();
        }
        return result;
    }

    @Override
    public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        // 经 compute 进入同一条钩子路径
        return this.compute(key, (k, old) -> old == null ? value : remappingFunction.apply(old, value));
    }

    // 移除

    @Override
    public V remove(Object key) {
        if (this.delegate.get(key) == null) {
            // 允许 null 的 delegate 用二参 remove 原子确认 null 映射, 并发 map 的 null 则表示未命中
            if (this.delegate.containsKey(key) && this.delegate.remove(key, null)) {
                this.removedThenChanged(key, null);
            }
            return null;
        }
        V old = this.delegate.remove(key);
        if (old != null) {
            this.removedThenChanged(key, old);
        }
        return old;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (this.removing == null) {
            boolean removed = this.delegate.remove(key, value);
            if (removed) {
                this.changed();
            }
            return removed;
        }
        if (!this.delegate.containsKey(key)) return false;
        V current = this.delegate.get(key);
        if (!Objects.equals(current, value)) return false;
        boolean removed = this.delegate.remove(key, current);
        if (removed) {
            this.removedThenChanged(key, current);
        }
        return removed;
    }

    // 移除已经落地, 钩子失败时仍在 finally 中通知
    @SuppressWarnings("unchecked")
    private void removedThenChanged(Object key, V value) {
        try {
            this.removed((K) key, value);
        } finally {
            this.changed();
        }
    }

    // 一批移除逐个执行钩子, 最后只通知一次
    private void allRemovedThenChanged(@Nullable List<Map.Entry<K, V>> doomed) {
        try {
            if (doomed != null) {
                for (int i = 0; i < doomed.size(); i++) {
                    this.removed(doomed.get(i).getKey(), doomed.get(i).getValue());
                }
            }
        } finally {
            this.changed();
        }
    }

    // 删除单个 key, 并发竞争中未命中时不执行钩子或通知
    private boolean removeKey(Object key) {
        if (this.delegate.get(key) == null) {
            if (this.delegate.containsKey(key) && this.delegate.remove(key, null)) {
                this.removedThenChanged(key, null);
                return true;
            }
            return false;
        }
        V old = this.delegate.remove(key);
        if (old == null) return false;
        this.removedThenChanged(key, old);
        return true;
    }

    // 按条目谓词批量删除, 有钩子时逐条确认实际删除结果
    private boolean removeEntries(Predicate<? super Map.Entry<K, V>> matching) {
        if (this.removing == null) {
            boolean removed = this.delegate.entrySet().removeIf(matching);
            if (removed) {
                this.changed();
            }
            return removed;
        }
        List<Map.Entry<K, V>> doomed = new ArrayList<>();
        for (Map.Entry<K, V> entry : this.delegate.entrySet()) {
            if (matching.test(entry)) {
                doomed.add(new AbstractMap.SimpleImmutableEntry<>(entry));
            }
        }
        if (doomed.isEmpty()) return false;
        // 先完成全部删除, 再按原顺序执行钩子
        List<Map.Entry<K, V>> gone = new ArrayList<>(doomed.size());
        for (int i = 0; i < doomed.size(); i++) {
            Map.Entry<K, V> entry = doomed.get(i);
            if (this.delegate.remove(entry.getKey(), entry.getValue())) {
                gone.add(entry);
            }
        }
        // 并发竞争可能让全部候选落空
        if (gone.isEmpty()) return false;
        this.allRemovedThenChanged(gone);
        return true;
    }

    @Override
    public void clear() {
        if (this.delegate.isEmpty()) return;
        List<Map.Entry<K, V>> doomed = null;
        if (this.removing != null) {
            doomed = new ArrayList<>(this.delegate.size());
            for (Map.Entry<K, V> entry : this.delegate.entrySet()) {
                doomed.add(new AbstractMap.SimpleImmutableEntry<>(entry));
            }
        }
        this.delegate.clear();
        this.allRemovedThenChanged(doomed);
    }

    // 视图

    @Override
    @NotNull
    public Set<K> keySet() {
        return new KeySetView();
    }

    @Override
    @NotNull
    public Collection<V> values() {
        return new ValuesView();
    }

    @Override
    @NotNull
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySetView();
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    // 三个集合视图共用读取与批量删除, 子类只转换条目形态
    private abstract class View<T> implements Collection<T> {

        abstract Collection<T> target();

        abstract T elementOf(Map.Entry<K, V> entry);

        @Override
        public int size() {
            return this.target().size();
        }

        @Override
        public boolean isEmpty() {
            return this.target().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.target().contains(o);
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return this.target().containsAll(c);
        }

        @Override
        @NotNull
        public Object[] toArray() {
            return this.snapshot().toArray();
        }

        @Override
        @NotNull
        public <A> A[] toArray(@NotNull A[] a) {
            return this.snapshot().toArray(a);
        }

        @Override
        public <A> A[] toArray(IntFunction<A[]> generator) {
            return this.snapshot().toArray(generator);
        }

        // 经包装迭代器复制, entrySet 才能保留写穿的 EntryView
        private List<T> snapshot() {
            List<T> copy = new ArrayList<>(this.size());
            for (T element : this) {
                copy.add(element);
            }
            return copy;
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            for (T element : this) {
                action.accept(element);
            }
        }

        @Override
        public Spliterator<T> spliterator() {
            return Spliterators.spliterator(this.iterator(), this.size(), 0);
        }

        @Override
        public Stream<T> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }

        @Override
        public Stream<T> parallelStream() {
            return StreamSupport.stream(this.spliterator(), true);
        }

        @Override
        @NotNull
        public Iterator<T> iterator() {
            return new EntryIterator<>(MapSignalImpl.this.delegate.entrySet().iterator(), this::elementOf);
        }

        @Override
        public boolean add(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            Collection<?> lookup = lookupOf(c);
            return MapSignalImpl.this.removeEntries(entry -> lookup.contains(this.elementOf(entry)));
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            Collection<?> lookup = lookupOf(c);
            return MapSignalImpl.this.removeEntries(entry -> !lookup.contains(this.elementOf(entry)));
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter) {
            return MapSignalImpl.this.removeEntries(entry -> filter.test(this.elementOf(entry)));
        }

        @Override
        public void clear() {
            MapSignalImpl.this.clear();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || this.target().equals(o);
        }

        @Override
        public int hashCode() {
            return this.target().hashCode();
        }

        @Override
        public String toString() {
            return this.target().toString();
        }
    }

    private final class KeySetView extends View<K> implements Set<K> {

        @Override
        Collection<K> target() {
            return MapSignalImpl.this.delegate.keySet();
        }

        @Override
        K elementOf(Map.Entry<K, V> entry) {
            return entry.getKey();
        }

        @Override
        public boolean remove(Object o) {
            return MapSignalImpl.this.removeKey(o);
        }
    }

    private final class ValuesView extends View<V> {

        @Override
        Collection<V> target() {
            return MapSignalImpl.this.delegate.values();
        }

        @Override
        V elementOf(Map.Entry<K, V> entry) {
            return entry.getValue();
        }

        // Collection.remove 只删除第一个等值元素
        @Override
        public boolean remove(Object o) {
            for (Map.Entry<K, V> entry : MapSignalImpl.this.delegate.entrySet()) {
                if (Objects.equals(entry.getValue(), o)) {
                    return MapSignalImpl.this.removeKey(entry.getKey());
                }
            }
            return false;
        }
    }

    private final class EntrySetView extends View<Map.Entry<K, V>> implements Set<Map.Entry<K, V>> {

        @Override
        Collection<Map.Entry<K, V>> target() {
            return MapSignalImpl.this.delegate.entrySet();
        }

        @Override
        Map.Entry<K, V> elementOf(Map.Entry<K, V> entry) {
            return new EntryView(entry);
        }

        @Override
        public boolean remove(Object o) {
            // 直接按 key 与值条件删除
            if (!(o instanceof Map.Entry<?, ?> entry)) return false;
            return MapSignalImpl.this.remove(entry.getKey(), entry.getValue());
        }
    }

    // 将 delegate 条目转换成各视图元素, remove 写回包装器
    private final class EntryIterator<T> implements Iterator<T> {
        private final Iterator<Map.Entry<K, V>> it;
        private final Function<Map.Entry<K, V>, T> elementOf;
        @Nullable private Map.Entry<K, V> last;   // 最近一次 next 给出的条目, remove 的对象

        private EntryIterator(Iterator<Map.Entry<K, V>> it, Function<Map.Entry<K, V>, T> elementOf) {
            this.it = it;
            this.elementOf = elementOf;
        }

        @Override
        public boolean hasNext() {
            return this.it.hasNext();
        }

        @Override
        public T next() {
            this.last = this.it.next();
            return this.elementOf.apply(this.last);
        }

        @Override
        public void remove() {
            Map.Entry<K, V> last = this.last;
            if (last == null) {
                throw new IllegalStateException();
            }
            K key = last.getKey();
            V value = last.getValue();
            this.it.remove();
            this.last = null;
            MapSignalImpl.this.removedThenChanged(key, value);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            // 经 next 更新 last, 后续 remove 才能拿到对应条目
            while (this.it.hasNext()) {
                action.accept(this.next());
            }
        }
    }

    // setValue 写穿包装器, 先摘旧值再放新值
    private final class EntryView implements Map.Entry<K, V> {
        private final Map.Entry<K, V> entry;

        private EntryView(Map.Entry<K, V> entry) {
            this.entry = entry;
        }

        @Override
        public K getKey() {
            return this.entry.getKey();
        }

        @Override
        public V getValue() {
            return this.entry.getValue();
        }

        @Override
        public V setValue(V value) {
            V old = this.entry.getValue();
            if (old == null ? value == null : old.equals(value)) return old;
            if (!MapSignalImpl.this.delegate.entrySet().contains(this.entry)) return old;
            this.entry.setValue(MapSignalImpl.this.swapping(this.entry.getKey(), old, value));
            MapSignalImpl.this.changed();
            return old;
        }

        @Override
        public boolean equals(Object o) {
            return this.entry.equals(o);
        }

        @Override
        public int hashCode() {
            return this.entry.hashCode();
        }

        @Override
        public String toString() {
            return this.entry.toString();
        }
    }
}
