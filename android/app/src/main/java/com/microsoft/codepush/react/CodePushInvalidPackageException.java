package com.microsoft.codepush.react;

public class CodePushInvalidPackageException  extends RuntimeException {
    public CodePushInvalidPackageException() {
        super("Update is invalid - no files with extension .jsbundle or .bundle were found in the update package.");
    }
}
