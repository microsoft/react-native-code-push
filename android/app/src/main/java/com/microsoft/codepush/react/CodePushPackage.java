package com.microsoft.codepush.react;

import android.content.Context;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

public class CodePushPackage {

    public final String CODE_PUSH_FOLDER_PREFIX = "CodePush";
    public final String CURRENT_PACKAGE_KEY = "currentPackage";
    public final String DIFF_MANIFEST_FILE_NAME = "hotcodepush.json";
    public final int DOWNLOAD_BUFFER_SIZE = 1024 * 256;
    public final String DOWNLOAD_FILE_NAME = "download.zip";
    public final String DOWNLOAD_URL_KEY = "downloadUrl";
    public final String PACKAGE_FILE_NAME = "app.json";
    public final String PACKAGE_HASH_KEY = "packageHash";
    public final String PREVIOUS_PACKAGE_KEY = "previousPackage";
    public final String RELATIVE_BUNDLE_PATH_KEY = "bundlePath";
    public final String STATUS_FILE = "codepush.json";
    public final String UNZIPPED_FOLDER_NAME = "unzipped";
    public final String UPDATE_BUNDLE_FILE_NAME = "app.jsbundle";

    private String documentsDirectory;

    public CodePushPackage(String documentsDirectory) {
        this.documentsDirectory = documentsDirectory;
    }

    public String getDownloadFilePath() {
        return CodePushUtils.appendPathComponent(getCodePushPath(), DOWNLOAD_FILE_NAME);
    }

    public String getUnzippedFolderPath() {
        return CodePushUtils.appendPathComponent(getCodePushPath(), UNZIPPED_FOLDER_NAME);
    }

    public String getDocumentsDirectory() {
        return documentsDirectory;
    }

    public String getCodePushPath() {
        String codePushPath = CodePushUtils.appendPathComponent(getDocumentsDirectory(), CODE_PUSH_FOLDER_PREFIX);
        if (CodePush.isUsingTestConfiguration()) {
            codePushPath = CodePushUtils.appendPathComponent(codePushPath, "TestPackages");
        }

        return codePushPath;
    }

    public String getStatusFilePath() {
        return CodePushUtils.appendPathComponent(getCodePushPath(), STATUS_FILE);
    }

    public WritableMap getCurrentPackageInfo() {
        String statusFilePath = getStatusFilePath();
        if (!FileUtils.fileAtPathExists(statusFilePath)) {
            return new WritableNativeMap();
        }

        try {
            return CodePushUtils.getWritableMapFromFile(statusFilePath);
        } catch (IOException e) {
            throw new CodePushUnknownException("Error getting current package info" , e);
        }
    }

    public void updateCurrentPackageInfo(ReadableMap packageInfo) {
        try {
            CodePushUtils.writeReadableMapToFile(packageInfo, getStatusFilePath());
        } catch (IOException e) {
            throw new CodePushUnknownException("Error updating current package info" , e);
        }
    }

    public String getCurrentPackageFolderPath() {
        WritableMap info = getCurrentPackageInfo();
        String packageHash = CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY);
        if (packageHash == null) {
            return null;
        }

        return getPackageFolderPath(packageHash);
    }

    public String getCurrentPackageBundlePath() {
        String packageFolder = getCurrentPackageFolderPath();
        if (packageFolder == null) {
            return null;
        }

        WritableMap currentPackage = getCurrentPackage();
        String relativeBundlePath = CodePushUtils.tryGetString(currentPackage, RELATIVE_BUNDLE_PATH_KEY);
        if (relativeBundlePath == null) {
            return CodePushUtils.appendPathComponent(packageFolder, UPDATE_BUNDLE_FILE_NAME);
        } else {
            return CodePushUtils.appendPathComponent(packageFolder, relativeBundlePath);
        }
    }

    public String getPackageFolderPath(String packageHash) {
        return CodePushUtils.appendPathComponent(getCodePushPath(), packageHash);
    }

    public String getCurrentPackageHash() {
        WritableMap info = getCurrentPackageInfo();
        return CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY);
    }

    public String getPreviousPackageHash() {
        WritableMap info = getCurrentPackageInfo();
        return CodePushUtils.tryGetString(info, PREVIOUS_PACKAGE_KEY);
    }

    public WritableMap getCurrentPackage() {
        String folderPath = getCurrentPackageFolderPath();
        if (folderPath == null) {
            return null;
        }

        String packagePath = CodePushUtils.appendPathComponent(folderPath, PACKAGE_FILE_NAME);
        try {
            return CodePushUtils.getWritableMapFromFile(packagePath);
        } catch (IOException e) {
            // Should not happen unless the update metadata was somehow deleted.
            return null;
        }
    }

    public WritableMap getPackage(String packageHash) {
        String folderPath = getPackageFolderPath(packageHash);
        String packageFilePath = CodePushUtils.appendPathComponent(folderPath, PACKAGE_FILE_NAME);
        try {
            return CodePushUtils.getWritableMapFromFile(packageFilePath);
        } catch (IOException e) {
            return null;
        }
    }

    public void downloadPackage(Context applicationContext, ReadableMap updatePackage,
                                DownloadProgressCallback progressCallback) throws IOException {
        String newUpdateHash = CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY);
        String newUpdateFolderPath = getPackageFolderPath(newUpdateHash);
        String newUpdateMetadataPath = CodePushUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
        if (FileUtils.fileAtPathExists(newUpdateFolderPath)) {
            // This removes any stale data in newPackageFolderPath that could have been left
            // uncleared due to a crash or error during the download or install process.
            FileUtils.deleteDirectoryAtPath(newUpdateFolderPath);
        }

        String downloadUrlString = CodePushUtils.tryGetString(updatePackage, DOWNLOAD_URL_KEY);
        URL downloadUrl = null;
        HttpURLConnection connection = null;
        BufferedInputStream bin = null;
        FileOutputStream fos = null;
        BufferedOutputStream bout = null;
        File downloadFile = null;
        boolean isZip = false;

        // Download the file while checking if it is a zip and notifying client of progress.
        try {
            downloadUrl = new URL(downloadUrlString);
            connection = (HttpURLConnection) (downloadUrl.openConnection());

            long totalBytes = connection.getContentLength();
            long receivedBytes = 0;

            bin = new BufferedInputStream(connection.getInputStream());
            File downloadFolder = new File(getCodePushPath());
            downloadFolder.mkdirs();
            downloadFile = new File(downloadFolder, DOWNLOAD_FILE_NAME);
            fos = new FileOutputStream(downloadFile);
            bout = new BufferedOutputStream(fos, DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
            byte[] header = new byte[4];

            int numBytesRead = 0;
            while ((numBytesRead = bin.read(data, 0, DOWNLOAD_BUFFER_SIZE)) >= 0) {
                if (receivedBytes < 4) {
                    for (int i = 0; i < numBytesRead; i++) {
                        int headerOffset = (int)(receivedBytes) + i;
                        if (headerOffset >= 4) {
                            break;
                        }

                        header[headerOffset] = data[i];
                    }
                }

                receivedBytes += numBytesRead;
                bout.write(data, 0, numBytesRead);
                progressCallback.call(new DownloadProgress(totalBytes, receivedBytes));
            }

            assert totalBytes == receivedBytes;
            isZip = ByteBuffer.wrap(header).getInt() == 0x504b0304;
        } catch (MalformedURLException e) {
            throw new CodePushMalformedDataException(downloadUrlString, e);
        } finally {
            try {
                if (bout != null) bout.close();
                if (fos != null) fos.close();
                if (bin != null) bin.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                throw new CodePushUnknownException("Error closing IO resources.", e);
            }
        }

        if (isZip) {
            // Unzip the downloaded file and then delete the zip
            String unzippedFolderPath = getUnzippedFolderPath();
            FileUtils.unzipFile(downloadFile, unzippedFolderPath);
            FileUtils.deleteFileOrFolderSilently(downloadFile);

            // Merge contents with current update based on the manifest
            String diffManifestFilePath = CodePushUtils.appendPathComponent(unzippedFolderPath,
                    DIFF_MANIFEST_FILE_NAME);
            boolean isDiffUpdate = FileUtils.fileAtPathExists(diffManifestFilePath);
            if (isDiffUpdate) {
                String currentPackageFolderPath = getCurrentPackageFolderPath();
                CodePushUpdateUtils.copyNecessaryFilesFromCurrentPackage(diffManifestFilePath, currentPackageFolderPath, newUpdateFolderPath);
                File diffManifestFile = new File(diffManifestFilePath);
                diffManifestFile.delete();
            }

            FileUtils.copyDirectoryContents(unzippedFolderPath, newUpdateFolderPath);
            FileUtils.deleteFileAtPathSilently(unzippedFolderPath);

            // For zip updates, we need to find the relative path to the jsBundle and save it in the
            // metadata so that we can find and run it easily the next time.
            String relativeBundlePath = CodePushUpdateUtils.findJSBundleInUpdateContents(newUpdateFolderPath);

            if (relativeBundlePath == null) {
                throw new CodePushInvalidUpdateException("Update is invalid - no files with extension .bundle, .js or .jsbundle were found in the update package.");
            } else {
                if (FileUtils.fileAtPathExists(newUpdateMetadataPath)) {
                    File metadataFileFromOldUpdate = new File(newUpdateMetadataPath);
                    metadataFileFromOldUpdate.delete();
                }

                if (isDiffUpdate) {
                    CodePushUpdateUtils.verifyHashForDiffUpdate(newUpdateFolderPath, newUpdateHash);
                }

                JSONObject updatePackageJSON = CodePushUtils.convertReadableToJsonObject(updatePackage);
                try {
                    updatePackageJSON.put(RELATIVE_BUNDLE_PATH_KEY, relativeBundlePath);
                } catch (JSONException e) {
                    throw new CodePushUnknownException("Unable to set key " +
                            RELATIVE_BUNDLE_PATH_KEY + " to value " + relativeBundlePath +
                            " in update package.", e);
                }

                updatePackage = CodePushUtils.convertJsonObjectToWriteable(updatePackageJSON);
            }
        } else {
            // File is a jsbundle, move it to a folder with the packageHash as its name
            FileUtils.moveFile(downloadFile, newUpdateFolderPath, UPDATE_BUNDLE_FILE_NAME);
        }

        // Save metadata to the folder.
        CodePushUtils.writeReadableMapToFile(updatePackage, newUpdateMetadataPath);
    }

    public void installPackage(ReadableMap updatePackage, boolean removePendingUpdate) throws IOException {
        String packageHash = CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY);
        WritableMap info = getCurrentPackageInfo();
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

            info.putString(PREVIOUS_PACKAGE_KEY, CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY));
        }

        info.putString(CURRENT_PACKAGE_KEY, packageHash);
        updateCurrentPackageInfo(info);
    }

    public void rollbackPackage() {
        WritableMap info = getCurrentPackageInfo();
        String currentPackageFolderPath = getCurrentPackageFolderPath();
        FileUtils.deleteDirectoryAtPath(currentPackageFolderPath);
        info.putString(CURRENT_PACKAGE_KEY, CodePushUtils.tryGetString(info, PREVIOUS_PACKAGE_KEY));
        info.putNull(PREVIOUS_PACKAGE_KEY);
        updateCurrentPackageInfo(info);
    }

    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) throws IOException {
        URL downloadUrl;
        HttpURLConnection connection = null;
        BufferedInputStream bin = null;
        FileOutputStream fos = null;
        BufferedOutputStream bout = null;
        try {
            downloadUrl = new URL(remoteBundleUrl);
            connection = (HttpURLConnection) (downloadUrl.openConnection());
            bin = new BufferedInputStream(connection.getInputStream());
            File downloadFile = new File(getCurrentPackageBundlePath());
            downloadFile.delete();
            fos = new FileOutputStream(downloadFile);
            bout = new BufferedOutputStream(fos, DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
            int numBytesRead = 0;
            while ((numBytesRead = bin.read(data, 0, DOWNLOAD_BUFFER_SIZE)) >= 0) {
                bout.write(data, 0, numBytesRead);
            }
        } catch (MalformedURLException e) {
            throw new CodePushMalformedDataException(remoteBundleUrl, e);
        } finally {
            try {
                if (bout != null) bout.close();
                if (fos != null) fos.close();
                if (bin != null) bin.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                throw new CodePushUnknownException("Error closing IO resources.", e);
            }
        }
    }

    public void clearUpdates() {
        File statusFile = new File(getStatusFilePath());
        statusFile.delete();
        FileUtils.deleteDirectoryAtPath(getCodePushPath());
    }
}
