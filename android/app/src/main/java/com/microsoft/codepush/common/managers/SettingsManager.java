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
 * Manager responsible for saving and retrieving settings in local repository.
 */
public class SettingsManager {

    /**
     * Key for getting/storing info about failed CodePush updates.
     */
    private static final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";

    /**
     * Key for getting/storing info about pending CodePush update.
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
     * Gets an array with containing failed updates info arranged by time of the failure ascending.
     * Each item represents an instance of {@link CodePushLocalPackage} that has failed to update.
     *
     * @return an array of failed updates.
     */
    public ArrayList<CodePushLocalPackage> getFailedUpdates() {
        String failedUpdatesString = mSettings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Arrays.asList(CodePushUtils.convertStringToObject(failedUpdatesString, CodePushLocalPackage[].class)));
        } catch (JsonSyntaxException e) {

            /* Unrecognized data format, clear and replace with expected format. */
            AppCenterLog.error(LOG_TAG, "Unable to parse failed updates metadata " + failedUpdatesString +
                    " stored in SharedPreferences");
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
            AppCenterLog.error(LOG_TAG, "Unable to parse pending update metadata " + pendingUpdateString +
                    " stored in SharedPreferences");
            return null;
        }
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash, <code>false</code> otherwise.
     */
    public boolean existsFailedUpdate(String packageHash) {
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
     * Checks whether there is a pending update with the provided hash.
     * Pass <code>null</code> to check if there is any pending update.
     *
     * @param packageHash expected package hash of the pending update.
     * @return <code>true</code> if there is a pending update with the provided hash.
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
     * Removes information about the pending update.
     */
    public void removePendingUpdate() {
        mSettings.edit().remove(PENDING_UPDATE_KEY).apply();
    }

    /**
     * Adds another failed update info to the list of failed updates.
     *
     * @param failedPackage instance of failed {@link CodePushLocalPackage}.
     */
    public void saveFailedUpdate(CodePushLocalPackage failedPackage) {
        ArrayList<CodePushLocalPackage> failedUpdates = getFailedUpdates();
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
