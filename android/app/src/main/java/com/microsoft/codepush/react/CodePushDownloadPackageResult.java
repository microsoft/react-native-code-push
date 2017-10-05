package com.microsoft.codepush.react;

import java.io.File;

public class CodePushDownloadPackageResult {
    private File downloadFile;
    private boolean isZip;

    public CodePushDownloadPackageResult(File downloadFile, boolean isZip) {
        this.downloadFile = downloadFile;
        this.isZip = isZip;
    }

    public File getDownloadFile() {
        return downloadFile;
    }

    public boolean isZip() {
        return isZip;
    }
}
