package com.microsoft.codepush.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.interfaces.AppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushBinaryVersionMismatchListener;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.interfaces.PublicKeyProvider;
import com.microsoft.codepush.common.managers.CodePushRestartManager;
import com.microsoft.codepush.common.managers.CodePushTelemetryManager;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.managers.SettingsManager;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;
import com.microsoft.codepush.common.utils.PlatformUtils;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.enums.CodePushInstallMode;
import com.microsoft.codepush.common.enums.CodePushSyncStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.ON_NEXT_RESTART;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.ON_NEXT_RESUME;

/**
 * Base core for CodePush. Singleton.
 */
public abstract class CodePushBaseCore {

    /**
     * Deployment key for checking for updates.
     */
    private String mDeploymentKey;

    /**
     * CodePush server URL.
     */
    private static String mServerUrl = "https://codepush.azurewebsites.net/";

    /**
     * Public key for code signing verification.
     */
    private static String mPublicKey;

    /**
     * Path to bundle
     */
    private String mAppEntryPoint;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Indicates whether application is running in debug mode.
     */
    private final boolean mIsDebugMode;

    /**
     * Current app version.
     */
    private String mAppVersion;

    /**
     * Current state of CodePush update.
     */
    private CodePushUpdateState mUpdateState;

    /**
     * Instance of {@link CodePushUtils}.
     */
    private final CodePushUtils mUtils;

    /**
     * Instance of {@link FileUtils}.
     */
    private final FileUtils mFileUtils;

    /**
     * Instance of {@link CodePushUpdateUtils}.
     */
    private final CodePushUpdateUtils mUpdateUtils;

    /**
     * Instance of {@link PlatformUtils}.
     */
    private final PlatformUtils mPlatformUtils;

    /**
     * Instance of {@link CodePushUpdateManager}.
     */
    private CodePushUpdateManager mUpdateManager;

    /**
     * Instance of {@link CodePushTelemetryManager}.
     */
    private CodePushTelemetryManager mTelemetryManager;

    /**
     * Instance of {@link SettingsManager}.
     */
    private SettingsManager mSettingsManager;

    /**
     * Instance of {@link CodePushRestartManager}.
     */
    private CodePushRestartManager mRestartManager;

    /**
     * List of {@link CodePushSyncStatusListener}.
     */
    private List<CodePushSyncStatusListener> mSyncStatusListeners = new ArrayList<>();

    /**
     * List of {@link CodePushDownloadProgressListener}.
     */
    private List<CodePushDownloadProgressListener> mDownloadProgressListeners = new ArrayList<>();

    /**
     * List of {@link CodePushBinaryVersionMismatchListener}.
     */
    private List<CodePushBinaryVersionMismatchListener> mBinaryVersionMismatchListeners = new ArrayList<>();

    /**
     * Self-reference to the current instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static CodePushBaseCore mCurrentInstance;

    void CodePushBaseCore(String deploymentKey, Context context) {
        CodePushBaseCore(deploymentKey, context, false);
    }

    abstract void CodePushBaseCore(String deploymentKey, Context context, boolean isDebugMode);

    CodePushBaseCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            PublicKeyProvider publicKeyProvider,
            AppEntryPointProvider appEntryPointProvider,
            PlatformUtils platformUtils,
            CodePushRestartListener restartListener
    ) throws CodePushInitializeException {

        /* Initialize configuration */
        mDeploymentKey = deploymentKey;
        mContext = context.getApplicationContext();
        mIsDebugMode = isDebugMode;
        if (serverUrl != null) {
            mServerUrl = serverUrl;
        }
        mPublicKey = publicKeyProvider.getPublicKey();
        mAppEntryPoint = appEntryPointProvider.getAppEntryPoint();
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new CodePushInitializeException("Unable to get package info for " + mContext.getPackageName(), e);
        }

        /* Initialize utils */
        mFileUtils = FileUtils.getInstance();
        mUtils = CodePushUtils.getInstance(mFileUtils);
        mUpdateUtils = CodePushUpdateUtils.getInstance(mFileUtils, mUtils);
        mPlatformUtils = platformUtils;

        /* Initialize managers */
        String documentsDirectory = mContext.getFilesDir().getAbsolutePath();
        mUpdateManager = new CodePushUpdateManager(documentsDirectory, platformUtils, mFileUtils, mUtils, mUpdateUtils);
        mSettingsManager = new SettingsManager(mContext, mUtils);
        mTelemetryManager = new CodePushTelemetryManager(mSettingsManager);
        mRestartManager = new CodePushRestartManager(restartListener);

        mCurrentInstance = this;

        if (mIsDebugMode && mSettingsManager.isPendingUpdate(null)) {
            try {
                mPlatformUtils.clearDebugCache();
            } catch (IOException e) {
                throw new CodePushInitializeException(e);
            }
        }

        try {
            initializeUpdateAfterRestart();
        } catch (CodePushGetPackageException | CodePushGeneralException | CodePushRollbackException e) {
            throw new CodePushInitializeException(e);
        }
    }

    /**
     * Gets current app version.
     *
     * @return current app version.
     */
    public String getAppVersion() {
        return mAppVersion;
    }

    /**
     * Sets current app version.
     *
     * @param mAppVersion current app version.
     */
    public void setAppVersion(String mAppVersion) {
        this.mAppVersion = mAppVersion;
    }

    /**
     * Adds listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mSyncStatusListeners.add(syncStatusListener);
    }

    /**
     * Adds listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mDownloadProgressListeners.add(downloadProgressListener);
    }

    /**
     * Adds listener for binary version misatch event.
     *
     * @param listener listener for binary version misatch event.
     */
    public void addBinaryVersionMismatchListener(CodePushBinaryVersionMismatchListener listener) {
        mBinaryVersionMismatchListeners.add(listener);
    }

    private void syncStatusChange(CodePushSyncStatus syncStatus) throws Exception {
        for (CodePushSyncStatusListener syncStatusListener : mSyncStatusListeners) {
            syncStatusListener.syncStatusChanged(syncStatus);
        }
        switch (syncStatus) {
            case CHECKING_FOR_UPDATE: {
                AppCenterLog.info(CodePush.LOG_TAG, "Checking for update.");
                break;
            }
            case AWAITING_USER_ACTION: {
                AppCenterLog.info(CodePush.LOG_TAG, "Awaiting user action.");
                break;
            }
            case DOWNLOADING_PACKAGE: {
                AppCenterLog.info(CodePush.LOG_TAG, "Downloading package.");
                break;
            }
            case INSTALLING_UPDATE: {
                AppCenterLog.info(CodePush.LOG_TAG, "Installing update.");
                break;
            }
            case UP_TO_DATE: {
                AppCenterLog.info(CodePush.LOG_TAG, "App is up to date.");
                break;
            }
            case UPDATE_IGNORED: {
                AppCenterLog.info(CodePush.LOG_TAG, "User cancelled the update.");
                break;
            }
            case UPDATE_INSTALLED: {
                if (mUpdateState.mCurrentInstallModeInProgress == ON_NEXT_RESTART) {
                    AppCenterLog.info(CodePush.LOG_TAG, "Update is installed and will be run on the next app restart.");
                } else if (mUpdateState.mCurrentInstallModeInProgress == ON_NEXT_RESUME) {
                    AppCenterLog.info(CodePush.LOG_TAG, "Update is installed and will be run after the app has been in the background for at least " + mMinimumBackgroundDuration + " seconds.");
                } else {
                    AppCenterLog.info(CodePush.LOG_TAG, "Update is installed and will be run when the app next resumes.");
                }
                break;
            }
            case UNKNOWN_ERROR: {
                AppCenterLog.info(CodePush.LOG_TAG, "An unknown error occurred.");
                break;
            }
        }
    }

    private void downloadProgressChange(long receivedBytes, long totalBytes) throws Exception {
        for (CodePushDownloadProgressListener downloadProgressListener : mDownloadProgressListeners) {
            downloadProgressListener.downloadProgressChanged(receivedBytes, totalBytes);
        }
    }

    private void binaryVersionMismatchChange(CodePushRemotePackage update) throws Exception {
        for (CodePushBinaryVersionMismatchListener listener : mBinaryVersionMismatchListeners) {
            listener.binaryVersionMismatchChanged(update);
        }
    }

    /**
     * Initializes update after app restart.
     */
    private void initializeUpdateAfterRestart() throws CodePushGetPackageException, CodePushGeneralException, CodePushRollbackException {

        /* Reset the state which indicates that
         * the app was just freshly updated. */
        mUpdateState.mDidUpdate = false;
        CodePushPendingUpdate pendingUpdate = mSettingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            CodePushLocalPackage packageMetadata = mUpdateManager.getCurrentPackage();
            if (packageMetadata == null || !mPlatformUtils.isPackageLatest(packageMetadata, mAppVersion) &&
                    !mAppVersion.equals(packageMetadata.getAppVersion())) {
                AppCenterLog.info(LOG_TAG, "Skipping initializeUpdateAfterRestart(), binary version is newer.");
                return;
            }
            boolean updateIsLoading = pendingUpdate.isPendingUpdateLoading();
            if (updateIsLoading) {

                /* Pending update was initialized, but notifyApplicationReady was not called.
                 * Therefore, deduce that it is a broken update and rollback. */
                AppCenterLog.info(LOG_TAG, "Update did not finish loading the last time, rolling back to a previous version.");
                mUpdateState.mNeedToReportRollback = true;
                rollbackPackage();
            } else {

                /* There is in fact a new update running for the first
                 * time, so update the local state to ensure the client knows. */
                mUpdateState.mDidUpdate = true;

                /* Mark that we tried to initialize the new update, so that if it crashes,
                 * we will know that we need to rollback when the app next starts. */
                mSettingsManager.savePendingUpdate(pendingUpdate);
            }
        }
    }

    private void rollbackPackage() throws CodePushGetPackageException, CodePushRollbackException {
        CodePushLocalPackage failedPackage = mUpdateManager.getCurrentPackage();
        mSettingsManager.saveFailedUpdate(failedPackage);
        mUpdateManager.rollbackPackage();
        mSettingsManager.removePendingUpdate();
    }
}
