package com.microsoft.codepush.common.managers;

import com.microsoft.codepush.common.CodePushStatusReportIdentifier;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.enums.CodePushDeploymentStatus;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

import org.json.JSONException;

import static android.text.TextUtils.isEmpty;
import static com.microsoft.codepush.common.enums.CodePushDeploymentStatus.FAILED;
import static com.microsoft.codepush.common.enums.CodePushDeploymentStatus.SUCCEEDED;

/**
 * Manager responsible for get/update telemetry reports on device.
 */
public class CodePushTelemetryManager {

    /**
     * Instance of {@link SettingsManager} to work with.
     */
    private SettingsManager mSettingsManager;

    /**
     * Creates an instance of {@link CodePushTelemetryManager}.
     *
     * @param settingsManager instance of {@link SettingsManager} to work with.
     */
    public CodePushTelemetryManager(SettingsManager settingsManager) {
        mSettingsManager = settingsManager;
    }

    /**
     * Builds binary update report using current app version.
     *
     * @param appVersion current app version.
     * @return new binary update report.
     */
    public CodePushDeploymentStatusReport buildBinaryUpdateReport(String appVersion) throws CodePushIllegalArgumentException {
        CodePushStatusReportIdentifier previousStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        CodePushDeploymentStatusReport report = null;
        if (previousStatusReportIdentifier == null) {

            /* There was no previous status report */
            mSettingsManager.removeStatusReportSavedForRetry();
            report = new CodePushDeploymentStatusReport();
            report.setAppVersion(appVersion);
            report.setLabel("");
            report.setStatus(CodePushDeploymentStatus.SUCCEEDED);
        } else {
            boolean identifierHasDeploymentKey = previousStatusReportIdentifier.hasDeploymentKey();
            String identifierLabel = previousStatusReportIdentifier.getVersionLabelOrEmpty();
            if (identifierHasDeploymentKey || !identifierLabel.equals(appVersion)) {
                mSettingsManager.removeStatusReportSavedForRetry();
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
    public CodePushDeploymentStatusReport buildUpdateReport(CodePushLocalPackage currentPackage) throws CodePushIllegalArgumentException {
        CodePushStatusReportIdentifier currentPackageIdentifier = buildPackageStatusReportIdentifier(currentPackage);
        CodePushStatusReportIdentifier previousStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        CodePushDeploymentStatusReport report = null;
        if (currentPackageIdentifier != null) {
            if (previousStatusReportIdentifier == null) {
                mSettingsManager.removeStatusReportSavedForRetry();
                report = new CodePushDeploymentStatusReport();
                report.setPackage(currentPackage);
                report.setStatus(SUCCEEDED);
            } else {

                /* Compare identifiers as strings for simplicity */
                if (!previousStatusReportIdentifier.toString().equals(currentPackageIdentifier.toString())) {
                    mSettingsManager.removeStatusReportSavedForRetry();
                    report = new CodePushDeploymentStatusReport();
                    if (previousStatusReportIdentifier.hasDeploymentKey()) {
                        String previousDeploymentKey = previousStatusReportIdentifier.getDeploymentKey();
                        String previousLabel = previousStatusReportIdentifier.getVersionLabel();
                        report.setPackage(currentPackage);
                        report.setStatus(SUCCEEDED);
                        report.setPreviousDeploymentKey(previousDeploymentKey);
                        report.setPreviousLabelOrAppVersion(previousLabel);
                    } else {

                        /* Previous status report was with a binary app version. */
                        report.setPackage(currentPackage);
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
    public CodePushDeploymentStatusReport buildRollbackReport(CodePushPackage lastFailedPackage) {
        CodePushDeploymentStatusReport report = new CodePushDeploymentStatusReport();
        report.setPackage(lastFailedPackage);
        report.setStatus(FAILED);
        return report;
    }

    /**
     * Saves already sent status report.
     *
     * @param statusReport report to save.
     */
    public void saveReportedStatus(CodePushDeploymentStatusReport statusReport) {

        /* We don't need to record rollback reports, so exit early if that's what was specified. */
        if (statusReport.getStatus() != null && statusReport.getStatus() == FAILED) {
            return;
        }
        if (!isEmpty(statusReport.getAppVersion())) {
            CodePushStatusReportIdentifier statusIdentifier = new CodePushStatusReportIdentifier(statusReport.getAppVersion());
            mSettingsManager.saveIdentifierOfReportedStatus(statusIdentifier);
        } else if (statusReport.getPackage() != null) {
            CodePushStatusReportIdentifier packageIdentifier = buildPackageStatusReportIdentifier(statusReport.getPackage());
            mSettingsManager.saveIdentifierOfReportedStatus(packageIdentifier);
        }
    }

    /**
     * Builds status report identifier using local package.
     *
     * @param updatePackage local package.
     * @return status report identifier.
     */
    private CodePushStatusReportIdentifier buildPackageStatusReportIdentifier(CodePushPackage updatePackage) {

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
     * Gets status report already saved for retry it's sending.
     *
     * @return report saved for retry sending.
     * @throws JSONException if there was error of deserialization of report from json document.
     */
    public CodePushDeploymentStatusReport getStatusReportSavedForRetry() throws JSONException {
        CodePushDeploymentStatusReport report = mSettingsManager.getStatusReportSavedForRetry();
        if (report != null) {
            mSettingsManager.removeStatusReportSavedForRetry();
        }
        return report;
    }
}
