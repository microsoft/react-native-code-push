package com.microsoft.codepush.common.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealFile;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getRealTestFolder;
import static com.microsoft.codepush.common.testutils.CommonFileTestUtils.getTestingDirectory;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.getFileMock;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.mockDirListFilesFail;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.mockDirMkDirsFail;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.mockFileRenameToFail;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.mockSetLastModifiedFail;
import static com.microsoft.codepush.common.testutils.FileAndroidTestUtils.mockZipEntry;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * This class tests all the {@link FileUtils} scenarios.
 */
public class FileAndroidTests {

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    @Before
    public void setUp() {
        mFileUtils = FileUtils.getInstance();
    }

    /**
     * Tests all the common file operations in a flow.
     */
    @Test
    public void fileOperationsSucceed() throws Exception {

        /* Creating files and directories. */
        String fileContent = "123";
        String newFileName = "newFileName.txt";
        String fileName = "file.txt";
        File testFolder = new File(getTestingDirectory(), "Test");
        File moveTestFolder = new File(getTestingDirectory(), "TestMove");
        File moveTestSubfolder = new File(moveTestFolder, "Internal");
        testFolder.mkdirs();
        moveTestSubfolder.mkdirs();
        File newFile = new File(testFolder, fileName);
        newFile.createNewFile();
        assertEquals(true, mFileUtils.fileAtPathExists(newFile.getPath()));

        /* Testing write/read. */
        mFileUtils.writeStringToFile(fileContent, newFile.getPath());
        assertEquals(fileContent, mFileUtils.readFileToString(newFile.getPath()));

        /* Testing move/copy. */
        mFileUtils.moveFile(newFile, moveTestFolder, newFileName);
        assertEquals(true, mFileUtils.fileAtPathExists(mFileUtils.appendPathComponent(moveTestFolder.getPath(), newFileName)));
        assertEquals(false, mFileUtils.fileAtPathExists(mFileUtils.appendPathComponent(testFolder.getPath(), fileName)));
        mFileUtils.copyDirectoryContents(moveTestFolder, testFolder);
        assertEquals(true, mFileUtils.fileAtPathExists(mFileUtils.appendPathComponent(moveTestFolder.getPath(), newFileName)));
        assertEquals(true, mFileUtils.fileAtPathExists(mFileUtils.appendPathComponent(testFolder.getPath(), newFileName)));

        /* Testing delete. */
        mFileUtils.deleteDirectoryAtPath(moveTestFolder.getPath());
        assertEquals(false, mFileUtils.fileAtPathExists(moveTestFolder.getPath()));
    }

    /**
     * Tests successful unzip process.
     */
    @Test
    public void unzipSucceeds() throws Exception {
        String zipEntryFileContent = "123";
        String zipFileName = "test.zip";
        String zipEntryFileName = "mytext.txt";
        File zipFolder = new File(getTestingDirectory(), "/TestZip");
        zipFolder.mkdirs();
        File unzipFolder = new File(getTestingDirectory(), "/TestZipMove");
        unzipFolder.mkdirs();
        File zip = new File(zipFolder, zipFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        ZipEntry e = new ZipEntry(zipEntryFileName);
        out.putNextEntry(e);
        byte[] data = zipEntryFileContent.getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        mFileUtils.unzipFile(zip, unzipFolder);
        assertEquals(true, mFileUtils.fileAtPathExists(mFileUtils.appendPathComponent(unzipFolder.getPath(), zipEntryFileName)));
    }

    /**
     * Unzipping files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on parent file returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void unzipFailsIfParentFileMkDirsFails() throws Exception {
        File parentFile = mockDirMkDirsFail();
        File sourceFile = getFileMock();
        doReturn(parentFile).when(sourceFile).getParentFile();
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        mFileUtils.unzipSingleFile(mock(ZipEntry.class), sourceFile, buffer, zipInputStream);
    }

    /**
     * Unzipping files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on file returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void unzipFailsIfFileMkDirsFails() throws Exception {
        File mocked = mockDirMkDirsFail();
        ZipEntry entry = mockZipEntry(true);
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        mFileUtils.unzipSingleFile(entry, mocked, buffer, zipInputStream);
    }

    /**
     * {@link FileUtils#fileAtPathExists(String)} should return <code>false</code>
     * if <code>null</code> path is passed.
     */
    @Test
    public void fileAtNullPathNotExist() throws Exception {
        assertFalse(mFileUtils.fileAtPathExists(null));
    }

    /**
     * Unzipping files should throw an {@link IOException}
     * if a {@link File#setLastModified(long)} on file returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void unzipFailsIfSetLastModifiedFails() throws Exception {
        File file = mockSetLastModifiedFail();
        ZipEntry entry = mockZipEntry(false);
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        doReturn(-1).when(zipInputStream).read(buffer);
        mFileUtils.unzipSingleFile(entry, file, buffer, zipInputStream);
    }

    /**
     * Tests unzip process if entry's modification time is less than zero.
     */
    @Test
    public void unzipEntryTimeIsLessThanZero() throws Exception {
        File file = getRealTestFolder();
        ZipEntry entry = mockZipEntry(true);
        doReturn((long) -1).when(entry).getTime();
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        doReturn(-1).when(zipInputStream).read(buffer);
        mFileUtils.unzipSingleFile(entry, file, buffer, zipInputStream);
    }

    /**
     * Unzipping files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void unzipFailsIfDestinationMkDirsFails() throws Exception {
        File newFile = getRealFile();
        File testDirMove = mockDirMkDirsFail();
        mFileUtils.unzipFile(newFile, testDirMove);
    }

    /**
     * Moving files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void moveFailsIfDestinationMkDirsFails() throws Exception {
        File testDir = getFileMock();
        File testDirMove = mockDirMkDirsFail();
        mFileUtils.moveFile(testDir, testDirMove, "");
    }

    /**
     * Moving files should throw an {@link IOException}
     * if a {@link File#renameTo(File)} on new file returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void moveFailsIfRenameToFails() throws Exception {
        File testDirMove = getRealTestFolder();
        File newFile = mockFileRenameToFail();
        mFileUtils.moveFile(newFile, testDirMove, "");
    }

    /**
     * Deleting files should throw an {@link IOException}
     * if pass <code>null</code> path to it.
     */
    @Test(expected = IOException.class)
    public void deleteFailsIfPassNull() throws Exception {
        mFileUtils.deleteDirectoryAtPath(null);
    }

    /**
     * Copying files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void copyFailsIfDestinationMkDirsFails() throws Exception {
        File destDir = mockDirMkDirsFail();
        mFileUtils.copyDirectoryContents(getFileMock(), destDir);
    }

    /**
     * Copying files should throw an {@link IOException}
     * if an {@link File#listFiles()} on source folder returns <code>null</code>.
     */
    @Test(expected = IOException.class)
    public void copyFailsIfSourceListFilesFails() throws Exception {
        File sourceDir = mockDirListFilesFail();
        mFileUtils.copyDirectoryContents(sourceDir, getFileMock(true));
    }

    /**
     * After running tests on file, we must delete all the created folders.
     */
    @After
    public void tearDown() throws Exception {
        File testFolder = getTestingDirectory();
        testFolder.delete();
    }

    /**
     * {@link FileUtils#deleteFileOrFolderSilently(File)} should not throw a {@link IOException}
     * if <code>listFiles</code> returned <code>null</code>.
     */
    @Test
    public void deleteFileFailsIfListFilesFails() throws Exception {
        File testFile = mock(File.class);
        doReturn(null).when(testFile).listFiles();
        doReturn(true).when(testFile).isDirectory();
        mFileUtils.deleteFileOrFolderSilently(testFile);
    }

    /**
     * {@link FileUtils#deleteFileOrFolderSilently(File)} should not throw a {@link IOException}
     * if <code>delete</code> returned <code>null</code>.
     */
    @Test
    public void deleteFileFailsIfDeleteFails() throws Exception {
        File testFile = mock(File.class);
        doReturn(false).when(testFile).delete();
        mFileUtils.deleteFileOrFolderSilently(testFile);
    }

    /**
     * {@link FileUtils#deleteFileOrFolderSilently(File)} should not throw a {@link IOException}
     * if <code>delete</code> on child file returned <code>null</code>.
     */
    @Test
    public void deleteFileFailsIfDeleteOnChildFails() throws Exception {
        File testFile = mock(File.class);
        doReturn(false).when(testFile).delete();
        File newTestFile = mock(File.class);
        doReturn(true).when(newTestFile).isDirectory();
        doReturn(new File[]{testFile}).when(newTestFile).listFiles();
        mFileUtils.deleteFileOrFolderSilently(newTestFile);
    }
}
