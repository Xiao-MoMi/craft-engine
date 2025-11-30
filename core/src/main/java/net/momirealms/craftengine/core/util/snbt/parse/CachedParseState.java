package net.momirealms.craftengine.core.util.snbt.parse;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.util.MiscUtils;

import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public abstract class CachedParseState<S> implements ParseState<S> {
    private PositionCache[] positionCache = new PositionCache[256];
    private final ErrorCollector<S> errorCollector;
    private final Scope scope = new Scope();
    private SimpleControl[] controlCache = new SimpleControl[16];
    private int nextControlToReturn;
    private final Silent silent = new Silent();
    private final IntList markedNull = new IntArrayList();
    public static final Object JAVA_NULL_VALUE_MARKER = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    protected CachedParseState(ErrorCollector<S> errorCollector) {
        this.errorCollector = errorCollector;
    }

    @Override
    public Scope scope() {
        return this.scope;
    }

    @Override
    public ErrorCollector<S> errorCollector() {
        return this.errorCollector;
    }

    @Nullable
    @Override
    public <T> T parse(NamedRule<S, T> rule) {
        int i = this.mark();
        PositionCache cacheForPosition = this.getCacheForPosition(i);
        int i1 = cacheForPosition.findKeyIndex(rule.name());
        if (i1 != -1) {
            CacheEntry<T> value = cacheForPosition.getValue(i1);
            if (value != null) {
                if (value == CachedParseState.CacheEntry.NEGATIVE) {
                    return null;
                }
                this.restore(value.markAfterParse);
                return value.value;
            }
        } else {
            i1 = cacheForPosition.allocateNewEntry(rule.name());
        }

        T object = rule.value().parse(this);
        CacheEntry<T> cacheEntry;
        if (object == null) {
            cacheEntry = (CacheEntry<T>) CacheEntry.NEGATIVE;
        } else {
            cacheEntry = new CacheEntry<>(object, this.mark());
        }

        cacheForPosition.setValue(i1, cacheEntry);
        return object;
    }

    private PositionCache getCacheForPosition(int position) {
        int i = this.positionCache.length;
        if (position >= i) {
            int i1 = MiscUtils.growByHalf(i, position + 1);
            PositionCache[] positionCaches = new PositionCache[i1];
            System.arraycopy(this.positionCache, 0, positionCaches, 0, i);
            this.positionCache = positionCaches;
        }

        PositionCache positionCache = this.positionCache[position];
        if (positionCache == null) {
            positionCache = new PositionCache();
            this.positionCache[position] = positionCache;
        }

        return positionCache;
    }

    @Override
    public Control acquireControl() {
        int i = this.controlCache.length;
        if (this.nextControlToReturn >= i) {
            int i1 = MiscUtils.growByHalf(i, this.nextControlToReturn + 1);
            SimpleControl[] simpleControls = new SimpleControl[i1];
            System.arraycopy(this.controlCache, 0, simpleControls, 0, i);
            this.controlCache = simpleControls;
        }

        int i1 = this.nextControlToReturn++;
        SimpleControl simpleControl = this.controlCache[i1];
        if (simpleControl == null) {
            simpleControl = new SimpleControl();
            this.controlCache[i1] = simpleControl;
        } else {
            simpleControl.reset();
        }

        return simpleControl;
    }

    @Override
    public void releaseControl() {
        this.nextControlToReturn--;
    }

    @Override
    public ParseState<S> silent() {
        return this.silent;
    }

    @Override
    public void markNull(int mark) {
        this.markedNull.add(mark);
    }

    @Override
    public boolean isNull(int mark) {
        return this.markedNull.contains(mark);
    }

    record CacheEntry<T>(@Nullable T value, int markAfterParse) {
        public static final CacheEntry<?> NEGATIVE = new CacheEntry<>(null, -1);
    }

    static class PositionCache {
        public static final int ENTRY_STRIDE = 2;
        private static final int NOT_FOUND = -1;
        private Object[] atomCache = new Object[16];
        private int nextKey;

        public int findKeyIndex(Atom<?> atom) {
            for (int i = 0; i < this.nextKey; i += ENTRY_STRIDE) {
                if (this.atomCache[i] == atom) {
                    return i;
                }
            }

            return NOT_FOUND;
        }

        public int allocateNewEntry(Atom<?> entry) {
            int i = this.nextKey;
            this.nextKey += 2;
            int i1 = i + 1;
            int i2 = this.atomCache.length;
            if (i1 >= i2) {
                int i3 = MiscUtils.growByHalf(i2, i1 + 1);
                Object[] objects = new Object[i3];
                System.arraycopy(this.atomCache, 0, objects, 0, i2);
                this.atomCache = objects;
            }

            this.atomCache[i] = entry;
            return i;
        }

        @Nullable
        public <T> CacheEntry<T> getValue(int index) {
            return (CacheEntry<T>)this.atomCache[index + 1];
        }

        public void setValue(int index, CacheEntry<?> value) {
            this.atomCache[index + 1] = value;
        }
    }

    class Silent implements ParseState<S> {
        private final ErrorCollector<S> silentCollector = new ErrorCollector.Nop<>();

        @Override
        public ErrorCollector<S> errorCollector() {
            return this.silentCollector;
        }

        @Override
        public Scope scope() {
            return CachedParseState.this.scope();
        }

        @Nullable
        @Override
        public <T> T parse(NamedRule<S, T> rule) {
            return CachedParseState.this.parse(rule);
        }

        @Override
        public S input() {
            return CachedParseState.this.input();
        }

        @Override
        public int mark() {
            return CachedParseState.this.mark();
        }

        @Override
        public void restore(int cursor) {
            CachedParseState.this.restore(cursor);
        }

        @Override
        public Control acquireControl() {
            return CachedParseState.this.acquireControl();
        }

        @Override
        public void releaseControl() {
            CachedParseState.this.releaseControl();
        }

        @Override
        public ParseState<S> silent() {
            return this;
        }

        @Override
        public void markNull(int mark) {
            CachedParseState.this.markNull(mark);
        }

        @Override
        public boolean isNull(int mark) {
            return CachedParseState.this.isNull(mark);
        }
    }

    static class SimpleControl implements Control {
        private boolean hasCut;

        @Override
        public void cut() {
            this.hasCut = true;
        }

        @Override
        public boolean hasCut() {
            return this.hasCut;
        }

        public void reset() {
            this.hasCut = false;
        }
    }
}
