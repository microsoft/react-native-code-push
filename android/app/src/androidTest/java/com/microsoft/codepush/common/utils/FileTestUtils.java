package com.microsoft.codepush.common.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.zip.ZipEntry;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Utils to make {@link FileUtils} testing process easier and avoid code repetition.
 */
public class FileTestUtils {

    /**
     * Mocks a file to fail when performing <code>mkdirs()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>mkdirs()</code> is called.
     */
    public static File mockDirMkDirsFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).mkdirs();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>renameTo()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>renameTo()</code> is called.
     */
    public static File mockFileRenameToFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).renameTo(any(File.class));
        return mocked;
    }

    /**
     * Gets a real (not mocked) randomly named folder for testing.
     * Note: folder is not created.
     *
     * @return real test folder.
     */
    public static File getRealTestFolder() {
        Random random = new Random(System.currentTimeMillis());
        return new File(getTestingDirectory(), "Test" + random.nextInt());
    }

    /**
     * Creates a real (not mocked) file for testing.
     *
     * @return real test file.
     */
    public static File getRealFile() throws IOException {
        File testFolder = getRealTestFolder();
        testFolder.mkdirs();
        File realFile = new File(testFolder, "file.txt");
        realFile.createNewFile();
        return realFile;
    }

    /**
     * Mocks a file to fail when performing <code>setLastModified()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>setLastModified()</code> is called.
     */
    public static File mockSetLastModifiedFail() throws IOException {
        File mocked = getRealFile();
        mocked = spy(mocked);
        doReturn(false).when(mocked).setLastModified(anyLong());
        return mocked;
    }

    /**
     * Mocks a {@link ZipEntry} to simulate real entry behaviour.
     *
     * @param isDirectory whether this should represent a directory.
     * @return mocked zip entry.
     */
    public static ZipEntry mockZipEntry(boolean isDirectory) {
        ZipEntry mocked = mock(ZipEntry.class);
        doReturn(new Date().getTime()).when(mocked).getTime();
        doReturn(isDirectory).when(mocked).isDirectory();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>listFiles()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>listFiles()</code> is called.
     */
    public static File mockDirListFilesFail() {
        File mocked = getFileMock();
        doReturn(null).when(mocked).listFiles();
        return mocked;
    }

    /**
     * Gets a mock of the {@link File} class.
     * Note: Its method <code>exists()</code> by default returns <code>false</code>.
     *
     * @return mocked file.
     */
    public static File getFileMock() {
        return getFileMock(false);
    }

    /**
     * Gets a mock of the {@link File} class.
     *
     * @param exists whether the file <code>exists()</code> call should return <code>true</code> or <code>false</code>.
     * @return mocked file.
     */
    public static File getFileMock(boolean exists) {
        File mocked = mock(File.class);
        doReturn(exists).when(mocked).exists();
        return mocked;
    }

    /**
     * Gets shared directory to create test folders in.
     * Note: must be deleted on tearDown.
     *
     * @return shared test directory.
     */
    public static File getTestingDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "/Test");
    }
}
