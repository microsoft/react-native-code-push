package com.microsoft.codepush.common.exceptions;

/**
 * Exception class for the cases when invalid public key was used for CodePush initilization.
 */
public class CodePushInvalidPublicKeyException extends Exception {

    /**
     * Creates instance of the CodePush public key exception using
     * <code>message</code> and <code>cause</code> arguments.
     *
     * @param message the custom message to provide exception context.
     * @param cause   the cause why CodePush public key cannot be received.
     */
    public CodePushInvalidPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of CodePush public key exception using <code>message</code> argument.
     *
     * @param message the custom message to provide exception context.
     */
    public CodePushInvalidPublicKeyException(String message) {
        super(message);
    }
}
