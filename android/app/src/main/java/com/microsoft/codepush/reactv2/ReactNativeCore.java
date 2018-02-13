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
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.ChoreographerCompat;
import com.facebook.react.modules.core.ReactChoreographer;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.CodePushBaseCore;
import com.microsoft.codepush.common.DownloadProgress;
import com.microsoft.codepush.common.enums.CodePushInstallMode;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.interfaces.AppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
import com.microsoft.codepush.common.interfaces.PublicKeyProvider;
import com.microsoft.codepush.common.utils.CodePushLogUtils;
import com.microsoft.codepush.common.utils.PlatformUtils;
import com.microsoft.codepush.reactv2.interfaces.ReactInstanceHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

public class ReactNativeCore extends CodePushBaseCore {

    /**
     * Instance of {@link ReactInstanceHolder}.
     */
    private static ReactInstanceHolder sReactInstanceHolder;

    /**
     * Instance of {@link CodePushNativeModule} to work with.
     */
    private static CodePushNativeModule sCodePushModule;

    /**
     * Instance of {@link CodePushDialog} to work with.
     */
    private static CodePushDialog sDialogModule;

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
            PublicKeyProvider publicKeyProvider,
            AppEntryPointProvider appEntryPointProvider,
            PlatformUtils platformUtils,
            CodePushRestartListener restartListener,
            CodePushConfirmationDialog confirmationDialog
    ) throws CodePushInitializeException {
        super(deploymentKey, context, isDebugMode, serverUrl, publicKeyProvider, appEntryPointProvider, platformUtils, restartListener, confirmationDialog);
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
    static ReactInstanceManager getReactInstanceManager() {
        if (sReactInstanceHolder == null) {
            return null;
        }
        return sReactInstanceHolder.getReactInstanceManager();
    }

    /**
     * Creates react-specific modules.
     *
     * @param reactApplicationContext app context.
     * @return {@link List} of {@link NativeModule} instances.
     */
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        sReactApplicationContext = reactApplicationContext;
        sCodePushModule = new CodePushNativeModule(sReactApplicationContext, this);
        sDialogModule = new CodePushDialog(sReactApplicationContext);
        addSyncStatusListener(sCodePushModule);
        addDownloadProgressListener(sCodePushModule);
        List<NativeModule> nativeModules = new ArrayList<>();
        nativeModules.add(sCodePushModule);
        nativeModules.add(sDialogModule);
        return nativeModules;
    }

    /**
     * This workaround has been implemented in order to fix https://github.com/facebook/react-native/issues/14533
     * resetReactRootViews allows to call recreateReactContextInBackground without any exceptions
     * This fix also relates to https://github.com/Microsoft/react-native-code-push/issues/878
     *
     * @param instanceManager instance of {@link ReactInstanceHolder}.
     */
    private void resetReactRootViews(ReactInstanceManager instanceManager) throws NoSuchFieldException, IllegalAccessException {
        Field mAttachedRootViewsField = instanceManager.getClass().getDeclaredField("mAttachedRootViews");
        mAttachedRootViewsField.setAccessible(true);
        List<ReactRootView> mAttachedRootViews = (List<ReactRootView>) mAttachedRootViewsField.get(instanceManager);
        for (ReactRootView reactRootView : mAttachedRootViews) {
            reactRootView.removeAllViews();
            reactRootView.setId(View.NO_ID);
        }
        mAttachedRootViewsField.set(instanceManager, mAttachedRootViews);
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

    /*
     * Use reflection to find the ReactInstanceManager. See #556 for a proposal for a less brittle way to approach this.
     */
    private ReactInstanceManager resolveInstanceManager() throws NoSuchFieldException, IllegalAccessException {
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
     */
    private void setJSBundle(ReactInstanceManager instanceManager, String latestJSBundleFile) throws IllegalAccessException {

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
            CodePushLogUtils.trackException(new CodePushGeneralException("Unable to set JSBundle - CodePush may not support this version of React Native", e));
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
                        mRestartManager.restartApp(false);
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
                            mRestartManager.restartApp(false);
                        }
                    }
                }

                @Override
                public void onHostPause() {

                    /* Save the current time so that when the app is later resumed, we can detect how long it was in the background. */
                    lastPausedDate = new Date();
                    if (installMode == CodePushInstallMode.ON_NEXT_SUSPEND && mSettingsManager.isPendingUpdate(null)) {
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
    protected void retrySendStatusReportOnAppResume(final Callable<Void> sender) throws Exception {
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
}
