package com.microsoft.codepush.common.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Contains information about packages available for user.
 */
public class CodePushPackageInfo {

    /**
     * Currently installed package hash.
     */
    @SerializedName("currentPackage")
    private String currentPackage;

    /**
     * Package hash of the update installed before the current.
     */
    @SerializedName("previousPackage")
    private String previousPackage;

    /**
     * Gets the value of currently installed package hash and returns it.
     *
     * @return currently installed package hash.
     */
    public String getCurrentPackage() {
        return currentPackage;
    }

    /**
     * Sets the currently installed package hash.
     *
     * @param currentPackage currently installed package hash.
     */
    public void setCurrentPackage(String currentPackage) {
        this.currentPackage = currentPackage;
    }

    /**
     * Gets the value of package hash of the update installed before the current and returns it.
     *
     * @return package hash of the update installed before the current.
     */
    public String getPreviousPackage() {
        return previousPackage;
    }

    /**
     * Sets the package hash of the update installed before the current.
     *
     * @param previousPackage package hash of the update installed before the current.
     */
    public void setPreviousPackage(String previousPackage) {
        this.previousPackage = previousPackage;
    }
}
