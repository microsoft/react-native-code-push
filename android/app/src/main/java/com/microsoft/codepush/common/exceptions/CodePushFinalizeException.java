package com.microsoft.codepush.common.exceptions;

import java.io.IOException;

/**
 * Exception class for handling resource finalize exceptions.
 */
public class CodePushFinalizeException extends IOException {

    /**
     * Type of the operation being performed before closing resources.
     */
    public enum OperationType {

        DEFAULT("Error closing IO resources."),

        COPY("Error closing IO resources when copying files."),

        READ("Error closing IO resources when reading file."),

        WRITE("Error closing IO resources when writing to a file.");

        /**
         * Message describing the exception depending on the operation type.
         */
        private final String message;

        /**
         * Creates instance of the enum using the provided message.
         *
         * @param message message describing the exception.
         */
        OperationType(String message) {
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
     * Creates instance of the resource finalize exception.
     *
     * @param cause the cause why resource cannot be finalized.
     */
    public CodePushFinalizeException(Throwable cause) {
        super(OperationType.DEFAULT.getMessage(), cause);
    }

    /**
     * Creates instance of the resource finalize exception using
     * <code>message</code> and <code>cause</code> arguments.
     *
     * @param type  type of the operation being performed before closing resources.
     * @param cause the cause why resource cannot be finalized.
     */
    public CodePushFinalizeException(OperationType type, Throwable cause) {
        super(type.getMessage(), cause);
    }
}
