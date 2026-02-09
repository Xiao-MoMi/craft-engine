package net.momirealms.craftengine.core.block;

public final class UpdateOption {
    public static final UpdateOption UPDATE_NONE = new UpdateOption(Flags.UPDATE_NONE);
    public static final UpdateOption UPDATE_ALL = new UpdateOption(Flags.UPDATE_ALL);
    public static final UpdateOption UPDATE_ALL_IMMEDIATE = new UpdateOption(Flags.UPDATE_ALL_IMMEDIATE);
    public static final UpdateOption UPDATE_SKIP_ALL_SIDEEFFECTS = new UpdateOption(Flags.UPDATE_SKIP_ALL_SIDEEFFECTS);
    private final int flags;

    private UpdateOption(int flags) {
        this.flags = flags;
    }

    public int flags() {
        return flags;
    }

    public static Builder builder() {
        return new Builder();
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

        public Builder updateSkipShapeUpdateOnWire() {
            flags |= Flags.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE;
            return this;
        }

        public Builder updateSkipBlockEntitySideEffects() {
            flags |= Flags.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
            return this;
        }

        public Builder updateSkipOnPlace() {
            flags |= Flags.UPDATE_SKIP_ON_PLACE;
            return this;
        }

        public UpdateOption build() {
            return new UpdateOption(flags);
        }
    }

    public static class Flags {
        public static final int UPDATE_NEIGHBORS                     = 0b0000000001;
        public static final int UPDATE_CLIENTS                       = 0b0000000010;
        public static final int UPDATE_INVISIBLE                     = 0b0000000100;
        public static final int UPDATE_IMMEDIATE                     = 0b0000001000;
        public static final int UPDATE_KNOWN_SHAPE                   = 0b0000010000;
        public static final int UPDATE_SUPPRESS_DROPS                = 0b0000100000;
        public static final int UPDATE_MOVE_BY_PISTON                = 0b0001000000;
        public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE     = 0b0010000000;
        public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 0b0100000000;
        public static final int UPDATE_SKIP_ON_PLACE                 = 0b1000000000;
        // 组合常量
        public static final int UPDATE_NONE = UPDATE_INVISIBLE | UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
        public static final int UPDATE_ALL = UPDATE_NEIGHBORS | UPDATE_CLIENTS;
        public static final int UPDATE_ALL_IMMEDIATE = UPDATE_ALL | UPDATE_IMMEDIATE;
        public static final int UPDATE_SKIP_ALL_SIDEEFFECTS = UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS | UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | UPDATE_SKIP_ON_PLACE;
    }
}
