package com.microsoft.codepush.common.utils;

import org.junit.Test;

import static com.microsoft.codepush.common.utils.StringUtils.isNullOrEmpty;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * StringUtils unit tests.
 */
public class StringUtilsUnitTest {

    /**
     * Tests {@link StringUtils#isNullOrEmpty} method.
     */
    @Test
    public void testIsNullOrEmpty() {
        assertTrue(isNullOrEmpty(null));
        assertTrue(isNullOrEmpty(""));
        assertFalse(isNullOrEmpty("non empty"));
        assertFalse(isNullOrEmpty("   ")); //still non empty!
    }
}
