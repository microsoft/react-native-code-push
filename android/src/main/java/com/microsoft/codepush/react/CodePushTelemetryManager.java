package com.microsoft.codepush.react;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.json.JSONObject;

public class CodePushTelemetryManager {
    private SharedPreferences mSettings;
    private final String APP_VERSION_KEY = "appVersion";
    private final String DEPLOYMENT_FAILED_STATUS = "DeploymentFailed";
    private final String DEPLOYMENT_KEY_KEY = "deploymentKey";
    private final String DEPLOYMENT_SUCCEEDED_STATUS = "DeploymentSucceeded";
    private final String LABEL_KEY = "label";
    private final String LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";
    private final String PACKAGE_KEY = "package";
    private final String PREVIOUS_DEPLOYMENT_KEY_KEY = "previousDeploymentKey";
    private final String PREVIOUS_LABEL_OR_APP_VERSION_KEY = "previousLabelOrAppVersion";
    private final String RETRY_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_RETRY_DEPLOYMENT_REPORT";
    private final String STATUS_KEY = "status";

    public CodePushTelemetryManager(Context applicationContext) {
        mSettings = applicationContext.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
    }

    public WritableMap getBinaryUpdateReport(String appVersion) {
        String previousStatusReportIdentifier = this.getPreviousStatusReportIdentifier();
        WritableMap reportMap = null;
        if (previousStatusReportIdentifier == null) {
            this.clearRetryStatusReport();
            reportMap = Arguments.createMap();
            reportMap.putString(APP_VERSION_KEY, appVersion);
        } else if (!previousStatusReportIdentifier.equals(appVersion)) {
            this.clearRetryStatusReport();
            reportMap = Arguments.createMap();
            if (this.isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier)) {
                String previousDeploymentKey = this.getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                String previousLabel = this.getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);
                reportMap.putString(APP_VERSION_KEY, appVersion);
                reportMap.putString(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                reportMap.putString(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
            } else {
                // Previous status report was with a binary app version.
                reportMap.putString(APP_VERSION_KEY, appVersion);
                reportMap.putString(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
            }
        }

        return reportMap;
    }

    public WritableMap getRetryStatusReport() {
        String retryStatusReportString = mSettings.getString(RETRY_DEPLOYMENT_REPORT_KEY, null);
        if (retryStatusReportString != null) {
            clearRetryStatusReport();
            try {
                JSONObject retryStatusReport = new JSONObject(retryStatusReportString);
                return CodePushUtils.convertJsonObjectToWritable(retryStatusReport);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public WritableMap getRollbackReport(WritableMap lastFailedPackage) {
        WritableMap reportMap =  Arguments.createMap();
        reportMap.putMap(PACKAGE_KEY, lastFailedPackage);
        reportMap.putString(STATUS_KEY, DEPLOYMENT_FAILED_STATUS);
        return reportMap;
    }

    public WritableMap getUpdateReport(WritableMap currentPackage) {
        String currentPackageIdentifier = this.getPackageStatusReportIdentifier(currentPackage);
        String previousStatusReportIdentifier = this.getPreviousStatusReportIdentifier();
        WritableMap reportMap = null;
        if (currentPackageIdentifier != null) {
            if (previousStatusReportIdentifier == null) {
                this.clearRetryStatusReport();
                reportMap = Arguments.createMap();
                reportMap.putMap(PACKAGE_KEY, currentPackage);
                reportMap.putString(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
            } else if (!previousStatusReportIdentifier.equals(currentPackageIdentifier)) {
                this.clearRetryStatusReport();
                reportMap = Arguments.createMap();
                if (this.isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier)) {
                    String previousDeploymentKey = this.getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    String previousLabel = this.getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);
                    reportMap.putMap(PACKAGE_KEY, currentPackage);
                    reportMap.putString(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
                    reportMap.putString(PREVIOUS_DEPLOYMENT_KEY_KEY, previousDeploymentKey);
                    reportMap.putString(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousLabel);
                } else {
                    // Previous status report was with a binary app version.
                    reportMap.putMap(PACKAGE_KEY, currentPackage);
                    reportMap.putString(STATUS_KEY, DEPLOYMENT_SUCCEEDED_STATUS);
                    reportMap.putString(PREVIOUS_LABEL_OR_APP_VERSION_KEY, previousStatusReportIdentifier);
                }
            }
        }

        return reportMap;
    }

    public void recordStatusReported(ReadableMap statusReport) {
        // We don't need to record rollback reports, so exit early if that's what was specified.
        if (statusReport.hasKey(STATUS_KEY) && DEPLOYMENT_FAILED_STATUS.equals(statusReport.getString(STATUS_KEY))) {
            return;
        }
        
        if (statusReport.hasKey(APP_VERSION_KEY)) {
            saveStatusReportedForIdentifier(statusReport.getString(APP_VERSION_KEY));
        } else if (statusReport.hasKey(PACKAGE_KEY)) {
            String packageIdentifier = getPackageStatusReportIdentifier(statusReport.getMap(PACKAGE_KEY));
            saveStatusReportedForIdentifier(packageIdentifier);
        }
    }

    public void saveStatusReportForRetry(ReadableMap statusReport) {
        JSONObject statusReportJSON = CodePushUtils.convertReadableToJsonObject(statusReport);
        mSettings.edit().putString(RETRY_DEPLOYMENT_REPORT_KEY, statusReportJSON.toString()).commit();
    }

    private void clearRetryStatusReport() {
        mSettings.edit().remove(RETRY_DEPLOYMENT_REPORT_KEY).commit();
    }

    private String getDeploymentKeyFromStatusReportIdentifier(String statusReportIdentifier) {
        String[] parsedIdentifier = statusReportIdentifier.split(":");
        if (parsedIdentifier.length > 0) {
            return parsedIdentifier[0];
        } else {
            return null;
        }
    }

    private String getPackageStatusReportIdentifier(ReadableMap updatePackage) {
        // Because deploymentKeys can be dynamically switched, we use a
        // combination of the deploymentKey and label as the packageIdentifier.
        String deploymentKey = CodePushUtils.tryGetString(updatePackage, DEPLOYMENT_KEY_KEY);
        String label = CodePushUtils.tryGetString(updatePackage, LABEL_KEY);
        if (deploymentKey != null && label != null) {
            return deploymentKey + ":" + label;
        } else {
            return null;
        }
    }

    private String getPreviousStatusReportIdentifier() {
        return mSettings.getString(LAST_DEPLOYMENT_REPORT_KEY, null);
    }

    private String getVersionLabelFromStatusReportIdentifier(String statusReportIdentifier) {
        String[] parsedIdentifier = statusReportIdentifier.split(":");
        if (parsedIdentifier.length > 1) {
            return parsedIdentifier[1];
        } else {
            return null;
        }
    }

    private boolean isStatusReportIdentifierCodePushLabel(String statusReportIdentifier) {
        return statusReportIdentifier != null && statusReportIdentifier.contains(":");
    }

    private void saveStatusReportedForIdentifier(String appVersionOrPackageIdentifier) {
        mSettings.edit().putString(LAST_DEPLOYMENT_REPORT_KEY, appVersionOrPackageIdentifier).commit();
    }
}
