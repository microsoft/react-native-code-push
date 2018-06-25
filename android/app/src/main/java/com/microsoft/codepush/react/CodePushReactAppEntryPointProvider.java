package com.microsoft.codepush.react;

import com.microsoft.codepush.common.interfaces.CodePushAppEntryPointProvider;

/**
 * React-specific implementation of {@link CodePushAppEntryPointProvider}.
 */
public class CodePushReactAppEntryPointProvider implements CodePushAppEntryPointProvider {

    /**
     * Path to the application entry point.
     */
    private String mAppEntryPoint;

    /**
     * Creates an instance of {@link CodePushReactAppEntryPointProvider}.
     *
     * @param appEntryPoint path to the application entry point.
     */
    public CodePushReactAppEntryPointProvider(String appEntryPoint) {
        mAppEntryPoint = appEntryPoint;
    }

    @Override
    public String getAppEntryPoint() {
        if (mAppEntryPoint == null) {
            return CodePushReactNativeCore.DEFAULT_JS_BUNDLE_NAME;
        } else {
            return mAppEntryPoint;
        }
    }
}
