package com.microsoft.codepush.common.core;

import com.microsoft.codepush.common.interfaces.CodePushBinaryVersionMismatchListener;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates listeners that {@link CodePushBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class CodePushListeners {

    /**
     * List of {@link CodePushSyncStatusListener}.
     */
    public final List<CodePushSyncStatusListener> mSyncStatusListeners;

    /**
     * List of {@link CodePushDownloadProgressListener}.
     */
    public final List<CodePushDownloadProgressListener> mDownloadProgressListeners;

    /**
     * List of {@link CodePushBinaryVersionMismatchListener}.
     */
    public final List<CodePushBinaryVersionMismatchListener> mBinaryVersionMismatchListeners;

    /**
     * Create instance of {@link CodePushListeners}.
     */
    public CodePushListeners() {
        mSyncStatusListeners = new ArrayList<>();
        mDownloadProgressListeners = new ArrayList<>();
        mBinaryVersionMismatchListeners = new ArrayList<>();
    }
}
