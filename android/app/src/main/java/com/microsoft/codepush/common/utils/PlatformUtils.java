package com.microsoft.codepush.common.utils;

/**
 * Interface describing the methods that should be implemented in platform-specific instances of utils.
 * It can be implemented via platform-specific singleton.
 */
public interface PlatformUtils {

    /**
     * Gets path of the update folder specific for each platform.
     * F. i., it should be "CodePush/" for RN and "www/" for Cordova.
     *
     * @param hash hash of the update needed to be retrieved.
     * @return path to update folder.
     */
    String getUpdateFolderPath(String hash);
}
