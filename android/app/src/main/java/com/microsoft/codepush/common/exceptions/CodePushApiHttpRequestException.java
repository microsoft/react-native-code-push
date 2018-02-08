package com.microsoft.codepush.common.exceptions;

/**
 * An exception occurred during making HTTP request to CodePush server.
 */
public class CodePushApiHttpRequestException extends Exception {

    /**
     * Creates instance of {@link CodePushApiHttpRequestException}.
     *
     * @param throwable the cause why request failed.
     */
    public CodePushApiHttpRequestException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates instance of {@link CodePushApiHttpRequestException}.
     *
     * @param detailMessage the detailed message of why request failed.
     */
    public CodePushApiHttpRequestException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Creates instance of {@link CodePushApiHttpRequestException}.
     *
     * @param detailMessage the cause why request failed.
     * @param throwable     the detailed message of why request failed.
     */
    public CodePushApiHttpRequestException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
