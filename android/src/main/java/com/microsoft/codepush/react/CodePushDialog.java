package com.microsoft.codepush.react;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class CodePushDialog extends ReactContextBaseJavaModule{

    public CodePushDialog(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void showDialog(final String title, final String message, final String button1Text,
                           final String button2Text, final Callback successCallback, Callback errorCallback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null || currentActivity.isFinishing()) {
            // If getCurrentActivity is null, it could be because the app is backgrounded,
            // so we show the dialog when the app resumes)
            getReactApplicationContext().addLifecycleEventListener(new LifecycleEventListener() {
                @Override
                public void onHostResume() {
                    Activity currentActivity = getCurrentActivity();
                    if (currentActivity != null) {
                        getReactApplicationContext().removeLifecycleEventListener(this);
                        showDialogInternal(title, message, button1Text, button2Text, successCallback, currentActivity);
                    }
                }

                @Override
                public void onHostPause() {

                }

                @Override
                public void onHostDestroy() {

                }
            });
        } else {
            showDialogInternal(title, message, button1Text, button2Text, successCallback, currentActivity);
        }
    }

    private void showDialogInternal(String title, String message, String button1Text,
                                    String button2Text, final Callback successCallback, Activity currentActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);

        builder.setCancelable(false);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
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
                } catch (Throwable e) {
                    CodePushUtils.log(e);
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
