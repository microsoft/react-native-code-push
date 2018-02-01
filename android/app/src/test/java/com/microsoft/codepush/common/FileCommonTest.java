package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
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
        testFolder = new File(Environment.getExternalStorageDirectory(), "/Test/Test");
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
        File copyFolder = new File(Environment.getExternalStorageDirectory(), "/Test/TestMove");
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
     * If write file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void writeDoubleFailure() throws Exception {
        String fileContent = "123";
        FileUtils.writeStringToFile(fileContent, "/***");
    }

    /**
     * If read file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void readDoubleFailure() throws Exception {
        FileUtils.readFileToString("/***");
    }

    public static File getRealTestFolder() {
        Random random = new Random(System.currentTimeMillis());
        return new File(Environment.getExternalStorageDirectory(), "/Test/Test" + random.nextInt());
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
    public static ZipEntry mockZipEntry(boolean isDirectory) {
        ZipEntry mocked = Mockito.mock(ZipEntry.class);
        doReturn(new Date().getTime()).when(mocked).getTime();
        doReturn(isDirectory).when(mocked).isDirectory();
        return mocked;
    }

    /**
     * If unzip file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link CodePushUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void unzipDoubleFailure() throws Exception {
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        doThrow(new IOException()).when(zipInputStream).read(any(byte[].class));
        FileUtils.unzipSingleFile(mockZipEntry(false), getRealFile(), new byte[1024], zipInputStream);
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
        File zipFolder = new File(Environment.getExternalStorageDirectory(), "/Test/TestZip");
        zipFolder.mkdir();
        File unzipFolder = new File(Environment.getExternalStorageDirectory(), "/Test/TestZipMove");
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

    /**
     * After running tests on file, we must delete all the created folders.
     */
    @After
    public void tearDown() throws Exception {
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test");
        testFolder.delete();
    }
}