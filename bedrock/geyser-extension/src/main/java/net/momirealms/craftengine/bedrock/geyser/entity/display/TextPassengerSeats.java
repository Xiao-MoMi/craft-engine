package net.momirealms.craftengine.bedrock.geyser.entity.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.geyser.proxy.CamelSeatProxy;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.MinecartEntity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class TextPassengerSeats {
    private static final float PLAYER_PASSENGER_Y_OFFSET = 0.15f;
    private static final float BEDROCK_SNEAK_ROOT_DROP = 0.2f;
    private final Map<EntityType, Vector3f[]> adults;
    private final Map<EntityType, Vector3f[]> babies;

    private TextPassengerSeats(Map<EntityType, Vector3f[]> adults, Map<EntityType, Vector3f[]> babies) {
        this.adults = adults;
        this.babies = babies;
    }

    public static TextPassengerSeats load() {
        try (InputStream stream = TextPassengerSeats.class.getResourceAsStream("/passenger_seats.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing passenger_seats.json");
            }
            JsonObject data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return new TextPassengerSeats(read(data.getAsJsonObject("default")), read(data.getAsJsonObject("baby")));
        } catch (IOException exception) {
            throw new IllegalStateException("Reading passenger_seats.json", exception);
        }
    }

    private static Map<EntityType, Vector3f[]> read(JsonObject table) {
        Map<EntityType, Vector3f[]> entries = new EnumMap<>(EntityType.class);
        for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
            EntityType type = EntityType.valueOf(entry.getKey().substring("minecraft:".length()).toUpperCase(Locale.ROOT));
            JsonArray points = entry.getValue().getAsJsonArray();
            Vector3f[] seats = new Vector3f[points.size()];
            for (int i = 0; i < seats.length; i++) {
                JsonArray point = points.get(i).getAsJsonArray();
                seats[i] = Vector3f.from(point.get(0).getAsFloat(), point.get(1).getAsFloat(), point.get(2).getAsFloat());
            }
            entries.put(type, seats);
        }
        return entries;
    }

    Vector3f resolve(Entity mount, Entity passenger, Vector3f original, float animation, Vector3f previous) {
        if (mount instanceof PlayerEntity player) {
            // 将从脚底量起的头顶高度换算成基岩玩家根节点上的座位偏移。
            Float scale = player.getMetadata().value(EntityDataTypes.SCALE);
            float rootHeight = player.getOffset() - (player.getFlag(EntityFlag.SNEAKING) ? BEDROCK_SNEAK_ROOT_DROP : 0);
            float height = player.getBoundingBoxHeight() * (scale == null ? 1f : scale) + PLAYER_PASSENGER_Y_OFFSET - rootHeight;
            return seatPosition(previous, 0, height, 0);
        }
        if (mount == null) {
            return original;
        }
        EntityType type = mount.getEntityType();
        boolean baby = mount.getFlag(EntityFlag.BABY);
        Vector3f[] young = baby ? this.babies.get(type) : null;
        Vector3f[] points = young == null ? this.adults.get(type) : young;
        if (points == null) {
            return original;
        }
        int seat = mount.getPassengers().indexOf(passenger);
        if (seat < 0) {
            return original;
        }
        if (mount instanceof BoatEntity boat) {
            return boatSeat(boat, seat, points[0].getY(), previous);
        }
        Float combinedScale = mount instanceof LivingEntity ? mount.getMetadata().value(EntityDataTypes.SCALE) : null;
        float attributeScale = combinedScale == null || mount.getScale() == 0 ? 1 : combinedScale / mount.getScale();
        if (mount instanceof CamelEntity camel) {
            return camelSeat(camel, seat, baby, points[0].getY(), attributeScale, previous);
        }
        Vector3f point = points[Math.min(seat, points.length - 1)];
        float scale = baby && young == null ? attributeScale * 0.5f : attributeScale;
        float origin = mount instanceof MinecartEntity ? mount.getJavaDefinition().height() * 0.5f : mount.getOffset();
        if (mount instanceof ArmorStandEntity stand) {
            float height = stand.isMarker() ? 0 : this.adults.get(type)[0].getY() * (stand.isSmall() ? 0.5f : 1f);
            return seatPosition(previous, 0, height * attributeScale - stand.getYOffset(), 0);
        }
        switch (type) {
            case PHANTOM -> scale *= mount.getScale();
            case SLIME, MAGMA_CUBE -> {
                int size = Math.max(1, Math.round(mount.getScale() - 0.1f));
                return seatPosition(previous, 0, (point.getY() - 0.015625f) * size * attributeScale - origin, 0);
            }
            case PUFFERFISH -> {
                Byte state = mount.getMetadata().value(EntityDataTypes.PUFFED_STATE);
                scale *= state == null || state == 0 ? 0.5f : (state == 1 ? 0.7f : 1f);
            }
            case WARDEN -> {
                if (mount.getFlag(EntityFlag.DIGGING) || mount.getFlag(EntityFlag.EMERGING)) {
                    return seatPosition(previous, 0, 1f - origin, 0);
                }
            }
            case SNIFFER -> {
                if (mount.getFlag(EntityFlag.DIGGING)) {
                    return seatPosition(previous, 0, (1.75f - 0.4f) * scale - origin, 0);
                }
            }
            default -> {
            }
        }
        if (animation != 0) {
            switch (type) {
                case HORSE, DONKEY, MULE, ZOMBIE_HORSE, SKELETON_HORSE -> {
                    float raise = animation * attributeScale * (baby ? 0.5f : 1f);
                    return seatPosition(previous, point.getX() * scale, point.getY() * scale + 0.15f * raise - origin, point.getZ() * scale - 0.7f * raise);
                }
                case POLAR_BEAR, SHULKER -> {
                    return seatPosition(previous, point.getX() * scale, point.getY() * scale * (1 + animation) - origin, point.getZ() * scale);
                }
                default -> {
                }
            }
        }
        if (origin == 0 && scale == 1) {
            return point;
        }
        return seatPosition(previous, point.getX() * scale, point.getY() * scale - origin, point.getZ() * scale);
    }

    private static Vector3f seatPosition(Vector3f previous, float x, float y, float z) {
        if (previous != null && Float.compare(previous.getX(), x) == 0 && Float.compare(previous.getY(), y) == 0 && Float.compare(previous.getZ(), z) == 0) {
            return previous;
        }
        return Vector3f.from(x, y, z);
    }

    private static Vector3f boatSeat(BoatEntity boat, int seat, float javaHeight, Vector3f previous) {
        float ridingHeight = boat.getVariant() == BoatEntity.BoatVariant.BAMBOO ? javaHeight * (8f / 9f) : javaHeight / 3f;
        float sideways = boat.getPassengers().size() > 1 ? (seat == 0 ? 0.2f : -0.6f) : (boat instanceof ChestBoatEntity ? 0.15f : 0);
        return seatPosition(previous, sideways, ridingHeight - boat.getJavaDefinition().height() * 0.5f, 0);
    }

    private static Vector3f camelSeat(CamelEntity camel, int seat, boolean baby, float standingHeight, float attributeScale, Vector3f previous) {
        long poseTick = CamelSeatProxy.INSTANCE.lastPoseTick(camel.getVehicleComponent());
        long elapsed = camel.getSession().getGameTicks() - Math.abs(poseTick);
        boolean sitting = poseTick < 0;
        float height = camel.getFlag(EntityFlag.SITTING) ? (baby ? 0.425f : standingHeight - 1.43f) : standingHeight;
        float scale = attributeScale * (baby ? 0.6f : 1);
        float y = camelHeight(height * attributeScale, baby, seat == 0, sitting, elapsed, scale);
        return seatPosition(previous, 0, y, (seat == 0 ? 0.5f : -0.7f) * scale);
    }

    static float nextAnimation(Entity mount, float previous) {
        return switch (mount.getEntityType()) {
            case HORSE, DONKEY, MULE, ZOMBIE_HORSE, SKELETON_HORSE -> mount.getFlag(EntityFlag.STANDING)
                    ? Math.min(1f, previous + (1f - previous) * 0.4f + 0.05f)
                    : Math.max(0f, previous + (0.8f * previous * previous * previous - previous) * 0.6f - 0.05f);
            case POLAR_BEAR -> Math.clamp(previous + (mount.getFlag(EntityFlag.STANDING) ? 1f : -1f) / 6f, 0f, 1f);
            case SHULKER -> {
                Integer face = mount.getMetadata().value(EntityDataTypes.SHULKER_ATTACH_FACE);
                Integer peek = mount.getMetadata().value(EntityDataTypes.SHULKER_PEEK_AMOUNT);
                float target = (face == null || face == 0) && peek != null ? peek * 0.01f : 0f;
                yield previous + Math.clamp(target - previous, -0.05f, 0.05f);
            }
            default -> 0f;
        };
    }

    static float camelHeight(float height, boolean baby, boolean front, boolean sitting, long elapsed, float scale) {
        float base = height - (baby ? 0.09375f : 0.375f);
        int duration = sitting ? 40 : 52;
        float bottom = 0.2f * scale;
        if (elapsed >= duration) {
            return sitting ? base + bottom : base;
        }
        // Java 的前后座在起身时经过最低点的时刻不同。
        int pivot = sitting ? 28 : (front ? 24 : 32);
        float flex = sitting ? (front ? 0.5f : 0.1f) : (front ? 0.6f : 0.35f);
        float top = 1.43f * scale;
        float middle = top - flex * (top - bottom);
        float start = sitting ? top : bottom - top;
        float end = sitting ? bottom : 0;
        if (!sitting) {
            middle = bottom - middle;
        }
        if (elapsed < pivot) {
            return base + start + (middle - start) * Math.max(0, elapsed) / pivot;
        }
        return base + middle + (end - middle) * (elapsed - pivot) / (duration - pivot);
    }
}
