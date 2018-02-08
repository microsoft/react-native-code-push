package com.microsoft.codepush.common.exceptions;

import com.microsoft.codepush.common.enums.ReportType;

/**
 * An exception occurred during reporting the status to server.
 */
public class CodePushReportStatusException extends CodePushApiHttpRequestException {

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
