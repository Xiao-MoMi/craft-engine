package net.momirealms.craftengine.core.world.chunk.client.light;

public final class LightSection {
    private ClientSectionLightStorage storage;

    public LightSection(ClientSectionLightStorage storage) {
        this.storage = storage;
    }

    public int blockType(int x, int y, int z) {
        return blockType((y << 4 | z) << 4 | x);
    }

    public int blockType(int index) {
        return this.storage.blockType(index);
    }

    public void setBlockType(int x, int y, int z, int blockType) {
        int index = (y << 4 | z) << 4 | x;
        this.setBlockType(index, blockType);
    }

    public void setBlockType(int index, int blockType) {
        if (blockType == 0) this.setSolid(index);
        else if (blockType == 1) this.setAir(index);
        else if (blockType == 2) this.setWater(index);
    }

    public void setWater(int x, int y, int z) {
        this.setWater((y << 4 | z) << 4 | x);
    }

    public void setWater(int index) {
        boolean wasWater = this.storage.isWater(index);
        if (!wasWater) {
            if (this.storage instanceof PackedLightStorage arrayStorage) {
                arrayStorage.setWater(index);
            } else {
                int previousType = this.storage.blockType(index);
                PackedLightStorage newStorage = new PackedLightStorage(previousType);
                newStorage.setWater(index);
                this.storage = newStorage;
            }
        }
    }

    public void setAir(int x, int y, int z) {
        this.setAir((y << 4 | z) << 4 | x);
    }

    public void setAir(int index) {
        boolean wasAir = this.storage.isAir(index);
        if (!wasAir) {
            if (this.storage instanceof PackedLightStorage arrayStorage) {
                arrayStorage.setAir(index);
            } else {
                int previousType = this.storage.blockType(index);
                PackedLightStorage newStorage = new PackedLightStorage(previousType);
                newStorage.setAir(index);
                this.storage = newStorage;
            }
        }
    }

    public void setSolid(int x, int y, int z) {
        this.setSolid((y << 4 | z) << 4 | x);
    }

    public void setSolid(int index) {
        boolean wasSolid = this.storage.isSolid(index);
        if (!wasSolid) {
            if (this.storage instanceof PackedLightStorage arrayStorage) {
                arrayStorage.setSolid(index);
            } else {
                int previousType = this.storage.blockType(index);
                PackedLightStorage newStorage = new PackedLightStorage(previousType);
                newStorage.setSolid(index);
                this.storage = newStorage;
            }
        }
    }
}
