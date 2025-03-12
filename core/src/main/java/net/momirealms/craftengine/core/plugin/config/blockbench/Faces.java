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

package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

public class Faces {
    @SerializedName("north")
    private Face north;
    @SerializedName("east")
    private Face east;
    @SerializedName("south")
    private Face south;
    @SerializedName("west")
    private Face west;
    @SerializedName("up")
    private Face up;
    @SerializedName("down")
    private Face down;

    public Face north() {
        return north;
    }

    public Face east() {
        return east;
    }

    public Face south() {
        return south;
    }

    public Face west() {
        return west;
    }

    public Face up() {
        return up;
    }

    public Face down() {
        return down;
    }

    public static class Face {
        @SerializedName("uv")
        private double[] uv;
        @SerializedName("texture")
        private int texture;

        public double[] uv() {
            return uv;
        }

        public int texture() {
            return texture;
        }
    }
}
