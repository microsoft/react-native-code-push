package com.microsoft.codepush.common.utils;

/**
 * Interface describing the methods that should be implemented in platform-specific instances of utils.
 */
public interface PlatformUtils {

    /**
     * Gets path to update folder specific for each platform.
     * F. i., it is "CodePush/" for RN and "www/" for Cordova.
     *
     * @param hash hash of the desired update.
     * @return oath to update folder.
     */
    String getUpdateFolderPath(String hash);
}
