package com.microsoft.codepush.common.exceptions;

public class CodePushNativeApiCallException extends Exception {
    public CodePushNativeApiCallException(String detailMessage) {
        super(detailMessage);
    }

    public CodePushNativeApiCallException(Throwable throwable) {
        super(throwable);
    }

    public CodePushNativeApiCallException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
