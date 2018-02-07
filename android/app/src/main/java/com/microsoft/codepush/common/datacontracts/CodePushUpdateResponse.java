package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;

/**
 * A response class containing info about the update.
 */
public class CodePushUpdateResponse {

    /**
     * Exception occurred when querying for update, <code>null</code> if query is successful.
     */
    private CodePushQueryUpdateException codePushQueryUpdateException;

    /**
     * Creates failed update response.
     *
     * @param codePushQueryUpdateException exception that has occurred.
     * @return instance of {@link CodePushUpdateResponse}.
     */
    public static CodePushUpdateResponse createFailed(CodePushQueryUpdateException codePushQueryUpdateException) {
        CodePushUpdateResponse codePushUpdateResponse = new CodePushUpdateResponse();
        codePushUpdateResponse.setCodePushQueryUpdateException(codePushQueryUpdateException);
        return codePushUpdateResponse;
    }

    /**
     * Information about the existing update.
     */
    @SerializedName("updateInfo")
    private CodePushUpdateResponseUpdateInfo updateInfo;

    /**
     * Gets the information about the existing update and returns it.
     *
     * @return information about the existing update.
     */
    public CodePushUpdateResponseUpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    /**
     * Sets the information about the existing update.
     *
     * @param updateInfo information about the existing update.
     */
    public void setUpdateInfo(CodePushUpdateResponseUpdateInfo updateInfo) {
        if (updateInfo != null) {
            this.updateInfo = updateInfo;
        } else {
            AppCenterLog.error(CodePush.LOG_TAG, "\"updateInfo\" property cannot be null.");
        }
    }

    /**
     * Gets the exception occurred when querying for update.
     *
     * @return exception occurred when querying for update.
     */
    public CodePushQueryUpdateException getCodePushQueryUpdateException() {
        return codePushQueryUpdateException;
    }

    /**
     * Checks whether the query has failed.
     *
     * @return <code>true</code> if an exception has occurred.
     */
    public boolean isFailed() {
        return codePushQueryUpdateException != null;
    }

    /**
     * Sets the exception occurred when querying for update.
     *
     * @param codePushQueryUpdateException exception occurred when querying for update.
     */
    public void setCodePushQueryUpdateException(CodePushQueryUpdateException codePushQueryUpdateException) {
        this.codePushQueryUpdateException = codePushQueryUpdateException;
    }
}
