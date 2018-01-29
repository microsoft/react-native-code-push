package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred when unzipping the package.
 */
public class CodePushUnzipException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during package unzipping.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushUnzipException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
