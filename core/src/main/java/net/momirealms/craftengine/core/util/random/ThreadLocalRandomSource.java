package net.momirealms.craftengine.core.util.random;

import java.util.concurrent.ThreadLocalRandom;

public final class ThreadLocalRandomSource implements RandomSource {
    public static final ThreadLocalRandomSource INSTANCE = new ThreadLocalRandomSource();

    private ThreadLocalRandomSource() {}

    @Override
    public void setSeed(long seed) {
        ThreadLocalRandom.current().setSeed(seed);
    }

    @Override
    public int nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public long nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    @Override
    public float nextFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public double nextGaussian() {
        return ThreadLocalRandom.current().nextGaussian();
    }
}
