package com.microsoft.codepush.react;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class BackgroundDetector {

    public static boolean isInBackground(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            //If your app is the process in foreground, then it's not in running in background
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            CodePushUtils.log(e);
            return false;
        }
        return true;
    }
}
