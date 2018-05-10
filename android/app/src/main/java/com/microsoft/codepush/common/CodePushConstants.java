package com.microsoft.codepush.common;

/**
 * Common set of the CodePush-specific constants.
 */
public final class CodePushConstants {
    
    /**
     * Key from <code>build.gradle</code> file for TimeStamp value.
     * TimeStamp represents the time when binary package has been build.
     */
    public static final String BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";

    /**
     * Default app name if not provided when building CodePush instance.
     */
    public static final String CODE_PUSH_DEFAULT_APP_NAME = "CodePush";

    /**
     * Root folder name inside each update.
     */
    public static final String CODE_PUSH_FOLDER_PREFIX = "CodePush";

    /**
     * Key for getting hash file for binary contents from assets folder.
     */
    public static final String CODE_PUSH_HASH_FILE_NAME = "CodePushHash";

    /**
     * The same as CODE_PUSH_HASH_FILE_NAME that used for backwards compatibility.
     */
    public static final String CODE_PUSH_OLD_HASH_FILE_NAME = "CodePushHash.json";

    /**
     * Key for getting CodePush shared preferences from application context.
     */
    public static final String CODE_PUSH_PREFERENCES = "CodePush";

    /**
     * File name for diff manifest that distributes with CodePush updates.
     */
    public static final String DIFF_MANIFEST_FILE_NAME = "hotcodepush.json";

    /**
     * Buffer size for downloading CodePush updates.
     */
    public static final int DOWNLOAD_BUFFER_SIZE = 1024 * 256;

    /**
     * Default file name for downloading CodePush updates.
     */
    public static final String DOWNLOAD_FILE_NAME = "download.zip";

    /**
     * Event name for dispatching CodePush sync status to JavaScript.
     * See {@link com.microsoft.codepush.common.enums.CodePushSyncStatus} for details.
     */
    public static final String SYNC_STATUS_EVENT_NAME = "CodePushSyncStatus";

    /**
     * Event name for dispatching CodePush download progress to JavaScript.
     */
    public static final String DOWNLOAD_PROGRESS_EVENT_NAME = "CodePushDownloadProgress";

    /**
     * Event name for dispatching to JavaScript CodePush update package that targets to other binary version.
     */
    public static final String BINARY_VERSION_MISMATCH_EVENT_NAME = "CodePushBinaryVersionMismatch";

    /**
     * Key for download url property from CodePush update manifest.
     */
    public static final String DOWNLOAD_URL_KEY = "downloadUrl";

    /**
     * Package file name to store CodePush update metadata file.
     */
    public static final String PACKAGE_FILE_NAME = "app.json";

    /**
     * Package hash key for running CodePush update.
     */
    public static final String PACKAGE_HASH_KEY = "packageHash";

    /**
     * Name of the file containing information about the available packages.
     */
    public static final String STATUS_FILE_NAME = "codepush.json";

    /**
     * Folder name for unzipped CodePush update.
     */
    public static final String UNZIPPED_FOLDER_NAME = "unzipped";

    /**
     * Key for getting binary resources modified time from <code>build.gradle</code> file.
     */
    public static final String CODE_PUSH_APK_BUILD_TIME_KEY = "CODE_PUSH_APK_BUILD_TIME";

    /**
     * File name for jwt file of signed CodePush update.
     */
    public static final String BUNDLE_JWT_FILE_NAME = ".codepushrelease";
}
