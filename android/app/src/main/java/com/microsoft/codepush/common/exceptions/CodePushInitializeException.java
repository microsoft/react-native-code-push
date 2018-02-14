package com.microsoft.codepush.common.exceptions;

import com.microsoft.codepush.common.core.CodePushBaseCore;

/**
 * Exception class for handling {@link CodePushBaseCore creating exceptions.
 */
public class CodePushInitializeException extends RuntimeException {

    /**
     * Creates instance of {@link CodePushInitializeException}.
     *
     * @param cause cause of error.
     */
    public CodePushInitializeException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates instance of {@link CodePushInitializeException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public CodePushInitializeException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
