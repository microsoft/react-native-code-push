package com.microsoft.codepush.common.utils;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.lang.reflect.Method;
import java.util.Map;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * Utils for tracking in-app sdk exceptions.
 * Represents wrapper on {@link Crashes} methods.
 */
public class CodePushLogUtils {

    /**
     * Represents wrapper on {@link Crashes#trackException(Throwable)} method.
     *
     * @param throwable exception instance.
     * @param shouldLog <code>true</code> if log exception on device.
     */
    public static void trackException(Throwable throwable, boolean shouldLog) {
        try {
            Method method = Crashes.class.getMethod("trackException", Throwable.class);
            method.invoke(throwable);
            if (shouldLog) {
                AppCenterLog.error(LOG_TAG, throwable.getMessage());
            }
        } catch (Exception e) {
            /* Do nothing. */
        }
    }

    /**
     * Represents wrapper on {@link Crashes#trackException(Throwable, Map)} method.
     *
     * @param throwable  exception instance.
     * @param properties additional properties.
     * @param shouldLog <code>true</code> if log exception on device.
     */
    public static void trackException(@NonNull Throwable throwable, Map<String, String> properties, boolean shouldLog) {
        try {
            Method method = Crashes.class.getMethod("trackException", Throwable.class, Map.class);
            method.invoke(throwable, properties);
            if (shouldLog) {
                AppCenterLog.error(LOG_TAG, throwable.getMessage());
            }
        } catch (Exception e) {
            /* Do nothing. */
        }
    }
}
