/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.reactnativecodepush;

import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.ReactBridge;

public class CodePushJSBundleLoader {
    public static JSBundleLoader createFileLoader(
            final String sourceURL,
            final String file) {
        return new JSBundleLoader() {
            @Override
            public void loadScript(ReactBridge bridge) {
                bridge.loadScriptFromNetworkCached(sourceURL, file);
            }
        };
    }
}
