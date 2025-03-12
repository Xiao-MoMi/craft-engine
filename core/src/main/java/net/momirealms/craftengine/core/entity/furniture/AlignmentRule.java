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

package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Pair;

import java.util.function.Function;

public enum AlignmentRule {
    ANY(Function.identity()),
    CORNER(pair -> {
        double p1 = (double) Math.round(pair.left());
        double p2 = (double) Math.round(pair.right());
        return new Pair<>(p1, p2);
    }),
    CENTER(pair -> {
        double p1 = Math.floor(pair.left()) + 0.5;
        double p2 = Math.floor(pair.right()) + 0.5;
        return new Pair<>(p1, p2);
    }),
    HALF(pair -> {
        double p1 = Math.round(pair.left() * 2) / 2.0;
        double p2 = Math.round(pair.right() * 2) / 2.0;
        return new Pair<>(p1, p2);
    }),
    QUARTER(pair -> {
        double p1 = Math.round(pair.left() * 4) / 4.0;
        double p2 = Math.round(pair.right() * 4) / 4.0;
        return new Pair<>(p1, p2);
    });

    private final Function<Pair<Double, Double>, Pair<Double, Double>> function;

    AlignmentRule(Function<Pair<Double, Double>, Pair<Double, Double>> function) {
        this.function = function;
    }

    public Pair<Double, Double> apply(final Pair<Double, Double> pair) {
        return function.apply(pair);
    }
}
