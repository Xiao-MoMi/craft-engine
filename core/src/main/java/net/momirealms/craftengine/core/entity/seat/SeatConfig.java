package net.momirealms.craftengine.core.entity.seat;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import org.joml.Vector3f;

import java.util.Map;

// forcePlayerRotation 为相对家具朝向的角度；NaN 表示不调整玩家视角。
public record SeatConfig(Vector3f position, float yRot, boolean limitPlayerRotation, float forcePlayerRotation) {
    private static final String[] FORCE_PLAYER_ROTATION = ConfigKeys.of("force_player_rotation");
    private static final String[] LIMIT_PLAYER_ROTATION = ConfigKeys.of("limit_player_rotation");

    public SeatConfig(Vector3f position, float yRot, boolean limitPlayerRotation) {
        this(position, yRot, limitPlayerRotation, Float.NaN);
    }

    public static SeatConfig fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            ConfigSection section = value.getAsSection();
            float yaw = section.getFloat("yaw");
            float forcePlayerRotation = section.getValue(FORCE_PLAYER_ROTATION, v -> parseForcePlayerRotation(v, yaw), Float.NaN);
            return new SeatConfig(
                    section.getNonNullVector3f("position"),
                    yaw,
                    section.getBoolean(LIMIT_PLAYER_ROTATION, section.containsKey("yaw")),
                    forcePlayerRotation
            );
        }
        ConfigValue[] split = value.splitValues(" ");
        ConfigValue[] vecSplit = split[0].splitValuesRestrict(",", 3);
        if (split.length == 1) {
            return new SeatConfig(
                    new Vector3f(vecSplit[0].getAsFloat(), vecSplit[1].getAsFloat(), vecSplit[2].getAsFloat()),
                    0, false
            );
        } else {
            float yaw = split[1].getAsFloat();
            return new SeatConfig(
                    new Vector3f(vecSplit[0].getAsFloat(), vecSplit[1].getAsFloat(), vecSplit[2].getAsFloat()),
                    yaw, true,
                    split.length > 2 ? parseForcePlayerRotation(split[2], yaw) : Float.NaN
            );
        }
    }

    private static float parseForcePlayerRotation(ConfigValue value, float yaw) {
        String rotation = value.getAsString();
        if ("true".equalsIgnoreCase(rotation)) return yaw;
        if ("false".equalsIgnoreCase(rotation)) return Float.NaN;
        return value.getAsFloat();
    }
}
