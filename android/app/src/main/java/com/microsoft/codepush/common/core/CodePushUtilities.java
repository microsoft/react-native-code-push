package com.microsoft.codepush.common.core;

import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

/**
 * Encapsulates utilities that {@link CodePushBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class CodePushUtilities {

    /**
     * Instance of {@link CodePushUtils}.
     */
    public final CodePushUtils mUtils;

    /**
     * Instance of {@link FileUtils}.
     */
    public final FileUtils mFileUtils;

    /**
     * Instance of {@link CodePushUpdateUtils}.
     */
    public final CodePushUpdateUtils mUpdateUtils;

    /**
     * Instance of {@link CodePushPlatformUtils}.
     */
    public final CodePushPlatformUtils mPlatformUtils;

    /**
     * Create instance of CodePushUtilities.
     *
     * @param utils         instance of {@link CodePushUtils}.
     * @param fileUtils     instance of {@link FileUtils}.
     * @param updateUtils   instance of {@link CodePushUpdateUtils}.
     * @param platformUtils instance of {@link CodePushPlatformUtils}.
     */
    public CodePushUtilities(
            CodePushUtils utils,
            FileUtils fileUtils,
            CodePushUpdateUtils updateUtils,
            CodePushPlatformUtils platformUtils) {
        this.mUtils = utils;
        this.mFileUtils = fileUtils;
        this.mUpdateUtils = updateUtils;
        this.mPlatformUtils = platformUtils;
    }
}
