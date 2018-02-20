package com.microsoft.codepush.common.utils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;

import java.lang.reflect.Method;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * Utils for tracking in-app sdk exceptions.
 * Represents wrapper on {@link Crashes} methods.
 */
public class CodePushLogUtils {

    /**
     * Whether usage of {@link AppCenter} for tracking exceptions is enabled.
     * AppCenter usage is enabled if user has passed the appSecret and AppCenter has started.
     */
    private static boolean sEnabled;

    /**
     * Sets whether AppCenter usage is enabled.
     * @param enabled whether AppCenter usage is enabled.
     */
    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    /**
     * Represents wrapper on {@link Crashes#saveUncaughtException(Thread, Throwable)} method. Automatically tracks exception in logs, too.
     *
     * @param throwable exception instance.
     */
    public static void trackException(Throwable throwable) {
        trackException(throwable, true);
    }

    /**
     * Represents wrapper on {@link Crashes#saveUncaughtException(Thread, Throwable)} method.
     * Automatically tracks exception in logs, too.
     *
     * @param message message to log (instance of {@link CodePushGeneralException} is created automatically).
     */
    public static void trackException(String message) {
        trackException(new CodePushGeneralException(message), true);
    }

    /**
     * Represents wrapper on {@link Crashes#saveUncaughtException(Thread, Throwable)} method.
     *
     * @param throwable exception instance.
     * @param shouldLog <code>true</code> if log exception on device.
     */
    public static void trackException(Throwable throwable, boolean shouldLog) {
        try {
            if (sEnabled) {
                Method method = Crashes.class.getDeclaredMethod("saveUncaughtException", Thread.class, Throwable.class);
                method.setAccessible(true);
                method.invoke(Crashes.getInstance(), Thread.currentThread(), throwable);
            }
            if (shouldLog) {
                AppCenterLog.error(LOG_TAG, throwable.getMessage(), throwable);
            }
        } catch (Exception e) {

            /* Do nothing because this exception can occur if crashes are simply not enabled, then just log it on device. */
        }
    }
}
