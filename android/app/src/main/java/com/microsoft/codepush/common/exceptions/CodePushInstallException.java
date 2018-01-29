package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred during installing the package.
 */
public class CodePushInstallException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during installing the package.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushInstallException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
