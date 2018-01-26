package com.microsoft.codepush.common.interfaces;

import com.microsoft.codepush.common.datacontracts.CodePushRemotePackage;

/**
 * Interface for listener of binary version mismatch event.
 */
public interface CodePushBinaryVersionMismatchListener {

    /**
     * Callback for handling binary version mismatch event.
     *
     * @param update Remote package (from server).
     */
    void binaryVersionMismatchChanged(CodePushRemotePackage update);
}