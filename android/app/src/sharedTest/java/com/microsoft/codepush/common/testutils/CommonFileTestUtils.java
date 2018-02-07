package com.microsoft.codepush.common.testutils;

import android.os.Environment;

import com.microsoft.codepush.common.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
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
        return getRealNamedFile("file.txt");
    }

    /**
     * Creates a real (not mocked) file for testing.
     *
     * @param fileName name for the file.
     * @return real test file.
     */
    public static File getRealNamedFile(String fileName) throws IOException {
        File testFolder = getRealTestFolder();
        testFolder.mkdirs();
        File realFile = new File(testFolder, fileName);
        realFile.createNewFile();
        return realFile;
    }

    /**
     * Creates a real (not mocked) file for testing.
     *
     * @param fileName name for the file.
     * @param content  content of file.
     * @return real test file.
     */
    public static File getRealNamedFileWithContent(String fileName, String content) throws IOException {
        File file = getRealNamedFile(fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(content);
        fileWriter.flush();
        fileWriter.close();
        return file;
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