package com.microsoft.codepush.common.interfaces;

import android.content.Context;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;

import java.io.IOException;

/**
 * Interface describing the methods that should be implemented in platform-specific instances of utils.
 * It can be implemented via platform-specific singleton.
 */
public interface CodePushPlatformUtils {

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
     * Gets binary version apk build time.
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
