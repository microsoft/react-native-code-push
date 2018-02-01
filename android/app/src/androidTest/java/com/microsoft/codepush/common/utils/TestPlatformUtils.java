package com.microsoft.codepush.common.utils;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;

import java.io.File;

/**
 * Platform specific implementation of utils (only for testing).
 */
public class TestPlatformUtils implements PlatformUtils {
    @Override public String getUpdateFolderPath(String hash) {
        return new File(new File(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX), hash), "www").getPath();
    }
}
