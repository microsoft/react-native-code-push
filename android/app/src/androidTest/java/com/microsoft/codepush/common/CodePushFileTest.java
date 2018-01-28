package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.microsoft.codepush.common.AndroidTestUtils.getFileMock;
import static com.microsoft.codepush.common.AndroidTestUtils.getRealFile;
import static com.microsoft.codepush.common.AndroidTestUtils.getRealTestFolder;
import static com.microsoft.codepush.common.AndroidTestUtils.mockDirListFilesFail;
import static com.microsoft.codepush.common.AndroidTestUtils.mockDirMkDirFail;
import static com.microsoft.codepush.common.AndroidTestUtils.mockFileRenameToFail;
import static com.microsoft.codepush.common.AndroidTestUtils.mockSetLastModifiedFail;
import static com.microsoft.codepush.common.AndroidTestUtils.mockZipEntry;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class CodePushFileTest {

    @Test
    public void fileOperations_succeed() throws Exception {
        new FileUtils();

        /* Creating files and directories. */
        String fileContent = "123";
        String newFileName = "newFileName.txt";
        String fileName = "file.txt";
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test");
        File moveTestFolder = new File(Environment.getExternalStorageDirectory(), "TestMove");
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

    @Test
    public void unzip_succeeds() throws Exception {
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
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(unzipFolder.getPath(), zipEntryFileName)));
    }

    @Test(expected = IOException.class)
    public void unzip_fails_ifParentFileMkDirFails() throws Exception {
        File parentFile = mockDirMkDirFail();
        File sourceFile = getFileMock();
        doReturn(parentFile).when(sourceFile).getParentFile();
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        FileUtils.unzipSingleFile(mock(ZipEntry.class), sourceFile, buffer, zipInputStream);
    }

    @Test(expected = IOException.class)
    public void unzip_fails_ifFileMkDirFails() throws Exception {
        File mocked = mockDirMkDirFail();
        ZipEntry entry = mockZipEntry(true);
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        FileUtils.unzipSingleFile(entry, mocked, buffer, zipInputStream);
    }

    @Test(expected = IOException.class)
    public void unzip_fails_ifSetLastModifiedFails() throws Exception {
        File file = mockSetLastModifiedFail();
        ZipEntry entry = mockZipEntry(false);
        ZipInputStream zipInputStream = mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        doReturn(-1).when(zipInputStream).read(buffer);
        FileUtils.unzipSingleFile(entry, file, buffer, zipInputStream);
    }

    @Test(expected = IOException.class)
    public void unzip_fails_ifDestinationMkDirFails() throws Exception {
        File newFile = getRealFile();
        File testDirMove = mockDirMkDirFail();
        FileUtils.unzipFile(newFile, testDirMove);
    }

    @Test(expected = IOException.class)
    public void move_fails_ifDestinationMkDirFails() throws Exception {
        File testDir = getFileMock();
        File testDirMove = mockDirMkDirFail();
        FileUtils.moveFile(testDir, testDirMove, "");
    }

    @Test(expected = IOException.class)
    public void move_fails_ifRenameToFails() throws Exception {
        File testDirMove = getRealTestFolder();
        File newFile = mockFileRenameToFail();
        FileUtils.moveFile(newFile, testDirMove, "");
    }

    @Test(expected = IOException.class)
    public void delete_fails_ifPassNull() throws Exception {
        FileUtils.deleteDirectoryAtPath(null);
    }

    @Test(expected = IOException.class)
    public void copy_fails_ifDestinationMkDirFails() throws Exception {
        File destDir = mockDirMkDirFail();
        FileUtils.copyDirectoryContents(getFileMock(), destDir);
    }

    @Test(expected = IOException.class)
    public void copy_fails_ifSourceListFilesFails() throws Exception {
        File sourceDir = mockDirListFilesFail();
        FileUtils.copyDirectoryContents(sourceDir, getFileMock(true));
    }
}
