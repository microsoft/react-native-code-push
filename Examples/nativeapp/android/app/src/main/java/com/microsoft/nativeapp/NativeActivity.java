package com.microsoft.nativeapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.BuildConfig;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.GsonBuilder;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;
import com.microsoft.codepush.common.exceptions.CodePushInitializeException;
import com.microsoft.codepush.common.exceptions.CodePushNativeApiCallException;
import com.microsoft.codepush.reactv2.CodePush;

public class NativeActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 2;

    private static com.microsoft.codepush.reactv2.CodePush codePushInstance;

    private static boolean restartSwitch = false;

    private void initializeCodePush() {
        if (codePushInstance == null) {
            try {
                codePushInstance = new CodePush(
                        "hvm05O3aAG-KJA3zR7NaBjPm7C0Ka4c23a71-4e6c-4f9b-8f95-4fb15b6dd8b5",
                        getApplicationContext(),
                        BuildConfig.DEBUG
                );
            } catch (CodePushInitializeException e) {
                e.printStackTrace();
            }

        }
        setRestartState(false);
        setCurrentPackageData(null);
    }

    private void setRestartState(boolean state) {
        restartSwitch = state;
        Button restartButton = (Button) findViewById(R.id.button_allow_restart_switch);
        restartButton.setText(restartSwitch ? "Disallow restart" : "Allow restart");

        if (restartSwitch) {
            try {
                codePushInstance.allowRestart();
            } catch (CodePushNativeApiCallException e) {
                e.printStackTrace();
            }
        } else {
            codePushInstance.disallowRestart();
        }
    }

    private void setCurrentPackageData(CodePushLocalPackage currentPackage) {
        String data;
        if (currentPackage == null) {
            data = "<empty>";
        } else {
            data = new GsonBuilder().setPrettyPrinting().create().toJson(currentPackage);
        }

        TextView currentPackageTextView = (TextView) findViewById(R.id.textview_current_package);

        currentPackageTextView.setText("Current package:\n" + data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);

        initializeCodePush();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // SYSTEM_ALERT_WINDOW permission not granted...
                }
            }
        }
    }

    /**
     * Eventhandlers
     */

    public void onOpenRnViewClick(View view) {
        Intent intent = new Intent(this, ReactActivity.class);
        intent.putExtra("CodePushInstance", codePushInstance);
        startActivity(intent);
    }

    public void onCheckForUpdateClick(View view) {
        TextView infoView = (TextView) findViewById(R.id.info_view);

        CodePushRemotePackage remotePackage = null;
        try {
            remotePackage = codePushInstance.checkForUpdate();
        } catch (CodePushNativeApiCallException e) {
            e.printStackTrace();
        }
        if (remotePackage == null) {
            infoView.setText("No update available");
        } else {
            infoView.setText("Update available");
        }
    }

    public void onAllowRestartSwitchClick(View view) {
        this.setRestartState(!restartSwitch);
    }

    public void onGetCurrentPackageClick(View view) {
        CodePushLocalPackage currentPackage = null;
        try {
            currentPackage = codePushInstance.getCurrentPackage();
        } catch (CodePushNativeApiCallException e) {
            e.printStackTrace();
        }
        setCurrentPackageData(currentPackage);
    }

    public void onGetUpdateMetadataClick(View view) {
        CodePushLocalPackage currentPackage = null;
        try {
            currentPackage = codePushInstance.getUpdateMetadata();
        } catch (CodePushNativeApiCallException e) {
            e.printStackTrace();
        }
        setCurrentPackageData(currentPackage);
    }

}
