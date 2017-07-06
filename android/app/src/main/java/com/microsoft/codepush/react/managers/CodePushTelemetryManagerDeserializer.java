package com.microsoft.codepush.react.managers;

import com.facebook.react.bridge.WritableMap;
import com.microsoft.codepush.react.datacontracts.CodePushStatusReport;
import com.microsoft.codepush.react.managers.CodePushTelemetryManager;
import com.microsoft.codepush.react.utils.CodePushUtils;

public class CodePushTelemetryManagerDeserializer {
    private CodePushTelemetryManager mTelemetryManager;

    public CodePushTelemetryManagerDeserializer(CodePushTelemetryManager telemetryManager) {
        mTelemetryManager = telemetryManager;
    }

    public CodePushStatusReport getRollbackReport(WritableMap lastFailedPackage) {
        WritableMap failedStatusReport = mTelemetryManager.getRollbackReport(lastFailedPackage);
        if (failedStatusReport != null) {
            return CodePushUtils.convertWritableMapToObject(failedStatusReport, CodePushStatusReport.class);
        }
        return null;
    }

    public CodePushStatusReport getUpdateReport(WritableMap currentPackage) {
        WritableMap newPackageStatusReport = mTelemetryManager.getUpdateReport(currentPackage);
        if (newPackageStatusReport != null) {
            return CodePushUtils.convertWritableMapToObject(newPackageStatusReport, CodePushStatusReport.class);
        }
        return null;
    }

    public CodePushStatusReport getBinaryUpdateReport(String appVersion) {
        WritableMap newAppVersionStatusReport = mTelemetryManager.getBinaryUpdateReport(appVersion);
        if (newAppVersionStatusReport != null) {
            return CodePushUtils.convertWritableMapToObject(newAppVersionStatusReport, CodePushStatusReport.class);
        }
        return null;
    }

    public CodePushStatusReport getRetryStatusReport() {
        WritableMap retryStatusReport = mTelemetryManager.getRetryStatusReport();
        if (retryStatusReport != null) {
            return CodePushUtils.convertWritableMapToObject(retryStatusReport, CodePushStatusReport.class);
        }
        return null;
    }
}
