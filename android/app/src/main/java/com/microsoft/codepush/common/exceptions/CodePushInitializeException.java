package com.microsoft.codepush.common.exceptions;

import android.content.Context;

import com.microsoft.codepush.common.core.CodePushBaseCore;
import com.microsoft.codepush.common.interfaces.CodePushAppEntryPointProvider;
import com.microsoft.codepush.common.interfaces.CodePushConfirmationDialog;
import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.interfaces.CodePushPublicKeyProvider;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

/**
 * Exception class for handling {@link CodePushBaseCore#CodePushBaseCore(String, Context, boolean, String, CodePushPublicKeyProvider, CodePushAppEntryPointProvider, CodePushPlatformUtils, CodePushRestartListener, CodePushConfirmationDialog)} exceptions.
 */
public class CodePushInitializeException extends Exception {

    /**
     * Creates instance of {@link CodePushInitializeException}.
     *
     * @param cause cause of error.
     */
    public CodePushInitializeException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates instance of {@link CodePushInitializeException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public CodePushInitializeException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
