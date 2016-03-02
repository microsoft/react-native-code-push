package com.microsoft.codepush.react;

import android.app.Activity;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

public class CodePushUpdateUtils {

    private static final String CODE_PUSH_HASH_FILE_NAME = "CodePushHash.json";

    private static void addContentsOfFolderToManifest(String folderPath, String pathPrefix, ArrayList<String> manifest) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fullFilePath = file.getAbsolutePath();
            String relativePath = (pathPrefix.isEmpty() ? "" : (pathPrefix + "/")) + file.getName();
            if (file.isDirectory()) {
                addContentsOfFolderToManifest(fullFilePath, relativePath, manifest);
            } else {
                try {
                    manifest.add(relativePath + ":" + computeHash(new FileInputStream(file)));
                } catch (FileNotFoundException e) {
                    // Should not happen.
                    throw new CodePushUnknownException("Unable to compute hash of update contents.", e);
                }
            }
        }
    }

    private static String computeHash(InputStream dataStream) {
        MessageDigest messageDigest = null;
        DigestInputStream digestInputStream = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            digestInputStream = new DigestInputStream(dataStream, messageDigest);
            byte[] byteBuffer = new byte[1024 * 8];
            while (digestInputStream.read(byteBuffer) != -1);
        } catch (NoSuchAlgorithmException | IOException e) {
            // Should not happen.
            throw new CodePushUnknownException("Unable to compute hash of update contents.", e);
        } finally {
            try {
                if (digestInputStream != null) digestInputStream.close();
                if (dataStream != null) dataStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] hash = messageDigest.digest();
        return String.format("%064x", new java.math.BigInteger(1, hash));
    }

    public static void copyNecessaryFilesFromCurrentPackage(String diffManifestFilePath, String currentPackageFolderPath, String newPackageFolderPath) throws IOException{
        FileUtils.copyDirectoryContents(currentPackageFolderPath, newPackageFolderPath);
        WritableMap diffManifest = CodePushUtils.getWritableMapFromFile(diffManifestFilePath);
        ReadableArray deletedFiles = diffManifest.getArray("deletedFiles");
        for (int i = 0; i < deletedFiles.size(); i++) {
            String fileNameToDelete = deletedFiles.getString(i);
            File fileToDelete = new File(newPackageFolderPath, fileNameToDelete);
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
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

    public static String getHashForBinaryContents(Activity mainActivity, boolean isDebugMode) {
        try {
            return CodePushUtils.getStringFromInputStream(mainActivity.getAssets().open(CODE_PUSH_HASH_FILE_NAME));
        } catch (IOException e) {
            if (!isDebugMode) {
                // Only print this message in "Release" mode. In "Debug", we may not have the
                // hash if the build skips bundling the files.
                CodePushUtils.log("Unable to get the hash of the binary's bundled resources - \"codepush.gradle\" may have not been added to the build definition.");
            }

            return null;
        }
    }

    public static void verifyHashForDiffUpdate(String folderPath, String expectedHash) {
        ArrayList<String> updateContentsManifest = new ArrayList<String>();
        addContentsOfFolderToManifest(folderPath, "", updateContentsManifest);
        Collections.sort(updateContentsManifest);
        JSONArray updateContentsJSONArray = new JSONArray();
        for (String manifestEntry : updateContentsManifest) {
            updateContentsJSONArray.put(manifestEntry);
        }

        // The JSON serialization turns path separators into "\/", e.g. "CodePush\/assets\/image.png"
        String updateContentsManifestString = updateContentsJSONArray.toString().replace("\\/", "/");
        String updateContentsManifestHash = computeHash(new ByteArrayInputStream(updateContentsManifestString.getBytes()));
        if (!expectedHash.equals(updateContentsManifestHash)) {
            throw new CodePushInvalidUpdateException("The update contents failed the data integrity check.");
        }
    }
}
