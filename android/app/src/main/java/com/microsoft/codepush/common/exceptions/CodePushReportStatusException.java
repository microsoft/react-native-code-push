package com.microsoft.codepush.common.exceptions;

import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadStatusReport;

/**
 * An exception occurred during reporting the status to server.
 */
public class CodePushReportStatusException extends Exception {

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

    /**
     * Creates an instance of the exception with default detail message and specified cause and report type.
     *
     * @param cause      cause of exception.
     * @param reportType type of the sent report.
     */
    public CodePushReportStatusException(Throwable cause, ReportType reportType) {
        super(reportType.getMessage(), cause);
    }

    /**
     * Creates an instance of the exception with custom detail message and specified report type.
     *
     * @param result     result that came from server or any custom message.
     * @param reportType type of the sent report.
     */
    public CodePushReportStatusException(String result, ReportType reportType) {
        super(reportType.getMessage() + result);
    }
}
