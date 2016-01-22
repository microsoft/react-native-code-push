package com.microsoft.codepush.react;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class CodePushDialog extends ReactContextBaseJavaModule{

    Activity mainActivity;

    public CodePushDialog(ReactApplicationContext reactContext, Activity mainActivity) {
        super(reactContext);
        this.mainActivity = mainActivity;
    }

    @ReactMethod
    public void showDialog(String title, String message, String button1Text, String button2Text,
                      final Callback successCallback, Callback errorCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

        builder.setCancelable(false);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        successCallback.invoke(0);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        successCallback.invoke(1);
                        break;
                    default:
                        throw new CodePushUnknownException("Unknown button ID pressed.");
                }
            }
        };

        if (title != null) {
            builder.setTitle(title);
        }

        if (message != null) {
            builder.setMessage(message);
        }

        if (button1Text != null) {
            builder.setPositiveButton(button1Text, clickListener);
        }

        if (button2Text != null) {
            builder.setNegativeButton(button2Text, clickListener);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public String getName() {
        return "CodePushDialog";
    }
}
