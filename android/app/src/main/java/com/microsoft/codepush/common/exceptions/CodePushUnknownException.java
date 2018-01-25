package com.microsoft.codepush.common.exceptions;

/**
 * Exception class for throwing unknown CodePush exceptions.
 */
public class CodePushUnknownException extends Exception {

    /**
     * Constructor for unknown CodePush exception that have
     * <code>message</code> and <code>cause</code> arguments.
     *
     * @param message the custom message to provide exception context.
     * @param cause   the cause why CodePush cannot be initialized.
     */
    public CodePushUnknownException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for unknown CodePush exception that have <code>message</code> argument.
     *
     * @param message the custom message to provide exception context.
     */
    public CodePushUnknownException(String message) {
        super(message);
    }
}
