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
        CodePushPackage.setHomeDrectory(getFilesDir());
        SoLoader.init(this, false);
        mReactRootView = new CodePushReactRootView(this);

        try {
            Properties codePushProperties = new Properties();
            InputStream in = getAssets().open("CodePush.properties");
            codePushProperties.load(in);
            in.close();

            mReactInstanceManager = CodePushReactInstanceManager.builder()
                    .setApplication(getApplication())
                    .setBundleAssetName("index.android.bundle")
                    .setJSMainModuleName("index.android")
                    .addPackage(new CodePush.CodePushReactPackage(codePushProperties, this))
                    .setUseDeveloperSupport(BuildConfig.DEBUG)
                    .setInitialLifecycleState(LifecycleState.RESUMED)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Unable to load Code Push properties.", e);
        }

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
