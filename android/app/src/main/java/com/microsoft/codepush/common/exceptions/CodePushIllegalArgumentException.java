package com.microsoft.codepush.common.exceptions;

import java.util.Locale;

/**
 * An exception occurred during getting the package.
 */
public class CodePushIllegalArgumentException extends Exception {

    /**
     * The default error message.
     */
    private static String MESSAGE = "%s.%s can't be null.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param className    name of the class where an exception happened.
     * @param propertyName name of the class property an illegal value has been set to.
     */
    public CodePushIllegalArgumentException(String className, String propertyName) {
        super(String.format(Locale.getDefault(), MESSAGE, className, propertyName));
    }

}
