package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Created by anna.kocheshkova on 2/2/2018.
 */

public class CodePushPendingUpdate {

    @SerializedName("isLoading")
    private boolean pendingUpdateIsLoading;
    @SerializedName("hash")
    private String pendingUpdateHash;

    /**
     * Gets the value of pendingUpdateIsLoading and returns it
     *
     * @return pendingUpdateIsLoading
     */
    public boolean isPendingUpdateLoading() {
        return pendingUpdateIsLoading;
    }

    /**
     * Sets the pendingUpdateIsLoading
     *
     * @param pendingUpdateIsLoading new value
     */
    public void setPendingUpdateIsLoading(boolean pendingUpdateIsLoading) {
        this.pendingUpdateIsLoading = pendingUpdateIsLoading;
    }

    /**
     * Gets the value of pendingUpdateHash and returns it
     *
     * @return pendingUpdateHash
     */
    public String getPendingUpdateHash() {
        return pendingUpdateHash;
    }

    /**
     * Sets the pendingUpdateHash
     *
     * @param pendingUpdateHash new value
     */
    public void setPendingUpdateHash(String pendingUpdateHash) {
        this.pendingUpdateHash = pendingUpdateHash;
    }
}
