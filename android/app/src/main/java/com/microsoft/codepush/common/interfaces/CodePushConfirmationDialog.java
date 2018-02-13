package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.exceptions.CodePushGeneralException;

/**
 * Represents interface for update install confirmation dialog.
 */
public interface CodePushConfirmationDialog {

    /**
     * Proposes user to install update.
     *
     * @param title       title for dialog.
     * @param message     message to show.
     * @param acceptText  text for accept button.
     * @param declineText text for decline button.
     * @return true if user accepts proposal, false otherwise.
     * @throws CodePushGeneralException if error occured during the asking.
     */
    boolean shouldInstallUpdate(String title, String message, String acceptText, String declineText) throws CodePushGeneralException;
}
