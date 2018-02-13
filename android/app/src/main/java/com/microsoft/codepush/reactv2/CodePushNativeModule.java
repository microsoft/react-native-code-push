package com.microsoft.codepush.reactv2;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.core.CodePushBaseCore;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.enums.CodePushInstallMode;
import com.microsoft.codepush.common.enums.CodePushSyncStatus;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.interfaces.CodePushBinaryVersionMismatchListener;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.utils.CodePushLogUtils;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;
import com.microsoft.codepush.reactv2.utils.ReactConvertUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around {@link CodePushBaseCore} specific for React.
 */
@SuppressWarnings("unused")
public class CodePushNativeModule extends ReactContextBaseJavaModule implements CodePushDownloadProgressListener, CodePushSyncStatusListener, CodePushBinaryVersionMismatchListener {

    /**
     * Hash of the binary version update.
     */
    private String mBinaryContentsHash = null;

    /**
     * Indicates whether notify about package download progress.
     */
    private static boolean mNotifyDownloadProgress = false;

    /**
     * Indicates whether notify that sync status has changed.
     */
    private static boolean mNotifySyncStatusChanged = false;

    /**
     * Indicated whether notify that binary version mismatch has happened.
     */
    private static boolean mNotifyBinaryVersionMismatch = false;

    /**
     * Instance of {@link CodePushBaseCore} containing basic android logic.
     */
    private CodePushBaseCore mCodePushCore;

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Instance of {@link ReactConvertUtils} to work with.
     */
    private ReactConvertUtils mReactConvertUtils;

    /**
     * Creates an instance of {@link CodePushNativeModule}.
     *
     * @param reactContext application context.
     * @param codePushCore instance of {@link CodePushBaseCore}.
     */
    public CodePushNativeModule(ReactApplicationContext reactContext, CodePushBaseCore codePushCore) {
        super(reactContext);
        mCodePushCore = codePushCore;
        FileUtils fileUtils = FileUtils.getInstance();
        mCodePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, mCodePushUtils);
        mReactConvertUtils = ReactConvertUtils.getInstance(mCodePushUtils);

        /* Initialize module state while we have a reference to the current context. */
        mBinaryContentsHash = codePushUpdateUtils.getHashForBinaryContents(reactContext, mCodePushCore.isDebugMode());
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
        constants.put("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
        constants.put("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());
        constants.put("codePushInstallModeOnNextSuspend", CodePushInstallMode.ON_NEXT_SUSPEND.getValue());
        constants.put("codePushUpdateStateRunning", CodePushUpdateState.RUNNING.getValue());
        constants.put("codePushUpdateStatePending", CodePushUpdateState.PENDING.getValue());
        constants.put("codePushUpdateStateLatest", CodePushUpdateState.LATEST.getValue());
        constants.put("codePushSyncStatusUpToDate", CodePushSyncStatus.UP_TO_DATE.getValue());
        constants.put("codePushSyncStatusUpdateInstalled", CodePushSyncStatus.UPDATE_INSTALLED.getValue());
        constants.put("codePushSyncStatusUpdateIgnored", CodePushSyncStatus.UPDATE_IGNORED.getValue());
        constants.put("codePushSyncStatusUnknownError", CodePushSyncStatus.UNKNOWN_ERROR.getValue());
        constants.put("codePushSyncStatusSyncInProgress", CodePushSyncStatus.SYNC_IN_PROGRESS.getValue());
        constants.put("codePushSyncStatusCheckingForUpdate", CodePushSyncStatus.CHECKING_FOR_UPDATE.getValue());
        constants.put("codePushSyncStatusAwaitingUserAction", CodePushSyncStatus.AWAITING_USER_ACTION.getValue());
        constants.put("codePushSyncStatusDownloadingPackage", CodePushSyncStatus.DOWNLOADING_PACKAGE.getValue());
        constants.put("codePushSyncStatusInstallingUpdate", CodePushSyncStatus.INSTALLING_UPDATE.getValue());
        return constants;
    }

    @Override

    public String getName() {
        return "CodePush";
    }

    @Override
    public void syncStatusChanged(CodePushSyncStatus codePushSyncStatus) {
        if (mNotifySyncStatusChanged) {
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(CodePushConstants.SYNC_STATUS_EVENT_NAME, codePushSyncStatus.getValue());
        }
    }

    @Override
    public void downloadProgressChanged(long receivedBytes, long totalBytes) {
        if (mNotifyDownloadProgress) {
            DownloadProgress downloadProgress = new DownloadProgress(totalBytes, receivedBytes);
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(CodePushConstants.DOWNLOAD_PROGRESS_EVENT_NAME, mReactConvertUtils.convertDownloadProgressToWritableMap(downloadProgress));
        }
    }

    @Override
    public void binaryVersionMismatchChanged(CodePushRemotePackage update) {
        if (mNotifyBinaryVersionMismatch) {
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(CodePushConstants.BINARY_VERSION_MISMATCH_EVENT_NAME, update);
        }
    }

    /**
     * Checks if there is an update available by the provided deployment key.
     *
     * @param deploymentKey deployment key of the desired update.
     * @param promise       js promise to handle results.
     *                      Resolved, it waits for the instance of the {@link CodePushRemotePackage} converted to {@link WritableMap}.
     */
    @ReactMethod
    public void checkForUpdate(String deploymentKey, Promise promise) {
        try {
            CodePushRemotePackage remotePackage = mCodePushCore.checkForUpdate(deploymentKey);
            if (remotePackage != null) {
                JSONObject jsonObject = mCodePushUtils.convertObjectToJsonObject(remotePackage);
                promise.resolve(mReactConvertUtils.convertJsonObjectToWritable(jsonObject));
            } else {
                promise.resolve("");
            }
        } catch (JSONException | CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Performs synchronization.
     *
     * @param syncOptionsMap              instance of {@link ReadableMap} containing synchronization options.
     *                                    Should be convertible to {@link CodePushSyncOptions}.
     * @param notifySyncStatusChanged     <code>true</code> if notify that synchronization status has changed.
     * @param notifyDownloadProgress      <code>true</code> if notify about package download progress.
     * @param notifyBinaryVersionMismatch <code>true</code> if notify about the binary version mismatch.
     * @param promise                     js promise to handle results.
     *                                    Does not wait for any result except <code>reject</code> if necessary.
     */
    @ReactMethod
    public void sync(ReadableMap syncOptionsMap, boolean notifySyncStatusChanged, boolean notifyDownloadProgress, boolean notifyBinaryVersionMismatch, Promise promise) {
        mNotifySyncStatusChanged = notifySyncStatusChanged;
        mNotifyDownloadProgress = notifyDownloadProgress;
        mNotifyBinaryVersionMismatch = notifyBinaryVersionMismatch;
        try {
            CodePushSyncOptions syncOptions = mReactConvertUtils.convertReadableToObject(syncOptionsMap, CodePushSyncOptions.class);
            mCodePushCore.sync(syncOptions);
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Performs an update download based on the metadata.
     *
     * @param updatePackage  instance of {@link ReadableMap} containing information about the update.
     *                       Should be convertible to {@link CodePushRemotePackage}.
     * @param notifyProgress <code>true</code> if notify about the download progress.
     * @param promise        js promise to handle results.
     *                       Resolved, it waits for the instance of the {@link CodePushLocalPackage} converted to {@link WritableMap}.
     */
    @ReactMethod
    public void downloadUpdate(ReadableMap updatePackage, boolean notifyProgress, Promise promise) {
        mNotifyDownloadProgress = notifyProgress;
        try {
            CodePushLocalPackage newPackage = mCodePushCore.downloadUpdate(
                    mReactConvertUtils.convertReadableToObject(updatePackage, CodePushRemotePackage.class)
            );
            promise.resolve(mReactConvertUtils.convertObjectToWritableMap(newPackage));
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Gets application configuration.
     *
     * @param promise js promise to handle results.
     *                Resolved, it waits for the instance of the {@link CodePushConfiguration} converted to {@link WritableMap}.
     */
    @ReactMethod
    public void getConfiguration(Promise promise) {
        WritableMap configMap = Arguments.createMap();
        CodePushConfiguration nativeConfiguration = mCodePushCore.getNativeConfiguration();
        configMap.putString("appVersion", nativeConfiguration.getAppVersion());
        configMap.putString("clientUniqueId", nativeConfiguration.getClientUniqueId());
        configMap.putString("deploymentKey", nativeConfiguration.getDeploymentKey());
        configMap.putString("serverUrl", nativeConfiguration.getServerUrl());

        /* The binary hash may be null in debug builds. */
        if (mBinaryContentsHash != null) {
            configMap.putString(CodePushConstants.PACKAGE_HASH_KEY, mBinaryContentsHash);
        }
        promise.resolve(configMap);
    }

    /**
     * Gets information about currently installed update package.
     *
     * @param updateState index of the update state as listed in {@link CodePushUpdateState} enum.
     * @param promise     js promise to handle results.
     *                    Resolved, it waits for the {@link CodePushLocalPackage} instance converted to {@link WritableMap}.
     */
    @ReactMethod
    public void getUpdateMetadata(int updateState, Promise promise) {
        try {
            CodePushLocalPackage currentPackage = mCodePushCore.getUpdateMetadata(CodePushUpdateState.values()[updateState]);
            if (currentPackage != null) {
                promise.resolve(mReactConvertUtils.convertObjectToWritableMap(currentPackage));
            } else {
                promise.resolve("");
            }
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Gets new update report.
     *
     * @param promise js promise to handle the results.
     *                Resolved, it waits for the instance of the {@link CodePushDeploymentStatusReport} converted to {@link WritableMap}.
     */
    @ReactMethod
    public void getNewStatusReport(Promise promise) {
        try {
            CodePushDeploymentStatusReport statusReport = mCodePushCore.getNewStatusReport();
            if (statusReport != null) {
                promise.resolve(mReactConvertUtils.convertObjectToWritableMap(statusReport));
            } else {
                promise.resolve("");
            }
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Installs the desired update.
     *
     * @param updatePackage             instance of {@link ReadableMap} containing information about the update.
     *                                  Should be convertible to {@link CodePushLocalPackage}.
     * @param installMode               index of the install mode as listed in the {@link CodePushInstallMode} enum.
     * @param minimumBackgroundDuration the minimum number of seconds that the app needs to have been in the background before restarting the app.
     * @param promise                   js promise to handle the results.
     *                                  Waits either for <code>resolve</code> with empty string indicating that the update has been installed or <code>reject</code> with error.
     */
    @ReactMethod
    public void installUpdate(ReadableMap updatePackage, int installMode, int minimumBackgroundDuration, Promise promise) {
        try {
            mCodePushCore.installUpdate(
                    mReactConvertUtils.convertReadableToObject(updatePackage, CodePushLocalPackage.class),
                    CodePushInstallMode.values()[installMode],
                    minimumBackgroundDuration);
            promise.resolve("");
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            promise.reject(e);
        }
    }

    /**
     * Checks whether the update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @param promise     js promise to handle the results.
     *                    Waits to be resolved with the boolean value.
     */
    @ReactMethod
    public void isFailedUpdate(String packageHash, Promise promise) {
        promise.resolve(mCodePushCore.existsFailedUpdate(packageHash));
    }

    /**
     * Checks whether this is the first time the update has been run after being installed.
     *
     * @param packageHash hash to checks.
     * @param promise     js promise to handle the results.
     *                    Waits to be resolved with the boolean value.
     */
    @ReactMethod
    public void isFirstRun(String packageHash, Promise promise) {
        try {
            promise.resolve(mCodePushCore.isFirstRun(packageHash));
        } catch (CodePushNativeApiCallException e) {
            promise.resolve(false);
        }
    }

    /**
     * Removes information about the pending update.
     *
     * @param promise js promise to handle the results.
     *                Resolved no matter the result.
     */
    @ReactMethod
    public void removePendingUpdate(Promise promise) {
        mCodePushCore.removePendingUpdate();
        promise.resolve("");
    }

    /**
     * Performs an application restart.
     *
     * @param onlyIfUpdateIsPending restart only if update is pending.
     * @param promise               js promise to handle the results.
     *                              Waits to be resolved with the boolean value.
     */
    @ReactMethod
    public void restartApplication(boolean onlyIfUpdateIsPending, Promise promise) {
        promise.resolve(mCodePushCore.getRestartManager().restartApp(onlyIfUpdateIsPending));
    }

    /**
     * Clears information about pending restarts.
     */
    @ReactMethod
    public void clearPendingRestart() {
        mCodePushCore.getRestartManager().clearPendingRestart();
    }

    /**
     * Permits application to be restarted.
     */
    @ReactMethod
    public void disallowRestart() {
        mCodePushCore.getRestartManager().disallowRestarts();
    }

    /**
     * Allows application to be restarted.
     */
    @ReactMethod
    public void allowRestart() {
        mCodePushCore.getRestartManager().allowRestarts();
    }

    /**
     * Saves status report.
     *
     * @param statusReport instance of {@link ReadableMap} containing information about the update.
     *                     Should be convertible to {@link CodePushDeploymentStatusReport}.
     */
    @ReactMethod
    public void recordStatusReported(ReadableMap statusReport) {
        try {
            mCodePushCore.saveReportedStatus(mReactConvertUtils.convertReadableToObject(statusReport, CodePushDeploymentStatusReport.class));
        } catch (CodePushMalformedDataException e) {
            CodePushLogUtils.trackException(e);
        }
    }

    @ReactMethod
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        try {
            mCodePushCore.saveStatusReportForRetry(mReactConvertUtils.convertReadableToObject(statusReport, CodePushDeploymentStatusReport.class));
        } catch (CodePushMalformedDataException | CodePushNativeApiCallException e) {
            CodePushLogUtils.trackException(e);
        }
    }
}
