package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushGeneralException;

/**
 * Callback for delivering results of the confirmation callback proposal.
 */
public interface CodePushConfirmationCallback {

    /**
     * Called when user pressed some button.
     *
     * @param accept <code>true</code> if user accepted proposal, <code>false</code> otherwise.
     */
    void onResult(boolean accept);

    /**
     * Called on some error.
     *
     * @param e exception that has occurred.
     */
    void throwError(CodePushGeneralException e);
}
