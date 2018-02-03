package com.microsoft.codepush.common.utils;

import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipEntry;

import static com.microsoft.codepush.common.utils.CommonFileTestUtils.getRealFile;
import static org.mockito.Matchers.any;

/**
 * Utils to make {@link FileUtils} testing process easier and avoid code repetition.
 */
public class FileAndroidTestUtils {

    /**
     * Mocks a file to fail when performing <code>mkdirs()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>mkdirs()</code> is called.
     */
    public static File mockDirMkDirsFail() {
        File mocked = getFileMock();
        Mockito.doReturn(false).when(mocked).mkdirs();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>renameTo()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>renameTo()</code> is called.
     */
    public static File mockFileRenameToFail() {
        File mocked = getFileMock();
        Mockito.doReturn(false).when(mocked).renameTo(any(File.class));
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>setLastModified()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>setLastModified()</code> is called.
     */
    public static File mockSetLastModifiedFail() throws IOException {
        File mocked = getRealFile();
        mocked = Mockito.spy(mocked);
        Mockito.doReturn(false).when(mocked).setLastModified(Matchers.anyLong());
        return mocked;
    }

    /**
     * Mocks a {@link ZipEntry} to simulate real entry behaviour.
     *
     * @param isDirectory whether this should represent a directory.
     * @return mocked zip entry.
     */
    public static ZipEntry mockZipEntry(boolean isDirectory) {
        ZipEntry mocked = Mockito.mock(ZipEntry.class);
        Mockito.doReturn(new Date().getTime()).when(mocked).getTime();
        Mockito.doReturn(isDirectory).when(mocked).isDirectory();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>listFiles()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>listFiles()</code> is called.
     */
    public static File mockDirListFilesFail() {
        File mocked = getFileMock();
        Mockito.doReturn(null).when(mocked).listFiles();
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
        File mocked = Mockito.mock(File.class);
        Mockito.doReturn(exists).when(mocked).exists();
        return mocked;
    }
}