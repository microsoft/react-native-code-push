package com.microsoft.codepush.react;

public class CodePushInvalidUpdateException extends RuntimeException {
    public CodePushInvalidUpdateException() {
        super("Update is invalid - no files with extension .bundle, .js or .jsbundle were found in the update package.");
    }
}
