package net.momirealms.craftengine.core.item.network.encrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * 从明文派生确定性nonce (合成IV / SIV 思路)。
 * <p>
 * 为什么需要确定性: 1.21.5+ 的容器点击包发送的是 HashedStack, 服务端要重新对物品跑一遍 s2c 再比对哈希
 * (见 HashedStackGenerator.MatchesInterceptor)。带 client-bound 名称/描述的物品在 s2c 时会把原始数据
 * 加密后塞进 CUSTOM_DATA, 所以只要 encrypt() 每次输出不同的字节, 重新生成的物品就和发给客户端的那个
 * 对不上, 哈希比对必然失败 -> 服务端把正常点击当成不同步 -> 幽灵物品。
 * <p>
 * 安全性: nonce = HMAC-SHA256(nonceKey, plaintext), nonceKey 由密钥派生并做了域分隔, 客户端无法计算。
 * 同一个nonce只会配同一段明文出现, 因此不存在 GCM 最忌讳的"同nonce不同明文"问题。代价是确定性加密
 * 固有的: 相同明文产生相同密文, 观察者能看出两个物品藏的数据一样 —— 而默认的 xor 本来就是如此,
 * 所以相对默认配置没有变弱。
 */
final class SyntheticNonce {
    private static final String MAC_ALGORITHM = "HmacSHA256";
    // 域分隔: 保证派生出的nonce密钥和加密密钥不是同一个值
    private static final byte[] DOMAIN = "craftengine:synthetic-nonce".getBytes(StandardCharsets.UTF_8);
    private static final ThreadLocal<Mac> MAC = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance(MAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HmacSHA256 unavailable", e);
        }
    });

    private SyntheticNonce() {}

    static SecretKeySpec deriveKey(byte[] cipherKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(cipherKey);
            sha256.update(DOMAIN);
            return new SecretKeySpec(sha256.digest(), MAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to derive nonce key", e);
        }
    }

    /** 把 HMAC(nonceKey, data) 的前 nonce.length 个字节写进 nonce */
    static void derive(SecretKeySpec nonceKey, byte[] data, byte[] nonce) throws GeneralSecurityException {
        Mac mac = MAC.get();
        mac.init(nonceKey);
        byte[] digest = mac.doFinal(data);
        System.arraycopy(digest, 0, nonce, 0, nonce.length);
    }
}
