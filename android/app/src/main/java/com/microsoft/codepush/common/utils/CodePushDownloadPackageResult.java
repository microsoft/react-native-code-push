package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;

import java.io.File;

/**
 * Class representing the downloaded update package.
 */
public class CodePushDownloadPackageResult {

    /**
     * The file containing the update.
     */
    private File downloadFile;

    /**
     * Whether the file is zipped.
     */
    private boolean isZip;

    /**
     * An exception that has occurred while downloading the package.
     */
    private CodePushDownloadPackageException codePushDownloadPackageException;

    /**
     * Creates an instance of the class.
     *
     * @param downloadFile the file containing the update.
     * @param isZip        whether the file is zipped.
     */
    public CodePushDownloadPackageResult(File downloadFile, boolean isZip) {
        this.downloadFile = downloadFile;
        this.isZip = isZip;
    }

    /**
     * Creates an instance of the package that has failed to download.
     *
     * @param codePushDownloadPackageException exception that has occurred while downloading the package.
     */
    public CodePushDownloadPackageResult(CodePushDownloadPackageException codePushDownloadPackageException) {
        this.codePushDownloadPackageException = codePushDownloadPackageException;
    }

    /**
     * Gets the download file and returns it.
     *
     * @return download file.
     */
    public File getDownloadFile() {
        return downloadFile;
    }

    /**
     * Gets whether the file is zipped.
     *
     * @return whether the file is zipped.
     */
    public boolean isZip() {
        return isZip;
    }

    /**
     * Gets the value of codePushDownloadPackageException and returns it
     *
     * @return codePushDownloadPackageException
     */
    public CodePushDownloadPackageException getCodePushDownloadPackageException() {
        return codePushDownloadPackageException;
    }

    /**
     * Checks whether the current package is corrupt.
     *
     * @return <code>true</code> if the package has not been downloaded successfully, <code>false</code> otherwise.
     */
    public boolean isFailed() {
        return codePushDownloadPackageException != null;
    }
}
