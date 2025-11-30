package net.momirealms.craftengine.core.util.snbt.parse;

import com.google.common.annotations.VisibleForTesting;
import net.momirealms.craftengine.core.util.MiscUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class Scope {
    private static final int NOT_FOUND = -1;
    private static final Object FRAME_START_MARKER = new Object() {
        @Override
        public String toString() {
            return "frame";
        }
    };
    private static final int ENTRY_STRIDE = 2;
    private Object[] stack = new Object[128];
    private int topEntryKeyIndex = 0;
    private int topMarkerKeyIndex = 0;
    private int depth;

    public Scope() {
        this.stack[0] = FRAME_START_MARKER;
        this.stack[1] = null;
    }

    private int valueIndex(Atom<?> name) {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= ENTRY_STRIDE) {
            Object object = this.stack[i];

            assert object instanceof Atom;

            if (object == name) {
                return i + 1;
            }
        }

        return NOT_FOUND;
    }

    public int valueIndexForAny(Atom<?>... names) {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= ENTRY_STRIDE) {
            Object object = this.stack[i];

            assert object instanceof Atom;

            for (Atom<?> atom : names) {
                if (atom == object) {
                    return i + 1;
                }
            }
        }

        return NOT_FOUND;
    }

    private void ensureCapacity(int requiredCapacitty) {
        int i = this.stack.length;
        int i1 = this.topEntryKeyIndex + 1;
        int i2 = i1 + requiredCapacitty * 2;
        if (i2 >= i) {
            int i3 = MiscUtils.growByHalf(i, i2 + 1);
            Object[] objects = new Object[i3];
            System.arraycopy(this.stack, 0, objects, 0, i);
            this.stack = objects;
        }

        assert this.validateStructure();
    }

    private void setupNewFrame() {
        this.topEntryKeyIndex += ENTRY_STRIDE;
        this.stack[this.topEntryKeyIndex] = FRAME_START_MARKER;
        this.stack[this.topEntryKeyIndex + 1] = this.topMarkerKeyIndex;
        this.topMarkerKeyIndex = this.topEntryKeyIndex;
    }

    public void pushFrame() {
        this.ensureCapacity(1);
        this.setupNewFrame();

        assert this.validateStructure();
    }

    private int getPreviousMarkerIndex(int markerIndex) {
        return (Integer)this.stack[markerIndex + 1];
    }

    public void popFrame() {
        assert this.topMarkerKeyIndex != 0;

        this.topEntryKeyIndex = this.topMarkerKeyIndex - ENTRY_STRIDE;
        this.topMarkerKeyIndex = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);

        assert this.validateStructure();
    }

    public void splitFrame() {
        int i = this.topMarkerKeyIndex;
        int i1 = (this.topEntryKeyIndex - this.topMarkerKeyIndex) / ENTRY_STRIDE;
        this.ensureCapacity(i1 + 1);
        this.setupNewFrame();
        int i2 = i + ENTRY_STRIDE;
        int i3 = this.topEntryKeyIndex;

        for (int i4 = 0; i4 < i1; i4++) {
            i3 += ENTRY_STRIDE;
            Object object = this.stack[i2];

            assert object != null;

            this.stack[i3] = object;
            this.stack[i3 + 1] = null;
            i2 += ENTRY_STRIDE;
        }

        this.topEntryKeyIndex = i3;

        assert this.validateStructure();
    }

    public void clearFrameValues() {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= ENTRY_STRIDE) {
            assert this.stack[i] instanceof Atom;

            this.stack[i + 1] = null;
        }

        assert this.validateStructure();
    }

    public void mergeFrame() {
        int previousMarkerIndex = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);
        int i = previousMarkerIndex;
        int i1 = this.topMarkerKeyIndex;

        while (i1 < this.topEntryKeyIndex) {
            i += ENTRY_STRIDE;
            i1 += ENTRY_STRIDE;
            Object object = this.stack[i1];

            assert object instanceof Atom;

            Object object1 = this.stack[i1 + 1];
            Object object2 = this.stack[i];
            if (object2 != object) {
                this.stack[i] = object;
                this.stack[i + 1] = object1;
            } else if (object1 != null) {
                this.stack[i + 1] = object1;
            }
        }

        this.topEntryKeyIndex = i;
        this.topMarkerKeyIndex = previousMarkerIndex;

        assert this.validateStructure();
    }

    public <T> void put(Atom<T> atom, @Nullable T value) {
        int i = this.valueIndex(atom);
        if (i != NOT_FOUND) {
            this.stack[i] = value;
        } else {
            this.ensureCapacity(1);
            this.topEntryKeyIndex += ENTRY_STRIDE;
            this.stack[this.topEntryKeyIndex] = atom;
            this.stack[this.topEntryKeyIndex + 1] = value;
        }

        assert this.validateStructure();
    }

    @Nullable
    public <T> T get(Atom<T> atom) {
        int i = this.valueIndex(atom);
        return (T)(i != NOT_FOUND ? this.stack[i] : null);
    }

    public <T> T getOrThrow(Atom<T> atom) {
        int i = this.valueIndex(atom);
        if (i == NOT_FOUND) {
            throw new IllegalArgumentException("No value for atom " + atom);
        } else {
            return (T)this.stack[i];
        }
    }

    public <T> T getOrDefault(Atom<T> atom, T defaultValue) {
        int i = this.valueIndex(atom);
        return (T)(i != NOT_FOUND ? this.stack[i] : defaultValue);
    }

    @Nullable
    @SafeVarargs
    public final <T> T getAny(Atom<? extends T>... atoms) {
        int i = this.valueIndexForAny(atoms);
        return (T)(i != NOT_FOUND ? this.stack[i] : null);
    }

    @SafeVarargs
    public final <T> T getAnyOrThrow(Atom<? extends T>... atoms) {
        int i = this.valueIndexForAny(atoms);
        if (i == NOT_FOUND) {
            throw new IllegalArgumentException("No value for atoms " + Arrays.toString(atoms));
        } else {
            return (T)this.stack[i];
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean flag = true;

        for (int i = 0; i <= this.topEntryKeyIndex; i += ENTRY_STRIDE) {
            Object object = this.stack[i];
            Object object1 = this.stack[i + 1];
            if (object == FRAME_START_MARKER) {
                stringBuilder.append('|');
                flag = true;
            } else {
                if (!flag) {
                    stringBuilder.append(',');
                }

                flag = false;
                stringBuilder.append(object).append(':').append(object1);
            }
        }

        return stringBuilder.toString();
    }

    @VisibleForTesting
    public Map<Atom<?>, ?> lastFrame() {
        HashMap<Atom<?>, Object> map = new HashMap<>();

        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= ENTRY_STRIDE) {
            Object object = this.stack[i];
            Object object1 = this.stack[i + 1];
            map.put((Atom<?>)object, object1);
        }

        return map;
    }

    public boolean hasOnlySingleFrame() {
        for (int i = this.topEntryKeyIndex; i > 0; i--) {
            if (this.stack[i] == FRAME_START_MARKER) {
                return false;
            }
        }

        if (this.stack[0] != FRAME_START_MARKER) {
            throw new IllegalStateException("Corrupted stack");
        } else {
            return true;
        }
    }

    private boolean validateStructure() {
        assert this.topMarkerKeyIndex >= 0;

        assert this.topEntryKeyIndex >= this.topMarkerKeyIndex;

        for (int i = 0; i <= this.topEntryKeyIndex; i += ENTRY_STRIDE) {
            Object object = this.stack[i];
            if (object != FRAME_START_MARKER && !(object instanceof Atom)) {
                return false;
            }
        }

        for (int ix = this.topMarkerKeyIndex; ix != 0; ix = this.getPreviousMarkerIndex(ix)) {
            Object object = this.stack[ix];
            if (object != FRAME_START_MARKER) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <S> Term<S> increaseDepth() {
        class IncreasingDepthTerm<W> implements Term<W> {
            public static final IncreasingDepthTerm INSTANCE = new IncreasingDepthTerm();
            @Override
            public boolean parse(final ParseState<W> parseState, final Scope scope, final Control control) {
                if (++scope.depth > 512) {
                    parseState.errorCollector().store(parseState.mark(), new IllegalStateException("Too deep"));
                    return false;
                }
                return true;
            }
        }
        return (Term<S>) IncreasingDepthTerm.INSTANCE;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static <S> Term<S> decreaseDepth() {
        class DecreasingDepthTerm<W> implements Term<W> {
            public static final DecreasingDepthTerm INSTANCE = new DecreasingDepthTerm();
            @Override
            public boolean parse(final ParseState<W> parseState, final Scope scope, final Control control) {
                scope.depth--;
                return true;
            }
        }
        return (Term<S>) DecreasingDepthTerm.INSTANCE;
    }
}
