package com.microsoft.codepush.common.datacontracts;

import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;

/**
 * Represents the result of sending status report to server.
 */
public class CodePushReportStatusResult {

    /**
     * An exception that has occurred when sending the report, <code>null</code> if the report has been sent successfully.
     */
    private CodePushReportStatusException codePushReportStatusException;

    /**
     * The result string from server.
     */
    private String result;

    /**
     * Creates an instance of the {@link CodePushReportStatusResult} that has been successful.
     *
     * @param result result string from server.
     * @return instance of the class.
     */
    public static CodePushReportStatusResult createSuccessful(String result) {
        CodePushReportStatusResult codePushReportStatusResult = new CodePushReportStatusResult();
        codePushReportStatusResult.setResult(result);
        return codePushReportStatusResult;
    }

    /**
     * Creates an instance of the {@link CodePushReportStatusResult} that has failed.
     *
     * @param codePushReportStatusException an exception that has occurred when sending the report.
     * @return instance of the class.
     */
    public static CodePushReportStatusResult createFailed(CodePushReportStatusException codePushReportStatusException) {
        CodePushReportStatusResult codePushReportStatusResult = new CodePushReportStatusResult();
        codePushReportStatusResult.setCodePushReportStatusException(codePushReportStatusException);
        return codePushReportStatusResult;
    }

    /**
     * Gets the exception that has occurred when sending the report.
     *
     * @return the exception that has occurred when sending the report.
     */
    public CodePushReportStatusException getCodePushReportStatusException() {
        return codePushReportStatusException;
    }

    /**
     * Sets the exception that has occurred when sending the report.
     *
     * @param codePushReportStatusException the exception that has occurred when sending the report.
     */
    public void setCodePushReportStatusException(CodePushReportStatusException codePushReportStatusException) {
        this.codePushReportStatusException = codePushReportStatusException;
    }

    /**
     * Checks whether sending the report has failed.
     *
     * @return <code>true</code> if the report has not been sent successfully.
     */
    public boolean isFailed() {
        return codePushReportStatusException != null;
    }

    /**
     * Gets the result string from server.
     *
     * @return result string from server.
     */
    public String getResult() {
        return result;
    }

    /**
     * Sets the result string from server.
     *
     * @param result result string from server.
     */
    public void setResult(String result) {
        this.result = result;
    }
}
