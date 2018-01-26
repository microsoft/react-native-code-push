package com.microsoft.codepush.common.exceptions;

import java.net.MalformedURLException;

/**
 * Exception class for throwing malformed CodePush data exceptions.
 * Malformed data could be json blob of CodePush update manifest and other json blobs
 * saved locally, received from server and etc.
 */
public class CodePushMalformedDataException extends Exception {

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>path</code> and <code>message</code> arguments.
     *
     * @param message the message with explanation of error.
     * @param cause   the cause why CodePush data cannot be parsed.
     */
    public CodePushMalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the malformed CodePush data exception using
     * <code>url</code> and <code>cause</code> arguments.
     *
     * @param url   the malformed <code>downloadUrl</code> of CodePush update.
     * @param cause the cause why CodePush <code>downloadUrl</code> cannot be used.
     */
    public CodePushMalformedDataException(String url, MalformedURLException cause) {
        super("The package has an invalid \"downloadUrl\": " + url, cause);
    }
}
