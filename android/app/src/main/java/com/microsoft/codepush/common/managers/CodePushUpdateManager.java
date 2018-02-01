package com.microsoft.codepush.common.managers;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.exceptions.CodePushSignatureVerificationException;
import com.microsoft.codepush.common.exceptions.CodePushSignatureVerificationException.SignatureExceptionType;
import com.microsoft.codepush.common.exceptions.CodePushUnzipException;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Manager responsible for update read/write actions.
 */
public class CodePushUpdateManager {

    /**
     * Whether to use test configuration.
     */
    private static boolean sTestConfigurationFlag = false;

    /**
     * General path for storing files.
     */
    private String mDocumentsDirectory;

    /**
     * Creates instance of CodePushUpdateManager.
     *
     * @param documentsDirectory path for storing files.
     */
    public CodePushUpdateManager(String documentsDirectory) {
        mDocumentsDirectory = documentsDirectory;
    }

    /**
     * Sets flag to use test configuration.
     *
     * @param shouldUseTestConfiguration <code>true</code> to use test configuration.
     */
    public static void setUsingTestConfiguration(boolean shouldUseTestConfiguration) {
        sTestConfigurationFlag = shouldUseTestConfiguration;
    }

    /**
     * Gets path to unzip files to.
     *
     * @return path to unzip files to.
     */
    private String getUnzippedFolderPath() {
        return FileUtils.appendPathComponent(getCodePushPath(), CodePushConstants.UNZIPPED_FOLDER_NAME);
    }

    /**
     * Gets general path for storing files.
     *
     * @return general path for storing files.
     */
    private String getDocumentsDirectory() {
        return mDocumentsDirectory;
    }

    /**
     * Gets application-specific folder.
     *
     * @return application-specific folder.
     */
    private String getCodePushPath() {
        String codePushPath = FileUtils.appendPathComponent(getDocumentsDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        if (sTestConfigurationFlag) {
            codePushPath = FileUtils.appendPathComponent(codePushPath, "TestPackages");
        }
        return codePushPath;
    }

    /**
     * Gets path to json file containing information about the available packages.
     *
     * @return path to json file containing information about the available packages.
     */
    private String getStatusFilePath() {
        return FileUtils.appendPathComponent(getCodePushPath(), CodePushConstants.STATUS_FILE_NAME);
    }

    /**
     * Gets metadata about the current update.
     *
     * @return metadata about the current update.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public JSONObject getCurrentPackageInfo() throws CodePushMalformedDataException, IOException {
        String statusFilePath = getStatusFilePath();
        if (!FileUtils.fileAtPathExists(statusFilePath)) {
            return new JSONObject();
        }
        return CodePushUtils.getJsonObjectFromFile(statusFilePath);

    }

    /**
     * Updates file containing information about the available packages.
     *
     * @param packageInfo new information.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void updateCurrentPackageInfo(JSONObject packageInfo) throws IOException {
        try {
            CodePushUtils.writeJsonToFile(packageInfo, getStatusFilePath());
        } catch (IOException e) {
            throw new IOException("Error updating current package info", e);
        }
    }

    /**
     * Gets folder for storing current package files.
     *
     * @return folder for storing current package files.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public String getCurrentPackageFolderPath() throws CodePushMalformedDataException, IOException {
        String packageHash = getCurrentPackageHash();
        if (packageHash == null) {
            return null;
        }
        return getPackageFolderPath(packageHash);
    }

    /**
     * Gets folder for the package by the package hash.
     *
     * @param packageHash current package identifier (hash).
     * @return path to package folder.
     */
    public String getPackageFolderPath(String packageHash) {
        return FileUtils.appendPathComponent(getCodePushPath(), packageHash);
    }

    /**
     * Gets entry path to the application.
     *
     * @param entryFileName file name of the entry file.
     * @return entry path to the application.
     * @throws IOException                 read/write error occurred while accessing the file system.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public String getCurrentPackageEntryPath(String entryFileName) throws CodePushGetPackageException, IOException {
        String packageFolder;
        try {
            packageFolder = getCurrentPackageFolderPath();
        } catch (CodePushMalformedDataException e) {
            throw new CodePushGetPackageException(e);
        }
        if (packageFolder == null) {
            return null;
        }
        JSONObject currentPackage = getCurrentPackage();
        if (currentPackage == null) {
            return null;
        }
        String relativeEntryPath = currentPackage.optString(CodePushConstants.APP_ENTRY_POINT_PATH_KEY, null);
        if (relativeEntryPath == null) {
            return FileUtils.appendPathComponent(packageFolder, entryFileName);
        } else {
            return FileUtils.appendPathComponent(packageFolder, relativeEntryPath);
        }
    }

    /**
     * Gets the identifier of the current package (hash).
     *
     * @return the identifier of the current package.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public String getCurrentPackageHash() throws IOException, CodePushMalformedDataException {
        JSONObject info = getCurrentPackageInfo();
        return info.optString(CodePushConstants.CURRENT_PACKAGE_KEY, null);
    }

    /**
     * Gets the identifier of the previous installed package (hash).
     *
     * @return the identifier of the previous installed package.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     **/
    public String getPreviousPackageHash() throws IOException, CodePushMalformedDataException {
        JSONObject info = getCurrentPackageInfo();
        return info.optString(CodePushConstants.PREVIOUS_PACKAGE_KEY, null);
    }

    /**
     * Gets current package json object.
     *
     * @return current package json object.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public JSONObject getCurrentPackage() throws CodePushGetPackageException {
        String packageHash;
        try {
            packageHash = getCurrentPackageHash();
        } catch (IOException | CodePushMalformedDataException e) {
            throw new CodePushGetPackageException(e);
        }
        if (packageHash == null) {
            return null;
        }
        return getPackage(packageHash);
    }

    /**
     * Gets previous installed package json object.
     *
     * @return previous installed package json object.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public JSONObject getPreviousPackage() throws CodePushGetPackageException {
        String packageHash;
        try {
            packageHash = getPreviousPackageHash();
        } catch (IOException | CodePushMalformedDataException e) {
            throw new CodePushGetPackageException(e);
        }
        if (packageHash == null) {
            return null;
        }
        return getPackage(packageHash);
    }

    /**
     * Gets package object by its hash.
     *
     * @param packageHash package identifier (hash).
     * @return package object.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public JSONObject getPackage(String packageHash) throws CodePushGetPackageException {
        String folderPath = getPackageFolderPath(packageHash);
        String packageFilePath = FileUtils.appendPathComponent(folderPath, CodePushConstants.PACKAGE_FILE_NAME);
        try {
            return CodePushUtils.getJsonObjectFromFile(packageFilePath);
        } catch (CodePushMalformedDataException e) {
            throw new CodePushGetPackageException(e);
        }
    }

    /**
     * Deletes the current package and installs the previous one.
     *
     * @throws CodePushRollbackException exception occurred during package rollback.
     */
    public void rollbackPackage() throws CodePushRollbackException {
        try {
            JSONObject info = getCurrentPackageInfo();
            String currentPackageFolderPath = getCurrentPackageFolderPath();
            FileUtils.deleteDirectoryAtPath(currentPackageFolderPath);
            info.put(CodePushConstants.CURRENT_PACKAGE_KEY, info.optString(CodePushConstants.PREVIOUS_PACKAGE_KEY, null));
            info.put(CodePushConstants.PREVIOUS_PACKAGE_KEY, null);
            updateCurrentPackageInfo(info);
        } catch (IOException | JSONException | CodePushMalformedDataException e) {
            throw new CodePushRollbackException(e);
        }
    }

    /**
     * Installs the new package.
     *
     * @param updatePackage       json containing the information about the current package.
     * @param removePendingUpdate whether to remove pending updates data.
     * @throws CodePushInstallException exception occurred during package installation.
     */
    public void installPackage(JSONObject updatePackage, boolean removePendingUpdate) throws CodePushInstallException {
        try {
            String packageHash = updatePackage.optString(CodePushConstants.PACKAGE_HASH_KEY, null);
            JSONObject info = getCurrentPackageInfo();
            String currentPackageHash = getCurrentPackageHash();
            if (packageHash != null && packageHash.equals(currentPackageHash)) {

                /* The current package is already the one being installed, so we should no-op. */
                return;
            }
            if (removePendingUpdate) {
                String currentPackageFolderPath = getCurrentPackageFolderPath();
                if (currentPackageFolderPath != null) {
                    FileUtils.deleteDirectoryAtPath(currentPackageFolderPath);
                }
            } else {
                String previousPackageHash = getPreviousPackageHash();
                if (previousPackageHash != null && !previousPackageHash.equals(packageHash)) {
                    FileUtils.deleteDirectoryAtPath(getPackageFolderPath(previousPackageHash));
                }
                info.put(CodePushConstants.PREVIOUS_PACKAGE_KEY, info.optString(CodePushConstants.CURRENT_PACKAGE_KEY, null));
            }
            info.put(CodePushConstants.CURRENT_PACKAGE_KEY, packageHash);
            updateCurrentPackageInfo(info);
        } catch (IOException | JSONException | CodePushMalformedDataException e) {
            throw new CodePushInstallException(e);
        }
    }

    /**
     * Clears all the updates data.
     *
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void clearUpdates() throws IOException {
        FileUtils.deleteDirectoryAtPath(getCodePushPath());
    }

    /**
     * Downloads the update package.
     *
     * @param updatePackage    information about the package.
     * @param progressCallback callback to send information about download to.
     *                         Can be <code>null</code> if code signing is not enabled.
     * @return downloaded package.
     * @throws CodePushDownloadPackageException an exception occurred during package downloading.
     */
    public CodePushDownloadPackageResult downloadPackage(JSONObject updatePackage,
                                                         final DownloadProgressCallback progressCallback, PackageDownloader packageDownloader) throws CodePushDownloadPackageException {
        String newUpdateHash = updatePackage.optString(CodePushConstants.PACKAGE_HASH_KEY, null);
        String newUpdateFolderPath = getPackageFolderPath(newUpdateHash);
        if (FileUtils.fileAtPathExists(newUpdateFolderPath)) {

            /* This removes any stale data in <code>newPackageFolderPath</code> that could have been left
             * uncleared due to a crash or error during the download or install process. */
            try {
                FileUtils.deleteDirectoryAtPath(newUpdateFolderPath);
            } catch (IOException e) {
                throw new CodePushDownloadPackageException(e);
            }
        }
        final String downloadUrlString = updatePackage.optString(CodePushConstants.DOWNLOAD_URL_KEY, null);
        File downloadFolder = new File(getCodePushPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);

        /* Download the file while checking if it is a zip and notifying client of progress. */
        packageDownloader.setParameters(downloadUrlString, downloadFilePath, progressCallback);
        packageDownloader.execute();
        CodePushDownloadPackageResult downloadPackageResult;
        try {
            downloadPackageResult = packageDownloader.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CodePushDownloadPackageException(e);
        }
        return downloadPackageResult;
    }

    /**
     * Unzips the following package file.
     *
     * @param downloadFile package file.
     * @throws CodePushUnzipException an exception occurred during unzipping.
     */
    public void unzipPackage(File downloadFile) throws CodePushUnzipException {
        String unzippedFolderPath = getUnzippedFolderPath();
        try {
            FileUtils.unzipFile(downloadFile, new File(unzippedFolderPath));
        } catch (IOException e) {
            throw new CodePushUnzipException(e);
        }
        FileUtils.deleteFileOrFolderSilently(downloadFile);
    }

    /**
     * Merges contents with the current update based on the manifest.
     *
     * @param newUpdateHash              hash of the new update package.
     * @param stringPublicKey            public key used to verify signature.
     *                                   Can be <code>null</code> if code signing is not enabled.
     * @param expectedEntryPointFileName file name of the entry app point.
     * @return actual new app entry point.
     * @throws CodePushMergeException an exception occurred during merging.
     */
    public String mergeDiff(String newUpdateHash, String stringPublicKey, String expectedEntryPointFileName) throws CodePushMergeException {
        String newUpdateFolderPath = getPackageFolderPath(newUpdateHash);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        String unzippedFolderPath = getUnzippedFolderPath();
        String diffManifestFilePath = FileUtils.appendPathComponent(unzippedFolderPath, CodePushConstants.DIFF_MANIFEST_FILE_NAME);

        /* If this is a diff, not full update, copy the new files to the package directory. */
        boolean isDiffUpdate = FileUtils.fileAtPathExists(diffManifestFilePath);
        try {
            if (isDiffUpdate) {
                String currentPackageFolderPath = getCurrentPackageFolderPath();
                if (currentPackageFolderPath != null) {
                    CodePushUpdateUtils.copyNecessaryFilesFromCurrentPackage(diffManifestFilePath, currentPackageFolderPath, newUpdateFolderPath);
                }
                File diffManifestFile = new File(diffManifestFilePath);
                diffManifestFile.delete();
            }
            FileUtils.copyDirectoryContents(new File(unzippedFolderPath), new File(newUpdateFolderPath));
            FileUtils.deleteDirectoryAtPath(unzippedFolderPath);
        } catch (IOException | CodePushMalformedDataException | JSONException e) {
            throw new CodePushMergeException(e);
        }
        String appEntryPoint = CodePushUpdateUtils.findEntryPointInUpdateContents(newUpdateFolderPath, expectedEntryPointFileName);
        if (appEntryPoint == null) {
            throw new CodePushMergeException("Update is invalid - An entry point file named \"" + expectedEntryPointFileName + "\" could not be found within the downloaded contents. Please check that you are releasing your CodePush updates using the exact same JS entry point file name that was shipped with your app's binary.");
        } else {
            if (FileUtils.fileAtPathExists(newUpdateMetadataPath)) {
                File metadataFileFromOldUpdate = new File(newUpdateMetadataPath);
                metadataFileFromOldUpdate.delete();
            }
            if (isDiffUpdate) {
                AppCenterLog.logAssert(CodePush.LOG_TAG, "Applying diff update.");
            } else {
                AppCenterLog.logAssert(CodePush.LOG_TAG, "Applying full update.");
            }
            try {
                verifySignature(stringPublicKey, newUpdateHash, isDiffUpdate);
            } catch (CodePushSignatureVerificationException e) {
                throw new CodePushMergeException(e);
            }
            return appEntryPoint;
        }
    }

    /**
     * Verifies package signature if code signing is enabled.
     *
     * @param stringPublicKey public key used to verify signature.
     *                        Can be <code>null</code> if code signing is not enabled.
     * @param newUpdateHash   hash of the update package.
     * @param isDiffUpdate    <code>true</code> if this is a diff update, <code>false</code> if this is a full update.
     * @throws CodePushSignatureVerificationException an exception during verifying package signature.
     */
    public void verifySignature(String stringPublicKey, String newUpdateHash, boolean isDiffUpdate) throws CodePushSignatureVerificationException {
        try {
            String newUpdateFolderPath = getPackageFolderPath(newUpdateHash);
            boolean isSignatureVerificationEnabled = (stringPublicKey != null);
            String signaturePath = CodePushUpdateUtils.getJWTFilePath(newUpdateFolderPath);
            boolean isSignatureAppearedInApp = FileUtils.fileAtPathExists(signaturePath);
            if (isSignatureVerificationEnabled) {
                if (isSignatureAppearedInApp) {
                    CodePushUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                    CodePushUpdateUtils.verifyUpdateSignature(newUpdateFolderPath, newUpdateHash, stringPublicKey);
                } else {
                    throw new CodePushSignatureVerificationException(SignatureExceptionType.NO_SIGNATURE);
                }
            } else {
                if (isSignatureAppearedInApp) {
                    AppCenterLog.logAssert(CodePush.LOG_TAG,
                            "Warning! JWT signature exists in codepush update but code integrity check couldn't be performed because there is no public key configured. "
                                    + "Please ensure that public key is properly configured within your application."
                    );
                    CodePushUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                } else {
                    if (isDiffUpdate) {
                        CodePushUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                    }
                }
            }
        } catch (IOException e) {
            throw new CodePushSignatureVerificationException(e);
        }
    }
}
