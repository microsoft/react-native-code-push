package com.microsoft.codepush.common.exceptions;

import com.microsoft.codepush.common.core.CodePushBaseCore;

/**
 * Class for all exceptions that is coming from {@link CodePushBaseCore} public methods.
 */
public class CodePushNativeApiCallException extends Exception {

    /**
     * Creates instance of {@link CodePushNativeApiCallException}.
     *
     * @param detailMessage detailed message.
     */
    public CodePushNativeApiCallException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Creates instance of {@link CodePushNativeApiCallException}.
     *
     * @param cause cause of error.
     */
    public CodePushNativeApiCallException(Throwable cause) {
        super(cause);
    }


    /**
     * Creates instance of {@link CodePushNativeApiCallException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public CodePushNativeApiCallException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
