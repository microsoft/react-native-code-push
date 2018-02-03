package com.microsoft.codepush.common.utils;

import java.util.Date;
import java.util.zip.ZipEntry;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Utils to make {@link FileUtils} testing process easier and avoid code repetition.
 */
public class FileUnitTestUtils {

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
}
