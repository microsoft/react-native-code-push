package com.microsoft.codepush.common;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipEntry;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Utils to make testing process easier and avoid code repetition.
 */
class AndroidTestUtils {

    final static String FULL_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/6CjTRZUgaYrHlhH3mKy2JsQVIJtsa0021bd2-9be1-4904-b4c6-16ce9c797779";
    final static String DIFF_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/8wuI2wwTlf4RioIb1cLRtyQyzRW80840428d-683e-4d30-a120-c592a355a594";
    final static String FULL_PACKAGE_HASH = "a1d28a073a1fa45745a8b1952ccc5c2bd4753e533e7b9e48459a6c186ecd32af";
    final static String DIFF_PACKAGE_HASH = "ff46674f196ae852ccb67e49346a11cb9d8c0243ba24003e11b83dd7469b5dd4";

    /**
     * Mocks a file to fail when performing <code>mkdirs()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>mkdirs()</code> is called.
     */
    static File mockDirMkDirFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).mkdirs();
        return mocked;
    }

    /**
     * Mocks a file to fail when performing <code>renameTo()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>renameTo()</code> is called.
     */
    static File mockFileRenameToFail() {
        File mocked = getFileMock();
        doReturn(false).when(mocked).renameTo(any(File.class));
        return mocked;
    }

    /**
     * Creates a real (not mocked) folder for testing.
     *
     * @return real test folder.
     */
    static File getRealTestFolder() {
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test35941");
        testFolder.mkdirs();
        return testFolder;
    }

    /**
     * Creates a real (not mocked) file for testing.
     *
     * @return real test file.
     * @throws IOException exception occurred when creating a file.
     */
    static File getRealFile() throws IOException {
        File testFolder = getRealTestFolder();
        File realFile = new File(testFolder, "file.txt");
        realFile.createNewFile();
        return realFile;
    }

    /**
     * Mocks a file to fail when performing <code>setLastModified()</code>.
     *
     * @return mocked file returning <code>false</code> when <code>setLastModified()</code> is called.
     */
    static File mockSetLastModifiedFail() throws IOException {
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
    static ZipEntry mockZipEntry(boolean isDirectory) {
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
    static File mockDirListFilesFail() {
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
    static File getFileMock() {
        return getFileMock(false);
    }

    /**
     * Gets a mock of the {@link File} class.
     *
     * @param exists whether the file <code>exists()</code> call should return <code>true</code> or <code>false</code>.
     * @return mocked file.
     */
    static File getFileMock(boolean exists) {
        File mocked = mock(File.class);
        doReturn(exists).when(mocked).exists();
        return mocked;
    }
}
