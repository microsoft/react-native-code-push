package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;

/**
 * Listener for restart events.
 */
public interface CodePushRestartListener {

    /**
     * Called when application has performed a restart.
     *
     * @throws CodePushMalformedDataException error thrown when the actual data is broken.
     */
    void onRestartFinished() throws CodePushMalformedDataException;
}
