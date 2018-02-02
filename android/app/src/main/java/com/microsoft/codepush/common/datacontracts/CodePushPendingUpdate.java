package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Contains info about pending update.
 */
public class CodePushPendingUpdate {

    /**
     * Whether the update is loading.
     */
    @SerializedName("isLoading")
    private boolean pendingUpdateIsLoading;

    /**
     * Pending update package hash.
     */
    @SerializedName("hash")
    private String pendingUpdateHash;

    /**
     * Gets whether the update is loading.
     *
     * @return whether the update is loading.
     */
    public boolean isPendingUpdateLoading() {
        return pendingUpdateIsLoading;
    }

    /**
     * Sets whether the update is loading.
     *
     * @param pendingUpdateIsLoading whether the update is loading.
     */
    public void setPendingUpdateIsLoading(boolean pendingUpdateIsLoading) {
        this.pendingUpdateIsLoading = pendingUpdateIsLoading;
    }

    /**
     * Gets the value of pending update package hash and returns it
     *
     * @return pending update package hash.
     */
    public String getPendingUpdateHash() {
        return pendingUpdateHash;
    }

    /**
     * Sets the pending update package hash.
     *
     * @param pendingUpdateHash pending update package hash.
     */
    public void setPendingUpdateHash(String pendingUpdateHash) {
        this.pendingUpdateHash = pendingUpdateHash;
    }
}
