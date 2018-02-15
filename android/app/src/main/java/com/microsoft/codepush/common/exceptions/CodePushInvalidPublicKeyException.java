package com.microsoft.codepush.common.exceptions;

/**
 * Class to handle exception occurred when obtaining public key.
 */
public class CodePushInvalidPublicKeyException extends Exception {

    /**
     * Creates an instance of {@link CodePushInvalidPublicKeyException} with custom message and cause.
     *
     * @param message custom message.
     * @param cause   exception-cause.
     */
    public CodePushInvalidPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance of {@link CodePushInvalidPublicKeyException} with custom message.
     *
     * @param message custom message.
     */
    public CodePushInvalidPublicKeyException(String message) {
        super(message);
    }
}