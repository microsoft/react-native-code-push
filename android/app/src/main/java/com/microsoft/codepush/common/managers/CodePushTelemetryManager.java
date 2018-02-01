package com.microsoft.codepush.common.managers;

import android.content.SharedPreferences;

import com.microsoft.codepush.common.CodePushStatusReportIdentifier;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.utils.CodePushUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.microsoft.codepush.common.enums.CodePushDeploymentStatus.FAILED;
import static com.microsoft.codepush.common.enums.CodePushDeploymentStatus.SUCCEEDED;
import static com.microsoft.codepush.common.utils.StringUtils.isNullOrEmpty;

/**
 * Manager responsible for get/update telemetry reports on device.
 */
public class CodePushTelemetryManager {

    /**
     * TODO remove this line after CodePush preferences storage addition
     * Manager of CodePush preferences storage.
     */
    private SharedPreferences mSettings;

    /**
     * Key for storing last deployment report identifier.
     */
    private final String LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";

    /**
     * Key for storing last retry deployment report identifier.
     */
    private final String RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";

    /**
     * Creates an instance of {@link CodePushTelemetryManager}.
     *
     * @param settings manager of CodePush preferences storage.
     */
    public CodePushTelemetryManager(SharedPreferences settings) {
        mSettings = settings;
    }

    /**
     * Builds binary update report using current app version.
     *
     * @param appVersion current app version.
     * @return new binary update report.
     */
    public CodePushDeploymentStatusReport buildBinaryUpdateReport(String appVersion) {
        CodePushStatusReportIdentifier previousStatusReportIdentifier = getPreviousStatusReportIdentifier();
        CodePushDeploymentStatusReport report = null;
        if (previousStatusReportIdentifier == null) {

            /* There was no previous status report */
            removeStatusReportSavedForRetry();
            report = new CodePushDeploymentStatusReport();
            report.setAppVersion(appVersion);
        } else {
            boolean identifierHasDeploymentKey = previousStatusReportIdentifier.hasDeploymentKey();
            String identifierLabel = previousStatusReportIdentifier.getVersionLabelOrEmpty();
            if (identifierHasDeploymentKey || !identifierLabel.equals(appVersion)) {
                removeStatusReportSavedForRetry();
                report = new CodePushDeploymentStatusReport();
                if (identifierHasDeploymentKey) {
                    String previousDeploymentKey = previousStatusReportIdentifier.getDeploymentKey();
                    String previousLabel = previousStatusReportIdentifier.getVersionLabel();
                    report = new CodePushDeploymentStatusReport();
                    report.setAppVersion(appVersion);
                    report.setPreviousDeploymentKey(previousDeploymentKey);
                    report.setPreviousLabelOrAppVersion(previousLabel);
                } else {

                    /* Previous status report was with a binary app version. */
                    report.setAppVersion(appVersion);
                    report.setPreviousLabelOrAppVersion(previousStatusReportIdentifier.getVersionLabel());
                }
            }
        }
        return report;
    }

    /**
     * Builds update report using current local package information.
     *
     * @param currentPackage current local package information.
     * @return new update report.
     */
    public CodePushDeploymentStatusReport buildUpdateReport(CodePushLocalPackage currentPackage) {
        CodePushStatusReportIdentifier currentPackageIdentifier = buildPackageStatusReportIdentifier(currentPackage);
        CodePushStatusReportIdentifier previousStatusReportIdentifier = getPreviousStatusReportIdentifier();
        CodePushDeploymentStatusReport report = null;
        if (currentPackageIdentifier != null) {
            if (previousStatusReportIdentifier == null) {
                removeStatusReportSavedForRetry();
                report = new CodePushDeploymentStatusReport();
                report.setLocalPackage(currentPackage);
                report.setStatus(SUCCEEDED);
            } else {

                /* Compare identifiers as strings for simplicity */
                if (!previousStatusReportIdentifier.toString().equals(currentPackageIdentifier.toString())) {
                    removeStatusReportSavedForRetry();
                    report = new CodePushDeploymentStatusReport();
                    if (previousStatusReportIdentifier.hasDeploymentKey()) {
                        String previousDeploymentKey = previousStatusReportIdentifier.getDeploymentKey();
                        String previousLabel = previousStatusReportIdentifier.getVersionLabel();
                        report.setLocalPackage(currentPackage);
                        report.setStatus(SUCCEEDED);
                        report.setPreviousDeploymentKey(previousDeploymentKey);
                        report.setPreviousLabelOrAppVersion(previousLabel);
                    } else {

                        /* Previous status report was with a binary app version. */
                        report.setLocalPackage(currentPackage);
                        report.setStatus(SUCCEEDED);
                        report.setPreviousLabelOrAppVersion(previousStatusReportIdentifier.getVersionLabel());
                    }
                }
            }
        }
        return report;
    }

    /**
     * Builds rollback report using current local package information.
     *
     * @param lastFailedPackage current local package information.
     * @return new rollback report.
     */
    public CodePushDeploymentStatusReport buildRollbackReport(CodePushLocalPackage lastFailedPackage) {
        CodePushDeploymentStatusReport report = new CodePushDeploymentStatusReport();
        report.setLocalPackage(lastFailedPackage);
        report.setStatus(FAILED);
        return report;
    }

    /**
     * Gets status report already saved for retry it's sending.
     *
     * @return report saved for retry sending.
     * @throws JSONException if there was error of deserialization of report from json document.
     */
    public CodePushDeploymentStatusReport getStatusReportSavedForRetry() throws JSONException {
        //TODO move to settings manager
        String retryStatusReportString = mSettings.getString(RETRY_DEPLOYMENT_REPORT_KEY, null);
        if (retryStatusReportString != null) {
            removeStatusReportSavedForRetry();
            JSONObject retryStatusReport = new JSONObject(retryStatusReportString);
            return CodePushUtils.convertJsonObjectToObject(retryStatusReport, CodePushDeploymentStatusReport.class);
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
        JSONObject statusReportJSON = CodePushUtils.convertObjectToJsonObject(statusReport);
        //TODO move to settings manager
        mSettings.edit().putString(RETRY_DEPLOYMENT_REPORT_KEY, statusReportJSON.toString()).commit();
    }

    /**
     * Remove status report that was saved for retry of it's sending.
     */
    private void removeStatusReportSavedForRetry() {
        //TODO move to settings manager
        mSettings.edit().remove(RETRY_DEPLOYMENT_REPORT_KEY).commit();
    }

    /**
     * Saves already sent status report.
     *
     * @param statusReport report to save.
     */
    public void saveReportedStatus(CodePushDeploymentStatusReport statusReport) {
        if (statusReport.getStatus() != null && statusReport.getStatus() == FAILED) {
            return;
        }
        if (!isNullOrEmpty(statusReport.getAppVersion())) {
            CodePushStatusReportIdentifier statusIdentifier = new CodePushStatusReportIdentifier(statusReport.getAppVersion());
            saveIdentifierOfReportedStatus(statusIdentifier);
        } else if (statusReport.getLocalPackage() != null) {
            CodePushStatusReportIdentifier packageIdentifier = buildPackageStatusReportIdentifier(statusReport.getLocalPackage());
            saveIdentifierOfReportedStatus(packageIdentifier);
        }
    }
    /**
     * Builds status report identifier using local package.
     *
     * @param updatePackage local package.
     * @return status report identifier.
     */
    private CodePushStatusReportIdentifier buildPackageStatusReportIdentifier(CodePushLocalPackage updatePackage) {

        /* Because deploymentKeys can be dynamically switched, we use a
           combination of the deploymentKey and label as the packageIdentifier. */
        String deploymentKey = updatePackage.getDeploymentKey();
        String label = updatePackage.getLabel();
        if (deploymentKey != null && label != null) {
            return new CodePushStatusReportIdentifier(deploymentKey, label);
        } else {
            return null;
        }
    }

    /**
     * Gets previously saved status report identifier.
     *
     * @return previously saved status report identifier.
     */
    private CodePushStatusReportIdentifier getPreviousStatusReportIdentifier() {
        //TODO move to Settings manager
        String identifierString = mSettings.getString(LAST_DEPLOYMENT_REPORT_KEY, null);
        return CodePushStatusReportIdentifier.fromString(identifierString);
    }

    /**
     * Saves identifier of already sent status report.
     *
     * @param identifier identifier of already sent status report.
     */
    private void saveIdentifierOfReportedStatus(CodePushStatusReportIdentifier identifier) {
        //TODO move to Settings manager
        mSettings.edit().putString(LAST_DEPLOYMENT_REPORT_KEY, identifier.toString()).commit();
    }
}
