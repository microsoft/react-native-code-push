package com.microsoft.codepush.common.enums;

import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;

/**
 * Type of the sent report.
 */
public enum ReportType {

    /**
     * {@link CodePushDownloadStatusReport}.
     */
    DOWNLOAD("Error occurred during delivering download status report."),

    /**
     * {@link CodePushDeploymentStatusReport}.
     */
    DEPLOY("Error occurred during delivering deploy status report.");

    /**
     * Message describing the exception depending on the report type.
     */
    private final String message;

    /**
     * Creates instance of the enum using the provided message.
     *
     * @param message message describing the exception.
     */
    ReportType(String message) {
        this.message = message;
    }

    /**
     * Gets the message of the specified type.
     *
     * @return message.
     */
    public String getMessage() {
        return this.message;
    }
}
