package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.random.RandomSource;
import net.momirealms.craftengine.core.util.random.ThreadLocalRandomSource;

public interface NumberProvider {

    default float getFloat(Context context) {
        return getFloat(ThreadLocalRandomSource.INSTANCE);
    }

    float getFloat(RandomSource random);

    default double getDouble(Context context) {
        return getDouble(ThreadLocalRandomSource.INSTANCE);
    }

    double getDouble(RandomSource random);

    default int getInt(Context context) {
        return Math.round(this.getFloat(context));
    }

    default int getInt(RandomSource random) {
        return Math.round(this.getFloat(random));
    }
}
