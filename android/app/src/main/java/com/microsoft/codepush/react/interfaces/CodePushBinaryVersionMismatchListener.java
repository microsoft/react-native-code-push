package com.microsoft.codepush.react.interfaces;

import com.microsoft.codepush.react.datacontracts.CodePushRemotePackage;

public interface CodePushBinaryVersionMismatchListener {
    void binaryVersionMismatchChanged(CodePushRemotePackage update);
}
