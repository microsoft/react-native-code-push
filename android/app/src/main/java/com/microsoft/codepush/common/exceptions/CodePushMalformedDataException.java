package com.microsoft.codepush.common.exceptions;

/**
 * Exception class for throwing malformed CodePush data exceptions.
 * Malformed data could be json blob of CodePush update manifest and other json blobs
 * saved locally, received from server and etc.
 */
public class CodePushMalformedDataException extends Exception {

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>path</code> and <code>message</code> arguments.
     *
     * @param message the message with explanation of error.
     * @param cause   the cause why CodePush data cannot be parsed.
     */
    public CodePushMalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>message</code> argument.
     *
     * @param message the message with explanation of error.
     */
    public CodePushMalformedDataException(String message) {
        super(message);
    }
}
