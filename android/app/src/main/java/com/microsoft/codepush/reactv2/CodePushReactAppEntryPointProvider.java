package com.microsoft.codepush.reactv2;

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
        return mAppEntryPoint;
    }
}
