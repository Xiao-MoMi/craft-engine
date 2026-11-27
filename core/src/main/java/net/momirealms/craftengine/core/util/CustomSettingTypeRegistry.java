package net.momirealms.craftengine.core.util;

public final class CustomSettingTypeRegistry {
    private final String name;
    private int size;
    private volatile int frozenSize = -1;

    public CustomSettingTypeRegistry(String name) {
        this.name = name;
    }

    public synchronized int register() {
        if (this.frozenSize >= 0) {
            throw new IllegalStateException("Custom " + this.name + " setting types must be registered before configuration loading");
        }
        return this.size++;
    }

    public synchronized void freeze() {
        this.frozenSize = this.size;
    }

    public Object[] newDataArray() {
        int size = this.frozenSize;
        if (size < 0) {
            throw new IllegalStateException("Custom " + this.name + " setting types must be frozen before creating settings");
        }
        return new Object[size];
    }
}
