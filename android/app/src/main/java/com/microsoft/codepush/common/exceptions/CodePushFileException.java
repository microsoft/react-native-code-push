package com.microsoft.codepush.common.exceptions;

/**
 * Exception class for throwing exceptions occurred when working with files.
 */
public class CodePushFileException extends Exception {

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>path</code> and <code>message</code> arguments.
     *
     * @param message the message with explanation of error.
     * @param cause   the cause why CodePush data cannot be parsed.
     */
    public CodePushFileException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>message</code> argument.
     *
     * @param message the message with explanation of error.
     */
    public CodePushFileException(String message) {
        super(message);
    }
}
