package com.microsoft.codepush.common.testutils;

import android.content.Context;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

/**
 * Platform specific implementation of utils (only for testing).
 */
public class CommonTestPlatformUtils implements CodePushPlatformUtils {

    /**
     * Instance of the utils implementation (singleton).
     */
    private static CommonTestPlatformUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private CommonTestPlatformUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static CommonTestPlatformUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CommonTestPlatformUtils();
        }
        return INSTANCE;
    }

    //TODO Implement test for this method
    @Override
    public boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion, Context context) {
        return false;
    }

    //TODO Implement test for this method
    @Override
    public long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException {
        return 0;
    }

    //TODO Implement test for this method
    @Override
    public void clearDebugCache(Context context) {

    }
}
