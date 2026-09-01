package net.momirealms.craftengine.core.item.network.encrypt;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public final class AESGCM implements CryptoAlgorithm {
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = 16;
    private static final ThreadLocal<Cipher> DECRYPT_CIPHER = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance("AES/GCM/NoPadding");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    });
    private static final ThreadLocal<byte[]> IV_BUF = ThreadLocal.withInitial(() -> new byte[IV_LEN]);
    private final SecretKeySpec keySpec;
    private final SecretKeySpec nonceKey;

    public AESGCM(@NotNull String key) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashed = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(hashed, "AES");
            this.nonceKey = SyntheticNonce.deriveKey(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        byte[] iv = IV_BUF.get();
        // 必须是确定性的, 否则 1.21.5+ 的 HashedStack 比对会失败, 见 SyntheticNonce
        SyntheticNonce.derive(this.nonceKey, data, iv);
        // 这里不能复用 ThreadLocal 的 Cipher: JDK 的 GCM 实现会记住上一次 encrypt 用的 key+iv,
        // 再用同一组初始化就抛 "Cannot reuse iv for GCM encryption"。而我们的 iv 按设计就是确定性的,
        // 同一件物品重复加密必然撞上这个检查, 所以每次新建。只影响非默认的 aes-gcm 配置。
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
        byte[] result = new byte[IV_LEN + cipher.getOutputSize(data.length)];
        System.arraycopy(iv, 0, result, 0, IV_LEN);
        cipher.doFinal(data, 0, data.length, result, IV_LEN);
        return result;
    }

    @Override
    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        if (data.length < IV_LEN + TAG_BYTES) {
            throw new IllegalArgumentException("Data too short");
        }
        Cipher cipher = DECRYPT_CIPHER.get();
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, data, 0, IV_LEN));
        int cipherLen = data.length - IV_LEN;
        byte[] plain = new byte[cipher.getOutputSize(cipherLen)];
        int written = cipher.doFinal(data, IV_LEN, cipherLen, plain, 0);
        return written == plain.length ? plain : trimTo(plain, written);
    }

    private static byte[] trimTo(byte[] src, int len) {
        byte[] dst = new byte[len];
        System.arraycopy(src, 0, dst, 0, len);
        return dst;
    }
}
