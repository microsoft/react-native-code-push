package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

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
     * Gets shared directory to create test folders in.
     * Note: must be deleted on tearDown.
     *
     * @return shared test directory.
     */
    public static File getTestingDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "/Test");
    }
}
