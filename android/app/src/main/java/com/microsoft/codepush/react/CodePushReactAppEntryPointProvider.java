package com.microsoft.codepush.react;

import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.common.interfaces.CodePushAppEntryPointProvider;
import com.microsoft.codepush.react.CodePush;

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
    public String getAppEntryPoint() throws CodePushNativeApiCallException {
        if (mAppEntryPoint == null) {
            return CodePush.getJSBundleFile();
        } else {
            return CodePush.getJSBundleFile(mAppEntryPoint);
        }
    }
}
