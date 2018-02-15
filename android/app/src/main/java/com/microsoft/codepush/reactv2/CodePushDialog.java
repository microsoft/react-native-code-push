package com.microsoft.codepush.reactv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationCallback;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.utils.CodePushLogUtils;

import java.util.Arrays;

/**
 * Represents a react native dialog.
 */
public class CodePushDialog extends ReactContextBaseJavaModule implements CodePushConfirmationDialog {

    /**
     * Creates an instance of the {@link CodePushDialog}.
     *
     * @param reactContext instance of the {@link ReactApplicationContext}.
     */
    public CodePushDialog(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    /**
     * Displays dialog with the provided options.
     *
     * @param title              title of the dialog.
     * @param message            message to be displayed.
     * @param positiveButtonText text on the positive action button.
     * @param negativeButtonText test on the negative action button.
     * @param successCallback    callback to handle "OK" events.
     * @param errorCallback      callback to handle "Discard" events.
     */
    @ReactMethod
    public void showDialog(final String title, final String message, final String positiveButtonText,
                           final String negativeButtonText, final Callback successCallback, Callback errorCallback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {

            /* If getCurrentActivity is null, it could be because the app is backgrounded,
            * so we show the dialog when the app resumes). */
            getReactApplicationContext().addLifecycleEventListener(new LifecycleEventListener() {
                @Override
                public void onHostResume() {
                    Activity currentActivity = getCurrentActivity();
                    if (currentActivity != null) {
                        getReactApplicationContext().removeLifecycleEventListener(this);
                        showDialogInternal(title, message, positiveButtonText, negativeButtonText, successCallback, currentActivity);
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
            showDialogInternal(title, message, positiveButtonText, negativeButtonText, successCallback, currentActivity);
        }
    }

    /**
     * Internal method for actually showing the dialog.
     *
     * @param title              title of the dialog.
     * @param message            message to be displayed.
     * @param positiveButtonText text on the positive action button.
     * @param negativeButtonText test on the negative action button.
     * @param successCallback    callback to handle "OK" events.
     * @param currentActivity    application activity.
     */
    private void showDialogInternal(String title, String message, String positiveButtonText,
                                    String negativeButtonText, final Callback successCallback, Activity currentActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
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
                        CodePushLogUtils.trackException("Unknown button ID pressed.");
                }
            }
        };
        if (title != null) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        if (positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText, clickListener);
        }
        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText, clickListener);
        }
        try {
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            CodePushLogUtils.trackException(e);
        }
    }

    @Override
    public String getName() {
        return "CodePushDialog";
    }

    @Override
    public void shouldInstallUpdate(String title, String message, String acceptText, String declineText, final CodePushConfirmationCallback codePushConfirmationCallback) {
        final Callback successCallback = new Callback() {
            @Override
            public void invoke(Object... args) {
                String buttonNumber = args[0].toString();
                if (buttonNumber.equals("0")) {
                    codePushConfirmationCallback.onResult(true);
                } else if (buttonNumber.equals("1")) {
                    codePushConfirmationCallback.onResult(false);
                }
            }
        };
        final Callback errorCallback = new Callback() {
            @Override
            public void invoke(Object... args) {
                codePushConfirmationCallback.throwError(new CodePushGeneralException("Exception occurred when showing a dialog. Args: " + Arrays.toString(args)));
            }
        };
        showDialog(title, message, acceptText, declineText, successCallback, errorCallback);
    }
}
