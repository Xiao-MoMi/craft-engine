package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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

    public TriangleNumberProvider {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        if (mode < min || mode > max) {
            throw new IllegalArgumentException("mode must be between min and max");
        }
    }

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
        public TriangleNumberProvider create(Map<String, Object> arguments) {
            double min = ResourceConfigUtils.getAsDouble(
                ResourceConfigUtils.requireNonNullOrThrow(arguments.get("min"), 
                "warning.config.number.triangle.missing_min"), "min");
            
            double max = ResourceConfigUtils.getAsDouble(
                ResourceConfigUtils.requireNonNullOrThrow(arguments.get("max"), 
                "warning.config.number.triangle.missing_max"), "max");
            
            // 默认众数在正中间（等腰三角形）
            double defaultMode = (min + max) / 2.0;
            double mode = ResourceConfigUtils.getAsDouble(
                arguments.getOrDefault("mode", defaultMode), "mode");
            
            return new TriangleNumberProvider(min, max, mode);
        }
    }

    @Override
    public @NotNull String toString() {
        return String.format("TriangleNumberProvider{min=%.2f, max=%.2f, mode=%.2f}", this.min, this.max, this.mode);
    }
}