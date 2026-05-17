package net.momirealms.craftengine.core.item.network.encrypt;

import net.momirealms.sparrow.nbt.ByteArrayTag;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;

public final class ItemCrypto {
    private static CryptoAlgorithm ALGORITHM = null;

    private ItemCrypto() {}

    public static Tag encrypt(CompoundTag compoundTag) {
        if (compoundTag == null) return null;
        if (ALGORITHM == null) return compoundTag;
        try {
            return new ByteArrayTag(ALGORITHM.encrypt(NBT.toBytes(compoundTag)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to encrypt NBT tag", e);
        }
    }

    public static CompoundTag decrypt(Tag tag) {
        if (tag == null) return null;
        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        if (!(tag instanceof ByteArrayTag byteArrayTag)) {
            throw new IllegalArgumentException("Invalid NBT tag");
        }
        try {
            return NBT.fromBytes(ALGORITHM.decrypt(byteArrayTag.getAsByteArray()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decrypt NBT tag", e);
        }
    }

    public static void setAlgorithm(CryptoAlgorithm algorithm) {
        ALGORITHM = algorithm;
    }
}
