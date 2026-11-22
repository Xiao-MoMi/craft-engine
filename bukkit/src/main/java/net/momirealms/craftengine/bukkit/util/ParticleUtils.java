package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.particle.*;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.core.particles.*;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundLevelParticlesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.gameevent.BlockPositionSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class ParticleUtils {
    private ParticleUtils() {}

    @Nullable
    public static Object getParticleType(Key key) {
        return RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.PARTICLE_TYPE, KeyUtils.toIdentifier(key));
    }

    @Nullable
    public static Object createParticlePacket(ParticleConfig particle, double x, double y, double z) {
        Context context = SimpleContext.EMPTY;
        x += particle.x.getDouble(context);
        y += particle.y.getDouble(context);
        z += particle.z.getDouble(context);
        Object options = createParticleOptions(particle.particleType.platformParticle(), particle.particleData, context, x, y, z);
        if (options == null) {
            return null;
        }
        return createParticlePacket(
                options,
                false,
                x,
                y,
                z,
                particle.count.getInt(context),
                particle.xOffset.getFloat(context),
                particle.yOffset.getFloat(context),
                particle.zOffset.getFloat(context),
                particle.speed.getFloat(context)
        );
    }

    public static Object createParticlePacket(Object options,
                                              boolean overrideLimiter,
                                              double x,
                                              double y,
                                              double z,
                                              int count,
                                              float dx,
                                              float dy,
                                              float dz,
                                              float speed) {
        if (VersionHelper.isOrAbove1_21_4) {
            return ClientboundLevelParticlesPacketProxy.INSTANCE.newInstance$1(options, overrideLimiter, false, x, y, z, dx, dy, dz, speed, count);
        }
        return ClientboundLevelParticlesPacketProxy.INSTANCE.newInstance$0(options, overrideLimiter, x, y, z, dx, dy, dz, speed, count);
    }

    public static void sendParticles(Object level, ParticleConfig particle, double x, double y, double z) {
        Context context = SimpleContext.EMPTY;
        x += particle.x.getDouble(context);
        y += particle.y.getDouble(context);
        z += particle.z.getDouble(context);
        Object options = createParticleOptions(particle.particleType.platformParticle(), particle.particleData, context, x, y, z);
        if (options != null) {
            ServerLevelProxy.INSTANCE.sendParticles(
                    level,
                    options,
                    x,
                    y,
                    z,
                    particle.count.getInt(context),
                    particle.xOffset.getDouble(context),
                    particle.yOffset.getDouble(context),
                    particle.zOffset.getDouble(context),
                    particle.speed.getDouble(context)
            );
        }
    }

    public static void sendStepParticles(Object level, Object entity, Object blockPos, ParticleConfig particle) {
        double x = EntityProxy.INSTANCE.getX(entity);
        double y = EntityProxy.INSTANCE.getY(entity);
        double z = EntityProxy.INSTANCE.getZ(entity);
        if (!particle.directionalStep()) {
            sendParticles(level, particle, x, y, z);
            return;
        }

        double movementX = x - EntityProxy.INSTANCE.getXo(entity);
        double movementZ = z - EntityProxy.INSTANCE.getZo(entity);
        if (movementX * movementX + movementZ * movementZ < 1.0E-8) {
            Object movement = EntityProxy.INSTANCE.getDeltaMovement(entity);
            movementX = Vec3Proxy.INSTANCE.getX(movement);
            movementZ = Vec3Proxy.INSTANCE.getZ(movement);
        }

        double halfWidth = EntityProxy.INSTANCE.getBbWidth(entity) * 0.5;
        double particleX = x + RandomUtils.generateRandomDouble(-halfWidth, halfWidth);
        double particleZ = z + RandomUtils.generateRandomDouble(-halfWidth, halfWidth);
        int blockX = Vec3iProxy.INSTANCE.getX(blockPos);
        int blockZ = Vec3iProxy.INSTANCE.getZ(blockPos);
        if (MiscUtils.floor(x) != blockX) {
            particleX = MiscUtils.clamp(particleX, blockX, blockX + 1.0);
        }
        if (MiscUtils.floor(z) != blockZ) {
            particleZ = MiscUtils.clamp(particleZ, blockZ, blockZ + 1.0);
        }
        Context context = SimpleContext.EMPTY;
        double particleY = y + particle.y.getDouble(context);
        Object options = createParticleOptions(particle.particleType.platformParticle(), particle.particleData, context,
                particleX, particleY, particleZ);
        if (options == null) {
            return;
        }

        double vx = -4.0 * movementX;
        double vy = 1.5;
        double vz = -4.0 * movementZ;
        if (!ParticleTypes.BLOCK.equals(particle.particleType.type())) {
            // Terrain particles normalize this vector on the client, while item particles add it directly.
            double scale = RandomUtils.triangle(0.12, 0.06) / Math.sqrt(vx * vx + vy * vy + vz * vz);
            vx *= scale;
            vy = vy * scale + (particle.particleData instanceof ItemStackData ? 0.09 : 0.1);
            vz *= scale;
        }
        ServerLevelProxy.INSTANCE.sendParticles(level, options, particleX, particleY, particleZ, 0, vx, vy, vz, 1);
    }

    public static void broadcastParticles(Object level,
                                          ParticleType particle,
                                          double x,
                                          double y,
                                          double z,
                                          int count,
                                          double dx,
                                          double dy,
                                          double dz,
                                          double speed,
                                          @Nullable ParticleData data,
                                          Context context) {
        Object options = createParticleOptions(particle.platformParticle(), data, context, x, y, z);
        if (options != null) {
            broadcastParticles(level, options, x, y, z, count, dx, dy, dz, speed);
        }
    }

    public static void broadcastParticles(Object level,
                                          Object options,
                                          double x,
                                          double y,
                                          double z,
                                          int count,
                                          double dx,
                                          double dy,
                                          double dz,
                                          double speed) {
        if (!VersionHelper.hasFoliaPatch) {
            ServerLevelProxy.INSTANCE.sendParticles(level, options, x, y, z, count, dx, dy, dz, speed);
            return;
        }
        // Folia's standard broadcast only visits the current region's players. World effects can reach across regions.
        Object packet = createParticlePacket(options, false, x, y, z, count, (float) dx, (float) dy, (float) dz, (float) speed);
        for (Object player : ServerLevelProxy.INSTANCE.players(level)) {
            ServerLevelProxy.INSTANCE.sendParticlesToPlayer(level, player, false, x, y, z, packet);
        }
    }

    @Nullable
    private static Object createParticleOptions(Object type, @Nullable ParticleData data, Context context, double x, double y, double z) {
        return switch (data) {
            case BlockStateData block -> BlockParticleOptionProxy.INSTANCE.newInstance(type, block.blockState().minecraftState());
            case ItemStackData item -> createItemParticleOptions(type, item.item());
            case ColorData color -> VersionHelper.isOrAbove1_20_5
                    ? ColorParticleOptionProxy.INSTANCE.create(type, color.color().color())
                    : simpleParticleOptions(type);
            case DustData dust -> VersionHelper.isOrAbove1_21_2
                    ? DustParticleOptionsProxy.INSTANCE.newInstance(Color.transparent(dust.color().color()), dust.size())
                    : DustParticleOptionsProxy.INSTANCE.newInstance(toColorVector(dust.color()), dust.size());
            case DustTransitionData dust -> VersionHelper.isOrAbove1_21_2
                    ? DustColorTransitionOptionsProxy.INSTANCE.newInstance(Color.transparent(dust.from().color()), Color.transparent(dust.to().color()), dust.size())
                    : DustColorTransitionOptionsProxy.INSTANCE.newInstance(toColorVector(dust.from()), toColorVector(dust.to()), dust.size());
            case SculkChargeData sculk -> SculkChargeParticleOptionsProxy.INSTANCE.newInstance(sculk.roll());
            case ShriekData shriek -> ShriekParticleOptionProxy.INSTANCE.newInstance(shriek.delay());
            case VibrationData vibration -> {
                Object destination = BlockPositionSourceProxy.INSTANCE.newInstance(LocationUtils.toBlockPos(
                        MiscUtils.floor(x + vibration.destinationX().getDouble(context)),
                        MiscUtils.floor(y + vibration.destinationY().getDouble(context)),
                        MiscUtils.floor(z + vibration.destinationZ().getDouble(context))));
                yield VibrationParticleOptionProxy.INSTANCE.newInstance(destination, vibration.arrivalTime().getInt(context));
            }
            case TrailData trail -> {
                if (!VersionHelper.isOrAbove1_21_4) {
                    yield null;
                }
                Object target = Vec3Proxy.INSTANCE.newInstance(x + trail.targetX().getDouble(context), y + trail.targetY().getDouble(context), z + trail.targetZ().getDouble(context));
                yield TrailParticleOptionProxy.INSTANCE.newInstance(target, Color.transparent(trail.color().color()), trail.duration().getInt(context));
            }
            case SpellParticleData spell -> VersionHelper.isOrAbove1_21_9
                    ? SpellParticleOptionProxy.INSTANCE.create(type, spell.color().color(), spell.power())
                    : simpleParticleOptions(type);
            case GeyserData geyser -> VersionHelper.isOrAbove26_2
                    ? GeyserParticleOptionsProxy.INSTANCE.newInstance(type, geyser.waterBlocks())
                    : null;
            case GeyserBaseData geyser -> VersionHelper.isOrAbove26_2
                    ? GeyserBaseParticleOptionsProxy.INSTANCE.newInstance(type, geyser.waterBlocks(), geyser.burstImpulseBase())
                    : null;
            case null, default -> simpleParticleOptions(type);
        };
    }

    @Nullable
    private static Object createItemParticleOptions(Object type, @Nullable Item item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        Object stack = item.minecraftItem();
        if (VersionHelper.isOrAbove26_1) {
            return ItemParticleOptionProxy.INSTANCE.newInstance$1(type, ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(stack));
        }
        return ItemParticleOptionProxy.INSTANCE.newInstance$0(type, stack);
    }

    @Nullable
    private static Object simpleParticleOptions(Object type) {
        return ParticleOptionsProxy.CLASS.isInstance(type) ? type : null;
    }

    private static Vector3f toColorVector(Color color) {
        return new Vector3f(color.r() / 255f, color.g() / 255f, color.b() / 255f);
    }
}
