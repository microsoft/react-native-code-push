package com.microsoft.codepush.common;

import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.microsoft.codepush.common.utils.FileTestUtils.getFileMock;
import static com.microsoft.codepush.common.utils.FileTestUtils.getRealFile;
import static com.microsoft.codepush.common.utils.FileTestUtils.getRealTestFolder;
import static com.microsoft.codepush.common.utils.FileTestUtils.getTestingDirectory;
import static com.microsoft.codepush.common.utils.FileTestUtils.mockDirListFilesFail;
import static com.microsoft.codepush.common.utils.FileTestUtils.mockDirMkDirsFail;
import static com.microsoft.codepush.common.utils.FileTestUtils.mockFileRenameToFail;
import static com.microsoft.codepush.common.utils.FileTestUtils.mockSetLastModifiedFail;
import static com.microsoft.codepush.common.utils.FileTestUtils.mockZipEntry;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * This class tests all the {@link FileUtils} scenarios.
 */
public class FileTest {

    /**
     * Tests all the common file operations in a flow.
     */
    @Test
    public void fileOperationsSucceed() throws Exception {
        new FileUtils();

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
        assertEquals(true, FileUtils.fileAtPathExists(newFile.getPath()));

        /* Testing write/read. */
        FileUtils.writeStringToFile(fileContent, newFile.getPath());
        assertEquals(fileContent, FileUtils.readFileToString(newFile.getPath()));

        /* Testing move/copy. */
        FileUtils.moveFile(newFile, moveTestFolder, newFileName);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(moveTestFolder.getPath(), newFileName)));
        assertEquals(false, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testFolder.getPath(), fileName)));
        FileUtils.copyDirectoryContents(moveTestFolder, testFolder);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(moveTestFolder.getPath(), newFileName)));
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testFolder.getPath(), newFileName)));

        /* Testing delete. */
        FileUtils.deleteDirectoryAtPath(moveTestFolder.getPath());
        assertEquals(false, FileUtils.fileAtPathExists(moveTestFolder.getPath()));
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
        FileUtils.unzipFile(zip, unzipFolder);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(unzipFolder.getPath(), zipEntryFileName)));
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
        FileUtils.unzipSingleFile(mock(ZipEntry.class), sourceFile, buffer, zipInputStream);
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
        FileUtils.unzipSingleFile(entry, mocked, buffer, zipInputStream);
    }

    /**
     * {@link FileUtils#fileAtPathExists(String)} should return <code>false</code>
     * if <code>null</code> path is passed.
     */
    @Test
    public void fileAtNullPathNotExist() throws Exception {
        assertFalse(FileUtils.fileAtPathExists(null));
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
        FileUtils.unzipSingleFile(entry, file, buffer, zipInputStream);
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
        FileUtils.unzipSingleFile(entry, file, buffer, zipInputStream);
    }

    /**
     * Unzipping files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void unzipFailsIfDestinationMkDirsFails() throws Exception {
        File newFile = getRealFile();
        File testDirMove = mockDirMkDirsFail();
        FileUtils.unzipFile(newFile, testDirMove);
    }

    /**
     * Moving files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void moveFailsIfDestinationMkDirsFails() throws Exception {
        File testDir = getFileMock();
        File testDirMove = mockDirMkDirsFail();
        FileUtils.moveFile(testDir, testDirMove, "");
    }

    /**
     * Moving files should throw an {@link IOException}
     * if a {@link File#renameTo(File)} on new file returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void moveFailsIfRenameToFails() throws Exception {
        File testDirMove = getRealTestFolder();
        File newFile = mockFileRenameToFail();
        FileUtils.moveFile(newFile, testDirMove, "");
    }

    /**
     * Deleting files should throw an {@link IOException}
     * if pass <code>null</code> path to it.
     */
    @Test(expected = IOException.class)
    public void deleteFailsIfPassNull() throws Exception {
        FileUtils.deleteDirectoryAtPath(null);
    }

    /**
     * Copying files should throw an {@link IOException}
     * if a {@link File#mkdirs()} on destination folder returns <code>false</code>.
     */
    @Test(expected = IOException.class)
    public void copyFailsIfDestinationMkDirsFails() throws Exception {
        File destDir = mockDirMkDirsFail();
        FileUtils.copyDirectoryContents(getFileMock(), destDir);
    }

    /**
     * Copying files should throw an {@link IOException}
     * if an {@link File#listFiles()} on source folder returns <code>null</code>.
     */
    @Test(expected = IOException.class)
    public void copyFailsIfSourceListFilesFails() throws Exception {
        File sourceDir = mockDirListFilesFail();
        FileUtils.copyDirectoryContents(sourceDir, getFileMock(true));
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
