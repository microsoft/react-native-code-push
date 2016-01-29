package com.microsoft.codepush.react;

public class CodePushInvalidPackageException  extends RuntimeException {
    public CodePushInvalidPackageException() {
        super("Update is invalid - no files with extension .bundle, .js or .jsbundle were found in the update package.");
    }
}
