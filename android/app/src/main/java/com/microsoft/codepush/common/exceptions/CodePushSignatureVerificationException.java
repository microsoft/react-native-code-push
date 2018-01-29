package com.microsoft.codepush.common.exceptions;

import java.security.SignatureException;

/**
 * Exception class for handling signature verification errors.
 */
public class CodePushSignatureVerificationException extends SignatureException {

    /**
     * Sub-types of the exception.
     */
    public enum SignatureExceptionType {

        DEFAULT("Error occurred during signature verification."),

        NO_SIGNATURE("Error! Public key was provided but there is no JWT signature within app to verify. \n" +
                "                                    Possible reasons, why that might happen: \n" +
                "                                    1. You've released a CodePush update using version of CodePush CLI that does not support code signing.\n" +
                "                                    2. You've released a CodePush update without providing --privateKeyPath option."),

        NOT_SIGNED("Signature was not signed by a trusted party."),

        PUBLIC_KEY_NOT_PARSED("Unable to parse public key."),

        READ_SIGNATURE_FILE_ERROR("Unable to read signature file."),

        NO_CONTENT_HASH("Signature file did not specify a content hash.");

        /**
         * Message describing the exception depending on the sub-type of it.
         */
        private final String message;

        /**
         * Creates instance of the enum using the provided message.
         *
         * @param message message describing teh exception.
         */
        SignatureExceptionType(String message) {
            this.message = message;
        }

        /**
         * Gets the message of the specified type.
         *
         * @return message.
         */
        public String getMessage() {
            return this.message;
        }
    }

    /**
     * Creates an instance of exception with specified sub-type.
     *
     * @param type sub-type of exception.
     */
    public CodePushSignatureVerificationException(SignatureExceptionType type) {
        super(type.getMessage());
    }

    /**
     * Constructs exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public CodePushSignatureVerificationException(Throwable cause) {
        super(SignatureExceptionType.DEFAULT.getMessage(), cause);
    }

    /**
     * Creates an instance of exception with specified sub-type and cause.
     *
     * @param type  exception sub-type.
     * @param cause cause of exception.
     */
    public CodePushSignatureVerificationException(SignatureExceptionType type, Throwable cause) {
        super(type.getMessage(), cause);
    }
}
