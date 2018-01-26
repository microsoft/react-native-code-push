package com.microsoft.codepush.common;

import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.codepush.common.exceptions.CodePushException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A custom class for logging. Contains implementation of logging for code push.
 */
public class CodePushLog {

    /**
     * Tracks exception. Calls {@link Crashes#trackException(Throwable)} method via reflection.
     *
     * @param message message of the exception.
     */
    public static void track(String message) {
        track(new CodePushException(message));
    }

    /**
     * Tracks exception. Calls {@link Crashes#trackException(Throwable)} method via reflection.
     *
     * @param throwable exception instance.
     */
    public static void track(Throwable throwable) {
        try {
            Method method = Crashes.class.getMethod("trackException", Throwable.class);
            method.invoke(throwable);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
        }
    }

    /**
     * Tracks exception. Calls {@link Crashes#trackException(Throwable, Map)} method via reflection.
     *
     * @param throwable  exception instance.
     * @param properties custom set of properties.
     */
    public static void track(Throwable throwable, Map<String, String> properties) {
        try {
            Method method = Crashes.class.getMethod("trackException", Throwable.class, Map.class);
            method.invoke(throwable, properties);
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
        }
    }

}
