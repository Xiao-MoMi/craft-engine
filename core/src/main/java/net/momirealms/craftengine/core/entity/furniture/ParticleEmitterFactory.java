package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.particle.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Map;
import java.util.function.Supplier;

public class ParticleEmitterFactory implements FurnitureEmitterFactory {

    @Override
    public FurnitureEmitter create(@NotNull Map<String, Object> map) {
        ParticleEmitter.Builder builder = new ParticleEmitter.Builder();
        
        // Position
        builder.position(parsePosition(map));
        
        // Particle type (required)
        if (map.containsKey("particle-type")) {
            String particleTypeStr = map.get("particle-type").toString();
            builder.particleType(Key.of(particleTypeStr));
        } else {
            throw new IllegalArgumentException("particle-type is required for ParticleEmitter");
        }
        
        // Basic position offsets
        builder.x(parseNumberProvider(map, "x", "0"));
        builder.y(parseNumberProvider(map, "y", "0"));
        builder.z(parseNumberProvider(map, "z", "0"));
        
        // Particle properties
        builder.count(parseNumberProvider(map, "count", "1"));
        builder.xOffset(parseNumberProvider(map, "x-offset", "0"));
        builder.yOffset(parseNumberProvider(map, "y-offset", "0"));
        builder.zOffset(parseNumberProvider(map, "z-offset", "0"));
        builder.speed(parseNumberProvider(map, "speed", "0"));
        
        // Timing
        builder.interval(parseNumberProvider(map, "interval", "20")); // Default 1 second
        builder.duration(parseNumberProvider(map, "duration", "-1")); // Default infinite
        
        // Advanced physics properties
        builder.gravityX(parseNumberProvider(map, "gravity-x", "0"));
        builder.gravityY(parseNumberProvider(map, "gravity-y", "-0.05")); // Default downward gravity
        builder.gravityZ(parseNumberProvider(map, "gravity-z", "0"));
        
        // Velocity (initial motion)
        builder.velocityX(parseNumberProvider(map, "velocity-x", "0"));
        builder.velocityY(parseNumberProvider(map, "velocity-y", "0"));
        builder.velocityZ(parseNumberProvider(map, "velocity-z", "0"));
        
        // Acceleration (rate of velocity change)
        builder.accelerationX(parseNumberProvider(map, "acceleration-x", "0"));
        builder.accelerationY(parseNumberProvider(map, "acceleration-y", "0"));
        builder.accelerationZ(parseNumberProvider(map, "acceleration-z", "0"));
        
        // Visual effects
        builder.randomness(parseNumberProvider(map, "randomness", "0"));
        builder.inheritMotion(parseBoolean(map, "inherit-motion", false));
        builder.maxAge(parseNumberProvider(map, "max-age", "-1"));
        builder.fadeInTime(parseNumberProvider(map, "fade-in-time", "0"));
        builder.fadeOutTime(parseNumberProvider(map, "fade-out-time", "0"));
        builder.rotationSpeed(parseNumberProvider(map, "rotation-speed", "0"));
        builder.scaleModifier(parseNumberProvider(map, "scale-modifier", "1"));
        
        // Particle data implementation
        builder.particleData(parseParticleData(map));
        
        return builder.build();
    }
    
    @SuppressWarnings("unchecked")
    private Vector3f parsePosition(Map<String, Object> map) {
        if (map.containsKey("position")) {
            Object posObj = map.get("position");
            if (posObj instanceof Map) {
                Map<String, Object> posMap = (Map<String, Object>) posObj;
                float x = parseFloat(posMap, "x", 0.0f);
                float y = parseFloat(posMap, "y", 0.0f);
                float z = parseFloat(posMap, "z", 0.0f);
                return new Vector3f(x, y, z);
            }
        }
        return new Vector3f(0, 0, 0);
    }
    
    private NumberProvider parseNumberProvider(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.getOrDefault(key, defaultValue);
        return NumberProviders.fromObject(value);
    }
    
    private boolean parseBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                try {
                    return Boolean.parseBoolean(value.toString());
                } catch (Exception e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
    
    private ParticleData parseParticleData(Map<String, Object> map) {
        if (!map.containsKey("particle-data")) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) map.get("particle-data");
        String particleTypeStr = map.get("particle-type").toString();
        Key particleType = Key.of(particleTypeStr);
        
        // Parse different particle data types based on particle type
        return switch (particleType.value()) {
            case "dust" -> {
                String colorStr = (String) dataMap.getOrDefault("color", "255,0,0");
                float size = parseFloat(dataMap, "size", 1.0f);
                yield new DustData(Color.fromString(colorStr.split(",")), size);
            }
            case "dust_color_transition" -> {
                String fromColorStr = (String) dataMap.getOrDefault("from-color", "255,0,0");
                String toColorStr = (String) dataMap.getOrDefault("to-color", "0,255,0");
                float size = parseFloat(dataMap, "size", 1.0f);
                yield new DustTransitionData(
                    Color.fromString(fromColorStr.split(",")), 
                    Color.fromString(toColorStr.split(",")), 
                    size
                );
            }
            case "entity_effect", "tinted_leaves" -> {
                String colorStr = (String) dataMap.getOrDefault("color", "255,255,255");
                yield new ColorData(Color.fromString(colorStr.split(",")));
            }
            case "block", "falling_dust", "dust_pillar", "block_crumble", "block_marker" -> {
                String blockState = (String) dataMap.getOrDefault("block-state", "minecraft:stone");
                yield new BlockStateData(LazyReference.lazyReference(new Supplier<BlockStateWrapper>() {
                    @Override
                    public BlockStateWrapper get() {
                        return CraftEngine.instance().blockManager().createPackedBlockState(blockState);
                    }
                }));
            }
            case "item" -> {
                String itemId = (String) dataMap.getOrDefault("item", "minecraft:apple");
                yield new ItemStackData(LazyReference.lazyReference(new Supplier<Item<?>>() {
                    @Override
                    public Item<?> get() {
                        return CraftEngine.instance().itemManager().createWrappedItem(Key.of(itemId), null);
                    }
                }));
            }
            case "vibration" -> {
                NumberProvider targetX = NumberProviders.fromObject(dataMap.getOrDefault("target-x", "0"));
                NumberProvider targetY = NumberProviders.fromObject(dataMap.getOrDefault("target-y", "0"));
                NumberProvider targetZ = NumberProviders.fromObject(dataMap.getOrDefault("target-z", "0"));
                NumberProvider arrivalTime = NumberProviders.fromObject(dataMap.getOrDefault("arrival-time", "10"));
                yield new VibrationData(targetX, targetY, targetZ, arrivalTime);
            }
            case "trail" -> {
                String colorStr = (String) dataMap.getOrDefault("color", "255,255,255");
                NumberProvider targetX = NumberProviders.fromObject(dataMap.getOrDefault("target-x", "0"));
                NumberProvider targetY = NumberProviders.fromObject(dataMap.getOrDefault("target-y", "0"));
                NumberProvider targetZ = NumberProviders.fromObject(dataMap.getOrDefault("target-z", "0"));
                NumberProvider duration = NumberProviders.fromObject(dataMap.getOrDefault("duration", "10"));
                yield new TrailData(targetX, targetY, targetZ, Color.fromString(colorStr.split(",")), duration);
            }
            case "sculk_charge" -> {
                float charge = parseFloat(dataMap, "charge", 1.0f);
                yield new JavaTypeData(charge);
            }
            case "shriek" -> {
                int shriekDelay = parseInt(dataMap, "delay", 0);
                yield new JavaTypeData(shriekDelay);
            }
            default -> null;
        };
    }
    
    private int parseInt(Map<String, Object> map, String key, int defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
    
    private float parseFloat(Map<String, Object> map, String key, float defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else {
                try {
                    return Float.parseFloat(value.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
}
