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

package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class CustomModelDataSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    private final int index;

    public CustomModelDataSelectProperty(int index) {
        this.index = index;
    }

    @Override
    public Key type() {
        return SelectProperties.CUSTOM_MODEL_DATA;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("index", index);
    }

    public static class Factory implements SelectPropertyFactory {

        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            int index = MiscUtils.getAsInt(arguments.getOrDefault("index", 0));
            return new CustomModelDataSelectProperty(index);
        }
    }
}
