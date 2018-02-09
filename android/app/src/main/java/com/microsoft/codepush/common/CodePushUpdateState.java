package com.microsoft.codepush.common;

import com.microsoft.codepush.common.enums.CodePushInstallMode;

public class CodePushUpdateState {

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
}
