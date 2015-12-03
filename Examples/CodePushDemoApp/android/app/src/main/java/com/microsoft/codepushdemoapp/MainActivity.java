package com.microsoft.codepushdemoapp;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;
import com.microsoft.codepush.react.CodePush;

public class MainActivity extends FragmentActivity implements DefaultHardwareBackBtnHandler {

    private final String TEST_FOLDER_PREFIX = "CodePushDemoAppTests/";
    private ReactInstanceManager mReactInstanceManager;
    private ReactRootView mReactRootView;

    private CodePush codePush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReactRootView = new ReactRootView(this);

        codePush = new CodePush("d73bf5d8-4fbd-4e55-a837-accd328a21ba", this);

        ReactInstanceManager.Builder builder = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setJSBundleFile(codePush.getBundleUrl("index.android.bundle"));

        String mainComponentName = null;
/*
        switch (BuildConfig.RUN_TEST) {
            case "DOWNLOAD_PROGRESS":
                builder = builder.setJSMainModuleName(TEST_FOLDER_PREFIX + "DownloadProgressTests/DownloadProgressTestApp");
                mainComponentName = "DownloadProgressTestApp";
                break;
            case "INSTALL_UPDATE":
                builder = builder.setJSMainModuleName(TEST_FOLDER_PREFIX + "InstallUpdateTests/InstallUpdateTestApp");
                mainComponentName = "InstallUpdateTestApp";
                break;
            case "QUERY_UPDATE":
                builder = builder.setJSMainModuleName(TEST_FOLDER_PREFIX + "QueryUpdateTests/QueryUpdateTestApp");
                mainComponentName = "QueryUpdateTestApp";
                break;
            default:
                // Run the standard demo app.
                builder = builder.setJSMainModuleName("index.android");
                mainComponentName = "CodePushDemoApp";
                break;
        }*/

        mReactInstanceManager = builder.addPackage(new MainReactPackage())
                .addPackage(codePush.getReactPackage())
                .setUseDeveloperSupport(true)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        mReactRootView.startReactApplication(mReactInstanceManager, mainComponentName, null);

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
            mReactInstanceManager.onResume(this, this);
        }
    }
}
