package com.microsoft.codepush.react;

import android.content.Context;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CodePushPackage {

    public final String CODE_PUSH_FOLDER_PREFIX = "CodePush";
    public final String STATUS_FILE = "codepush.json";
    public final String UPDATE_BUNDLE_FILE_NAME = "app.jsbundle";
    public final String CURRENT_PACKAGE_KEY = "currentPackage";
    public final String PREVIOUS_PACKAGE_KEY = "previousPackage";
    public final String PACKAGE_FILE_NAME = "app.json";
    public final String PACKAGE_HASH_KEY = "packageHash";
    public final String DOWNLOAD_URL_KEY = "downloadUrl";
    public final int DOWNLOAD_BUFFER_SIZE = 1024 * 256;

    private String documentsDirectory;

    public CodePushPackage(String documentsDirectory) {
        this.documentsDirectory = documentsDirectory;
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

    public WritableMap getCurrentPackageInfo() throws IOException {
        String statusFilePath = getStatusFilePath();
        if (!CodePushUtils.fileAtPathExists(statusFilePath)) {
            return new WritableNativeMap();
        }

        return CodePushUtils.getWritableMapFromFile(statusFilePath);
    }

    public void updateCurrentPackageInfo(ReadableMap packageInfo) throws IOException {
        CodePushUtils.writeReadableMapToFile(packageInfo, getStatusFilePath());
    }

    public String getCurrentPackageFolderPath() throws IOException {
        WritableMap info = getCurrentPackageInfo();
        String packageHash = CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY);
        if (packageHash == null) {
            return null;
        }

        return getPackageFolderPath(packageHash);
    }

    public String getCurrentPackageBundlePath() throws IOException {
        String packageFolder = getCurrentPackageFolderPath();
        if (packageFolder == null) {
            return null;
        }

        return CodePushUtils.appendPathComponent(packageFolder, UPDATE_BUNDLE_FILE_NAME);
    }

    public String getPackageFolderPath(String packageHash) {
        return CodePushUtils.appendPathComponent(getCodePushPath(), packageHash);
    }

    public String getCurrentPackageHash() throws IOException {
        WritableMap info = getCurrentPackageInfo();
        return CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY);
    }

    public String getPreviousPackageHash() throws IOException {
        WritableMap info = getCurrentPackageInfo();
        return CodePushUtils.tryGetString(info, PREVIOUS_PACKAGE_KEY);
    }

    public WritableMap getCurrentPackage() throws IOException {
        String folderPath = getCurrentPackageFolderPath();
        if (folderPath == null) {
            return new WritableNativeMap();
        }

        String packagePath = CodePushUtils.appendPathComponent(folderPath, PACKAGE_FILE_NAME);
        try {
            return CodePushUtils.getWritableMapFromFile(packagePath);
        } catch (IOException e) {
            // There is no current package.
            return null;
        }
    }

    public WritableMap getPackage(String packageHash) throws IOException {
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

        String packageFolderPath = getPackageFolderPath(CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY));
        String downloadUrlString = CodePushUtils.tryGetString(updatePackage, DOWNLOAD_URL_KEY);

        URL downloadUrl = null;
        HttpURLConnection connection = null;
        BufferedInputStream bin = null;
        FileOutputStream fos = null;
        BufferedOutputStream bout = null;

        try {
            downloadUrl = new URL(downloadUrlString);
            connection = (HttpURLConnection) (downloadUrl.openConnection());

            long totalBytes = connection.getContentLength();
            long receivedBytes = 0;

            bin = new BufferedInputStream(connection.getInputStream());
            File downloadFolder = new File(packageFolderPath);
            downloadFolder.mkdirs();
            File downloadFile = new File(downloadFolder, UPDATE_BUNDLE_FILE_NAME);
            fos = new FileOutputStream(downloadFile);
            bout = new BufferedOutputStream(fos, DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
            int numBytesRead = 0;
            while ((numBytesRead = bin.read(data, 0, DOWNLOAD_BUFFER_SIZE)) >= 0) {
                receivedBytes += numBytesRead;
                bout.write(data, 0, numBytesRead);
                progressCallback.call(new DownloadProgress(totalBytes, receivedBytes));
            }

            assert totalBytes == receivedBytes;

            String bundlePath = CodePushUtils.appendPathComponent(packageFolderPath, PACKAGE_FILE_NAME);
            CodePushUtils.writeReadableMapToFile(updatePackage, bundlePath);
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
    }

    public void installPackage(ReadableMap updatePackage) throws IOException {
        String packageHash = CodePushUtils.tryGetString(updatePackage, PACKAGE_HASH_KEY);
        WritableMap info = getCurrentPackageInfo();
        String previousPackageHash = getPreviousPackageHash();
        if (previousPackageHash != null && !previousPackageHash.equals(packageHash)) {
            CodePushUtils.deleteDirectoryAtPath(getPackageFolderPath(previousPackageHash));
        }

        info.putString(PREVIOUS_PACKAGE_KEY, CodePushUtils.tryGetString(info, CURRENT_PACKAGE_KEY));
        info.putString(CURRENT_PACKAGE_KEY, packageHash);
        updateCurrentPackageInfo(info);
    }

    public void rollbackPackage() throws IOException {
        WritableMap info = getCurrentPackageInfo();
        String currentPackageFolderPath = getCurrentPackageFolderPath();
        CodePushUtils.deleteDirectoryAtPath(currentPackageFolderPath);
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

    public void clearTestUpdates() {
        if (CodePush.isUsingTestConfiguration()) {
            File statusFile = new File(getStatusFilePath());
            statusFile.delete();
            CodePushUtils.deleteDirectoryAtPath(getCodePushPath());
        }
    }
}
