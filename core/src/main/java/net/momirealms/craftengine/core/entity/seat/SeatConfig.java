package net.momirealms.craftengine.core.entity.seat;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import org.joml.Vector3f;

public record SeatConfig(Vector3f position, float yRot, boolean limitPlayerRotation) {

    public static SeatConfig fromConfig(ConfigValue value) {
        ConfigValue[] split = value.splitValues(" ");
        ConfigValue[] vecSplit = split[0].splitValuesRestrict(",", 3);
        if (split.length == 1) {
            return new SeatConfig(
                    new Vector3f(vecSplit[0].getAsFloat(), vecSplit[1].getAsFloat(), vecSplit[2].getAsFloat()),
                    0, false
            );
        } else {
            return new SeatConfig(
                    new Vector3f(vecSplit[0].getAsFloat(), vecSplit[1].getAsFloat(), vecSplit[2].getAsFloat()),
                    split[1].getAsFloat(), true
            );
        }
    }
}
