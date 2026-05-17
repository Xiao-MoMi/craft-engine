package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.random.RandomSource;

/**
 * 三角形分布提供器
 * 一种连续概率分布，其概率密度函数图像呈三角形
 * 相比正态分布，它计算开销极低且天生有界
 */
public record TriangleNumberProvider(
    double min,
    double max,
    double mode
) implements NumberProvider {

    public static final NumberProviderFactory<TriangleNumberProvider> FACTORY = new Factory();

    @Override
    public int getInt(RandomSource random) {
        return (int) Math.round(getDouble(random));
    }

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        double u = random.nextDouble();
        
        // 逆变换采样法 (Inverse Transform Sampling)
        // 概率转折点：F(mode) = (mode - min) / (max - min)
        double fc = (this.mode - this.min) / (this.max - this.min);

        if (u < fc) {
            // 左半部分三角形
            return this.min + Math.sqrt(u * (this.max - this.min) * (this.mode - this.min));
        } else {
            // 右半部分三角形
            return this.max - Math.sqrt((1 - u) * (this.max - this.min) * (this.max - this.mode));
        }
    }

    private static class Factory implements NumberProviderFactory<TriangleNumberProvider> {

        @Override
        public TriangleNumberProvider create(ConfigSection section) {
            double min = section.getNonNullDouble("min");
            double max = section.getNonNullDouble("max");
            
            // 默认众数在正中间（等腰三角形）
            double defaultMode = (min + max) / 2.0;
            double mode = section.getDouble("mode", defaultMode);

            this.validateParameters(section.path(), min, max, mode);
            return new TriangleNumberProvider(min, max, mode);
        }

        private void validateParameters(String path, double min, double max, double mode) {
            if (min >= max) {
                throw new KnownResourceException("number.less_than", path, "min", "max");
            }
            if (mode < min) {
                throw new KnownResourceException("number.greater_than", path, "mode", "min");
            }
            if (mode > max) {
                throw new KnownResourceException("number.less_than", path, "mode", "max");
            }
        }
    }
}