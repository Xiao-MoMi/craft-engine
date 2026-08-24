package net.momirealms.craftengine.core.attribute.sync;

public final class ValueSyncValueProvider implements SyncValueProvider {
    public static final ValueSyncValueProvider INSTANCE = new ValueSyncValueProvider();

    private ValueSyncValueProvider() {
    }

    @Override
    public double resolve(double value, double base) {
        return value;
    }
}
