package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 对数正态分布提供器 (Log-Normal Distribution)
 * <p>
 * 适用于描述诸如伤害值、金币掉落、经验获取等右偏分布的数据（大多数值较小，但偶尔有极高值）。
 * <p>
 * 参数说明：
 * - location (μ): 对数变量的均值
 * - scale (σ): 对数变量的标准差
 * - 或者在配置中直接提供 mean (真实均值) 和 std-dev (真实标准差)，工厂类会自动转换。
 */
public record LogNormalNumberProvider(
        double min,
        double max,
        double location,    // μ
        double scale,       // σ
        int maxAttempts
) implements NumberProvider {

    public static final NumberProviderFactory<LogNormalNumberProvider> FACTORY = new Factory();
    private static final double EPSILON = 1e-6; // 防止 log(0) 的极小值

    public LogNormalNumberProvider {
        validateParameters(min, max, scale, maxAttempts);
    }

    private static void validateParameters(double min, double max, double scale, int maxAttempts) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be greater than 0");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("max-attempts must be greater than 0");
        }
        // 对数正态分布定义域为 (0, +∞)，min 必须大于 0
        if (min <= 0) {
            throw new IllegalArgumentException("min must be greater than 0 for log-normal distribution. If you need 0, consider shifting or clamping.");
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
        // 快速路径：如果范围极小，直接返回均值
        if (max - min < EPSILON) {
            return min;
        }

        for (int attempts = 0; attempts < this.maxAttempts; attempts++) {
            // 核心算法：X = exp(μ + σZ), 其中 Z ~ N(0, 1)
            double normalValue = random.nextGaussian() * this.scale + this.location;

            // 性能优化：在进行昂贵的 exp 运算前，先检查指数范围防止 Infinity
            if (normalValue > 700) { // Math.exp(710) > Double.MAX_VALUE
                continue;
            }

            double value = Math.exp(normalValue);

            if (value >= this.min && value <= this.max) {
                return value;
            }
        }

        // 失败回退：返回限制在范围内的真实中位数
        return MiscUtils.clamp(getRealMedian(), this.min, this.max);
    }

    /**
     * 获取真实分布的均值 (Real Mean)
     * E[X] = exp(μ + σ²/2)
     */
    public double getRealMean() {
        return Math.exp(this.location + (this.scale * this.scale) / 2.0);
    }

    /**
     * 获取真实分布的中位数 (Real Median)
     * Median[X] = exp(μ)
     */
    public double getRealMedian() {
        return Math.exp(this.location);
    }

    /**
     * 获取真实分布的众数 (Real Mode)
     * Mode[X] = exp(μ - σ²)
     */
    public double getRealMode() {
        return Math.exp(this.location - this.scale * this.scale);
    }

    /**
     * 获取真实分布的标准差 (Real StdDev)
     * SD[X] = sqrt( (exp(σ²)-1) * exp(2μ+σ²) )
     */
    public double getRealStdDev() {
        double var = (Math.exp(scale * scale) - 1) * Math.exp(2 * location + scale * scale);
        return Math.sqrt(var);
    }

    private static class Factory implements NumberProviderFactory<LogNormalNumberProvider> {

        @Override
        public LogNormalNumberProvider create(Map<String, Object> arguments) {
            double rawMin = ResourceConfigUtils.getAsDouble(
                    ResourceConfigUtils.requireNonNullOrThrow(arguments.get("min"),
                            "warning.config.number.log_normal.missing_min"), "min");

            double max = ResourceConfigUtils.getAsDouble(
                    ResourceConfigUtils.requireNonNullOrThrow(arguments.get("max"),
                            "warning.config.number.log_normal.missing_max"), "max");

            // 自动修正 min <= 0 的情况，防止 Log(0) 崩溃
            // 如果用户配置 min=0，我们将其修正为一个极小的正数
            double min = Math.max(rawMin, EPSILON);

            double location;
            double scale;

            // 优先检查用户是否直接配置了 mean (真实均值) 和 std-dev (真实标准差)
            // 这对用户来说比配置 location/scale 直观得多
            if (arguments.containsKey("mean") && arguments.containsKey("std-dev")) {
                double realMean = ResourceConfigUtils.getAsDouble(arguments.get("mean"), "mean");
                double realStdDev = ResourceConfigUtils.getAsDouble(arguments.get("std-dev"), "std-dev");

                // 将真实均值/方差转换为对数正态分布参数 μ 和 σ
                // μ = ln(mean^2 / sqrt(mean^2 + var))
                // σ = sqrt(ln(1 + var/mean^2))
                double meanSq = realMean * realMean;
                double var = realStdDev * realStdDev;

                scale = Math.sqrt(Math.log(1 + (var / meanSq)));
                location = Math.log(meanSq / Math.sqrt(meanSq + var));
            } else {
                // 回退到使用 location/scale 或根据 min/max 估算

                // 默认策略：假设 min 和 max 覆盖了大约 +/- 3个标准差的范围 (对数域)
                // log(min) ≈ μ - 3σ
                // log(max) ≈ μ + 3σ
                double logMin = Math.log(min);
                double logMax = Math.log(max);

                double defaultLocation = (logMin + logMax) / 2.0;
                double defaultScale = (logMax - logMin) / 6.0;

                location = ResourceConfigUtils.getAsDouble(
                        arguments.getOrDefault("location", defaultLocation), "location");
                scale = ResourceConfigUtils.getAsDouble(
                        arguments.getOrDefault("scale", defaultScale), "scale");
            }

            int maxAttempts = ResourceConfigUtils.getAsInt(
                    arguments.getOrDefault("max-attempts", 128), "max-attempts");

            return new LogNormalNumberProvider(min, max, location, scale, maxAttempts);
        }
    }

    @Override
    public @NotNull String toString() {
        return String.format(
                "LogNormalNumberProvider{range=[%.2f, %.2f], location(μ)=%.2f, scale(σ)=%.2f, realMean≈%.2f, realStdDev≈%.2f}",
                this.min, this.max, this.location, this.scale, getRealMean(), getRealStdDev()
        );
    }
}