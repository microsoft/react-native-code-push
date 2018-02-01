package com.microsoft.codepush.common.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.zip.ZipEntry;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Utils to make {@link FileUtils} testing process easier and avoid code repetition.
 */
public class CommonFileTestUtils {

    /**
     * Gets a real (not mocked) randomly named folder for testing.
     * Note: folder is not created.
     *
     * @return real test folder.
     */
    public static File getRealTestFolder() {
        Random random = new Random(System.currentTimeMillis());
        return new File(Environment.getExternalStorageDirectory(), "Test" + random.nextInt());
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
