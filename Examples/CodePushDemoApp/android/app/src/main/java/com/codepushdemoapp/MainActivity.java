/**
 * Copyright (c) 2015-present, Microsoft
 * All rights reserved.
 */

package com.codepushdemoapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.facebook.react.LifecycleState;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.soloader.SoLoader;
import com.reactnativecodepush.CodePush;
import com.reactnativecodepush.CodePushPackage;
import com.reactnativecodepush.CodePushReactInstanceManager;
import com.reactnativecodepush.CodePushReactRootView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {

    private CodePushReactInstanceManager mReactInstanceManager;
    private CodePushReactRootView mReactRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Tell CodePush where to write and find new downloaded updates
        CodePushPackage.setHomeDrectory(getFilesDir());
        SoLoader.init(this, false);

        // Initialize the CodePushReactRootView
        mReactRootView = new CodePushReactRootView(this);

        mReactInstanceManager = CodePushReactInstanceManager.builder()
                .setApplication(getApplication())

                // Load main bundle called "index.android.bundle" from the assets folder
                .setBundleAssetName("index.android.bundle")
                .setJSMainModuleName("index.android")

                // Set app version and deployment key
                .addPackage(new CodePush.CodePushReactPackage(BuildConfig.VERSION_NAME,
                        "YOUR_DEPLOYMENT_KEY_HERE", this))
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        mReactRootView.startReactApplication(mReactInstanceManager, "CodePushDemoApp", null);

        setContentView(mReactRootView);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && mReactInstanceManager != null) {
            mReactInstanceManager.showDevOptionsDialog();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void invokeDefaultOnBackPressed() {
      super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onResume(this);
        }
    }
}
