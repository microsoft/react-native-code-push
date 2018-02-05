package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.utils.PlatformUtils;

import java.io.File;

/**
 * Platform specific implementation of utils (only for testing).
 */
public class CommonTestPlatformUtils implements PlatformUtils {

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

    /**
     * Returns folder path of update package (cordova-specific for testing).
     *
     * @param hash hash of the desired update.
     * @return test folder path of update package.
     */
    @Override public String getUpdateFolderPath(String hash) {
        return new File(new File(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX), hash), "www").getPath();
    }
}
