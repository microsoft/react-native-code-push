package com.microsoft.codepush.common.interfaces;

/**
 * Listener for restart events.
 */
public interface CodePushRestartListener {

    /**
     * Called when application is ready to load a new bundle.
     *
     * @param onlyIfUpdateIsPending <code>true</code> if restart only if update is pending.
     * @return <code>true</code> if application restarted successfully.
     */
    boolean onRestartReady(boolean onlyIfUpdateIsPending);
}
