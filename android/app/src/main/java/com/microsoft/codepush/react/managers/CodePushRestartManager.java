package com.microsoft.codepush.react.managers;

import com.microsoft.codepush.react.CodePushCore;
import com.microsoft.codepush.react.utils.CodePushRNUtils;

import java.util.LinkedList;
import java.util.List;

public class CodePushRestartManager {
    private boolean mAllowed = true;

    private boolean mRestartInProgress = false;

    private List<Boolean> mRestartQueue = new LinkedList<>();

    private CodePushCore mCodePushCore;

    public CodePushRestartManager(CodePushCore codePushCore) {
        mCodePushCore = codePushCore;
    }

    public void allow() {
        CodePushRNUtils.log("Re-allowing restarts");
        mAllowed = true;

        if (mRestartQueue.size() > 0) {
            CodePushRNUtils.log("Executing pending restart");
            boolean onlyIfUpdateIsPending = mRestartQueue.get(0);
            mRestartQueue.remove(0);
            restartApp(onlyIfUpdateIsPending);
        }
    }

    public void disallow() {
        CodePushRNUtils.log("Disallowing restarts");
        mAllowed = false;
    }

    public void clearPendingRestart() {
        mRestartQueue.clear();
    }

    public boolean restartApp(boolean onlyIfUpdateIsPending) {
        if (mRestartInProgress) {
            CodePushRNUtils.log("Restart request queued until the current restart is completed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else if (!mAllowed) {
            CodePushRNUtils.log("Restart request queued until restarts are re-allowed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else {
            mRestartInProgress = true;
            if (mCodePushCore.restartApp(onlyIfUpdateIsPending)) {
                CodePushRNUtils.log("Restarting app");
                return true;
            }

            mRestartInProgress = false;
            if (mRestartQueue.size() > 0) {
                restartApp(mRestartQueue.remove(0));
            }
        }
        return false;
    }
 }
