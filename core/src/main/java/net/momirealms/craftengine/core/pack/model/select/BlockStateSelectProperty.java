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

import java.util.Map;
import java.util.Objects;

public class BlockStateSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    private final String blockStateProperty;

    public BlockStateSelectProperty(String blockStateProperty) {
        this.blockStateProperty = blockStateProperty;
    }

    @Override
    public Key type() {
        return SelectProperties.BLOCK_STATE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("block_state_property", blockStateProperty);
    }

    public static class Factory implements SelectPropertyFactory {

        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            String blockStateProperty = Objects.requireNonNull(arguments.get("block-state-property")).toString();
            return new BlockStateSelectProperty(blockStateProperty);
        }
    }
}
