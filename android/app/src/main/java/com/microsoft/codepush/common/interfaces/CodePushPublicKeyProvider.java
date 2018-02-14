package com.microsoft.codepush.common.interfaces;

/**
 * Represents interface for provider of public key.
 */
public interface CodePushPublicKeyProvider {

    /**
     * Gets public key.
     *
     * @return public key.
     */
    String getPublicKey();
}
