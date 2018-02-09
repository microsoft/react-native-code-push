package com.microsoft.codepush.common.exceptions;

public class CodePushInitializeException extends Exception {
    public CodePushInitializeException(Throwable throwable) {
        super(throwable);
    }

    public CodePushInitializeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
