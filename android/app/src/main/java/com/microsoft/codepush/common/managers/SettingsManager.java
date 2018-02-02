package com.microsoft.codepush.common.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.utils.CodePushUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * Manager responsible for saving and retrieving settings.
 */
public class SettingsManager {

    /**
     * Key to store info about failed CodePush updates.
     */
    private static final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";

    /**
     * Key for getting/storing pending CodePush update.
     */
    private static final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";

    /**
     * Instance of {@link SharedPreferences}.
     */
    private SharedPreferences mSettings;

    /**
     * Creates an instance of {@link SettingsManager} with {@link Context} provided.
     *
     * @param applicationContext current application context.
     */
    public SettingsManager(Context applicationContext) {
        mSettings = applicationContext.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
    }

    /**
     * Gets an array with failed updates info.
     *
     * @return an array with failed updates info.
     */
    public List<CodePushLocalPackage> getFailedUpdates() {
        String failedUpdatesString = mSettings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new ArrayList<>();
        }
        try {
            return Arrays.asList(CodePushUtils.convertStringToObject(failedUpdatesString, CodePushLocalPackage[].class));
        } catch (JsonSyntaxException e) {

            /* Unrecognized data format, clear and replace with expected format. */
            List<CodePushLocalPackage> emptyArray = new ArrayList<>();
            mSettings.edit().putString(FAILED_UPDATES_KEY, CodePushUtils.convertObjectToJsonString(emptyArray)).apply();
            return new ArrayList<>();
        }
    }

    /**
     * Gets object with pending update info.
     *
     * @return object with pending update info.
     */
    public CodePushPendingUpdate getPendingUpdate() {
        String pendingUpdateString = mSettings.getString(PENDING_UPDATE_KEY, null);
        if (pendingUpdateString == null) {
            return null;
        }
        try {
            return CodePushUtils.convertStringToObject(pendingUpdateString, CodePushPendingUpdate.class);
        } catch (JsonSyntaxException e) {
            AppCenterLog.logAssert(LOG_TAG, "Unable to parse pending update metadata " + pendingUpdateString +
                    " stored in SharedPreferences");
            return null;
        }
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash.
     */
    public boolean isFailedHash(String packageHash) {
        List<CodePushLocalPackage> failedUpdates = getFailedUpdates();
        if (packageHash != null) {
            for (CodePushLocalPackage failedPackage : failedUpdates) {
                if (packageHash.equals(failedPackage.getPackageHash())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether there is a pending update.
     *
     * @param packageHash expected package hash of the pending update.
     * @return whether there is a pending update with the provided hash.
     */
    public boolean isPendingUpdate(String packageHash) {
        CodePushPendingUpdate pendingUpdate = getPendingUpdate();
        return pendingUpdate != null && pendingUpdate.isPendingUpdateLoading() &&
                (packageHash == null || pendingUpdate.getPendingUpdateHash().equals(packageHash));
    }

    /**
     * Removes information about failed updates.
     */
    public void removeFailedUpdates() {
        mSettings.edit().remove(FAILED_UPDATES_KEY).apply();
    }

    /**
     * Removes information about pending update.
     */
    public void removePendingUpdate() {
        mSettings.edit().remove(PENDING_UPDATE_KEY).apply();
    }

    /**
     * Adds another failed updates to the list of failed updates.
     *
     * @param failedPackage failed update package.
     */
    public void saveFailedUpdate(CodePushLocalPackage failedPackage) {
        List<CodePushLocalPackage> failedUpdates = getFailedUpdates();
        failedUpdates.add(failedPackage);
        String failedUpdatesString = CodePushUtils.convertObjectToJsonString(failedUpdates);
        mSettings.edit().putString(FAILED_UPDATES_KEY, failedUpdatesString).apply();
    }

    /**
     * Saves information about the pending update.
     *
     * @param pendingUpdate instance of the {@link CodePushPendingUpdate}.
     */
    public void savePendingUpdate(CodePushPendingUpdate pendingUpdate) {
        mSettings.edit().putString(PENDING_UPDATE_KEY, CodePushUtils.convertObjectToJsonString(pendingUpdate)).apply();
    }
}
