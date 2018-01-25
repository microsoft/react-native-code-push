package com.microsoft.codepush.common.exceptions;

import java.security.SignatureException;

/**
 * Exception class for handling signature verification errors.
 */
public class CodePushSignatureVerificationException extends SignatureException {

    /**
     * Default message.
     */
    private static String MESSAGE = "Error occured during signature verification";

    /**
     * Constructs exception with specified detail message.
     *
     * @param message detailed message.
     */
    public CodePushSignatureVerificationException(String message) {
        super(message);
    }

    /**
     * Constructs exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushSignatureVerificationException(Throwable cause) {
        super(MESSAGE, cause);
    }

    /**
     * Constructs exception with with specified detail message and cause.
     *
     * @param message detailed message.
     * @param cause   cause of exception.
     */
    public CodePushSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
