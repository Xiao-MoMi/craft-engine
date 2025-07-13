package net.momirealms.craftengine.core.block;

public interface BlockStateWrapper {

    Object handle();

    int registryId();

    boolean isVanillaBlock();

    static BlockStateWrapper vanilla(Object handle, int registryId) {
        return new VanillaBlockState(handle, registryId);
    }

    static BlockStateWrapper custom(Object handle, int registryId) {
        return new CustomBlockState(handle, registryId);
    }

    static BlockStateWrapper create(Object handle, int registryId, boolean isVanillaBlock) {
        if (isVanillaBlock) return new VanillaBlockState(handle, registryId);
        else return new CustomBlockState(handle, registryId);
    }

    abstract class AbstractBlockState implements BlockStateWrapper {
        protected final Object handle;
        protected final int registryId;

        public AbstractBlockState(Object handle, int registryId) {
            this.handle = handle;
            this.registryId = registryId;
        }

        @Override
        public Object handle() {
            return this.handle;
        }

        @Override
        public int registryId() {
            return this.registryId;
        }
    }

    class VanillaBlockState extends AbstractBlockState {

        public VanillaBlockState(Object handle, int registryId) {
            super(handle, registryId);
        }

        @Override
        public boolean isVanillaBlock() {
            return true;
        }
    }

    class CustomBlockState extends AbstractBlockState {

        public CustomBlockState(Object handle, int registryId) {
            super(handle, registryId);
        }

        @Override
        public DelegatingBlockState handle() {
            return (DelegatingBlockState) super.handle();
        }

        @Override
        public boolean isVanillaBlock() {
            return false;
        }
    }
}
