package com.microsoft.codepush.common.managers;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.interfaces.CodePushRestartHandler;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;

import java.util.LinkedList;
import java.util.List;

import static com.microsoft.codepush.common.CodePush.LOG_TAG;

/**
 * Manager responsible for restarting the application.
 */
public class CodePushRestartManager implements CodePushRestartListener {

    /**
     * Listener for restart events.
     */
    private CodePushRestartHandler mRestartHandler;

    /**
     * <code>true</code> if restart is allowed.
     */
    private boolean mAllowed = true;

    /**
     * <code>true</code> if application is in the process of restart.
     */
    private boolean mRestartInProgress = false;

    /**
     * Queue containing pending restart requests.
     */
    private List<Boolean> mRestartQueue = new LinkedList<>();

    /**
     * Creates an instance of {@link CodePushRestartManager}.
     *
     * @param codePushRestartHandler listener for restart events.
     */
    public CodePushRestartManager(CodePushRestartHandler codePushRestartHandler) {
        mRestartHandler = codePushRestartHandler;
    }

    /**
     * Allows the manager to perform restarts and performs them if there are pending.
     */
    public void allowRestarts() throws CodePushMalformedDataException {
        AppCenterLog.info(LOG_TAG, "Re-allowing restarts");
        mAllowed = true;
        if (mRestartQueue.size() > 0) {
            AppCenterLog.info(LOG_TAG, "Executing pending restart");
            boolean onlyIfUpdateIsPending = mRestartQueue.get(0);
            mRestartQueue.remove(0);
            restartApp(onlyIfUpdateIsPending);
        }
    }

    /**
     * Disallows the manager to perform restarts.
     */
    public void disallowRestarts() {
        AppCenterLog.info(LOG_TAG, "Disallowing restarts");
        mAllowed = false;
    }

    /**
     * Clears the list of pending restarts.
     */
    public void clearPendingRestart() {
        mRestartQueue.clear();
    }

    /**
     * Performs the application restart.
     *
     * @param onlyIfUpdateIsPending if <code>true</code>, performs restart only if there is a pending update.
     * @return <code>true</code> if application has restarted successfully.
     */
    public boolean restartApp(boolean onlyIfUpdateIsPending) throws CodePushMalformedDataException {
        if (mRestartInProgress) {
            AppCenterLog.info(LOG_TAG, "Restart request queued until the current restart is completed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else if (!mAllowed) {
            AppCenterLog.info(LOG_TAG, "Restart request queued until restarts are re-allowed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else {
            mRestartInProgress = true;
            mRestartHandler.performRestart(this, onlyIfUpdateIsPending);
            AppCenterLog.info(LOG_TAG, "Restarting app");
            return true;
        }
        return false;
    }

    @Override
    public void onRestartFinished() throws CodePushMalformedDataException {
        mRestartInProgress = false;
        if (mRestartQueue.size() > 0) {
            restartApp(mRestartQueue.remove(0));
        }
    }
}
