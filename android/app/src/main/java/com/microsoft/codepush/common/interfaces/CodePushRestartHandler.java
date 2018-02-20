package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;

/**
 * Handler for restart events.
 */
public interface CodePushRestartHandler {

    /**
     * Called when application is ready to load a new bundle.
     *
     * @param onlyIfUpdateIsPending   <code>true</code> if restart only if update is pending.
     * @param codePushRestartListener listener to notify when restart has finished.
     * @throws CodePushMalformedDataException error thrown when the actual data is broken.
     */
    void performRestart(CodePushRestartListener codePushRestartListener, boolean onlyIfUpdateIsPending) throws CodePushMalformedDataException;
}
