package net.momirealms.craftengine.core.item.network.encrypt;

import net.momirealms.sparrow.nbt.ByteArrayTag;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;

public final class ItemCrypto {
    // 配置加载线程写, netty线程读
    private static volatile CryptoAlgorithm ALGORITHM = null;

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

    /**
     * 解密网络物品数据。输入直接来自客户端数据包, 所以这里对任何无效输入都返回 null 而不是抛异常:
     * 调用方 (ModernNetworkItemHandler / LegacyNetworkItemHandler 的 c2s) 本来就按 null 处理"没有数据",
     * 而在netty线程上按数据包频率抛异常会被伪造的包直接当成拒绝服务用。
     */
    public static CompoundTag decrypt(Tag tag) {
        if (tag == null) return null;
        CryptoAlgorithm algorithm = ALGORITHM;
        if (algorithm == null) {
            // 未启用加密时数据本来就是明文
            return tag instanceof CompoundTag compoundTag ? compoundTag : null;
        }
        // 启用加密后必须拒绝明文: 否则客户端只要把 network_data 作为普通 CompoundTag 发回来,
        // 就能完全绕过加密, 让未经验证的内容被当作服务端自己写的数据接受。
        if (!(tag instanceof ByteArrayTag byteArrayTag)) {
            return null;
        }
        try {
            return NBT.fromBytes(algorithm.decrypt(byteArrayTag.getAsByteArray()));
        } catch (Exception e) {
            // 伪造或损坏的数据, 当作没有数据处理
            return null;
        }
    }

    public static void setAlgorithm(CryptoAlgorithm algorithm) {
        ALGORITHM = algorithm;
    }
}
