package com.microsoft.codepush.react;

class CodePushUnknownException extends RuntimeException {

    public CodePushUnknownException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodePushUnknownException(String message) {
        super(message);
    }
}