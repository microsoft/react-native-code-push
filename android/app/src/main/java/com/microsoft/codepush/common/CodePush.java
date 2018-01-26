package com.microsoft.codepush.common;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * CodePush service.
 */
public class CodePush extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "CodePush";

    /**
     * TAG used in logging for CodePush.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String ERROR_GROUP = "groupErrors";

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
