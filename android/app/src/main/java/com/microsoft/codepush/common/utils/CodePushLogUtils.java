package com.microsoft.codepush.common.utils;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.crashes.Crashes;

import java.lang.reflect.Method;
import java.util.Map;

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
        } catch (Exception e) {
            /* Do nothing. */
        }
    }
}
