package com.microsoft.codepush.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.apirequests.ApiHttpRequest;
import com.microsoft.codepush.common.apirequests.DownloadPackageTask;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.datacontracts.CodePushSyncOptions;
import com.microsoft.codepush.common.datacontracts.CodePushUpdateDialog;
import com.microsoft.codepush.common.enums.CodePushInstallMode;
import com.microsoft.codepush.common.enums.CodePushSyncStatus;
import com.microsoft.codepush.common.enums.CodePushUpdateState;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushGetUpdateMetadataException;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;
import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.exceptions.CodePushUnzipException;
import com.microsoft.codepush.common.interfaces.AppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushBinaryVersionMismatchListener;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.interfaces.PublicKeyProvider;
import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.common.managers.CodePushRestartManager;
import com.microsoft.codepush.common.managers.CodePushTelemetryManager;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.managers.SettingsManager;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;
import com.microsoft.codepush.common.utils.PlatformUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static android.text.TextUtils.isEmpty;
import static com.microsoft.codepush.common.CodePush.LOG_TAG;
import static com.microsoft.codepush.common.datacontracts.CodePushLocalPackage.createLocalPackage;
import static com.microsoft.codepush.common.enums.CodePushCheckFrequency.ON_APP_START;
import static com.microsoft.codepush.common.enums.CodePushDeploymentStatus.SUCCEEDED;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.IMMEDIATE;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.ON_NEXT_RESTART;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.ON_NEXT_RESUME;
import static com.microsoft.codepush.common.enums.CodePushInstallMode.ON_NEXT_SUSPEND;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.AWAITING_USER_ACTION;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.CHECKING_FOR_UPDATE;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.DOWNLOADING_PACKAGE;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.SYNC_IN_PROGRESS;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.UNKNOWN_ERROR;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.UPDATE_IGNORED;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.UPDATE_INSTALLED;
import static com.microsoft.codepush.common.enums.CodePushSyncStatus.UP_TO_DATE;
import static com.microsoft.codepush.common.enums.CodePushUpdateState.LATEST;
import static com.microsoft.codepush.common.enums.CodePushUpdateState.PENDING;
import static com.microsoft.codepush.common.enums.CodePushUpdateState.RUNNING;

/**
 * Base core for CodePush. Singleton.
 */
public abstract class CodePushBaseCore {

    /**
     * Deployment key for checking for updates.
     */
    protected String mDeploymentKey;

    /**
     * CodePush server URL.
     */
    protected static String mServerUrl = "https://codepush.azurewebsites.net/";

    /**
     * Public key for code signing verification.
     */
    protected static String mPublicKey;

    /**
     * Path to bundle
     */
    protected final String mAppEntryPoint;

    /**
     * Application context.
     */
    protected Context mContext;

    /**
     * Indicates whether application is running in debug mode.
     */
    protected final boolean mIsDebugMode;

    /**
     * Current app version.
     */
    protected String mAppVersion;

    /**
     * Current state of CodePush update.
     */
    protected CodePushState mState;

    /**
     * Instance of {@link CodePushUtils}.
     */
    protected final CodePushUtils mUtils;

    /**
     * Instance of {@link FileUtils}.
     */
    protected final FileUtils mFileUtils;

    /**
     * Instance of {@link CodePushUpdateUtils}.
     */
    protected final CodePushUpdateUtils mUpdateUtils;

    /**
     * Instance of {@link PlatformUtils}.
     */
    protected final PlatformUtils mPlatformUtils;

    /**
     * Instance of {@link CodePushUpdateManager}.
     */
    protected CodePushUpdateManager mUpdateManager;

    /**
     * Instance of {@link CodePushTelemetryManager}.
     */
    protected CodePushTelemetryManager mTelemetryManager;

    /**
     * Instance of {@link SettingsManager}.
     */
    protected SettingsManager mSettingsManager;

    /**
     * Instance of {@link CodePushRestartManager}.
     */
    protected CodePushRestartManager mRestartManager;

    /**
     * Instance of {@link CodePushAcquisitionManager}.
     */
    protected CodePushAcquisitionManager mAcquisitionManager;

    /**
     * Instance of {@link CodePushConfirmationDialog}.
     */
    protected CodePushConfirmationDialog mConfirmationDialog;

    /**
     * List of {@link CodePushSyncStatusListener}.
     */
    protected List<CodePushSyncStatusListener> mSyncStatusListeners = new ArrayList<>();

    /**
     * List of {@link CodePushDownloadProgressListener}.
     */
    protected List<CodePushDownloadProgressListener> mDownloadProgressListeners = new ArrayList<>();

    /**
     * List of {@link CodePushBinaryVersionMismatchListener}.
     */
    protected List<CodePushBinaryVersionMismatchListener> mBinaryVersionMismatchListeners = new ArrayList<>();

    /**
     * Self-reference to the current instance.
     */
    @SuppressLint("StaticFieldLeak")
    protected static CodePushBaseCore mCurrentInstance;

    protected CodePushBaseCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            PublicKeyProvider publicKeyProvider,
            AppEntryPointProvider appEntryPointProvider,
            PlatformUtils platformUtils,
            CodePushRestartListener restartListener,
            CodePushConfirmationDialog confirmationDialog
    ) throws CodePushInitializeException {

        /* Initialize configuration. */
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

        /* Initialize utils. */
        mFileUtils = FileUtils.getInstance();
        mUtils = CodePushUtils.getInstance(mFileUtils);
        mUpdateUtils = CodePushUpdateUtils.getInstance(mFileUtils, mUtils);
        mPlatformUtils = platformUtils;

        /* Initialize managers. */
        String documentsDirectory = mContext.getFilesDir().getAbsolutePath();
        mUpdateManager = new CodePushUpdateManager(documentsDirectory, platformUtils, mFileUtils, mUtils, mUpdateUtils);
        mSettingsManager = new SettingsManager(mContext, mUtils);
        mTelemetryManager = new CodePushTelemetryManager(mSettingsManager);
        mRestartManager = new CodePushRestartManager(restartListener);
        mAcquisitionManager = new CodePushAcquisitionManager(mUtils, mFileUtils);

        /* Initialize confirmation dialog for update install */
        mConfirmationDialog = confirmationDialog;

        /* Set current instance. */
        mCurrentInstance = this;

        /* Initialize state */
        mState = new CodePushState();

        /* Clear debug cache if needed. */
        if (mIsDebugMode && mSettingsManager.isPendingUpdate(null)) {
            try {
                mPlatformUtils.clearDebugCache();
            } catch (IOException e) {
                throw new CodePushInitializeException(e);
            }
        }

        /* Initialize update after restart. */
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

    /**
     * Notifies listeners about changed sync status and log it.
     *
     * @param syncStatus sync status.
     */
    protected void notifyAboutSyncStatusChange(CodePushSyncStatus syncStatus) {
        for (CodePushSyncStatusListener syncStatusListener : mSyncStatusListeners) {
            syncStatusListener.syncStatusChanged(syncStatus);
        }
        switch (syncStatus) {
            case CHECKING_FOR_UPDATE: {
                AppCenterLog.info(LOG_TAG, "Checking for update.");
                break;
            }
            case AWAITING_USER_ACTION: {
                AppCenterLog.info(LOG_TAG, "Awaiting user action.");
                break;
            }
            case DOWNLOADING_PACKAGE: {
                AppCenterLog.info(LOG_TAG, "Downloading package.");
                break;
            }
            case INSTALLING_UPDATE: {
                AppCenterLog.info(LOG_TAG, "Installing update.");
                break;
            }
            case UP_TO_DATE: {
                AppCenterLog.info(LOG_TAG, "App is up to date.");
                break;
            }
            case UPDATE_IGNORED: {
                AppCenterLog.info(LOG_TAG, "User cancelled the update.");
                break;
            }
            case UPDATE_INSTALLED: {
                if (mState.mCurrentInstallModeInProgress == ON_NEXT_RESTART) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run on the next app restart.");
                } else if (mState.mCurrentInstallModeInProgress == ON_NEXT_RESUME) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run after the app has been in the background for at least " + mState.mMinimumBackgroundDuration + " seconds.");
                } else {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run when the app next resumes.");
                }
                break;
            }
            case UNKNOWN_ERROR: {
                AppCenterLog.info(LOG_TAG, "An unknown error occurred.");
                break;
            }
        }
    }

    /**
     * Notifies listeners about changed update download progress.
     *
     * @param receivedBytes received amount of bytes.
     * @param totalBytes    total amount of bytes.
     */
    protected void notifyAboutDownloadProgressChange(long receivedBytes, long totalBytes) {
        for (CodePushDownloadProgressListener downloadProgressListener : mDownloadProgressListeners) {
            downloadProgressListener.downloadProgressChanged(receivedBytes, totalBytes);
        }
    }

    /**
     * Notifies listeners about binary version mismatch between local and remote packages.
     *
     * @param update remote package.
     */
    protected void notifyAboutBinaryVersionMismatchChange(CodePushRemotePackage update) {
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
        mState.mDidUpdate = false;
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
                mState.mNeedToReportRollback = true;
                rollbackPackage();
            } else {

                /* There is in fact a new update running for the first
                 * time, so update the local state to ensure the client knows. */
                mState.mDidUpdate = true;

                /* Mark that we tried to initialize the new update, so that if it crashes,
                 * we will know that we need to rollback when the app next starts. */
                mSettingsManager.savePendingUpdate(pendingUpdate);
            }
        }
    }

    /**
     * Rollbacks package.
     *
     * @throws CodePushGetPackageException if error occured during getting current update.
     * @throws CodePushRollbackException   if error occured during rollbacking of package.
     */
    private void rollbackPackage() throws CodePushGetPackageException, CodePushRollbackException {
        CodePushLocalPackage failedPackage = mUpdateManager.getCurrentPackage();
        mSettingsManager.saveFailedUpdate(failedPackage);
        mUpdateManager.rollbackPackage();
        mSettingsManager.removePendingUpdate();
    }

    /**
     * Clears any saved updates on device.
     *
     * @throws IOException if i/o error occurred while accessing the file system.
     */
    private void clearUpdates() throws IOException {
        mUpdateManager.clearUpdates();
        mSettingsManager.removePendingUpdate();
        mSettingsManager.removeFailedUpdates();
    }

    /**
     * Gets native CodePush configuration.
     *
     * @return native CodePush configuration
     */
    public CodePushConfiguration getNativeConfiguration() {
        CodePushConfiguration configuration = new CodePushConfiguration();
        configuration.setAppVersion(mAppVersion);

        /* TODO can we just use InstanceId#getId ? */
        configuration.setClientUniqueId(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        configuration.setDeploymentKey(mDeploymentKey);
        configuration.setServerUrl(mServerUrl);
        configuration.setPackageHash(mUpdateUtils.getHashForBinaryContents(mContext, mIsDebugMode));
        return configuration;
    }

    public boolean existsFailedUpdate(String packageHash) {
        return mSettingsManager.existsFailedUpdate(packageHash);
    }

    public boolean isFirstRun(String packageHash) throws IOException, CodePushMalformedDataException {
        return mState.mDidUpdate
                && !isEmpty(packageHash)
                && packageHash.equals(mUpdateManager.getCurrentPackageHash());
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches {@link CodePushUpdateState#RUNNING}.
     *
     * @return installed update metadata.
     * @throws CodePushGetUpdateMetadataException if error occured during the operation.
     */
    public CodePushLocalPackage getUpdateMetadata() throws CodePushGetUpdateMetadataException {
        return getUpdateMetadata(RUNNING);
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches the specified <code>updateState</code> parameter.
     *
     * @param updateState current update state.
     * @return installed update metadata.
     * @throws CodePushGetUpdateMetadataException if error occured during the operation.
     */
    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) throws CodePushGetUpdateMetadataException {
        if (updateState == null) {
            updateState = RUNNING;
        }
        CodePushLocalPackage currentPackage;
        try {
            currentPackage = mUpdateManager.getCurrentPackage();
        } catch (CodePushGetPackageException e) {
            throw new CodePushGetUpdateMetadataException(e);
        }
        if (currentPackage == null) {
            return null;
        }
        Boolean currentUpdateIsPending = false;
        Boolean isDebugOnly = false;
        if (!isEmpty(currentPackage.getPackageHash())) {
            String currentHash = currentPackage.getPackageHash();
            currentUpdateIsPending = mSettingsManager.isPendingUpdate(currentHash);
        }
        if (updateState == PENDING && !currentUpdateIsPending) {

            /* The caller wanted a pending update but there isn't currently one. */
            return null;
        } else if (updateState == RUNNING && currentUpdateIsPending) {

            /* The caller wants the running update, but the current one is pending, so we need to grab the previous. */
            CodePushLocalPackage previousPackage;
            try {
                previousPackage = mUpdateManager.getPreviousPackage();
            } catch (CodePushGetPackageException e) {
                throw new CodePushGetUpdateMetadataException(e);
            }
            if (previousPackage == null) {
                return null;
            }
            return previousPackage;
        } else {

            /*
             * The current package satisfies the request:
             * 1) Caller wanted a pending, and there is a pending update
             * 2) Caller wanted the running update, and there isn't a pending
             * 3) Caller wants the latest update, regardless if it's pending or not
             */
            if (mState.mIsRunningBinaryVersion) {

                /*
                 * This only matters in Debug builds. Since we do not clear "outdated" updates,
                 * we need to indicate to the JS side that somehow we have a current update on
                 * disk that is not actually running.
                 */
                isDebugOnly = true;
            }

            /* Enable differentiating pending vs. non-pending updates */
            String packageHash = currentPackage.getPackageHash();
            currentPackage.setFailedInstall(existsFailedUpdate(packageHash));
            try {
                currentPackage.setFirstRun(isFirstRun(packageHash));
            } catch (IOException | CodePushMalformedDataException e) {
                throw new CodePushGetUpdateMetadataException(e);
            }
            currentPackage.setPending(currentUpdateIsPending);
            currentPackage.setDebugOnly(isDebugOnly);
            return currentPackage;
        }
    }

    public CodePushLocalPackage getCurrentPackage() throws CodePushGetUpdateMetadataException {
        return getUpdateMetadata(LATEST);
    }

    /**
     * Asks the CodePush service whether the configured app deployment has an update available
     * using deploymentKey already set in constructor.
     *
     * @return remote package info if there is an update, <code>null</code> otherwise.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public CodePushRemotePackage checkForUpdate() throws CodePushNativeApiCallException {
        CodePushConfiguration nativeConfiguration = getNativeConfiguration();
        return checkForUpdate(nativeConfiguration.getDeploymentKey());
    }

    /**
     * Asks the CodePush service whether the configured app deployment has an update available
     * using specified deployment key.
     *
     * @param deploymentKey deployment key to use.
     * @return remote package info if there is an update, <code>null</code> otherwise.
     */
    public CodePushRemotePackage checkForUpdate(String deploymentKey) throws CodePushNativeApiCallException {
        CodePushConfiguration config = getNativeConfiguration();
        config.setDeploymentKey(deploymentKey != null ? deploymentKey : config.getDeploymentKey());
        CodePushLocalPackage localPackage;
        try {
            localPackage = getCurrentPackage();
        } catch (CodePushGetUpdateMetadataException e) {
            throw new CodePushNativeApiCallException(e);
        }
        CodePushLocalPackage queryPackage;
        if (localPackage == null) {
            queryPackage = CodePushLocalPackage.createEmptyPackageForCheckForUpdateQuery(config.getAppVersion());
        } else {
            queryPackage = localPackage;
        }
        CodePushRemotePackage update;
        try {
            update = new CodePushAcquisitionManager(mUtils, mFileUtils)
                    .queryUpdateWithCurrentPackage(config, queryPackage);
        } catch (CodePushQueryUpdateException e) {
            throw new CodePushNativeApiCallException(e);
        }
        if (update == null || update.isUpdateAppVersion() ||
                localPackage != null && (update.getPackageHash().equals(localPackage.getPackageHash())) ||
                (localPackage == null || localPackage.isDebugOnly()) && config.getPackageHash().equals(update.getPackageHash())) {
            if (update != null && update.isUpdateAppVersion()) {
                AppCenterLog.info(LOG_TAG, "An update is available but it is not targeting the binary version of your app.");
                notifyAboutBinaryVersionMismatchChange(update);
            }
            return null;
        } else {
            if (deploymentKey != null) {
                update.setDeploymentKey(deploymentKey);
            }
            update.setFailedInstall(existsFailedUpdate(update.getPackageHash()));
            return update;
        }
    }

    /**
     * Synchronizes your app assets with the latest release to the configured deployment using default sync options.
     *
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public void sync() throws CodePushNativeApiCallException {
        sync(new CodePushSyncOptions());
    }

    /**
     * Synchronizes your app assets with the latest release to the configured deployment.
     *
     * @param syncOptions sync options.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public void sync(CodePushSyncOptions syncOptions) throws CodePushNativeApiCallException {
        if (mState.mSyncInProgress) {
            notifyAboutSyncStatusChange(SYNC_IN_PROGRESS);
            AppCenterLog.info(CodePush.LOG_TAG, "Sync already in progress.");
            return;
        }
        if (syncOptions == null) {
            syncOptions = new CodePushSyncOptions(mDeploymentKey);
        }
        if (isEmpty(syncOptions.getDeploymentKey())) {
            syncOptions.setDeploymentKey(mDeploymentKey);
        }
        if (syncOptions.getInstallMode() == null) {
            syncOptions.setInstallMode(ON_NEXT_RESTART);
        }
        if (syncOptions.getMandatoryInstallMode() == null) {
            syncOptions.setMandatoryInstallMode(IMMEDIATE);
        }

        /* minimumBackgroundDuration, ignoreFailedUpdates are primitives and always have default value */
        if (syncOptions.getCheckFrequency() == null) {
            syncOptions.setCheckFrequency(ON_APP_START);
        }
        CodePushConfiguration configuration = getNativeConfiguration();
        if (syncOptions.getDeploymentKey() != null) {
            configuration.setDeploymentKey(syncOptions.getDeploymentKey());
        }

        mState.mSyncInProgress = true;
        notifyApplicationReady();
        notifyAboutSyncStatusChange(CHECKING_FOR_UPDATE);
        final CodePushRemotePackage remotePackage = checkForUpdate(syncOptions.getDeploymentKey());

        final boolean updateShouldBeIgnored =
                remotePackage != null && (remotePackage.isFailedInstall() && syncOptions.getIgnoreFailedUpdates());
        if (remotePackage == null || updateShouldBeIgnored) {
            if (updateShouldBeIgnored) {
                AppCenterLog.info(CodePush.LOG_TAG, "An update is available, but it is being ignored due to having been previously rolled back.");
            }
            CodePushLocalPackage currentPackage;
            try {
                currentPackage = getCurrentPackage();
            } catch (CodePushGetUpdateMetadataException e) {
                throw new CodePushNativeApiCallException(e);
            }
            if (currentPackage != null && currentPackage.isPending()) {
                notifyAboutSyncStatusChange(UPDATE_INSTALLED);
                mState.mSyncInProgress = false;
                return;
            } else {
                notifyAboutSyncStatusChange(UP_TO_DATE);
                mState.mSyncInProgress = false;
                return;
            }
        } else if (syncOptions.getUpdateDialog() != null) {
            CodePushUpdateDialog updateDialogOptions = syncOptions.getUpdateDialog();
            String message;
            String acceptButtonText;
            String declineButtonText = updateDialogOptions.getOptionalIgnoreButtonLabel();
            if (remotePackage.isMandatory()) {
                message = updateDialogOptions.getMandatoryUpdateMessage();
                acceptButtonText = updateDialogOptions.getMandatoryContinueButtonLabel();
            } else {
                message = updateDialogOptions.getOptionalUpdateMessage();
                acceptButtonText = updateDialogOptions.getOptionalInstallButtonLabel();
            }
            if (updateDialogOptions.getAppendReleaseDescription() && !isEmpty(remotePackage.getDescription())) {
                message = updateDialogOptions.getDescriptionPrefix() + " " + remotePackage.getDescription();
            }

            /* Ask user whether he want to install update or ignore it. */
            notifyAboutSyncStatusChange(AWAITING_USER_ACTION);
            boolean userAcceptsProposal;
            try {
                userAcceptsProposal = mConfirmationDialog.shouldInstallUpdate(updateDialogOptions.getTitle(), message, acceptButtonText, declineButtonText);
            } catch (CodePushGeneralException e) {
                notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                mState.mSyncInProgress = false;
                throw new CodePushNativeApiCallException(e);
            }
            if (userAcceptsProposal) {
                try {
                    doDownloadAndInstall(remotePackage, syncOptions, configuration);
                    mState.mSyncInProgress = false;
                } catch (Exception e) {
                    notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                    mState.mSyncInProgress = false;
                    throw new CodePushNativeApiCallException(e);
                }
            } else {
                notifyAboutSyncStatusChange(UPDATE_IGNORED);
                mState.mSyncInProgress = false;
            }

        } else {
            try {
                doDownloadAndInstall(remotePackage, syncOptions, configuration);
                mState.mSyncInProgress = false;
            } catch (Exception e) {
                notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                mState.mSyncInProgress = false;
                throw new CodePushNativeApiCallException(e);
            }
        }
    }

    private void doDownloadAndInstall(final CodePushRemotePackage remotePackage, final CodePushSyncOptions syncOptions, final CodePushConfiguration configuration) throws CodePushNativeApiCallException {
        notifyAboutSyncStatusChange(DOWNLOADING_PACKAGE);
        CodePushLocalPackage localPackage = downloadUpdate(remotePackage);
        try {
            mAcquisitionManager.reportStatusDownload(configuration, localPackage);
        } catch (CodePushReportStatusException e) {
            throw new CodePushNativeApiCallException(e);
        }
        CodePushInstallMode resolvedInstallMode = localPackage.isMandatory() ? syncOptions.getMandatoryInstallMode() : syncOptions.getInstallMode();
        mState.mCurrentInstallModeInProgress = resolvedInstallMode;
        notifyAboutSyncStatusChange(CodePushSyncStatus.INSTALLING_UPDATE);
        installUpdate(localPackage, resolvedInstallMode, syncOptions.getMinimumBackgroundDuration());
        notifyAboutSyncStatusChange(UPDATE_INSTALLED);
        mState.mSyncInProgress = false;
        if (resolvedInstallMode == IMMEDIATE) {
            mRestartManager.restartApp(false);
        } else {
            mRestartManager.clearPendingRestart();
        }
    }

    public void installUpdate(final CodePushLocalPackage updatePackage, final CodePushInstallMode installMode, final int minimumBackgroundDuration) throws CodePushNativeApiCallException {
        try {
            mUpdateManager.installPackage(updatePackage.getPackageHash(), mSettingsManager.isPendingUpdate(null));
        } catch (CodePushInstallException e) {
            throw new CodePushNativeApiCallException(e);
        }
        String pendingHash = updatePackage.getPackageHash();
        if (pendingHash == null) {
            throw new CodePushNativeApiCallException("Update package to be installed has no hash.");
        } else {
            CodePushPendingUpdate pendingUpdate = new CodePushPendingUpdate();
            pendingUpdate.setPendingUpdateHash(pendingHash);
            pendingUpdate.setPendingUpdateIsLoading(false);
            mSettingsManager.savePendingUpdate(pendingUpdate);
        }
        if (installMode == ON_NEXT_RESUME ||

                /* We also add the resume listener if the installMode is IMMEDIATE, because
                 * if the current activity is backgrounded, we want to reload the bundle when
                 * it comes back into the foreground. */
                installMode == IMMEDIATE ||
                installMode == ON_NEXT_SUSPEND) {

            /* Store the minimum duration on the native module as an instance
             * variable instead of relying on a closure below, so that any
             * subsequent resume-based installs could override it. */
            mState.mMinimumBackgroundDuration = minimumBackgroundDuration;
            handleInstallModesForUpdateInstall(installMode);
        }
    }

    /**
     * Notifies the CodePush runtime that a freshly installed update should be considered successful,
     * and therefore, an automatic client-side rollback isn't necessary.
     */
    public void notifyApplicationReady() throws CodePushNativeApiCallException {
        mSettingsManager.removePendingUpdate();
        final CodePushDeploymentStatusReport statusReport = getNewStatusReport();
        if (statusReport != null) {
            tryReportStatus(statusReport);
        }
    }

    /**
     * @param statusReport
     */
    private void tryReportStatus(final CodePushDeploymentStatusReport statusReport) throws CodePushNativeApiCallException {
        try {
            CodePushConfiguration configuration = getNativeConfiguration();
            if (!isEmpty(statusReport.getAppVersion())) {
                AppCenterLog.info(CodePush.LOG_TAG, "Reporting binary update (" + statusReport.getAppVersion() + ")");
                mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
            } else {
                if (statusReport.getStatus().equals(SUCCEEDED)) {
                    AppCenterLog.info(CodePush.LOG_TAG, "Reporting CodePush update success (" + statusReport.getLabel() + ")");
                } else {
                    AppCenterLog.info(CodePush.LOG_TAG, "Reporting CodePush update rollback (" + statusReport.getLabel() + ")");
                }
                configuration.setDeploymentKey(statusReport.getLocalPackage().getDeploymentKey());
                mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
                saveReportedStatus(statusReport);
            }
        } catch (CodePushReportStatusException e) {

            /* In order to do not lose original exception if another one will be thrown during the retry
             * we need to wrap it */
            CodePushNativeApiCallException exceptionToThrow = new CodePushNativeApiCallException(e);
            try {
                retrySendStatusReport(statusReport);
            } catch (CodePushNativeApiCallException retryException) {
                exceptionToThrow = new CodePushNativeApiCallException(exceptionToThrow);
            }
            throw exceptionToThrow;
        }

        /* If there was several attempts to send error reports */
        clearScheduledAttemptsToRetrySendStatusReport();
    }

    /**
     * Attempts to retry sending status report if there was sending error before.
     *
     * @param statusReport status report.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    private void retrySendStatusReport(CodePushDeploymentStatusReport statusReport) throws CodePushNativeApiCallException {

        /* Try again when the app resumes */
        /* TODO check that statusReport.toString() will be serialized into JSON string! */
        AppCenterLog.info(CodePush.LOG_TAG, "Report status failed: " + statusReport.toString());
        saveStatusReportForRetry(statusReport);
        Callable<Void> sender = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final CodePushDeploymentStatusReport statusReport = getNewStatusReport();
                if (statusReport != null) {
                    tryReportStatus(statusReport);
                }
                return null;
            }
        };
        try {
            retrySendStatusReportOnAppResume(sender);
        } catch (Exception e) {
            throw new CodePushNativeApiCallException("Error retry sending status report. ", e);
        }
    }

    /**
     * Retries to send status report on app resume using platform-specific way for it.
     * Use <code>sender.call()</code> to invoke sending of report.
     *
     * @param sender task that sends status report.
     * @throws Exception if error occurred during the process.
     */
    protected abstract void retrySendStatusReportOnAppResume(Callable<Void> sender) throws Exception;

    /**
     * Clears any scheduled attempts to retry send status report.
     */
    protected abstract void clearScheduledAttemptsToRetrySendStatusReport();

    /**
     * Retrieves status report for sending.
     *
     * @return status report for sending.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public CodePushDeploymentStatusReport getNewStatusReport() throws CodePushNativeApiCallException {
        if (mState.mNeedToReportRollback) {
            mState.mNeedToReportRollback = false;
            ArrayList<CodePushLocalPackage> failedUpdates = mSettingsManager.getFailedUpdates();
            if (failedUpdates != null && failedUpdates.size() > 0) {
                CodePushLocalPackage lastFailedPackage = failedUpdates.get(failedUpdates.size() - 1);
                CodePushDeploymentStatusReport failedStatusReport = mTelemetryManager.buildRollbackReport(lastFailedPackage);
                if (failedStatusReport != null) {
                    return failedStatusReport;
                }
            }
        } else if (mState.mDidUpdate) {
            CodePushLocalPackage currentPackage;
            try {
                currentPackage = mUpdateManager.getCurrentPackage();
            } catch (CodePushGetPackageException e) {
                throw new CodePushNativeApiCallException(e);
            }
            if (currentPackage != null) {
                CodePushDeploymentStatusReport newPackageStatusReport =
                        mTelemetryManager.buildUpdateReport(currentPackage);
                if (newPackageStatusReport != null) {
                    return newPackageStatusReport;
                }
            }
        } else if (mState.mIsRunningBinaryVersion) {
            CodePushDeploymentStatusReport newAppVersionStatusReport = mTelemetryManager.buildBinaryUpdateReport(mAppVersion);
            if (newAppVersionStatusReport != null) {
                return newAppVersionStatusReport;
            }
        } else {
            CodePushDeploymentStatusReport retryStatusReport;
            try {
                retryStatusReport = mSettingsManager.getStatusReportSavedForRetry();
            } catch (JSONException e) {
                throw new CodePushNativeApiCallException(e);
            }
            if (retryStatusReport != null) {
                return retryStatusReport;
            }
        }
        return null;
    }

    /**
     * Saves already sent status report.
     *
     * @param statusReport report to save.
     */
    public void saveReportedStatus(CodePushDeploymentStatusReport statusReport) {
        mTelemetryManager.saveReportedStatus(statusReport);
    }

    /**
     * Saves status report for further retry os it's sending.
     *
     * @param statusReport status report.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public void saveStatusReportForRetry(CodePushDeploymentStatusReport statusReport) throws CodePushNativeApiCallException {
        try {
            mSettingsManager.saveStatusReportForRetry(statusReport);
        } catch (JSONException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    public CodePushLocalPackage downloadUpdate(final CodePushRemotePackage updatePackage) throws CodePushNativeApiCallException {
        try {
            String binaryModifiedTime = "" + mPlatformUtils.getBinaryResourcesModifiedTime();
            String appEntryPoint = null;
            String downloadUrl = updatePackage.getDownloadUrl();
            File downloadFile = mUpdateManager.getPackageDownloadFile();
            DownloadPackageTask downloadTask = new DownloadPackageTask(mFileUtils, downloadUrl, downloadFile, getDownloadProgressCallbackForUpdateDownload());
            ApiHttpRequest<CodePushDownloadPackageResult> downloadRequest = new ApiHttpRequest<>(downloadTask);
            CodePushDownloadPackageResult downloadPackageResult = mUpdateManager.downloadPackage(updatePackage.getPackageHash(), downloadRequest);
            boolean isZip = downloadPackageResult.isZip();
            if (isZip) {
                mUpdateManager.unzipPackage(downloadFile);
                appEntryPoint = mUpdateManager.mergeDiff(updatePackage.getPackageHash(), mPublicKey, mAppEntryPoint);
            } else {

            }
            CodePushLocalPackage newPackage = createLocalPackage(false, false, true, false, appEntryPoint, updatePackage);
            newPackage.setBinaryModifiedTime(binaryModifiedTime);

            newPackage = mUpdateManager.getPackage(updatePackage.getPackageHash());

            return newPackage;
        } catch (IOException | CodePushDownloadPackageException | CodePushUnzipException | CodePushMergeException | CodePushGetPackageException e) {

            mSettingsManager.saveFailedUpdate(updatePackage);
            throw new CodePushNativeApiCallException(e);
        }
    }

    protected abstract DownloadProgressCallback getDownloadProgressCallbackForUpdateDownload();

    /**
     * Performs all work needed to be done on native side to support install modes but {@link CodePushInstallMode#ON_NEXT_RESTART}.
     */
    protected abstract void handleInstallModesForUpdateInstall(CodePushInstallMode installMode);

    /**
     * Returns instance of {@link CodePushRestartManager}.
     *
     * @return instance of {@link CodePushRestartManager}.
     */
    public CodePushRestartManager getRestartManager() {
        return mRestartManager;
    }

    /**
     * Returns whether application is running in debug mode.
     *
     * @return whether application is running in debug mode.
     */
    public boolean isDebugMode() {
        return mIsDebugMode;
    }

    /**
     * Removes pending updates information.
     */
    public void removePendingUpdate() {
        mSettingsManager.removePendingUpdate();
    }
}
