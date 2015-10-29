/**
 * Code modified from Facebook, Inc
 * https://github.com/facebook/react-native/blob/42eb5464fd8a65ed84b799de5d4dc225349449be/ReactAndroid/src/main/java/com/facebook/react/CoreModulesPackage.java
 *
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.facebook.catalyst.uimanager.debug.DebugComponentOwnershipModule;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.ExceptionsManagerModule;
import com.facebook.react.modules.core.JSTimersExecution;
import com.facebook.react.modules.core.Timing;
import com.facebook.react.modules.debug.AnimationsDebugModule;
import com.facebook.react.modules.debug.SourceCodeModule;
import com.facebook.react.modules.systeminfo.AndroidInfoModule;
import com.facebook.react.uimanager.AppRegistry;
import com.facebook.react.uimanager.ReactNative;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.events.RCTEventEmitter;

/**
 * Package defining core framework modules (e.g. UIManager). It should be used for modules that
 * require special integration with other framework parts (e.g. with the list of packages to load
 * view managers from).
 */
/* package */ class CodePushCoreModulesPackage implements ReactPackage {

    private final CodePushReactInstanceManager mReactInstanceManager;
    private final DefaultHardwareBackBtnHandler mHardwareBackBtnHandler;

    CodePushCoreModulesPackage(
            CodePushReactInstanceManager reactInstanceManager,
            DefaultHardwareBackBtnHandler hardwareBackBtnHandler) {
        mReactInstanceManager = reactInstanceManager;
        mHardwareBackBtnHandler = hardwareBackBtnHandler;
    }

    @Override
    public List<NativeModule> createNativeModules(
            ReactApplicationContext catalystApplicationContext) {
        return Arrays.<NativeModule>asList(
                new AnimationsDebugModule(
                        catalystApplicationContext,
                        mReactInstanceManager.getDevSupportManager().getDevSettings()),
                new AndroidInfoModule(),
                new DeviceEventManagerModule(catalystApplicationContext, mHardwareBackBtnHandler),
                new ExceptionsManagerModule(mReactInstanceManager.getDevSupportManager()),
                new Timing(catalystApplicationContext),
                new SourceCodeModule(
                        mReactInstanceManager.getDevSupportManager().getSourceUrl(),
                        mReactInstanceManager.getDevSupportManager().getSourceMapUrl()),
                new UIManagerModule(
                        catalystApplicationContext,
                        mReactInstanceManager.createAllViewManagers(catalystApplicationContext)),
                new DebugComponentOwnershipModule(catalystApplicationContext));
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Arrays.asList(
                DeviceEventManagerModule.RCTDeviceEventEmitter.class,
                JSTimersExecution.class,
                RCTEventEmitter.class,
                AppRegistry.class,
                ReactNative.class,
                DebugComponentOwnershipModule.RCTDebugComponentOwnership.class);
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return new ArrayList<>(0);
    }
}