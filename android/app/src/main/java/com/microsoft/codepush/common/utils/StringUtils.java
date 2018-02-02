package com.microsoft.codepush.common.utils;

/**
 * String utils.
 */
public class StringUtils {

    /**
     * Indicates whether input string is null or an empty string.
     *
     * @param string input string.
     * @return true if input string is null or an empty string, false otherwise.
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
