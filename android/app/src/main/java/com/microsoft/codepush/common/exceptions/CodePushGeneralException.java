package com.microsoft.codepush.common.exceptions;

public class CodePushGeneralException extends Exception {
    public CodePushGeneralException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public CodePushGeneralException(String detailMessage) {
        super(detailMessage);
    }
}
