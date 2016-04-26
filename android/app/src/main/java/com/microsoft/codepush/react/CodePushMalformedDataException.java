package com.microsoft.codepush.react;

import java.net.MalformedURLException;

public class CodePushMalformedDataException extends RuntimeException {
    public CodePushMalformedDataException(String path, Throwable cause) {
        super("Unable to parse contents of " + path + ", the file may be corrupted.", cause);
    }
    public CodePushMalformedDataException(String url, MalformedURLException cause) {
        super("The package has an invalid downloadUrl: " + url, cause);
    }
}