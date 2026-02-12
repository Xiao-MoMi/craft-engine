package net.momirealms.craftengine.core.util.random;

public interface RandomSource {

    void setSeed(long seed);

    int nextInt();

    int nextInt(int bound);

    long nextLong();

    boolean nextBoolean();

    float nextFloat();

    double nextDouble();

    double nextGaussian();

    default int nextInt(int origin, int bound) {
        return origin + this.nextInt(bound - origin);
    }
}
