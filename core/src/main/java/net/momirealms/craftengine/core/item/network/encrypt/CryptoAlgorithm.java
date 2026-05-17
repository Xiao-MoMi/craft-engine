package net.momirealms.craftengine.core.item.network.encrypt;

import java.security.GeneralSecurityException;

public interface CryptoAlgorithm {

    byte[] encrypt(byte[] data) throws GeneralSecurityException;

    byte[] decrypt(byte[] data) throws GeneralSecurityException;
}
