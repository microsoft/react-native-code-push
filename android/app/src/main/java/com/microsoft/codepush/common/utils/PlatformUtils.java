package com.microsoft.codepush.common.utils;

import android.content.Context;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;

import java.io.IOException;

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

    /**
     * Checks whether the specified package is latest.
     *
     * @param packageMetadata   info about the package to be checked.
     * @param currentAppVersion version of the currently installed application.
     * @param context           application context.
     * @return <code>true</code> if package is latest.
     * @throws CodePushGeneralException some exception that might occur.
     */
    boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion, Context context) throws CodePushGeneralException;

    /**
     * gets binary version apk build time.
     *
     * @param context application context.
     * @return time in ms.
     * @throws NumberFormatException exception parsing time.
     */
    long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException;

    /**
     * Clears debug cache files.
     *
     * @param context application context.
     * @throws IOException exception occurred during read/write operations.
     */
    void clearDebugCache(Context context) throws IOException;
}
