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

package net.momirealms.craftengine.core.block;

public class UpdateOption {
    public static final UpdateOption UPDATE_ALL = new UpdateOption(3);
    public static final UpdateOption UPDATE_NONE = new UpdateOption(4);
    public static final UpdateOption UPDATE_ALL_IMMEDIATE = new UpdateOption(11);
    private final int flags;

    private UpdateOption(int flags) {
        this.flags = flags;
    }

    public int flags() {
        return flags;
    }

    public static class Builder {
        private int flags;

        public Builder() {
            flags = 0;
        }

        public Builder updateNeighbors() {
            flags |= Flags.UPDATE_NEIGHBORS;
            return this;
        }

        public Builder updateClients() {
            flags |= Flags.UPDATE_CLIENTS;
            return this;
        }

        public Builder updateInvisible() {
            flags |= Flags.UPDATE_INVISIBLE;
            return this;
        }

        public Builder updateImmediate() {
            flags |= Flags.UPDATE_IMMEDIATE;
            return this;
        }

        public Builder updateKnownShape() {
            flags |= Flags.UPDATE_KNOWN_SHAPE;
            return this;
        }

        public Builder updateSuppressDrops() {
            flags |= Flags.UPDATE_SUPPRESS_DROPS;
            return this;
        }

        public Builder updateMoveByPiston() {
            flags |= Flags.UPDATE_MOVE_BY_PISTON;
            return this;
        }

        public UpdateOption build() {
            return new UpdateOption(flags);
        }
    }

    public static class Flags {
        public static final int UPDATE_NEIGHBORS = 1;
        public static final int UPDATE_CLIENTS = 2;
        public static final int UPDATE_INVISIBLE = 4;
        public static final int UPDATE_IMMEDIATE = 8;
        public static final int UPDATE_KNOWN_SHAPE = 16;
        public static final int UPDATE_SUPPRESS_DROPS = 32;
        public static final int UPDATE_MOVE_BY_PISTON = 64;
    }
}
