package com.microsoft.codepush.react;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.ChoreographerCompat;
import com.facebook.react.modules.core.ReactChoreographer;
import com.microsoft.codepush.react.enums.CodePushDeploymentStatus;
import com.microsoft.codepush.react.enums.CodePushInstallMode;
import com.microsoft.codepush.react.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.react.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.react.datacontracts.CodePushStatusReport;
import com.microsoft.codepush.react.enums.CodePushSyncStatus;
import com.microsoft.codepush.react.enums.CodePushUpdateState;
import com.microsoft.codepush.react.exceptions.CodePushInvalidUpdateException;
import com.microsoft.codepush.react.exceptions.CodePushNotInitializedException;
import com.microsoft.codepush.react.exceptions.CodePushUnknownException;
import com.microsoft.codepush.react.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.react.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.react.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.react.interfaces.ReactInstanceHolder;
import com.microsoft.codepush.react.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.react.managers.CodePushRestartManager;
import com.microsoft.codepush.react.managers.CodePushTelemetryManager;
import com.microsoft.codepush.react.managers.CodePushTelemetryManagerDeserializer;
import com.microsoft.codepush.react.managers.CodePushUpdateManager;
import com.microsoft.codepush.react.managers.CodePushUpdateManagerDeserializer;
import com.microsoft.codepush.react.managers.SettingsManager;
import com.microsoft.codepush.react.utils.CodePushUpdateUtils;
import com.microsoft.codepush.react.utils.CodePushUtils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CodePushCore {
    private static boolean sIsRunningBinaryVersion = false;
    private static boolean sNeedToReportRollback = false;
    private static boolean sTestConfigurationFlag = false;
    private static String sAppVersion = null;

    private boolean mDidUpdate = false;

    private String mAssetsBundleFileName;

    // Helper classes.
    private CodePushUpdateManager mUpdateManager;
    private CodePushUpdateManagerDeserializer mUpdateManagerDeserializer;
    private CodePushTelemetryManager mTelemetryManager;
    private CodePushTelemetryManagerDeserializer mTelemetryManagerDeserializer;
    private SettingsManager mSettingsManager;
    private CodePushRestartManager mRestartManager;

    // Config properties.
    private String mDeploymentKey;
    private String mServerUrl = "https://codepush.azurewebsites.net/";

    private Context mContext;
    private final boolean mIsDebugMode;

    private static ReactInstanceHolder mReactInstanceHolder;
    private static CodePushCore mCurrentInstance;
    private static ReactApplicationContext mReactApplicationContext;

    private List<CodePushSyncStatusListener> mSyncStatusListeners = new ArrayList<>();
    private List<CodePushDownloadProgressListener> mDownloadProgressListeners = new ArrayList<>();

    private LifecycleEventListener mLifecycleEventListener = null;
    private int mMinimumBackgroundDuration = 0;

    private boolean mSyncInProgress = false;
    private CodePushInstallMode mCurrentInstallModeInProgress = CodePushInstallMode.ON_NEXT_RESTART;

    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mSyncStatusListeners.add(syncStatusListener);
    }

    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mDownloadProgressListeners.add(downloadProgressListener);
    }

    private void syncStatusChange(CodePushSyncStatus syncStatus) {
        for (CodePushSyncStatusListener syncStatusListener: mSyncStatusListeners) {
            try {
                syncStatusListener.syncStatusChanged(syncStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        switch (syncStatus){
            case CHECKING_FOR_UPDATE: {
                CodePushUtils.log("Checking for update.");
                break;
            }
            case AWAITING_USER_ACTION: {
                CodePushUtils.log("Awaiting user action.");
                break;
            }
            case DOWNLOADING_PACKAGE: {
                CodePushUtils.log("Downloading package.");
                break;
            }
            case INSTALLING_UPDATE: {
                CodePushUtils.log("Installing update.");
                break;
            }
            case UP_TO_DATE: {
                CodePushUtils.log("App is up to date.");
                break;
            }
            case UPDATE_IGNORED: {
                CodePushUtils.log("User cancelled the update.");
                break;
            }
            case UPDATE_INSTALLED: {
                if (mCurrentInstallModeInProgress == CodePushInstallMode.ON_NEXT_RESTART) {
                    CodePushUtils.log("Update is installed and will be run on the next app restart.");
                } else if (mCurrentInstallModeInProgress == CodePushInstallMode.ON_NEXT_RESUME) {
                    CodePushUtils.log("Update is installed and will be run after the app has been in the background for at least " + mMinimumBackgroundDuration + " seconds.");
                } else {
                    CodePushUtils.log("Update is installed and will be run when the app next resumes.");
                }
                break;
            }
            case UNKNOWN_ERROR: {
                CodePushUtils.log("An unknown error occurred.");
                break;
            }
        }
    }

    private void downloadProgressChange(long receivedBytes, long totalBytes) {
        for (CodePushDownloadProgressListener downloadProgressListener: mDownloadProgressListeners) {
            try {
                downloadProgressListener.downloadProgressChanged(receivedBytes, totalBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public CodePushCore(String deploymentKey, Context context) {
        this(deploymentKey, context, false);
    }

    public CodePushCore(String deploymentKey, Context context, boolean isDebugMode) {
        mContext = context.getApplicationContext();

        mUpdateManager = new CodePushUpdateManager(context.getFilesDir().getAbsolutePath());
        mUpdateManagerDeserializer = new CodePushUpdateManagerDeserializer(mUpdateManager);
        mTelemetryManager = new CodePushTelemetryManager(mContext);
        mTelemetryManagerDeserializer = new CodePushTelemetryManagerDeserializer(mTelemetryManager);
        mDeploymentKey = deploymentKey;
        mIsDebugMode = isDebugMode;
        mSettingsManager = new SettingsManager(mContext);
        mRestartManager = new CodePushRestartManager(this);

        if (sAppVersion == null) {
            try {
                PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                sAppVersion = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                throw new CodePushUnknownException("Unable to get package info for " + mContext.getPackageName(), e);
            }
        }

        mCurrentInstance = this;

        clearDebugCacheIfNeeded();
        initializeUpdateAfterRestart();
    }

    public CodePushCore(String deploymentKey, Context context, boolean isDebugMode, String serverUrl) {
        this(deploymentKey, context, isDebugMode);
        mServerUrl = serverUrl;
    }

    public void clearDebugCacheIfNeeded() {
        if (mIsDebugMode && mSettingsManager.isPendingUpdate(null)) {
            // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
            File cachedDevBundle = new File(mContext.getFilesDir(), "ReactNativeDevBundle.js");
            if (cachedDevBundle.exists()) {
                cachedDevBundle.delete();
            }
        }
    }

    public boolean didUpdate() {
        return mDidUpdate;
    }

    public String getAppVersion() {
        return sAppVersion;
    }

    public String getAssetsBundleFileName() {
        return mAssetsBundleFileName;
    }

    long getBinaryResourcesModifiedTime() {
        try {
            String packageName = this.mContext.getPackageName();
            int codePushApkBuildTimeId = this.mContext.getResources().getIdentifier(CodePushConstants.CODE_PUSH_APK_BUILD_TIME_KEY, "string", packageName);
            String codePushApkBuildTime = this.mContext.getResources().getString(codePushApkBuildTimeId);
            return Long.parseLong(codePushApkBuildTime);
        } catch (Exception e)  {
            throw new CodePushUnknownException("Error in getting binary resources modified time", e);
        }
    }

    @Deprecated
    public static String getBundleUrl() {
        return getJSBundleFile();
    }

    @Deprecated
    public static String getBundleUrl(String assetsBundleFileName) {
        return getJSBundleFile(assetsBundleFileName);
    }

    public Context getContext() {
        return mContext;
    }

    public String getDeploymentKey() {
        return mDeploymentKey;
    }

    public static String getJSBundleFile() {
        return CodePushCore.getJSBundleFile(CodePushConstants.DEFAULT_JS_BUNDLE_NAME);
    }

    public static String getJSBundleFile(String assetsBundleFileName) {
        if (mCurrentInstance == null) {
            throw new CodePushNotInitializedException("A CodePush instance has not been created yet. Have you added it to your app's list of ReactPackages?");
        }

        return mCurrentInstance.getJSBundleFileInternal(assetsBundleFileName);
    }

    public String getJSBundleFileInternal(String assetsBundleFileName) {
        this.mAssetsBundleFileName = assetsBundleFileName;
        String binaryJsBundleUrl = CodePushConstants.ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        long binaryResourcesModifiedTime = this.getBinaryResourcesModifiedTime();

        try {
            String packageFilePath = mUpdateManager.getCurrentPackageBundlePath(this.mAssetsBundleFileName);
            if (packageFilePath == null) {
                // There has not been any downloaded updates.
                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                sIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }

            JSONObject packageMetadata = this.mUpdateManager.getCurrentPackage();
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = packageMetadata.optString(CodePushConstants.BINARY_MODIFIED_TIME_KEY, null);
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }

            String packageAppVersion = packageMetadata.optString("appVersion", null);
            if (binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    (isUsingTestConfiguration() || sAppVersion.equals(packageAppVersion))) {
                CodePushUtils.logBundleUrl(packageFilePath);
                sIsRunningBinaryVersion = false;
                return packageFilePath;
            } else {
                // The binary version is newer.
                this.mDidUpdate = false;
                if (!this.mIsDebugMode || !sAppVersion.equals(packageAppVersion)) {
                    this.clearUpdates();
                }

                CodePushUtils.logBundleUrl(binaryJsBundleUrl);
                sIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        } catch (NumberFormatException e) {
            throw new CodePushUnknownException("Error in reading binary modified date from package metadata", e);
        }
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    void initializeUpdateAfterRestart() {
        // Reset the state which indicates that
        // the app was just freshly updated.
        mDidUpdate = false;

        JSONObject pendingUpdate = mSettingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            try {
                boolean updateIsLoading = pendingUpdate.getBoolean(CodePushConstants.PENDING_UPDATE_IS_LOADING_KEY);
                if (updateIsLoading) {
                    // Pending update was initialized, but notifyApplicationReady was not called.
                    // Therefore, deduce that it is a broken update and rollback.
                    CodePushUtils.log("Update did not finish loading the last time, rolling back to a previous version.");
                    sNeedToReportRollback = true;
                    rollbackPackage();
                } else {
                    // There is in fact a new update running for the first
                    // time, so update the local state to ensure the client knows.
                    mDidUpdate = true;

                    // Mark that we tried to initialize the new update, so that if it crashes,
                    // we will know that we need to rollback when the app next starts.
                    mSettingsManager.savePendingUpdate(pendingUpdate.getString(CodePushConstants.PENDING_UPDATE_HASH_KEY),
                            /* isLoading */true);
                }
            } catch (JSONException e) {
                // Should not happen.
                throw new CodePushUnknownException("Unable to read pending update metadata stored in SharedPreferences", e);
            }
        }
    }

    void invalidateCurrentInstance() {
        mCurrentInstance = null;
    }

    boolean isDebugMode() {
        return mIsDebugMode;
    }

    boolean isRunningBinaryVersion() {
        return sIsRunningBinaryVersion;
    }

    boolean needToReportRollback() {
        return sNeedToReportRollback;
    }

    public static void overrideAppVersion(String appVersionOverride) {
        sAppVersion = appVersionOverride;
    }

    private void rollbackPackage() {
        JSONObject failedPackage = mUpdateManager.getCurrentPackage();
        mSettingsManager.saveFailedUpdate(failedPackage);
        mUpdateManager.rollbackPackage();
        mSettingsManager.removePendingUpdate();
    }

    public void setNeedToReportRollback(boolean needToReportRollback) {
        CodePushCore.sNeedToReportRollback = needToReportRollback;
    }

    /* The below 3 methods are used for running tests.*/
    public static boolean isUsingTestConfiguration() {
        return sTestConfigurationFlag;
    }

    public static void setUsingTestConfiguration(boolean shouldUseTestConfiguration) {
        sTestConfigurationFlag = shouldUseTestConfiguration;
    }

    public void clearUpdates() {
        mUpdateManager.clearUpdates();
        mSettingsManager.removePendingUpdate();
        mSettingsManager.removeFailedUpdates();
    }

    public static void setReactInstanceHolder(ReactInstanceHolder reactInstanceHolder) {
        mReactInstanceHolder = reactInstanceHolder;
    }

    static ReactInstanceManager getReactInstanceManager() {
        if (mReactInstanceHolder == null) {
            return null;
        }
        return mReactInstanceHolder.getReactInstanceManager();
    }

    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        mReactApplicationContext = reactApplicationContext;
        CodePushNativeModule codePushModule = new CodePushNativeModule(mReactApplicationContext, this);
        CodePushDialog dialogModule = new CodePushDialog(mReactApplicationContext);

        addSyncStatusListener(codePushModule);
        addDownloadProgressListener(codePushModule);

        List<NativeModule> nativeModules = new ArrayList<>();
        nativeModules.add(codePushModule);
        nativeModules.add(dialogModule);
        return nativeModules;
    }

    public CodePushConfiguration getConfiguration() {
        return new CodePushConfiguration(
            sAppVersion,
            Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID),
            getDeploymentKey(),
            getServerUrl(),
            CodePushUpdateUtils.getHashForBinaryContents(getContext(), isDebugMode())
        );
    }

    public CodePushRemotePackage checkForUpdate() {
        CodePushConfiguration nativeConfiguration = getConfiguration();
        return checkForUpdate(nativeConfiguration.DeploymentKey);
    }

    public CodePushRemotePackage checkForUpdate(String deploymentKey) {
        CodePushConfiguration nativeConfiguration = getConfiguration();
        CodePushConfiguration configuration = new CodePushConfiguration(
                nativeConfiguration.AppVersion,
                nativeConfiguration.ClientUniqueId,
                deploymentKey != null ? deploymentKey : nativeConfiguration.DeploymentKey,
                nativeConfiguration.ServerUrl,
                nativeConfiguration.PackageHash
        );

        CodePushLocalPackage localPackage = getCurrentPackage();
        CodePushLocalPackage queryPackage;

        if (localPackage != null) {
            queryPackage = localPackage;
        } else {
            queryPackage = new CodePushLocalPackage(configuration.AppVersion, "", "", false, false, false, false, "" , "", false);
        }

        CodePushRemotePackage update = new CodePushAcquisitionManager(configuration).queryUpdateWithCurrentPackage(queryPackage);

        if (update == null || update.UpdateAppVersion ||
                localPackage != null && (update.PackageHash == localPackage.PackageHash) ||
                (localPackage == null || localPackage.IsDebugOnly) && configuration.PackageHash == update.PackageHash) {
            if (update != null && update.UpdateAppVersion) {
                CodePushUtils.log("An update is available but it is not targeting the binary version of your app.");
            }
            return null;
        } else {
            return new CodePushRemotePackage(
                    update.AppVersion,
                    deploymentKey != null ? deploymentKey : update.DeploymentKey,
                    update.Description,
                    isFailedUpdate(update.PackageHash),
                    update.IsMandatory,
                    update.Label,
                    update.PackageHash,
                    update.PackageSize,
                    update.DownloadUrl,
                    update.UpdateAppVersion
            );
        }
    }

    public CodePushLocalPackage getCurrentPackage() {
        return getUpdateMetadata(CodePushUpdateState.LATEST);
    }

    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) {
        if (updateState == null) {
            updateState = CodePushUpdateState.RUNNING;
        }

        CodePushLocalPackage currentPackage = mUpdateManagerDeserializer.getCurrentPackage();

        if (currentPackage == null) {
            return null;
        }

        Boolean currentUpdateIsPending = false;
        Boolean isDebugOnly = false;

        if (currentPackage.PackageHash != null && !currentPackage.PackageHash.isEmpty()) {
            String currentHash = currentPackage.PackageHash;
            currentUpdateIsPending = mSettingsManager.isPendingUpdate(currentHash);
        }

        if (updateState == CodePushUpdateState.PENDING && !currentUpdateIsPending) {
            // The caller wanted a pending update
            // but there isn't currently one.
            return null;
        } else if (updateState == CodePushUpdateState.RUNNING && currentUpdateIsPending) {
            // The caller wants the running update, but the current
            // one is pending, so we need to grab the previous.
            CodePushLocalPackage previousPackage = mUpdateManagerDeserializer.getPreviousPackage();

            if (previousPackage == null) {
                return null;
            }

            return previousPackage;
        } else {
            // The current package satisfies the request:
            // 1) Caller wanted a pending, and there is a pending update
            // 2) Caller wanted the running update, and there isn't a pending
            // 3) Caller wants the latest update, regardless if it's pending or not
            if (isRunningBinaryVersion()) {
                // This only matters in Debug builds. Since we do not clear "outdated" updates,
                // we need to indicate to the JS side that somehow we have a current update on
                // disk that is not actually running.
                isDebugOnly = true;
            }

            // Enable differentiating pending vs. non-pending updates
            currentPackage = new CodePushLocalPackage(
                    currentPackage.AppVersion,
                    currentPackage.DeploymentKey,
                    currentPackage.Description,
                    isFailedUpdate(currentPackage.PackageHash),
                    isFirstRun(currentPackage.PackageHash),
                    currentPackage.IsMandatory,
                    currentUpdateIsPending,
                    currentPackage.Label,
                    currentPackage.PackageHash,
                    isDebugOnly
            );
            return currentPackage;
        }
    }

    private CodePushSyncOptions getDefaultSyncOptions() {
        return new CodePushSyncOptions() {{
            DeploymentKey = mDeploymentKey;
            InstallMode = CodePushInstallMode.ON_NEXT_RESTART;
            MandatoryInstallMode = CodePushInstallMode.IMMEDIATE;
            MinimumBackgroundDuration = 0;
        }};
    }

    public void sync() {
        sync(getDefaultSyncOptions());
    }

    public void sync(CodePushSyncOptions syncOptions) {
        if (mSyncInProgress) {
            syncStatusChange(CodePushSyncStatus.SYNC_IN_PROGRESS);
            CodePushUtils.log("Sync already in progress.");
            return;
        }

        if (syncOptions == null) {
            syncOptions = getDefaultSyncOptions();
        }
        if (syncOptions.DeploymentKey == null || syncOptions.DeploymentKey.isEmpty()) {
            syncOptions.DeploymentKey = mDeploymentKey;
        }
        if (syncOptions.InstallMode == null) {
            syncOptions.InstallMode = CodePushInstallMode.ON_NEXT_RESTART;
        }
        if (syncOptions.MandatoryInstallMode == null) {
            syncOptions.MandatoryInstallMode = CodePushInstallMode.IMMEDIATE;
        }
        if (syncOptions.MinimumBackgroundDuration == null) {
            syncOptions.MinimumBackgroundDuration = 0;
        }
        if (syncOptions.IgnoreFailedUpdates == null) {
            syncOptions.IgnoreFailedUpdates = true;
        }

        CodePushConfiguration nativeConfiguration = getConfiguration();
        CodePushConfiguration configuration = new CodePushConfiguration(
                nativeConfiguration.AppVersion,
                nativeConfiguration.ClientUniqueId,
                syncOptions != null && syncOptions.DeploymentKey != null ? syncOptions.DeploymentKey : nativeConfiguration.DeploymentKey,
                nativeConfiguration.ServerUrl,
                nativeConfiguration.PackageHash
        );

        mSyncInProgress = true;
        notifyApplicationReady();
        syncStatusChange(CodePushSyncStatus.CHECKING_FOR_UPDATE);
        CodePushRemotePackage remotePackage = checkForUpdate(syncOptions.DeploymentKey);

        final boolean updateShouldBeIgnored = remotePackage != null && (remotePackage.FailedInstall && syncOptions.IgnoreFailedUpdates);
        if (remotePackage == null || updateShouldBeIgnored) {
            if (updateShouldBeIgnored) {
                CodePushUtils.log("An update is available, but it is being ignored due to having been previously rolled back.");
            }

            CodePushLocalPackage currentPackage = getCurrentPackage();
            if (currentPackage != null && currentPackage.IsPending) {
                syncStatusChange(CodePushSyncStatus.UPDATE_INSTALLED);
                mSyncInProgress = false;
                return;
            } else {
                syncStatusChange(CodePushSyncStatus.UP_TO_DATE);
                mSyncInProgress = false;
                return;
            }
        } else {
            syncStatusChange(CodePushSyncStatus.DOWNLOADING_PACKAGE);
            CodePushLocalPackage localPackage = downloadUpdate(remotePackage);
            new CodePushAcquisitionManager(configuration).reportStatusDownload(localPackage);

            CodePushInstallMode resolvedInstallMode = localPackage.IsMandatory ? syncOptions.MandatoryInstallMode: syncOptions.InstallMode;
            mCurrentInstallModeInProgress = resolvedInstallMode;
            syncStatusChange(CodePushSyncStatus.INSTALLING_UPDATE);
            installUpdate(localPackage, resolvedInstallMode, syncOptions.MinimumBackgroundDuration);
            syncStatusChange(CodePushSyncStatus.UPDATE_INSTALLED);
            mSyncInProgress = false;
            if(resolvedInstallMode == CodePushInstallMode.IMMEDIATE) {
                mRestartManager.restartApp(false);
            } else {
                mRestartManager.clearPendingRestart();
            }
        }
    }

    public CodePushLocalPackage downloadUpdate(final CodePushRemotePackage updatePackage) {
        CodePushLocalPackage newPackage;
        try {
            JSONObject mutableUpdatePackage = CodePushUtils.convertObjectToJsonObject(updatePackage);
            CodePushUtils.setJSONValueForKey(mutableUpdatePackage, CodePushConstants.BINARY_MODIFIED_TIME_KEY, "" + getBinaryResourcesModifiedTime());
            mUpdateManager.downloadPackage(mutableUpdatePackage, getAssetsBundleFileName(), new DownloadProgressCallback() {
                private boolean hasScheduledNextFrame = false;
                private DownloadProgress latestDownloadProgress = null;

                @Override
                public void call(final DownloadProgress downloadProgress) {
                    latestDownloadProgress = downloadProgress;
                    // If the download is completed, synchronously send the last event.
                    if (latestDownloadProgress.isCompleted()) {
                        downloadProgressChange(downloadProgress.getReceivedBytes(), downloadProgress.getTotalBytes());
                        return;
                    }

                    if (hasScheduledNextFrame) {
                        return;
                    }

                    hasScheduledNextFrame = true;
                    mReactApplicationContext.runOnUiQueueThread(new Runnable() {
                        @Override
                        public void run() {
                            ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.TIMERS_EVENTS, new ChoreographerCompat.FrameCallback() {
                                @Override
                                public void doFrame(long frameTimeNanos) {
                                    if (!latestDownloadProgress.isCompleted()) {
                                        downloadProgressChange(downloadProgress.getReceivedBytes(), downloadProgress.getTotalBytes());
                                    }

                                    hasScheduledNextFrame = false;
                                }
                            });
                        }
                    });
                }
            });

            newPackage = mUpdateManagerDeserializer.getPackage(updatePackage.PackageHash);
            return newPackage;
        } catch (IOException e) {
            e.printStackTrace();
            return new CodePushLocalPackage(e);
        } catch (CodePushInvalidUpdateException e) {
            e.printStackTrace();
            mSettingsManager.saveFailedUpdate(CodePushUtils.convertObjectToJsonObject(updatePackage));
            return new CodePushLocalPackage(e);
        }
    }

    public void installUpdate(final CodePushLocalPackage updatePackage, final CodePushInstallMode installMode, final int minimumBackgroundDuration) {
        mUpdateManager.installPackage(CodePushUtils.convertObjectToJsonObject(updatePackage), mSettingsManager.isPendingUpdate(null));

        String pendingHash = updatePackage.PackageHash;
        if (pendingHash == null) {
            throw new CodePushUnknownException("Update package to be installed has no hash.");
        } else {
            mSettingsManager.savePendingUpdate(pendingHash, /* isLoading */false);
        }

        if (installMode == CodePushInstallMode.ON_NEXT_RESUME ||
                // We also add the resume listener if the installMode is IMMEDIATE, because
                // if the current activity is backgrounded, we want to reload the bundle when
                // it comes back into the foreground.
                installMode == CodePushInstallMode.IMMEDIATE ||
                installMode == CodePushInstallMode.ON_NEXT_SUSPEND) {

            // Store the minimum duration on the native module as an instance
            // variable instead of relying on a closure below, so that any
            // subsequent resume-based installs could override it.
            mMinimumBackgroundDuration = minimumBackgroundDuration;

            if (mLifecycleEventListener == null) {
                // Ensure we do not add the listener twice.
                mLifecycleEventListener = new LifecycleEventListener() {
                    private Date lastPausedDate = null;
                    private Handler appSuspendHandler = new Handler(Looper.getMainLooper());
                    private Runnable loadBundleRunnable = new Runnable() {
                        @Override
                        public void run() {
                            CodePushUtils.log("Loading bundle on suspend");
                            mRestartManager.restartApp(false);
                        }
                    };

                    @Override
                    public void onHostResume() {
                        appSuspendHandler.removeCallbacks(loadBundleRunnable);
                        // As of RN 36, the resume handler fires immediately if the app is in
                        // the foreground, so explicitly wait for it to be backgrounded first
                        if (lastPausedDate != null) {
                            long durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                            if (installMode == CodePushInstallMode.IMMEDIATE
                                    || durationInBackground >= mMinimumBackgroundDuration) {
                                CodePushUtils.log("Loading bundle on resume");
                                mRestartManager.restartApp(false);
                            }
                        }
                    }

                    @Override
                    public void onHostPause() {
                        // Save the current time so that when the app is later
                        // resumed, we can detect how long it was in the background.
                        lastPausedDate = new Date();

                        if (installMode == CodePushInstallMode.ON_NEXT_SUSPEND && mSettingsManager.isPendingUpdate(null)) {
                            appSuspendHandler.postDelayed(loadBundleRunnable, minimumBackgroundDuration * 1000);
                        }
                    }

                    @Override
                    public void onHostDestroy() {
                    }
                };

                mReactApplicationContext.addLifecycleEventListener(mLifecycleEventListener);
            }
        }
    }

    public boolean isFailedUpdate(String packageHash) {
        return mSettingsManager.isFailedHash(packageHash);
    }

    public boolean isFirstRun(String packageHash) {
        return didUpdate()
                && packageHash != null
                && packageHash.length() > 0
                && packageHash.equals(mUpdateManager.getCurrentPackageHash());
    }

    public void removePendingUpdate() {
        mSettingsManager.removePendingUpdate();
    }

    public void notifyApplicationReady() {
        mSettingsManager.removePendingUpdate();
        final CodePushStatusReport statusReport = getNewStatusReport();
        if (statusReport != null) {
            tryReportStatus(statusReport);
        }
    }

    private void loadBundle() {
        clearLifecycleEventListener();
        clearDebugCacheIfNeeded();
        try {
            // #1) Get the ReactInstanceManager instance, which is what includes the
            //     logic to reload the current React context.
            final ReactInstanceManager instanceManager = resolveInstanceManager();
            if (instanceManager == null) {
                return;
            }

            String latestJSBundleFile = getJSBundleFileInternal(getAssetsBundleFileName());

            // #2) Update the locally stored JS bundle file path
            setJSBundle(instanceManager, latestJSBundleFile);

            // #3) Get the context creation method and fire it on the UI thread (which RN enforces)
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        // This workaround has been implemented in order to fix https://github.com/facebook/react-native/issues/14533
                        // resetReactRootViews allows to call recreateReactContextInBackground without any exceptions
                        // This fix also relates to https://github.com/Microsoft/react-native-code-push/issues/878
                        resetReactRootViews(instanceManager);

                        instanceManager.recreateReactContextInBackground();
                        initializeUpdateAfterRestart();
                    } catch (Exception e) {
                        // The recreation method threw an unknown exception
                        // so just simply fallback to restarting the Activity (if it exists)
                        loadBundleLegacy();
                    }
                }
            });

        } catch (Exception e) {
            // Our reflection logic failed somewhere
            // so fall back to restarting the Activity (if it exists)
            loadBundleLegacy();
        }
    }

    private void resetReactRootViews(ReactInstanceManager instanceManager) throws NoSuchFieldException, IllegalAccessException {
        Field mAttachedRootViewsField = instanceManager.getClass().getDeclaredField("mAttachedRootViews");
        mAttachedRootViewsField.setAccessible(true);
        List<ReactRootView> mAttachedRootViews = (List<ReactRootView>)mAttachedRootViewsField.get(instanceManager);
        for (ReactRootView reactRootView : mAttachedRootViews) {
            reactRootView.removeAllViews();
            reactRootView.setId(View.NO_ID);
        }
        mAttachedRootViewsField.set(instanceManager, mAttachedRootViews);
    }

    private void clearLifecycleEventListener() {
        // Remove LifecycleEventListener to prevent infinite restart loop
        if (mLifecycleEventListener != null) {
            mReactApplicationContext.removeLifecycleEventListener(mLifecycleEventListener);
            mLifecycleEventListener = null;
        }
    }

    // Use reflection to find the ReactInstanceManager. See #556 for a proposal for a less brittle way to approach this.
    private ReactInstanceManager resolveInstanceManager() throws NoSuchFieldException, IllegalAccessException {
        ReactInstanceManager instanceManager = CodePushCore.getReactInstanceManager();
        if (instanceManager != null) {
            return instanceManager;
        }

        final Activity currentActivity = mReactApplicationContext.getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }

        ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
        instanceManager = reactApplication.getReactNativeHost().getReactInstanceManager();

        return instanceManager;
    }

    // Use reflection to find and set the appropriate fields on ReactInstanceManager. See #556 for a proposal for a less brittle way
    // to approach this.
    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws IllegalAccessException {
        try {
            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            Class<?> jsBundleLoaderClass = Class.forName("com.facebook.react.cxxbridge.JSBundleLoader");
            Method createFileLoaderMethod = null;
            String createFileLoaderMethodName = latestJSBundleFile.toLowerCase().startsWith("assets://")
                    ? "createAssetLoader" : "createFileLoader";

            Method[] methods = jsBundleLoaderClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(createFileLoaderMethodName)) {
                    createFileLoaderMethod = method;
                    break;
                }
            }

            if (createFileLoaderMethod == null) {
                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            int numParameters = createFileLoaderMethod.getGenericParameterTypes().length;
            Object latestJSBundleLoader;

            if (numParameters == 1) {
                // RN >= v0.34
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, latestJSBundleFile);
            } else if (numParameters == 2) {
                // AssetLoader instance
                latestJSBundleLoader = createFileLoaderMethod.invoke(jsBundleLoaderClass, mReactApplicationContext, latestJSBundleFile);
            } else {
                throw new NoSuchMethodException("Could not find a recognized 'createFileLoader' method");
            }

            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {
            CodePushUtils.log("Unable to set JSBundle - CodePush may not support this version of React Native");
            throw new IllegalAccessException("Could not setJSBundle");
        }
    }

    private void loadBundleLegacy() {
        final Activity currentActivity = mReactApplicationContext.getCurrentActivity();
        if (currentActivity == null) {
            // The currentActivity can be null if it is backgrounded / destroyed, so we simply
            // no-op to prevent any null pointer exceptions.
            return;
        }
        invalidateCurrentInstance();

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.recreate();
            }
        });
    }

    public boolean restartApp(boolean onlyIfUpdateIsPending) {
        // If this is an unconditional restart request, or there
        // is current pending update, then reload the app.
        if (!onlyIfUpdateIsPending || mSettingsManager.isPendingUpdate(null)) {
            loadBundle();
            return true;
        }

        return false;
    }

    private void tryReportStatus(final CodePushStatusReport statusReport) {
        CodePushConfiguration nativeConfiguration = getConfiguration();
        if (statusReport.AppVersion != null && !statusReport.AppVersion.isEmpty()) {
            CodePushUtils.log("Reporting binary update (" + statusReport.AppVersion + ")");
            new CodePushAcquisitionManager(nativeConfiguration).reportStatusDeploy(statusReport);
        } else {
            if (statusReport.Status.equals(CodePushDeploymentStatus.SUCCEEDED)) {
                CodePushUtils.log("Reporting CodePush update success (" + statusReport.Label + ")");
            } else {
                CodePushUtils.log("Reporting CodePush update rollback (" + statusReport.Label + ")");
            }

            CodePushConfiguration configuration = new CodePushConfiguration(
                    nativeConfiguration.AppVersion,
                    nativeConfiguration.ClientUniqueId,
                    statusReport.Package.DeploymentKey,
                    nativeConfiguration.ServerUrl,
                    nativeConfiguration.PackageHash
            );
            if (new CodePushAcquisitionManager(configuration).reportStatusDeploy(statusReport)) {
                recordStatusReported(statusReport);
            }
        }
    }

    public void recordStatusReported(CodePushStatusReport statusReport) {
        mTelemetryManager.recordStatusReported(statusReport);
    }

    public CodePushStatusReport getNewStatusReport() {
        if (needToReportRollback()) {
            setNeedToReportRollback(false);
            JSONArray failedUpdates = mSettingsManager.getFailedUpdates();
            if (failedUpdates != null && failedUpdates.length() > 0) {
                try {
                    JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                    WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWritable(lastFailedPackageJSON);
                    CodePushStatusReport failedStatusReport = mTelemetryManagerDeserializer.getRollbackReport(lastFailedPackage);
                    if (failedStatusReport != null) {
                        return failedStatusReport;
                    }
                } catch (JSONException e) {
                    throw new CodePushUnknownException("Unable to read failed updates information stored in SharedPreferences.", e);
                }
            }
        } else if (didUpdate()) {
            JSONObject currentPackage = mUpdateManager.getCurrentPackage();
            if (currentPackage != null) {
                CodePushStatusReport newPackageStatusReport = mTelemetryManagerDeserializer.getUpdateReport(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                if (newPackageStatusReport != null) {
                    return newPackageStatusReport;
                }
            }
        } else if (isRunningBinaryVersion()) {
            CodePushStatusReport newAppVersionStatusReport = mTelemetryManagerDeserializer.getBinaryUpdateReport(getAppVersion());
            if (newAppVersionStatusReport != null) {
                return newAppVersionStatusReport;
            }
        } else {
            CodePushStatusReport retryStatusReport = mTelemetryManagerDeserializer.getRetryStatusReport();
            if (retryStatusReport != null) {
                return retryStatusReport;
            }
        }

        return null;
    }

    public CodePushRestartManager getRestartManager() {
        return mRestartManager;
    }

    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        if (isUsingTestConfiguration()) {
            try {
                mUpdateManager.downloadAndReplaceCurrentBundle(remoteBundleUrl, getAssetsBundleFileName());
            } catch (IOException e) {
                throw new CodePushUnknownException("Unable to replace current bundle", e);
            }
        }
    }

    public void saveStatusReportForRetry(CodePushStatusReport statusReport) {
        mTelemetryManager.saveStatusReportForRetry(statusReport);
    }
}
