package com.microsoft.codepush.common.utils;

import com.microsoft.codepush.common.exceptions.CodePushFinalizeException;
import com.microsoft.codepush.common.testutils.FileAndroidTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealFile;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getTestingDirectory;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * This class is for testing those {@link FileUtils} test cases that depend on {@link FileUtils#finalizeResources(List, String)} method failure.
 */
public class FileFinalizeAndroidTests {

    /**
     * Shared test file.
     */
    private File newFile;

    /**
     * Shared test folder.
     */
    private File testFolder;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    @Before
    public void setUp() throws Exception {
        String fileContent = "123";
        String fileName = "file.txt";
        testFolder = new File(getTestingDirectory(), "/Test");
        testFolder.mkdirs();
        newFile = new File(testFolder, fileName);
        newFile.createNewFile();
        mFileUtils = FileUtils.getInstance();
        mFileUtils.writeStringToFile(fileContent, newFile.getPath());
        mFileUtils = spy(mFileUtils);

        /* Set up that finalizeResources should always fail. */
        doReturn(new IOException()).when(mFileUtils).finalizeResources(anyListOf(Closeable.class), anyString());
    }

    /**
     * Read file should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void readFailsIfFinalizeFails() throws Exception {
        mFileUtils.readFileToString(newFile.getPath());
    }

    /**
     * Copy files should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void copyFailsIfFinalizeFails() throws Exception {
        File copyFolder = new File(getTestingDirectory(), "/TestMove");
        mFileUtils.copyDirectoryContents(testFolder, copyFolder);
    }

    /**
     * Write to file should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void writeFailsIfFinalizeFails() throws Exception {
        String fileContent = "123";
        mFileUtils.writeStringToFile(fileContent, newFile.getPath());
    }

    /**
     * If write file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void writeDoubleFailure() throws Exception {
        String fileContent = "123";
        mFileUtils.writeStringToFile(fileContent, "/***");
    }

    /**
     * If read file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void readDoubleFailure() throws Exception {
        mFileUtils.readFileToString("/***");
    }

    /**
     * If unzip file fails with an {@link IOException}, it should go straight to finally block,
     * where it should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void unzipDoubleFailure() throws Exception {
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        doThrow(new IOException()).when(zipInputStream).read(any(byte[].class));
        mFileUtils.unzipSingleFile(FileAndroidTestUtils.mockZipEntry(false), getRealFile(), new byte[1024], zipInputStream);
    }

    /**
     * Unzip files should throw a {@link CodePushFinalizeException}
     * if an {@link IOException} is thrown during {@link FileUtils#finalizeResources(List, String)}.
     */
    @Test(expected = CodePushFinalizeException.class)
    public void unzipFailsIfFinalizeFails() throws Exception {
        String zipEntryFileContent = "123";
        String zipFileName = "test.zip";
        String zipEntryFileName = "mytext.txt";
        File zipFolder = new File(getTestingDirectory(), "/TestZip");
        zipFolder.mkdir();
        File unzipFolder = new File(getTestingDirectory(), "/TestZipMove");
        unzipFolder.mkdir();
        File zip = new File(zipFolder, zipFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        ZipEntry e = new ZipEntry(zipEntryFileName);
        out.putNextEntry(e);
        byte[] data = zipEntryFileContent.getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        mFileUtils.unzipFile(zip, unzipFolder);
    }

    /**
     * After running tests on file, we must delete all the created folders.
     */
    @After
    public void tearDown() throws Exception {
        File testFolder = getTestingDirectory();
        testFolder.delete();
    }
}