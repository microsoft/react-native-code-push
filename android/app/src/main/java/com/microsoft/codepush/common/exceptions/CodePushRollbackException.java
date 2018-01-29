package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred during rollback.
 */
public class CodePushRollbackException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during the rollback.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushRollbackException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
