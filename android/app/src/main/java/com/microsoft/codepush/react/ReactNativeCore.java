package com.microsoft.codepush.react;

import android.app.Activity;
import android.view.View;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.microsoft.codepush.common.CodePushBaseCore;
import com.microsoft.codepush.react.interfaces.ReactInstanceHolder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
     * Instance of the {@link ReactNativeCore}.
     */
    private static ReactNativeCore INSTANCE;

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
                latestJSBundleLoader = JSBundleLoader.createFileLoader(mContext, latestJSBundleFile);
            }
            Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
            bundleLoaderField.setAccessible(true);
            bundleLoaderField.set(instanceManager, latestJSBundleLoader);
        } catch (Exception e) {

            //TODO: track exception "Unable to set JSBundle - CodePush may not support this version of React Native"
            throw new IllegalAccessException("Could not set JSBundle");
        }
    }

    /**
     * Clears debug cache if in debug mode.
     */
    public void clearDebugCacheIfNeeded() {
        if (mIsDebugMode && mSettingsManager.isPendingUpdate(null)) {

            /* This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78. */
            File cachedDevBundle = new File(mContext.getFilesDir(), "ReactNativeDevBundle.js");
            if (cachedDevBundle.exists()) {
                cachedDevBundle.delete();
            }
        }
    }
}
