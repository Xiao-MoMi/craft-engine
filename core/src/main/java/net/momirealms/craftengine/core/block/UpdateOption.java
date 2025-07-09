package net.momirealms.craftengine.core.block;

public final class UpdateOption {
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
