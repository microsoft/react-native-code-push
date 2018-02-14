package com.microsoft.codepush.common.exceptions;

import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

/**
 * Exception class for handling {@link CodePushPlatformUtils} exceptions.
 */
public class CodePushPlatformUtilsException extends Exception {

    /**
     * Creates instance of {@link CodePushPlatformUtilsException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public CodePushPlatformUtilsException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
