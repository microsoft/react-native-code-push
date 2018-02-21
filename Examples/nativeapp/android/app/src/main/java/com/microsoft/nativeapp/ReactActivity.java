package com.microsoft.nativeapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.facebook.react.BuildConfig;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.shell.MainReactPackage;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.react.CodePush;

public class ReactActivity extends AppCompatActivity implements DefaultHardwareBackBtnHandler {
    private ReactInstanceManager mReactInstanceManager;
    private final int OVERLAY_PERMISSION_REQ_CODE = 1235;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if ( !Settings.canDrawOverlays( this ) ) {
                Intent intent = new Intent( Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse( "package:" + getPackageName() ) );
                startActivityForResult( intent, OVERLAY_PERMISSION_REQ_CODE );
            }
        }

        CodePush codePushInstance = (CodePush) getIntent().getSerializableExtra("CodePushInstance");

        try {
            mReactInstanceManager = ReactInstanceManager.builder()
                    .setApplication(getApplication())
                    .addPackage(new MainReactPackage())
                    .addPackage(codePushInstance)
                    .setUseDeveloperSupport(BuildConfig.DEBUG)
                    .setInitialLifecycleState(LifecycleState.RESUMED)
                    .setJSBundleFile(CodePush.getJSBundleFile("index.android.bundle"))
                    .build();
        } catch (CodePushNativeApiCallException e) {
            e.printStackTrace();
        }
        CodePush.setReactInstanceManager(mReactInstanceManager);

        ReactRootView mReactRootView = new ReactRootView(this);
        mReactRootView.startReactApplication(mReactInstanceManager, "nativeapp", null);
        setContentView(mReactRootView);
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostPause(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostResume(this, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostDestroy(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && mReactInstanceManager != null) {
            mReactInstanceManager.showDevOptionsDialog();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

}

