package com.microsoft.codepush.react.managers;

import com.microsoft.codepush.react.CodePushCore;
import com.microsoft.codepush.react.utils.CodePushUtils;

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
        CodePushUtils.log("Re-allowing restarts");
        mAllowed = true;

        if (mRestartQueue.size() > 0) {
            CodePushUtils.log("Executing pending restart");
            boolean onlyIfUpdateIsPending = mRestartQueue.get(0);
            mRestartQueue.remove(0);
            restartApp(onlyIfUpdateIsPending);
        }
    }

    public void disallow() {
        CodePushUtils.log("Disallowing restarts");
        mAllowed = false;
    }

    public void clearPendingRestart() {
        mRestartQueue.clear();
    }

    public boolean restartApp(boolean onlyIfUpdateIsPending) {
        if (mRestartInProgress) {
            CodePushUtils.log("Restart request queued until the current restart is completed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else if (!mAllowed) {
            CodePushUtils.log("Restart request queued until restarts are re-allowed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else {
            mRestartInProgress = true;
            if (mCodePushCore.restartApp(onlyIfUpdateIsPending)) {
                CodePushUtils.log("Restarting app");
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
