package com.microsoft.codepush.react;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class CodePushTelemetryManager {

    private Context applicationContext;
    private final String CODE_PUSH_PREFERENCES;
    private final String DEPLOYMENT_FAILED_STATUS = "DeploymentFailed";
    private final String DEPLOYMENT_KEY_KEY = "deploymentKey";
    private final String DEPLOYMENT_SUCCEEDED_STATUS = "DeploymentSucceeded";
    private final String LABEL_KEY = "label";
    private final String LAST_DEPLOYMENT_REPORT_KEY = "CODE_PUSH_LAST_DEPLOYMENT_REPORT";

    public CodePushTelemetryManager(Context applicationContext, String codePushPreferencesKey) {
        this.applicationContext = applicationContext;
        this.CODE_PUSH_PREFERENCES = codePushPreferencesKey;
    }

    public WritableMap getBinaryUpdateReport(String appVersion) {
        String previousStatusReportIdentifier = this.getPreviousStatusReportIdentifier();
        if (previousStatusReportIdentifier == null) {
            this.recordDeploymentStatusReported(appVersion);
            WritableNativeMap reportMap = new WritableNativeMap();
            reportMap.putString("appVersion", appVersion);
            return reportMap;
        } else if (!previousStatusReportIdentifier.equals(appVersion)) {
            this.recordDeploymentStatusReported(appVersion);
            WritableNativeMap reportMap = new WritableNativeMap();
            if (this.isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier)) {
                String previousDeploymentKey = this.getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                String previousLabel = this.getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);
                reportMap.putString("appVersion", appVersion);
                reportMap.putString("previousDeploymentKey", previousDeploymentKey);
                reportMap.putString("previousLabelOrAppVersion", previousLabel);
            } else {
                // Previous status report was with a binary app version.
                reportMap.putString("appVersion", appVersion);
                reportMap.putString("previousLabelOrAppVersion", previousStatusReportIdentifier);
            }
            return reportMap;
        }

        return null;
    }

    public WritableMap getRollbackReport(WritableMap lastFailedPackage) {
        WritableNativeMap reportMap = new WritableNativeMap();
        reportMap.putMap("package", lastFailedPackage);
        reportMap.putString("status", DEPLOYMENT_FAILED_STATUS);
        return reportMap;
    }

    public WritableMap getUpdateReport(WritableMap currentPackage) {
        String currentPackageIdentifier = this.getPackageStatusReportIdentifier(currentPackage);
        String previousStatusReportIdentifier = this.getPreviousStatusReportIdentifier();
        if (currentPackageIdentifier != null) {
            if (previousStatusReportIdentifier == null) {
                this.recordDeploymentStatusReported(currentPackageIdentifier);
                WritableNativeMap reportMap = new WritableNativeMap();
                reportMap.putMap("package", currentPackage);
                reportMap.putString("status", DEPLOYMENT_SUCCEEDED_STATUS);
                return reportMap;
            } else if (!previousStatusReportIdentifier.equals(currentPackageIdentifier)) {
                this.recordDeploymentStatusReported(currentPackageIdentifier);
                if (this.isStatusReportIdentifierCodePushLabel(previousStatusReportIdentifier)) {
                    String previousDeploymentKey = this.getDeploymentKeyFromStatusReportIdentifier(previousStatusReportIdentifier);
                    String previousLabel = this.getVersionLabelFromStatusReportIdentifier(previousStatusReportIdentifier);
                    WritableNativeMap reportMap = new WritableNativeMap();
                    reportMap.putMap("package", currentPackage);
                    reportMap.putString("status", DEPLOYMENT_SUCCEEDED_STATUS);
                    reportMap.putString("previousDeploymentKey", previousDeploymentKey);
                    reportMap.putString("previousLabelOrAppVersion", previousLabel);
                    return reportMap;
                } else {
                    // Previous status report was with a binary app version.
                    WritableNativeMap reportMap = new WritableNativeMap();
                    reportMap.putMap("package", currentPackage);
                    reportMap.putString("status", DEPLOYMENT_SUCCEEDED_STATUS);
                    reportMap.putString("previousLabelOrAppVersion", previousStatusReportIdentifier);
                    return reportMap;
                }
            }
        }

        return null;
    }

    private String getDeploymentKeyFromStatusReportIdentifier(String statusReportIdentifier) {
        String[] parsedIdentifier = statusReportIdentifier.split(":");
        if (parsedIdentifier.length > 0) {
            return parsedIdentifier[0];
        } else {
            return null;
        }
    }

    private String getPackageStatusReportIdentifier(WritableMap updatePackage) {
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
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        return settings.getString(LAST_DEPLOYMENT_REPORT_KEY, null);
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

    private void recordDeploymentStatusReported(String appVersionOrPackageIdentifier) {
        SharedPreferences settings = applicationContext.getSharedPreferences(CODE_PUSH_PREFERENCES, 0);
        settings.edit().putString(LAST_DEPLOYMENT_REPORT_KEY, appVersionOrPackageIdentifier).commit();
    }
}
