package com.microsoft.codepush.common.exceptions;

/**
 * General code push exception that has no specified sub type or occasion.
 */
public class CodePushGeneralException extends Exception {

    /**
     * Creates an instance of {@link CodePushGeneralException} with custom message and exception provided.
     *
     * @param detailMessage custom message.
     * @param throwable     cause of the exception.
     */
    public CodePushGeneralException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Creates an instance of {@link CodePushGeneralException} with custom message provided.
     *
     * @param detailMessage custom message.
     */
    public CodePushGeneralException(String detailMessage) {
        super(detailMessage);
    }
}
