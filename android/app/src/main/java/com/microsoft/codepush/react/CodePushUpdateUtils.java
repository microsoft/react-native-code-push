package com.microsoft.codepush.react;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;

public class CodePushUpdateUtils {

    public static void copyNecessaryFilesFromCurrentPackage(String diffManifestFilePath, String currentPackageFolderPath, String newPackageFolderPath) throws IOException{
        FileUtils.copyDirectoryContents(currentPackageFolderPath, newPackageFolderPath);
        WritableMap diffManifest = CodePushUtils.getWritableMapFromFile(diffManifestFilePath);
        ReadableArray deletedFiles = diffManifest.getArray("deletedFiles");
        for (int i = 0; i < deletedFiles.size(); i++) {
            String fileNameToDelete = deletedFiles.getString(i);
            File fileToDelete = new File(newPackageFolderPath, fileNameToDelete);
            FileUtils.deleteFileSilently(fileToDelete);
        }
    }

    public static String findJSBundleInUpdateContents(String folderPath) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fullFilePath = CodePushUtils.appendPathComponent(folderPath, file.getName());
            if (file.isDirectory()) {
                String mainBundlePathInSubFolder = findJSBundleInUpdateContents(fullFilePath);
                if (mainBundlePathInSubFolder != null) {
                    return CodePushUtils.appendPathComponent(file.getName(), mainBundlePathInSubFolder);
                }
            } else {
                String fileName = file.getName();
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex >= 0) {
                    String fileExtension = fileName.substring(dotIndex + 1);
                    if (fileExtension.equals("bundle") || fileExtension.equals("js") || fileExtension.equals("jsbundle")) {
                        return fileName;
                    }
                }
            }
        }

        return null;
    }
}
