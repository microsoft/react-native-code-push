package com.microsoft.codepush.common.exceptions;

/**
 * Exception class for throwing CodePush not initialized exceptions.
 * Thrown everytime CodePush is used before initialization.
 */
public final class CodePushNotInitializedException extends Exception {

    /**
     * Creates instance of the not initialized CodePush exception using
     * <code>message</code> and <code>cause</code> arguments.
     *
     * @param message the custom message to provide exception context.
     * @param cause   the cause why CodePush cannot be initialized.
     */
    public CodePushNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the not initialized CodePush exception using <code>message</code> argument.
     *
     * @param message the custom message to provide exception context.
     */
    public CodePushNotInitializedException(String message) {
        super(message);
    }
}
