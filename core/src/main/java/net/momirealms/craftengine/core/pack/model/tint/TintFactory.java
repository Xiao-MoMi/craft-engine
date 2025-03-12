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

import org.incendo.cloud.type.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TintFactory {

    Tint create(Map<String, Object> arguments);

    default Either<Integer, List<Integer>> parseTintValue(Object value) {
        if (value instanceof Number i) {
            return Either.ofPrimary(i.intValue());
        } else if (value instanceof List<?> list) {
            if (list.size() != 3) {
                throw new IllegalArgumentException("Invalid tint value list size: " + list.size() + " which is expected to be 3");
            }
            List<Integer> intList = new ArrayList<>();
            for (Object o : list) {
                intList.add(Integer.parseInt(o.toString()));
            }
            return Either.ofFallback(intList);
        } else if (value instanceof String s) {
            String[] split = s.split(",");
            if (split.length != 3) {
                throw new IllegalArgumentException("Invalid tint value list size: " + split.length + " which is expected to be 3");
            }
            List<Integer> intList = new ArrayList<>();
            for (String string : split) {
                intList.add(Integer.parseInt(string));
            }
            return Either.ofFallback(intList);
        }
        throw new IllegalArgumentException("Invalid tint value: " + value);
    }
}
