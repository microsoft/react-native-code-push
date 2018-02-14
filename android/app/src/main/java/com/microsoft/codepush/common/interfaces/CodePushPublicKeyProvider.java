package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushInvalidPublicKeyException;

/**
 * Represents interface for provider of public key.
 */
public interface CodePushPublicKeyProvider {

    /**
     * Gets public key.
     *
     * @return public key.
     */
    String getPublicKey() throws CodePushInvalidPublicKeyException;
}
