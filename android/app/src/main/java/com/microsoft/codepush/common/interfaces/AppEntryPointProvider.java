package com.microsoft.codepush.common.interfaces;

/**
 * Interface for providing information about application entry point.
 */
public interface AppEntryPointProvider {

    /**
     * Gets location of application entry point.
     *
     * @return location of application entry point.
     */
    String getAppEntryPoint();
}
