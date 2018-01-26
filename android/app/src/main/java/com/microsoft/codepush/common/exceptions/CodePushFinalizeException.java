package com.microsoft.codepush.common.exceptions;

import java.io.IOException;

/**
 * Exception class for handling resource finalize exceptions.
 */
public class CodePushFinalizeException extends IOException {

    /**
     * Creates instance of the resource finalize exception.
     */
    public CodePushFinalizeException() {
        super("Error closing IO resources.");
    }

    /**
     * Creates instance of the resource finalize exception.
     *
     * @param cause the cause why resource cannot be finalized.
     */
    public CodePushFinalizeException(Throwable cause) {
        super("Error closing IO resources.", cause);
    }

    /**
     * Creates instance of the resource finalize exception using
     * <code>message</code> and <code>cause</code> arguments.
     *
     * @param message the custom message to provide exception context.
     * @param cause   the cause why resource cannot be finalized.
     */
    public CodePushFinalizeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the resource finalize exception using <code>message</code> argument.
     *
     * @param message the custom message to provide exception context.
     */
    public CodePushFinalizeException(String message) {
        super(message);
    }
}
