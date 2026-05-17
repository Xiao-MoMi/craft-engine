package net.momirealms.craftengine.core.util;

public final class MutableInt {
    private int value;

    public MutableInt(int value) {
        this.value = value;
    }

    public int intValue() {
        return value;
    }

    public void set(int value) {
        this.value = value;
    }

    public void add(final int value) {
        this.value += value;
    }
}
