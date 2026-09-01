package net.momirealms.craftengine.core.item.network.encrypt;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public final class ChaCha20 implements CryptoAlgorithm {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BYTES = 16;
    private static final ThreadLocal<Cipher> DECRYPT_CIPHER = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance("ChaCha20-Poly1305");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    });
    private static final ThreadLocal<byte[]> NONCE_BUF = ThreadLocal.withInitial(() -> new byte[NONCE_LENGTH]);
    private final SecretKeySpec keySpec;
    private final SecretKeySpec nonceKey;

    public ChaCha20(@NotNull String key) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashed = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(hashed, "ChaCha20-Poly1305");
            this.nonceKey = SyntheticNonce.deriveKey(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        byte[] nonce = NONCE_BUF.get();
        // 必须是确定性的, 否则 1.21.5+ 的 HashedStack 比对会失败, 见 SyntheticNonce
        SyntheticNonce.derive(this.nonceKey, data, nonce);
        // 同 AESGCM: JDK 会拒绝用同一组 key+nonce 再次初始化 encrypt, 而我们的 nonce 是确定性的
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(nonce));
        byte[] result = new byte[NONCE_LENGTH + cipher.getOutputSize(data.length)];
        System.arraycopy(nonce, 0, result, 0, NONCE_LENGTH);
        cipher.doFinal(data, 0, data.length, result, NONCE_LENGTH);
        return result;
    }

    @Override
    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        if (data.length < NONCE_LENGTH + TAG_BYTES) {
            throw new IllegalArgumentException("Data too short");
        }
        Cipher cipher = DECRYPT_CIPHER.get();
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(data, 0, NONCE_LENGTH));
        int cipherLen = data.length - NONCE_LENGTH;
        return cipher.doFinal(data, NONCE_LENGTH, cipherLen);
    }
}
