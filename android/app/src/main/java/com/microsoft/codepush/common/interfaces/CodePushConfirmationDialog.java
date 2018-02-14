package com.microsoft.codepush.common.interfaces;

/**
 * Represents interface for update install confirmation dialog.
 */
public interface CodePushConfirmationDialog {

    /**
     * Proposes user to install update.
     *
     * @param title                        title for dialog.
     * @param message                      message to show.
     * @param acceptText                   text for accept button.
     * @param declineText                  text for decline button.
     * @param codePushConfirmationCallback callback for delivering results of the proposal.
     */
    void shouldInstallUpdate(String title, String message, String acceptText, String declineText, CodePushConfirmationCallback codePushConfirmationCallback);
}
