package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * This class is for testing those {@link FileUtils} test cases that depend on {@link CodePushUtils#finalizeResources(List, String)} static methods failure.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CodePushUtils.class)
public class FileCommonTest {

    /**
     * Shared test file.
     */
    private File newFile;

    /**
     * Shared test folder.
     */
    private File testFolder;

    @Before
    public void setUp() throws Exception {
        String fileContent = "123";
        String fileName = "file.txt";
        testFolder = new File(Environment.getExternalStorageDirectory(), "Test");
        testFolder.mkdirs();
        newFile = new File(testFolder, fileName);
        newFile.createNewFile();
        FileUtils.writeStringToFile(fileContent, newFile.getPath());

        /* Set up that finalizeResources should always fail. */
        mockStatic(CodePushUtils.class);
        BDDMockito.given(CodePushUtils.finalizeResources(anyList(), anyString())).willReturn(new IOException());
    }

    /**
     * Read file should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void readFailsIfFinalizeFails() throws Exception {
        FileUtils.readFileToString(newFile.getPath());
    }

    /**
     * Copy files should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void copyFailsIfFinalizeFails() throws Exception {
        File copyFolder = new File(Environment.getExternalStorageDirectory(), "TestMove");
        FileUtils.copyDirectoryContents(testFolder, copyFolder);
    }

    /**
     * Write to file should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void writeFailsIfFinalizeFails() throws Exception {
        String fileContent = "123";
        FileUtils.writeStringToFile(fileContent, newFile.getPath());
    }

    /**
     * Unzip files should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void unzipFailsIfFinalizeFails() throws Exception {
        String zipEntryFileContent = "123";
        String zipFileName = "test.zip";
        String zipEntryFileName = "mytext.txt";
        File zipFolder = new File(Environment.getExternalStorageDirectory(), "/TestZip");
        zipFolder.mkdir();
        File unzipFolder = new File(Environment.getExternalStorageDirectory(), "/TestZipMove");
        unzipFolder.mkdir();
        File zip = new File(zipFolder, zipFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        ZipEntry e = new ZipEntry(zipEntryFileName);
        out.putNextEntry(e);
        byte[] data = zipEntryFileContent.getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        FileUtils.unzipFile(zip, unzipFolder);
    }
}