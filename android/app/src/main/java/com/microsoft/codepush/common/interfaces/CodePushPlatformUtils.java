package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushPlatformUtilsException;

import java.io.IOException;

/**
 * Interface describing the methods that should be implemented in platform-specific instances of utils.
 * It can be implemented via platform-specific singleton.
 */
public interface CodePushPlatformUtils {

    /**
     * Gets path of the update folder specific for each platform.
     * F. i., it should be "CodePush/" for RN and "www/" for Cordova.
     *
     * @param hash hash of the update needed to be retrieved.
     * @return path to update folder.
     */
    String getUpdateFolderPath(String hash);

    /**
     * Indicates whether specified package is latest.
     *
     * @param packageMetadata   package metadata.
     * @param currentAppVersion current app version to compare with.
     * @return true, if package is latest, false otherwise.
     * @throws CodePushPlatformUtilsException if error occurred during the execution of operation.
     */
    boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion) throws CodePushPlatformUtilsException;

    /**
     * Returns information about package modified time from binary resources.
     *
     * @return information about package modified time from binary resources.
     * @throws CodePushPlatformUtilsException if error occurred during the execution of operation.
     */
    long getBinaryResourcesModifiedTime() throws CodePushPlatformUtilsException;

    /**
     * Clears cache that is used for debugging.
     *
     * @throws IOException if read/write error occurred while accessing the file system.
     */
    void clearDebugCache() throws IOException;
}
