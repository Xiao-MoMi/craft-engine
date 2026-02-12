package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 权重随机提供器
 * 根据配置的权重比例随机选择一个数值
 */
public final class WeightedNumberProvider implements NumberProvider {
    public static final NumberProviderFactory<WeightedNumberProvider> FACTORY = new Factory();

    // 使用 TreeMap 存储前缀和，便于使用 higherEntry 进行二分查找
    private final NavigableMap<Double, Double> weightMap = new TreeMap<>();
    private final double totalWeight;

    public WeightedNumberProvider(Map<Double, Double> inputWeights) {
        double sum = 0;
        for (Map.Entry<Double, Double> entry : inputWeights.entrySet()) {
            double value = entry.getKey();
            double weight = entry.getValue();
            if (weight > 0) {
                sum += weight;
                // 存储累计权重 -> 目标值
                this.weightMap.put(sum, value);
            }
        }
        this.totalWeight = sum;
        
        if (this.weightMap.isEmpty()) {
            throw new IllegalArgumentException("Weighted provider must have at least one positive weight entry");
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
        // 生成 [0, totalWeight) 之间的随机数
        double randomValue = random.nextDouble() * totalWeight;
        
        // 查找第一个累计权重值大于 randomValue 的条目 (二分查找，O(log N))
        Map.Entry<Double, Double> entry = weightMap.higherEntry(randomValue);
        
        if (entry == null) {
            return weightMap.lastEntry().getValue();
        }
        return entry.getValue();
    }

    private static class Factory implements NumberProviderFactory<WeightedNumberProvider> {
        @Override
        public WeightedNumberProvider create(Map<String, Object> arguments) {
            // 期望配置格式: 
            // weights:
            //   "1.0": 50
            //   "2.0": 30
            //   "5.0": 20
            Map<String, Object> weightsObj = ResourceConfigUtils.getAsMap(arguments.get("weights"), "weights");
            Map<Double, Double> processedWeights = new HashMap<>();
            for (Map.Entry<String, Object> entry : weightsObj.entrySet()) {
                double value = Double.parseDouble(entry.getKey());
                double weight = Double.parseDouble(String.valueOf(entry.getValue()));
                processedWeights.put(value, weight);
            }
            return new WeightedNumberProvider(processedWeights);
        }
    }

    @Override
    public @NotNull String toString() {
        return "WeightedNumberProvider{entries=" + this.weightMap.size() + ", totalWeight=" + this.totalWeight + "}";
    }
}