package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred during querying the update.
 */
public class CodePushQueryUpdateException extends CodePushApiHttpRequestException {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during querying the update.";

    /**
     * Creates an instance of the exception with specified cause and package hash.
     *
     * @param cause       cause of exception.
     * @param packageHash hash of the queried update.
     */
    public CodePushQueryUpdateException(Throwable cause, String packageHash) {
        super(MESSAGE + " Package hash is " + packageHash, cause);
    }

    /**
     * Creates an instance of the exception with specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushQueryUpdateException(Throwable cause) {
        super(MESSAGE, cause);
    }

    /**
     * Creates an instance of the exception with custom detail message.
     *
     * @param result result of the query form the server or custom message.
     */
    public CodePushQueryUpdateException(String result) {
        super(MESSAGE + result);
    }

}
