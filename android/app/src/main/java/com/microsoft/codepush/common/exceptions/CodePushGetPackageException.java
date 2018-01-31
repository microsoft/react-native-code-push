package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred during getting the package.
 */
public class CodePushGetPackageException extends Exception {

    /**
     * The default error message.
     */
    private static String MESSAGE = "Error occurred during obtaining a package.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushGetPackageException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
