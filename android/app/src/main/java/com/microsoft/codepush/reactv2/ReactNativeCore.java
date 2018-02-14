package com.microsoft.codepush.reactv2;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.ChoreographerCompat;
import com.facebook.react.modules.core.ReactChoreographer;
import com.facebook.react.uimanager.ViewManager;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.core.CodePushBaseCore;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.enums.CodePushInstallMode;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.interfaces.CodePushAppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;
import com.microsoft.codepush.common.interfaces.CodePushPublicKeyProvider;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.utils.CodePushLogUtils;
import com.microsoft.codepush.reactv2.interfaces.ReactInstanceHolder;
import com.microsoft.codepush.reactv2.utils.ReactPlatformUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * React-specific instance of {@link CodePushBaseCore}.
 */
@SuppressWarnings("unused")
public class ReactNativeCore extends CodePushBaseCore {

    /**
     * Default file name for javascript bundle.
     */
    private static final String DEFAULT_JS_BUNDLE_NAME = "index.android.bundle";

    /**
     * Prefix to access the bundle.
     */
    private static final String ASSETS_BUNDLE_PREFIX = "assets://";

    /**
     * Instance of {@link ReactInstanceHolder}.
     */
    private static ReactInstanceHolder sReactInstanceHolder;

    /**
     * Instance of the {@link ReactApplicationContext}.
     */
    private static ReactApplicationContext sReactApplicationContext;

    /**
     * Basic listener for lifecycle events.
     */
    private LifecycleEventListener mLifecycleEventListener = null;

    /**
     * Listener for lifecycle events for report.
     */
    private LifecycleEventListener mLifecycleEventListenerForReport = null;

    ReactNativeCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            CodePushPublicKeyProvider publicKeyProvider,
            CodePushAppEntryPointProvider appEntryPointProvider,
            CodePushPlatformUtils platformUtils
    ) throws CodePushInitializeException {
        super(deploymentKey, context, isDebugMode, serverUrl, publicKeyProvider, appEntryPointProvider, platformUtils);
    }

    /**
     * Creates react-specific modules.
     *
     * @param reactApplicationContext app context.
     * @return {@link List} of {@link NativeModule} instances.
     */
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        sReactApplicationContext = reactApplicationContext;
        CodePushNativeModule codePushModule = new CodePushNativeModule(sReactApplicationContext, this);
        CodePushDialog dialogModule = new CodePushDialog(sReactApplicationContext);
        addSyncStatusListener(codePushModule);
        addDownloadProgressListener(codePushModule);
        List<NativeModule> nativeModules = new ArrayList<>();
        nativeModules.add(codePushModule);
        nativeModules.add(dialogModule);
        setConfirmationDialog(dialogModule);
        return nativeModules;
    }

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }

    /**
     * Gets a link to the default javascript bundle file.
     *
     * @return link starting with "assets://" and leading to javascript bundle file.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    public String getJSBundleFile() throws CodePushNativeApiCallException {
        return getJSBundleFile(DEFAULT_JS_BUNDLE_NAME);
    }

    /**
     * Gets a link to the specified javascript bundle file.
     *
     * @param assetsBundleFileName custom bundle file name.
     * @return link starting with "assets://" and leading to javascript bundle file.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    public String getJSBundleFile(String assetsBundleFileName) throws CodePushNativeApiCallException {
        String binaryJsBundleUrl = ASSETS_BUNDLE_PREFIX + assetsBundleFileName;
        try {
            String packageFilePath = mManagers.mUpdateManager.getCurrentPackageEntryPath(assetsBundleFileName);
            if (packageFilePath == null) {

                /* There has not been any downloaded updates. */
                AppCenterLog.info(LOG_TAG, "Loading JS bundle from \"" + binaryJsBundleUrl + "\"");
                mState.mIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
            CodePushLocalPackage packageMetadata = mManagers.mUpdateManager.getCurrentPackage();
            if (ReactPlatformUtils.getInstance().isPackageLatest(packageMetadata, mAppVersion, mContext)) {
                AppCenterLog.info(LOG_TAG, "Loading JS bundle from \"" + binaryJsBundleUrl + "\"");
                mState.mIsRunningBinaryVersion = true;
                return packageFilePath;
            } else {

                /* The binary version is newer. */
                mState.mDidUpdate = false;
                boolean hasBinaryVersionChanged = !mAppVersion.equals(packageMetadata.getAppVersion());
                if (!this.mIsDebugMode || hasBinaryVersionChanged) {
                    this.clearUpdates();
                }
                AppCenterLog.info(LOG_TAG, "Loading JS bundle from \"" + binaryJsBundleUrl + "\"");
                mState.mIsRunningBinaryVersion = true;
                return binaryJsBundleUrl;
            }
        } catch (CodePushGetPackageException | CodePushGeneralException | IOException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    @Override
    public void handleInstallModesForUpdateInstall(final CodePushInstallMode installMode) {
        if (mLifecycleEventListener == null) {

            /* Ensure we do not add the listener twice. */
            mLifecycleEventListener = new LifecycleEventListener() {
                private Date lastPausedDate = null;
                private Handler appSuspendHandler = new Handler(Looper.getMainLooper());
                private Runnable loadBundleRunnable = new Runnable() {
                    @Override
                    public void run() {
                        AppCenterLog.info(LOG_TAG, "Loading bundle on suspend");
                        mManagers.mRestartManager.restartApp(false);
                    }
                };

                @Override
                public void onHostResume() {
                    appSuspendHandler.removeCallbacks(loadBundleRunnable);

                    /* As of RN 36, the resume handler fires immediately if the app is in the foreground, so explicitly wait for it to be backgrounded first. */
                    if (lastPausedDate != null) {
                        long durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                        if (installMode == CodePushInstallMode.IMMEDIATE
                                || durationInBackground >= mState.mMinimumBackgroundDuration) {
                            AppCenterLog.info(LOG_TAG, "Loading bundle on resume");
                            mManagers.mRestartManager.restartApp(false);
                        }
                    }
                }

                @Override
                public void onHostPause() {

                    /* Save the current time so that when the app is later resumed, we can detect how long it was in the background. */
                    lastPausedDate = new Date();
                    if (installMode == CodePushInstallMode.ON_NEXT_SUSPEND && mManagers.mSettingsManager.isPendingUpdate(null)) {
                        appSuspendHandler.postDelayed(loadBundleRunnable, mState.mMinimumBackgroundDuration * 1000);
                    }
                }

                @Override
                public void onHostDestroy() {
                }
            };
            if (sReactApplicationContext != null) {
                sReactApplicationContext.addLifecycleEventListener(mLifecycleEventListener);
            }
        }
    }

    @Override
    protected void retrySendStatusReportOnAppResume(final Callable<Void> sender) {
        if (mLifecycleEventListenerForReport == null) {
            mLifecycleEventListenerForReport = new LifecycleEventListener() {

                @Override
                public void onHostResume() {
                    try {
                        sender.call();
                    } catch (Exception e) {
                        CodePushLogUtils.trackException(e);
                    }
                }

                @Override
                public void onHostPause() {

                }

                @Override
                public void onHostDestroy() {

                }
            };
            sReactApplicationContext.addLifecycleEventListener(mLifecycleEventListenerForReport);
        }
    }

    @Override
    protected void clearScheduledAttemptsToRetrySendStatusReport() {
        if (mLifecycleEventListenerForReport != null) {
            clearLifecycleEventListenerForReport();
        }
    }

    @Override
    protected void setConfirmationDialog(CodePushConfirmationDialog dialog) {
        mConfirmationDialog = dialog;
    }

    @Override
    protected void loadBundle() {
        try {
            clearLifecycleEventListener();
            mUtilities.mPlatformUtils.clearDebugCache(mContext);

            /* #1) Get the ReactInstanceManager instance, which is what includes the
            /*     logic to reload the current React context. */
            final ReactInstanceManager instanceManager = resolveInstanceManager();
            if (instanceManager == null) {
                return;
            }
            String latestJSBundleFile = getJSBundleFile(mAppEntryPoint);

            /* #2) Update the locally stored JS bundle file path. */
            setJSBundle(instanceManager, latestJSBundleFile);

            /* #3) Get the context creation method and fire it on the UI thread (which RN enforces). */
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {

                        /* We don't need to resetReactRootViews anymore
                        /* due the issue https://github.com/facebook/react-native/issues/14533
                        /* has been fixed in RN 0.46.0
                        /* resetReactRootViews(instanceManager); */
                        instanceManager.recreateReactContextInBackground();
                        initializeUpdateAfterRestart();
                    } catch (Exception e) {

                        /* The recreation method threw an unknown exception so just simply fallback to restarting the Activity (if it exists). */
                        loadBundleLegacy();
                    }
                }
            });

        } catch (Exception e) {

            /* Our reflection logic failed somewhere so fall back to restarting the Activity (if it exists). */
            loadBundleLegacy();
        }
    }

    private void loadBundleLegacy() {
        final Activity currentActivity = sReactApplicationContext.getCurrentActivity();
        if (currentActivity == null) {

            /* The currentActivity can be null if it is backgrounded / destroyed, so we simply
            /* no-op to prevent any null pointer exceptions. */
            return;
        }
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentActivity.recreate();
            }
        });
    }

    @Override
    protected DownloadProgressCallback getDownloadProgressCallbackForUpdateDownload() {
        return new DownloadProgressCallback() {
            private boolean hasScheduledNextFrame = false;
            private DownloadProgress latestDownloadProgress = null;

            @Override
            public void call(final DownloadProgress downloadProgress) {
                latestDownloadProgress = downloadProgress;

                /* If the download is completed, synchronously send the last event. */
                if (latestDownloadProgress.isCompleted()) {
                    notifyAboutDownloadProgressChange(downloadProgress.getReceivedBytes(), downloadProgress.getTotalBytes());
                    return;
                }
                if (hasScheduledNextFrame) {
                    return;
                }
                hasScheduledNextFrame = true;

                /* if ReactNative app wasn't been initialized, no need to send download progress to it. */
                if (sReactApplicationContext != null) {
                    sReactApplicationContext.runOnUiQueueThread(new Runnable() {
                        @Override
                        public void run() {
                            ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.TIMERS_EVENTS, new ChoreographerCompat.FrameCallback() {
                                @Override
                                public void doFrame(long frameTimeNanos) {
                                    if (!latestDownloadProgress.isCompleted()) {
                                        notifyAboutDownloadProgressChange(downloadProgress.getReceivedBytes(), downloadProgress.getTotalBytes());
                                    }
                                    hasScheduledNextFrame = false;
                                }
                            });
                        }
                    });
                } else {
                    notifyAboutDownloadProgressChange(downloadProgress.getReceivedBytes(), downloadProgress.getTotalBytes());
                }
            }
        };
    }

    /**
     * Sets instance holder.
     *
     * @param reactInstanceHolder instance of {@link ReactInstanceHolder}.
     */
    public static void setReactInstanceHolder(ReactInstanceHolder reactInstanceHolder) {
        sReactInstanceHolder = reactInstanceHolder;
    }

    /**
     * Gets instance of {@link ReactInstanceHolder}.
     *
     * @return instance of {@link ReactInstanceHolder}.
     */
    private static ReactInstanceManager getReactInstanceManager() {
        if (sReactInstanceHolder == null) {
            return null;
        }
        return sReactInstanceHolder.getReactInstanceManager();
    }

    /**
     * This workaround has been implemented in order to fix https://github.com/facebook/react-native/issues/14533
     * resetReactRootViews allows to call recreateReactContextInBackground without any exceptions
     * This fix also relates to https://github.com/Microsoft/react-native-code-push/issues/878
     *
     * @param instanceManager instance of {@link ReactInstanceHolder}.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    private void resetReactRootViews(ReactInstanceManager instanceManager) throws CodePushNativeApiCallException {
        try {
            Field mAttachedRootViewsField = instanceManager.getClass().getDeclaredField("mAttachedRootViews");
            mAttachedRootViewsField.setAccessible(true);
            List<ReactRootView> mAttachedRootViews = (List<ReactRootView>) mAttachedRootViewsField.get(instanceManager);
            for (ReactRootView reactRootView : mAttachedRootViews) {
                reactRootView.removeAllViews();
                reactRootView.setId(View.NO_ID);
            }
            mAttachedRootViewsField.set(instanceManager, mAttachedRootViews);
        } catch (NoSuchFieldException e) {
            throw new CodePushNativeApiCallException(e);
        } catch (IllegalAccessException e) {
            throw new CodePushNativeApiCallException(e);
        }
    }

    /**
     * Removes basic lifecycle listener.
     */
    private void clearLifecycleEventListener() {

        /* Remove LifecycleEventListener to prevent infinite restart loop. */
        if (mLifecycleEventListener != null) {
            sReactApplicationContext.removeLifecycleEventListener(mLifecycleEventListener);
            mLifecycleEventListener = null;
        }
    }

    /**
     * Removes lifecycle listener attached for report.
     */
    private void clearLifecycleEventListenerForReport() {

        /* Remove LifecycleEventListener to prevent infinite restart loop. */
        if (mLifecycleEventListenerForReport != null) {
            sReactApplicationContext.removeLifecycleEventListener(mLifecycleEventListenerForReport);
            mLifecycleEventListenerForReport = null;
        }
    }

    /**
     * Use reflection to find the ReactInstanceManager. See #556 for a proposal for a less brittle way to approach this.
     *
     * @return returns instance of {@link ReactInstanceManager}.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    private ReactInstanceManager resolveInstanceManager() throws CodePushNativeApiCallException {
        ReactInstanceManager instanceManager = ReactNativeCore.getReactInstanceManager();
        if (instanceManager != null) {
            return instanceManager;
        }
        final Activity currentActivity = sReactApplicationContext.getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }
        ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
        instanceManager = reactApplication.getReactNativeHost().getReactInstanceManager();
        return instanceManager;
    }

    /**
     * Sets js bundle file.
     *
     * @param instanceManager    instance of {@link ReactInstanceManager}.
     * @param latestJSBundleFile path to the latest js bundle file.
     * @throws CodePushNativeApiCallException exception occurred when performing the operation.
     */
    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws CodePushNativeApiCallException {

        /* Use reflection to find and set the appropriate fields on ReactInstanceManager. See #556 for a proposal for a less brittle way to approach this. */
        try {
            JSBundleLoader latestJSBundleLoader;
            if (latestJSBundleFile.toLowerCase().startsWith("assets://")) {
                latestJSBundleLoader = JSBundleLoader.createAssetLoader(sReactApplicationContext, latestJSBundleFile, false);
            } else {
                latestJSBundleLoader = JSBundleLoader.createFileLoader(latestJSBundleFile);
            }
            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {
            throw new CodePushNativeApiCallException(new CodePushGeneralException("Unable to set JSBundle - CodePush may not support this version of React Native", e));
        }
    }
}
