/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class GrassTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final float temperature;
    private final float downfall;

    public GrassTint(float temperature, float downfall) {
        this.temperature = temperature;
        this.downfall = downfall;
    }

    @Override
    public Key type() {
        return Tints.GRASS;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("temperature", temperature);
        json.addProperty("downfall", downfall);
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            float temperature = MiscUtils.getAsFloat(arguments.getOrDefault("temperature", 0));
            float downfall = MiscUtils.getAsFloat(arguments.getOrDefault("downfall", 0));
            if (temperature > 1 || temperature < 0) {
                throw new IllegalArgumentException("Invalid temperature: " + temperature + ". Valid range 0~1");
            }
            if (downfall > 1 || downfall < 0) {
                throw new IllegalArgumentException("Invalid downfall: " + downfall + ". Valid range 0~1");
            }
            return new GrassTint(temperature, downfall);
        }
    }
}
