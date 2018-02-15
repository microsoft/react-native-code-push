package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;

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
    boolean onRestart(boolean onlyIfUpdateIsPending) throws CodePushMalformedDataException;
}
