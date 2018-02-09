package com.microsoft.codepush.react.utils;

import android.os.Environment;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.utils.PlatformUtils;

import java.io.File;

/**
 * React-specific instance of {@link PlatformUtils}.
 */
public class ReactPlatformUtils implements PlatformUtils {

    private static ReactPlatformUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private ReactPlatformUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static ReactPlatformUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ReactPlatformUtils();
        }
        return INSTANCE;
    }

    @Override
    public String getUpdateFolderPath(String hash) {
        File codePushFolder = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File packageFolder = new File(codePushFolder, hash);
        return new File(packageFolder, "CodePush").getPath();
    }
}
