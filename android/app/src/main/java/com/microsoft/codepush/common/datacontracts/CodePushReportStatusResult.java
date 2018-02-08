package com.microsoft.codepush.common.datacontracts;

/**
 * Represents the result of sending status report to server.
 */
public class CodePushReportStatusResult {

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
