package com.microsoft.codepush.common.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.CodePushStatusReportIdentifier;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.utils.CodePushUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manager responsible for saving and retrieving settings in local repository.
 */
public class SettingsManager {

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Instance of {@link CodePushConfiguration} to work with.
     */
    private CodePushConfiguration mCodePushConfiguration;

    /**
     * Key for getting/storing info about failed CodePush updates.
     */
    private final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";

    /**
     * Key for getting/storing info about pending CodePush update.
     */
    private final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";

    /**
     * Key for storing last deployment report identifier.
     */
    private final String LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";

    /**
     * Key for storing last retry deployment report identifier.
     */
    private final String RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";

    /**
     * Instance of {@link SharedPreferences}.
     */
    private SharedPreferences mSettings;

    /**
     * Creates an instance of {@link SettingsManager} with {@link Context} provided.
     *
     * @param applicationContext current application context.
     * @param codePushUtils      instance of {@link CodePushUtils} to work with.
     * @param codePushConfiguration instance of {@link CodePushConfiguration} to work with.
     */
    public SettingsManager(Context applicationContext, CodePushUtils codePushUtils, CodePushConfiguration codePushConfiguration) {
        mCodePushUtils = codePushUtils;
        mCodePushConfiguration = codePushConfiguration;
        mSettings = applicationContext.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
    }

    /**
     * Gets an array with containing failed updates info arranged by time of the failure ascending.
     * Each item represents an instance of {@link CodePushPackage} that has failed to update.
     *
     * @return an array of failed updates.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public ArrayList<CodePushPackage> getFailedUpdates() throws CodePushMalformedDataException {
        String failedUpdatesString = mSettings.getString(getAppSpecificPrefix() + FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Arrays.asList(mCodePushUtils.convertStringToObject(failedUpdatesString, CodePushPackage[].class)));
        } catch (JsonSyntaxException e) {

            /* Unrecognized data format, clear and replace with expected format. */
            List<CodePushLocalPackage> emptyArray = new ArrayList<>();
            mSettings.edit().putString(getAppSpecificPrefix() + FAILED_UPDATES_KEY, mCodePushUtils.convertObjectToJsonString(emptyArray)).apply();
            throw new CodePushMalformedDataException("Unable to parse failed updates metadata " + failedUpdatesString + " stored in SharedPreferences", e);
        }
    }

    /**
     * Gets object with pending update info.
     *
     * @return object with pending update info.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public CodePushPendingUpdate getPendingUpdate() throws CodePushMalformedDataException {
        String pendingUpdateString = mSettings.getString(getAppSpecificPrefix() + PENDING_UPDATE_KEY, null);
        if (pendingUpdateString == null) {
            return null;
        }
        try {
            return mCodePushUtils.convertStringToObject(pendingUpdateString, CodePushPendingUpdate.class);
        } catch (JsonSyntaxException e) {
            throw new CodePushMalformedDataException("Unable to parse pending update metadata " + pendingUpdateString + " stored in SharedPreferences", e);
        }
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash, <code>false</code> otherwise.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public boolean existsFailedUpdate(String packageHash) throws CodePushMalformedDataException {
        List<CodePushPackage> failedUpdates = getFailedUpdates();
        if (packageHash != null) {
            for (CodePushPackage failedPackage : failedUpdates) {
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
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public boolean isPendingUpdate(String packageHash) throws CodePushMalformedDataException {
        CodePushPendingUpdate pendingUpdate = getPendingUpdate();
        return pendingUpdate != null && !pendingUpdate.isPendingUpdateLoading() &&
                (packageHash == null || pendingUpdate.getPendingUpdateHash().equals(packageHash));
    }

    /**
     * Removes information about failed updates.
     */
    public void removeFailedUpdates() {
        mSettings.edit().remove(getAppSpecificPrefix() + FAILED_UPDATES_KEY).apply();
    }

    /**
     * Removes information about the pending update.
     */
    public void removePendingUpdate() {
        mSettings.edit().remove(getAppSpecificPrefix() + PENDING_UPDATE_KEY).apply();
    }

    /**
     * Adds another failed update info to the list of failed updates.
     *
     * @param failedPackage instance of failed {@link CodePushRemotePackage}.
     * @throws CodePushMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public void saveFailedUpdate(CodePushPackage failedPackage) throws CodePushMalformedDataException {
        ArrayList<CodePushPackage> failedUpdates = getFailedUpdates();
        failedUpdates.add(failedPackage);
        String failedUpdatesString = mCodePushUtils.convertObjectToJsonString(failedUpdates);
        mSettings.edit().putString(getAppSpecificPrefix() + FAILED_UPDATES_KEY, failedUpdatesString).apply();
    }

    /**
     * Saves information about the pending update.
     *
     * @param pendingUpdate instance of the {@link CodePushPendingUpdate}.
     */
    public void savePendingUpdate(CodePushPendingUpdate pendingUpdate) {
        mSettings.edit().putString(getAppSpecificPrefix() + PENDING_UPDATE_KEY, mCodePushUtils.convertObjectToJsonString(pendingUpdate)).apply();
    }

    /**
     * Gets status report already saved for retry it's sending.
     *
     * @return report saved for retry sending.
     * @throws JSONException if there was error of deserialization of report from json document.
     */
    public CodePushDeploymentStatusReport getStatusReportSavedForRetry() throws JSONException {
        String retryStatusReportString = mSettings.getString(getAppSpecificPrefix() + RETRY_DEPLOYMENT_REPORT_KEY, null);
        if (retryStatusReportString != null) {
            JSONObject retryStatusReport = new JSONObject(retryStatusReportString);
            return mCodePushUtils.convertJsonObjectToObject(retryStatusReport, CodePushDeploymentStatusReport.class);
        }
        return null;
    }

    /**
     * Saves status report for further retry os it's sending.
     *
     * @param statusReport status report.
     * @throws JSONException if there was an error during report serialization into json document.
     */
    public void saveStatusReportForRetry(CodePushDeploymentStatusReport statusReport) throws JSONException {
        JSONObject statusReportJSON = mCodePushUtils.convertObjectToJsonObject(statusReport);
        mSettings.edit().putString(getAppSpecificPrefix() + RETRY_DEPLOYMENT_REPORT_KEY, statusReportJSON.toString()).apply();
    }

    /**
     * Remove status report that was saved for retry of it's sending.
     */
    public void removeStatusReportSavedForRetry() {
        mSettings.edit().remove(getAppSpecificPrefix() + RETRY_DEPLOYMENT_REPORT_KEY).apply();
    }

    /**
     * Gets previously saved status report identifier.
     *
     * @return previously saved status report identifier.
     */
    public CodePushStatusReportIdentifier getPreviousStatusReportIdentifier() {
        String identifierString = mSettings.getString(getAppSpecificPrefix() + LAST_DEPLOYMENT_REPORT_KEY, null);
        if (identifierString != null) {
            return CodePushStatusReportIdentifier.fromString(identifierString);
        }
        return null;
    }

    /**
     * Saves identifier of already sent status report.
     *
     * @param identifier identifier of already sent status report.
     */
    public void saveIdentifierOfReportedStatus(CodePushStatusReportIdentifier identifier) {
        mSettings.edit().putString(getAppSpecificPrefix() + LAST_DEPLOYMENT_REPORT_KEY, identifier.toString()).apply();
    }

    /**
     * Returns app-specific prefix for preferences keys.
     *
     * @return preference key prefix to get app specific preferences
     */
    private String getAppSpecificPrefix() {
        return mCodePushConfiguration != null ? mCodePushConfiguration.getAppName() + "-" : "";
    }
}
