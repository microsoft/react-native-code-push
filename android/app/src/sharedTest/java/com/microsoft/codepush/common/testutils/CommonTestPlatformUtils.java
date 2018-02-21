package com.microsoft.codepush.common.testutils;

import android.content.Context;
import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

import java.io.File;
import java.io.IOException;

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
    public boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion, Context context) throws CodePushGeneralException {
        return false;
    }

    //TODO Implement test for this method
    @Override
    public long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException {
        return 0;
    }

    //TODO Implement test for this method
    @Override
    public void clearDebugCache(Context context) throws IOException {

    }
}
