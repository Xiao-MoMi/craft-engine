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

package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

public class TimeRangeDispatchProperty implements RangeDispatchProperty {
    public static final Factory FACTORY = new Factory();
    private final String source;
    private final boolean wobble;

    public TimeRangeDispatchProperty(String source, boolean wobble) {
        this.source = source;
        this.wobble = wobble;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.TIME;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("source", source);
        if (!wobble) {
            jsonObject.addProperty("wobble", false);
        }
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            String source = Objects.requireNonNull(arguments.get("source")).toString();
            boolean wobble = (boolean) arguments.getOrDefault("wobble", true);
            return new TimeRangeDispatchProperty(source, wobble);
        }
    }
}
