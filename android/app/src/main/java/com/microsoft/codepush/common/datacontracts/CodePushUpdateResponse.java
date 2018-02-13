package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

/**
 * A response class containing info about the update.
 */
public class CodePushUpdateResponse {

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
    public void setUpdateInfo(CodePushUpdateResponseUpdateInfo updateInfo) throws CodePushIllegalArgumentException {
        if (updateInfo != null) {
            this.updateInfo = updateInfo;
        } else {
            throw new CodePushIllegalArgumentException(this.getClass().getName(), "updateInfo");
        }
    }
}
