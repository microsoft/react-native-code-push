package com.microsoft.codepush.common.core;

import com.microsoft.codepush.common.enums.CodePushInstallMode;

/**
 * Encapsulates state of {@link CodePushBaseCore}.
 */
@SuppressWarnings("WeakerAccess")
public class CodePushState {

    /**
     * Indicates whether a new update running for the first time.
     */
    public boolean mDidUpdate;

    /**
     * Indicates whether there is a need to send rollback report.
     */
    public boolean mNeedToReportRollback;

    /**
     * Indicates whether current install mode.
     */
    public CodePushInstallMode mCurrentInstallModeInProgress;

    /**
     * Indicates whether is running binary version of app.
     */
    public boolean mIsRunningBinaryVersion;

    /**
     * Indicates whether sync already in progress.
     */
    public boolean mSyncInProgress;

    /**
     * Minimum background duration value.
     */
    public int mMinimumBackgroundDuration;
}
