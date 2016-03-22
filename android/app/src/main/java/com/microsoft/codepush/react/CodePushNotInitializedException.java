package com.microsoft.codepush.react;

public class CodePushNotInitializedException extends RuntimeException {

    public CodePushNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodePushNotInitializedException(String message) {
        super(message);
    }
}