package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.testutils.FileAndroidTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealFile;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getTestingDirectory;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * This class is for testing those {@link CodePushUtils} test cases that depend on {@link FileUtils#finalizeResources(List, String)} method failure.
 */

public class UtilsFinalizeAndroidTests {

    /**
     * {@link FileUtils} instance.
     */
    private FileUtils mFileUtils;

    /**
     * {@link CodePushUtils} instance.
     */
    private CodePushUtils mUtils;

    @Before
    public void setUp() throws Exception {
        mFileUtils = FileUtils.getInstance();
        mFileUtils = spy(mFileUtils);
        mUtils = CodePushUtils.getInstance(mFileUtils);

        /* Set up that finalizeResources should always fail. */
        doReturn(new IOException()).when(mFileUtils).finalizeResources(anyListOf(Closeable.class), anyString());
    }

    /**
     * Getting string from InputStream should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void testGetStringFromInputStreamFailsIfFinalizeFails() throws Exception {
        InputStream stream = new ByteArrayInputStream("string".getBytes("UTF-8"));
        mUtils.getStringFromInputStream(stream);
    }
}