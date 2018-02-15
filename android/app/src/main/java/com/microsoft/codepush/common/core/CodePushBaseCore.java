package com.microsoft.codepush.common.core;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePush;
import com.microsoft.codepush.common.CodePushConfiguration;
import com.microsoft.codepush.common.apirequests.ApiHttpRequest;
import com.microsoft.codepush.common.apirequests.DownloadPackageTask;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
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
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushInvalidPublicKeyException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.exceptions.CodePushPlatformUtilsException;
import com.microsoft.codepush.common.exceptions.CodePushQueryUpdateException;
import com.microsoft.codepush.common.exceptions.CodePushReportStatusException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.exceptions.CodePushUnzipException;
import com.microsoft.codepush.common.interfaces.CodePushAppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushBinaryVersionMismatchListener;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationCallback;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.interfaces.CodePushDownloadProgressListener;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;
import com.microsoft.codepush.common.interfaces.CodePushPublicKeyProvider;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.interfaces.CodePushSyncStatusListener;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.managers.CodePushAcquisitionManager;
import com.microsoft.codepush.common.managers.CodePushRestartManager;
import com.microsoft.codepush.common.managers.CodePushTelemetryManager;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.managers.SettingsManager;
import com.microsoft.codepush.common.utils.CodePushLogUtils;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import static android.text.TextUtils.isEmpty;
import static com.microsoft.codepush.common.CodePush.LOG_TAG;
import static com.microsoft.codepush.common.CodePushConstants.PACKAGE_FILE_NAME;
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
    @SuppressWarnings("WeakerAccess")
    protected String mDeploymentKey;

    /**
     * CodePush server URL.
     */
    @SuppressWarnings("WeakerAccess")
    protected static String mServerUrl = "https://codepush.azurewebsites.net/";

    /**
     * Public key for code signing verification.
     */
    @SuppressWarnings("WeakerAccess")
    protected static String mPublicKey;

    /**
     * Entry point for application.
     */
    @SuppressWarnings("WeakerAccess")
    protected String mAppEntryPoint;

    /**
     * Application context.
     */
    @SuppressWarnings("WeakerAccess")
    protected final Context mContext;

    /**
     * Indicates whether application is running in debug mode.
     */
    @SuppressWarnings("WeakerAccess")
    protected final boolean mIsDebugMode;

    /**
     * Current app version.
     */
    @SuppressWarnings("WeakerAccess")
    protected String mAppVersion;

    /**
     * Current state of CodePush update.
     */
    @SuppressWarnings("WeakerAccess")
    protected CodePushState mState;

    /**
     * Used utilities.
     */
    @SuppressWarnings("WeakerAccess")
    protected CodePushUtilities mUtilities;

    /**
     * Used managers.
     */
    @SuppressWarnings("WeakerAccess")
    protected CodePushManagers mManagers;

    /**
     * Used listeners.
     */
    @SuppressWarnings("WeakerAccess")
    protected CodePushListeners mListeners;

    /**
     * Instance of {@link CodePushConfirmationDialog}.
     */
    @SuppressWarnings("WeakerAccess")
    protected CodePushConfirmationDialog mConfirmationDialog;

    /**
     * Current instance of {@link CodePushBaseCore}.
     */
    protected static CodePushBaseCore mCurrentInstance;

    /**
     * Creates instance of {@link CodePushBaseCore}. Default constructor.
     * We pass {@link Application} and app secret here, too, because we can't initialize AppCenter in another constructor and then call this.
     * However, AppCenter must be initialized before creating anything else.
     *
     * @param deploymentKey         deployment key.
     * @param context               application context.
     * @param isDebugMode           indicates whether application is running in debug mode.
     * @param serverUrl             CodePush server url.
     * @param publicKeyProvider     instance of {@link CodePushPublicKeyProvider}.
     * @param appEntryPointProvider instance of {@link CodePushAppEntryPointProvider}.
     * @param platformUtils         instance of {@link CodePushPlatformUtils}.
     * @param application           application instance (pass <code>null</code> if you don't need {@link Crashes} integration for tracking exceptions).
     * @param appSecret             the value of app secret from AppCenter portal to configure {@link Crashes} sdk.
     *                              Pass <code>null</code> if you don't need {@link Crashes} integration for tracking exceptions.
     * @throws CodePushInitializeException error occurred during the initialization.
     */
    protected CodePushBaseCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            CodePushPublicKeyProvider publicKeyProvider,
            CodePushAppEntryPointProvider appEntryPointProvider,
            CodePushPlatformUtils platformUtils,
            Application application,
            String appSecret
    ) throws CodePushInitializeException {
        if (appSecret != null) {
            AppCenter.start(application, appSecret, Crashes.class);
            CodePushLogUtils.setEnabled(true);
        }

        /* Initialize configuration. */
        mDeploymentKey = deploymentKey;
        mContext = context.getApplicationContext();
        mIsDebugMode = isDebugMode;
        if (serverUrl != null) {
            mServerUrl = serverUrl;
        }
        try {
            mPublicKey = publicKeyProvider.getPublicKey();
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException | CodePushInvalidPublicKeyException e) {
            throw new CodePushInitializeException("Unable to get package info for " + mContext.getPackageName(), e);
        }

        /* Initialize utilities. */
        FileUtils fileUtils = FileUtils.getInstance();
        CodePushUtils utils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils updateUtils = CodePushUpdateUtils.getInstance(fileUtils, utils);
        mUtilities = new CodePushUtilities(utils, fileUtils, updateUtils, platformUtils);

        /* Initialize managers. */
        String documentsDirectory = mContext.getFilesDir().getAbsolutePath();
        CodePushUpdateManager updateManager = new CodePushUpdateManager(documentsDirectory, platformUtils, fileUtils, utils, updateUtils);
        final SettingsManager settingsManager = new SettingsManager(mContext, utils);
        CodePushTelemetryManager telemetryManager = new CodePushTelemetryManager(settingsManager);
        CodePushRestartManager restartManager = new CodePushRestartManager(new CodePushRestartListener() {
            @Override
            public boolean onRestart(boolean onlyIfUpdateIsPending) throws CodePushMalformedDataException {

                /* If this is an unconditional restart request, or there
                /* is current pending update, then reload the app. */
                if (!onlyIfUpdateIsPending || settingsManager.isPendingUpdate(null)) {
                    loadApp();
                    return true;
                }

                return false;
            }
        });
        CodePushAcquisitionManager acquisitionManager = new CodePushAcquisitionManager(utils, fileUtils);
        mManagers = new CodePushManagers(updateManager, telemetryManager, settingsManager, restartManager, acquisitionManager);

        /* Initialize state */
        mState = new CodePushState();
        try {

            /* Clear debug cache if needed. */
            if (mIsDebugMode && mManagers.mSettingsManager.isPendingUpdate(null)) {
                mUtilities.mPlatformUtils.clearDebugCache(mContext);
            }
        } catch (IOException | CodePushMalformedDataException e) {
            throw new CodePushInitializeException(e);
        }
        /* Initialize update after restart. */
        try {
            initializeUpdateAfterRestart();
        } catch (CodePushGetPackageException | CodePushPlatformUtilsException | CodePushRollbackException | CodePushGeneralException | CodePushMalformedDataException e) {
            throw new CodePushInitializeException(e);
        }
        mCurrentInstance = this;

        /* appEntryPointProvider.getAppEntryPoint() implementation for RN uses static instance on CodePushBaseCore
         * so we place it here to avoid null pointer reference. */
        try {
            mAppEntryPoint = appEntryPointProvider.getAppEntryPoint();
        } catch (CodePushNativeApiCallException e) {
            throw new CodePushInitializeException(e);
        }
    }

    /**
     * Creates instance of {@link CodePushBaseCore} for those who want to track exceptions (includes additional parameters).
     *
     * @param deploymentKey         deployment key.
     * @param application           application instance (pass <code>null</code> if you don't need {@link Crashes} integration for tracking exceptions).
     * @param isDebugMode           indicates whether application is running in debug mode.
     * @param serverUrl             CodePush server url.
     * @param appSecret             the value of app secret from AppCenter portal to configure {@link Crashes} sdk.
     *                              Pass <code>null</code> if you don't need {@link Crashes} integration for tracking exceptions.
     * @param publicKeyProvider     instance of {@link CodePushPublicKeyProvider}.
     * @param appEntryPointProvider instance of {@link CodePushAppEntryPointProvider}.
     * @param platformUtils         instance of {@link CodePushPlatformUtils}.
     * @throws CodePushInitializeException error occurred during the initialization.
     */
    protected CodePushBaseCore(
            @NonNull String deploymentKey,
            @NonNull Application application,
            boolean isDebugMode,
            String serverUrl,
            String appSecret,
            CodePushPublicKeyProvider publicKeyProvider,
            CodePushAppEntryPointProvider appEntryPointProvider,
            CodePushPlatformUtils platformUtils
    ) throws CodePushInitializeException {
        this(deploymentKey, application.getApplicationContext(), isDebugMode, serverUrl, publicKeyProvider, appEntryPointProvider, platformUtils, application, appSecret);
    }

    /**
     * Adds listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mListeners.mSyncStatusListeners.add(syncStatusListener);
    }

    /**
     * Adds listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mListeners.mDownloadProgressListeners.add(downloadProgressListener);
    }

    /**
     * Removes listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    public void removeSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mListeners.mSyncStatusListeners.remove(syncStatusListener);
    }

    /**
     * Removes listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    public void removeDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mListeners.mDownloadProgressListeners.remove(downloadProgressListener);
    }

    /**
     * Gets native CodePush configuration.
     *
     * @return native CodePush configuration.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushConfiguration getNativeConfiguration() throws CodePushNativeApiCallException {
        CodePushConfiguration configuration = new CodePushConfiguration();
        try {
            configuration.setAppVersion(mAppVersion);

            configuration.setClientUniqueId(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
            configuration.setDeploymentKey(mDeploymentKey);
            configuration.setServerUrl(mServerUrl);
            configuration.setPackageHash(mUtilities.mUpdateUtils.getHashForBinaryContents(mContext, mIsDebugMode));
        } catch (CodePushIllegalArgumentException | CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
        return configuration;
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches {@link CodePushUpdateState#RUNNING}.
     *
     * @return installed update metadata.
     * @throws CodePushNativeApiCallException if error occurred during the operation.
     */
    public CodePushLocalPackage getUpdateMetadata() throws CodePushNativeApiCallException {
        return getUpdateMetadata(RUNNING);
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches the specified <code>updateState</code> parameter.
     *
     * @param updateState current update state.
     * @return installed update metadata.
     * @throws CodePushNativeApiCallException if error occurred during the operation.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) throws CodePushNativeApiCallException {
        if (updateState == null) {
            updateState = RUNNING;
        }
        CodePushLocalPackage currentPackage;
        try {
            currentPackage = mManagers.mUpdateManager.getCurrentPackage();
        } catch (CodePushGetPackageException e) {
            throw new CodePushNativeApiCallException(e);
        }
        if (currentPackage == null) {
            return null;
        }
        Boolean currentUpdateIsPending = false;
        Boolean isDebugOnly = false;
        if (!isEmpty(currentPackage.getPackageHash())) {
            String currentHash = currentPackage.getPackageHash();
            try {
                currentUpdateIsPending = mManagers.mSettingsManager.isPendingUpdate(currentHash);
            } catch (CodePushMalformedDataException e) {
                throw new CodePushNativeApiCallException(e);
            }
        }
        if (updateState == PENDING && !currentUpdateIsPending) {

            /* The caller wanted a pending update but there isn't currently one. */
            return null;
        } else if (updateState == RUNNING && currentUpdateIsPending) {

            /* The caller wants the running update, but the current one is pending, so we need to grab the previous. */
            CodePushLocalPackage previousPackage;
            try {
                previousPackage = mManagers.mUpdateManager.getPreviousPackage();
            } catch (CodePushGetPackageException e) {
                throw new CodePushNativeApiCallException(e);
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
            currentPackage.setFirstRun(isFirstRun(packageHash));
            currentPackage.setPending(currentUpdateIsPending);
            currentPackage.setDebugOnly(isDebugOnly);
            return currentPackage;
        }
    }

    /**
     * Gets current installed package.
     *
     * @return current installed package.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushLocalPackage getCurrentPackage() throws CodePushNativeApiCallException {
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
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushRemotePackage checkForUpdate(String deploymentKey) throws CodePushNativeApiCallException {
        CodePushConfiguration config = getNativeConfiguration();
        try {
            config.setDeploymentKey(deploymentKey != null ? deploymentKey : config.getDeploymentKey());
        } catch (CodePushIllegalArgumentException e) {
            throw new CodePushNativeApiCallException(e);
        }
        CodePushLocalPackage localPackage;
        localPackage = getCurrentPackage();
        CodePushLocalPackage queryPackage;
        if (localPackage == null) {
            queryPackage = CodePushLocalPackage.createEmptyPackageForCheckForUpdateQuery(config.getAppVersion());
        } else {
            queryPackage = localPackage;
        }
        CodePushRemotePackage update;
        try {
            update = new CodePushAcquisitionManager(mUtilities.mUtils, mUtilities.mFileUtils)
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
     * @param synchronizationOptions sync options.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    public void sync(CodePushSyncOptions synchronizationOptions) throws CodePushNativeApiCallException {
        if (mState.mSyncInProgress) {
            notifyAboutSyncStatusChange(SYNC_IN_PROGRESS);
            AppCenterLog.info(CodePush.LOG_TAG, "Sync already in progress.");
            return;
        }
        final CodePushSyncOptions syncOptions = synchronizationOptions == null ? new CodePushSyncOptions(mDeploymentKey) : synchronizationOptions;
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
        final CodePushConfiguration configuration = getNativeConfiguration();
        if (syncOptions.getDeploymentKey() != null) {
            try {
                configuration.setDeploymentKey(syncOptions.getDeploymentKey());
            } catch (CodePushIllegalArgumentException e) {
                throw new CodePushNativeApiCallException(e);
            }
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
            CodePushLocalPackage currentPackage = getCurrentPackage();
            if (currentPackage != null && currentPackage.isPending()) {
                notifyAboutSyncStatusChange(UPDATE_INSTALLED);
            } else {
                notifyAboutSyncStatusChange(UP_TO_DATE);
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
            mConfirmationDialog.shouldInstallUpdate(updateDialogOptions.getTitle(), message, acceptButtonText, declineButtonText, new CodePushConfirmationCallback() {
                @Override
                public void onResult(boolean userAcceptsProposal) {
                    if (userAcceptsProposal) {
                        try {
                            doDownloadAndInstall(remotePackage, syncOptions, configuration);
                            mState.mSyncInProgress = false;
                        } catch (Exception e) {
                            notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                            mState.mSyncInProgress = false;
                            CodePushLogUtils.trackException(new CodePushNativeApiCallException(e));
                        }
                    } else {
                        notifyAboutSyncStatusChange(UPDATE_IGNORED);
                        mState.mSyncInProgress = false;
                    }
                }

                @Override
                public void throwError(CodePushGeneralException e) {
                    notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                    mState.mSyncInProgress = false;
                    CodePushLogUtils.trackException(new CodePushNativeApiCallException(e));
                }
            });
        } else {
            try {
                doDownloadAndInstall(remotePackage, syncOptions, configuration);
            } catch (Exception e) {
                notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                mState.mSyncInProgress = false;
                throw new CodePushNativeApiCallException(e);
            }
        }
        mState.mSyncInProgress = false;
    }

    /**
     * Notifies the CodePush runtime that a freshly installed update should be considered successful,
     * and therefore, an automatic client-side rollback isn't necessary.
     *
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void notifyApplicationReady() throws CodePushNativeApiCallException {
        mManagers.mSettingsManager.removePendingUpdate();
        final CodePushDeploymentStatusReport statusReport = getNewStatusReport();
        if (statusReport != null) {
            tryReportStatus(statusReport);
        }
    }

    /**
     * Gets instance of {@link CodePushAcquisitionManager}.
     *
     * @return instance of {@link CodePushAcquisitionManager}.
     */
    public CodePushAcquisitionManager getAcquisitionSdk() {
        return mManagers.mAcquisitionManager;
    }

    /**
     * Logs custom message on device.
     *
     * @param message message to be logged.
     */
    public void log(String message) {
        AppCenterLog.info(LOG_TAG, message);
    }

    /**
     * Attempts to restart the application unconditionally (whether there is pending update is ignored).
     */
    public void restartApp() throws CodePushNativeApiCallException {
        try {
            mManagers.mRestartManager.restartApp(false);
        } catch (CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Attempts to restart the application.
     *
     * @param onlyIfUpdateIsPending if <code>true</code>, restart is performed only if update is pending.
     */
    public boolean restartApp(boolean onlyIfUpdateIsPending) throws CodePushNativeApiCallException {
        try {
            return mManagers.mRestartManager.restartApp(onlyIfUpdateIsPending);
        } catch (CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Permits restarts.
     */
    public void disallowRestart() {
        mManagers.mRestartManager.disallowRestarts();
    }

    /**
     * Allows restarts.
     */
    public void allowRestart() throws CodePushNativeApiCallException {
        try {
            mManagers.mRestartManager.allowRestarts();
        } catch (CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
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
     * @param appVersion current app version.
     */
    public void setAppVersion(String appVersion) {
        this.mAppVersion = appVersion;
    }

    /**
     * Adds listener for binary version mismatch event.
     *
     * @param listener listener for binary version mismatch event.
     */
    public void addBinaryVersionMismatchListener(CodePushBinaryVersionMismatchListener listener) {
        mListeners.mBinaryVersionMismatchListeners.add(listener);
    }

    /**
     * Notifies listeners about changed sync status and log it.
     *
     * @param syncStatus sync status.
     */
    @SuppressWarnings("WeakerAccess")
    protected void notifyAboutSyncStatusChange(CodePushSyncStatus syncStatus) {
        for (CodePushSyncStatusListener syncStatusListener : mListeners.mSyncStatusListeners) {
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
        for (CodePushDownloadProgressListener downloadProgressListener : mListeners.mDownloadProgressListeners) {
            downloadProgressListener.downloadProgressChanged(receivedBytes, totalBytes);
        }
    }

    /**
     * Notifies listeners about binary version mismatch between local and remote packages.
     *
     * @param update remote package.
     */
    @SuppressWarnings("WeakerAccess")
    protected void notifyAboutBinaryVersionMismatchChange(CodePushRemotePackage update) {
        for (CodePushBinaryVersionMismatchListener listener : mListeners.mBinaryVersionMismatchListeners) {
            listener.binaryVersionMismatchChanged(update);
        }
    }

    /**
     * Initializes update after app restart.
     *
     * @throws CodePushGetPackageException    if error occurred during the getting current package.
     * @throws CodePushPlatformUtilsException if error occurred during usage of {@link CodePushPlatformUtils}.
     * @throws CodePushRollbackException      if error occurred during rolling back of package.
     */
    @SuppressWarnings("WeakerAccess")
    protected void initializeUpdateAfterRestart() throws CodePushGetPackageException, CodePushRollbackException, CodePushPlatformUtilsException, CodePushGeneralException, CodePushMalformedDataException {

        /* Reset the state which indicates that the app was just freshly updated. */
        mState.mDidUpdate = false;
        CodePushPendingUpdate pendingUpdate = mManagers.mSettingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            CodePushLocalPackage packageMetadata = mManagers.mUpdateManager.getCurrentPackage();
            if (packageMetadata == null || !mUtilities.mPlatformUtils.isPackageLatest(packageMetadata, mAppVersion, mContext) &&
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
                mManagers.mSettingsManager.savePendingUpdate(pendingUpdate);
            }
        }
    }

    /**
     * Rolls back package.
     *
     * @throws CodePushGetPackageException if error occurred during getting current update.
     * @throws CodePushRollbackException   if error occurred during rolling back of package.
     */
    private void rollbackPackage() throws CodePushGetPackageException, CodePushRollbackException, CodePushMalformedDataException {
        CodePushLocalPackage failedPackage = mManagers.mUpdateManager.getCurrentPackage();
        mManagers.mSettingsManager.saveFailedUpdate(failedPackage);
        mManagers.mUpdateManager.rollbackPackage();
        mManagers.mSettingsManager.removePendingUpdate();
    }

    /**
     * Clears any saved updates on device.
     *
     * @throws IOException read/write error occurred while accessing the file system.
     */
    protected void clearUpdates() throws IOException {
        mManagers.mUpdateManager.clearUpdates();
        mManagers.mSettingsManager.removePendingUpdate();
        mManagers.mSettingsManager.removeFailedUpdates();
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash, <code>false</code> otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean existsFailedUpdate(String packageHash) throws CodePushNativeApiCallException {
        try {
            return mManagers.mSettingsManager.existsFailedUpdate(packageHash);
        } catch (CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Indicates whether update with specified packageHash is running for the first time.
     *
     * @param packageHash package hash for check.
     * @return true, if application is running for the first time, false otherwise.
     * @throws CodePushNativeApiCallException if error occurred during the operation.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isFirstRun(String packageHash) throws CodePushNativeApiCallException {
        try {
            return mState.mDidUpdate
                    && !isEmpty(packageHash)
                    && packageHash.equals(mManagers.mUpdateManager.getCurrentPackageHash());
        } catch (IOException | CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Downloads and installs update.
     *
     * @param remotePackage update to use.
     * @param syncOptions   sync options.
     * @param configuration configuration to use.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    private void doDownloadAndInstall(final CodePushRemotePackage remotePackage, final CodePushSyncOptions syncOptions, final CodePushConfiguration configuration) throws CodePushNativeApiCallException {
        notifyAboutSyncStatusChange(DOWNLOADING_PACKAGE);
        CodePushLocalPackage localPackage = downloadUpdate(remotePackage);
        try {
            mManagers.mAcquisitionManager.reportStatusDownload(configuration, localPackage);
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
            try {
                mManagers.mRestartManager.restartApp(false);
            } catch (CodePushMalformedDataException e) {
                throw new CodePushNativeApiCallException(e);
            }
        } else {
            mManagers.mRestartManager.clearPendingRestart();
        }
    }

    /**
     * Installs update.
     *
     * @param updatePackage             update to install.
     * @param installMode               installation mode.
     * @param minimumBackgroundDuration minimum background duration value (see {@link CodePushSyncOptions#minimumBackgroundDuration}).
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void installUpdate(final CodePushLocalPackage updatePackage, final CodePushInstallMode installMode, final int minimumBackgroundDuration) throws CodePushNativeApiCallException {
        try {
            mManagers.mUpdateManager.installPackage(updatePackage.getPackageHash(), mManagers.mSettingsManager.isPendingUpdate(null));
        } catch (CodePushInstallException | CodePushMalformedDataException e) {
            throw new CodePushNativeApiCallException(e);
        }
        String pendingHash = updatePackage.getPackageHash();
        if (pendingHash == null) {
            throw new CodePushNativeApiCallException("Update package to be installed has no hash.");
        } else {
            CodePushPendingUpdate pendingUpdate = new CodePushPendingUpdate();
            pendingUpdate.setPendingUpdateHash(pendingHash);
            pendingUpdate.setPendingUpdateIsLoading(false);
            mManagers.mSettingsManager.savePendingUpdate(pendingUpdate);
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
     * Tries to send status report.
     *
     * @param statusReport report to send.
     */
    @SuppressWarnings("WeakerAccess")
    protected void tryReportStatus(final CodePushDeploymentStatusReport statusReport) throws CodePushNativeApiCallException {
        try {
            CodePushConfiguration configuration = getNativeConfiguration();
            if (!isEmpty(statusReport.getAppVersion())) {
                AppCenterLog.info(CodePush.LOG_TAG, "Reporting binary update (" + statusReport.getAppVersion() + ")");
                mManagers.mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
            } else {
                if (statusReport.getStatus().equals(SUCCEEDED)) {
                    AppCenterLog.info(CodePush.LOG_TAG, "Reporting CodePush update success (" + statusReport.getLabel() + ")");
                } else {
                    AppCenterLog.info(CodePush.LOG_TAG, "Reporting CodePush update rollback (" + statusReport.getLabel() + ")");
                }
                configuration.setDeploymentKey(statusReport.getPackage().getDeploymentKey());
                mManagers.mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
                saveReportedStatus(statusReport);
            }
        } catch (CodePushReportStatusException | CodePushIllegalArgumentException e) {

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
        AppCenterLog.info(CodePush.LOG_TAG, "Report status failed: " + mUtilities.mUtils.convertObjectToJsonString(statusReport));
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
     * Retrieves status report for sending.
     *
     * @return status report for sending.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushDeploymentStatusReport getNewStatusReport() throws CodePushNativeApiCallException {
        if (mState.mNeedToReportRollback) {
            mState.mNeedToReportRollback = false;
            try {
                ArrayList<CodePushPackage> failedUpdates = mManagers.mSettingsManager.getFailedUpdates();
                if (failedUpdates != null && failedUpdates.size() > 0) {
                    CodePushPackage lastFailedPackage = failedUpdates.get(failedUpdates.size() - 1);
                    CodePushDeploymentStatusReport failedStatusReport = mManagers.mTelemetryManager.buildRollbackReport(lastFailedPackage);
                    if (failedStatusReport != null) {
                        return failedStatusReport;
                    }
                }
            } catch (CodePushMalformedDataException e) {
                throw new CodePushNativeApiCallException(e);
            }
        } else if (mState.mDidUpdate) {
            CodePushLocalPackage currentPackage;
            try {
                currentPackage = mManagers.mUpdateManager.getCurrentPackage();
            } catch (CodePushGetPackageException e) {
                throw new CodePushNativeApiCallException(e);
            }
            if (currentPackage != null) {
                try {
                    CodePushDeploymentStatusReport newPackageStatusReport =
                            mManagers.mTelemetryManager.buildUpdateReport(currentPackage);
                    if (newPackageStatusReport != null) {
                        return newPackageStatusReport;
                    }
                } catch (CodePushIllegalArgumentException e) {
                    throw new CodePushNativeApiCallException(e);
                }
            }
        } else if (mState.mIsRunningBinaryVersion) {
            try {
                CodePushDeploymentStatusReport newAppVersionStatusReport = mManagers.mTelemetryManager.buildBinaryUpdateReport(mAppVersion);
                if (newAppVersionStatusReport != null) {
                    return newAppVersionStatusReport;
                }
            } catch (CodePushIllegalArgumentException e) {
                throw new CodePushNativeApiCallException(e);
            }
        } else {
            CodePushDeploymentStatusReport retryStatusReport;
            try {
                retryStatusReport = mManagers.mSettingsManager.getStatusReportSavedForRetry();
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
    @SuppressWarnings("WeakerAccess")
    public void saveReportedStatus(CodePushDeploymentStatusReport statusReport) {
        mManagers.mTelemetryManager.saveReportedStatus(statusReport);
    }

    /**
     * Saves status report for further retry os it's sending.
     *
     * @param statusReport status report.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveStatusReportForRetry(CodePushDeploymentStatusReport statusReport) throws CodePushNativeApiCallException {
        try {
            mManagers.mSettingsManager.saveStatusReportForRetry(statusReport);
        } catch (JSONException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Downloads update.
     *
     * @param updatePackage update to download.
     * @return resulted local package.
     * @throws CodePushNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public CodePushLocalPackage downloadUpdate(final CodePushRemotePackage updatePackage) throws CodePushNativeApiCallException {
        try {
            String binaryModifiedTime = "" + mUtilities.mPlatformUtils.getBinaryResourcesModifiedTime(mContext);
            String appEntryPoint = null;
            String downloadUrl = updatePackage.getDownloadUrl();
            File downloadFile = mManagers.mUpdateManager.getPackageDownloadFile();
            DownloadPackageTask downloadTask = new DownloadPackageTask(mUtilities.mFileUtils, downloadUrl, downloadFile, getDownloadProgressCallbackForUpdateDownload());
            ApiHttpRequest<CodePushDownloadPackageResult> downloadRequest = new ApiHttpRequest<>(downloadTask);
            CodePushDownloadPackageResult downloadPackageResult = mManagers.mUpdateManager.downloadPackage(updatePackage.getPackageHash(), downloadRequest);
            boolean isZip = downloadPackageResult.isZip();
            String newUpdateFolderPath = mManagers.mUpdateManager.getPackageFolderPath(updatePackage.getPackageHash());
            String newUpdateMetadataPath = mUtilities.mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
            if (isZip) {
                mManagers.mUpdateManager.unzipPackage(downloadFile);
                appEntryPoint = mManagers.mUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, updatePackage.getPackageHash(), mPublicKey, mAppEntryPoint);
            } else {
                mUtilities.mFileUtils.moveFile(downloadFile, new File(newUpdateFolderPath), mAppEntryPoint);
            }
            CodePushLocalPackage newPackage = createLocalPackage(false, false, true, false, appEntryPoint, updatePackage);
            newPackage.setBinaryModifiedTime(binaryModifiedTime);
            mUtilities.mUtils.writeObjectToJsonFile(updatePackage, newUpdateMetadataPath);
            return newPackage;
        } catch (IOException | CodePushDownloadPackageException | CodePushUnzipException | CodePushMergeException e) {
            try {
                mManagers.mSettingsManager.saveFailedUpdate(updatePackage);
            } catch (CodePushMalformedDataException ex) {
                throw new CodePushNativeApiCallException(ex);
            }
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Removes pending update.
     */
    public void removePendingUpdate() {
        mManagers.mSettingsManager.removePendingUpdate();
    }

    /**
     * Returns instance of {@link CodePushRestartManager}.
     *
     * @return instance of {@link CodePushRestartManager}.
     */
    public CodePushRestartManager getRestartManager() {
        return mManagers.mRestartManager;
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
     * Gets {@link DownloadProgressCallback} for update downloading that could be used for platform-specific actions.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract DownloadProgressCallback getDownloadProgressCallbackForUpdateDownload();

    /**
     * Performs all work needed to be done on native side to support install modes but {@link CodePushInstallMode#ON_NEXT_RESTART}.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void handleInstallModesForUpdateInstall(CodePushInstallMode installMode);

    /**
     * Removes pending updates information.
     * Retries to send status report on app resume using platform-specific way for it.
     * Use <code>sender.call()</code> to invoke sending of report.
     *
     * @param sender task that sends status report.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void retrySendStatusReportOnAppResume(Callable<Void> sender);

    /**
     * Clears any scheduled attempts to retry send status report.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void clearScheduledAttemptsToRetrySendStatusReport();

    /**
     * Sets the actual confirmation dialog to use.
     *
     * @param dialog instance of {@link CodePushConfirmationDialog}.
     */
    protected abstract void setConfirmationDialog(CodePushConfirmationDialog dialog);

    /**
     * Loads application.
     */
    protected abstract void loadApp();
}
