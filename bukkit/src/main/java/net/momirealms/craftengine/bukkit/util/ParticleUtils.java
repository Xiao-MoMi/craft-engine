package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.particle.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ParticleUtils {
    private static final Map<Key, Particle> CACHE = new HashMap<>();

    private ParticleUtils() {}

    public static Particle getParticle(String particle) {
        try {
            return Particle.valueOf(particle);
        } catch (IllegalArgumentException e) {
            return switch (particle) {
                case "REDSTONE" -> Particle.valueOf("DUST");
                case "VILLAGER_HAPPY", "HAPPY_VILLAGER" -> Particle.valueOf(VersionHelper.isOrAbove1_20_5() ? "HAPPY_VILLAGER" : "VILLAGER_HAPPY");
                case "BUBBLE", "WATER_BUBBLE" -> Particle.valueOf(VersionHelper.isOrAbove1_20_5() ? "BUBBLE" : "WATER_BUBBLE");
                default -> Particle.valueOf(particle);
            };
        }
    }

    @Nullable
    public static Particle getParticle(Key particle) {
        return CACHE.computeIfAbsent(particle, k -> {
            Object nmsParticle = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.PARTICLE_TYPE, KeyUtils.toResourceLocation(particle));
            if (nmsParticle == null) return null;
            return FastNMS.INSTANCE.method$CraftParticle$toBukkit(nmsParticle);
        });
    }

    public static final Particle HAPPY_VILLAGER = getParticle("HAPPY_VILLAGER");
    public static final Particle BUBBLE = getParticle("BUBBLE");

    public static Object toBukkitParticleData(ParticleData particleData, Context context, World world, double x, double y, double z) {
        return switch (particleData) {
            case BlockStateData data -> BlockStateUtils.fromBlockData(data.blockState().handle());
            case ColorData data -> ColorUtils.toBukkit(data.color());
            case DustData data -> new Particle.DustOptions(ColorUtils.toBukkit(data.color()), data.size());
            case DustTransitionData data -> new Particle.DustTransition(ColorUtils.toBukkit(data.from()), ColorUtils.toBukkit(data.to()), data.size());
            case ItemStackData data -> data.item().getItem();
            case JavaTypeData data -> data.data();
            case VibrationData data -> new Vibration(new Vibration.Destination.BlockDestination(new Location(world, x + data.destinationX().getDouble(context), y + data.destinationY().getDouble(context), y + data.destinationZ().getDouble(context))), data.arrivalTime().getInt(context));
            case TrailData data -> new Particle.Trail(new Location(world, x + data.targetX().getDouble(context), y + data.targetZ().getDouble(context), z + data.targetZ().getDouble(context)), ColorUtils.toBukkit(data.color()), data.duration().getInt(context));
            default -> null;
        };
    }
}
