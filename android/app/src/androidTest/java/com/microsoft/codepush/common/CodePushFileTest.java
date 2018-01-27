package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;

public class CodePushFileTest {

    @Test
    public void fileTest() throws Exception {
        new FileUtils();

        /* Creating files and directories. */
        String testString = "123";
        String newName = "newFileName.txt";
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test");
        File testDirMove = new File(Environment.getExternalStorageDirectory(), "TestMove");
        File testDirMoveInternal = new File(testDirMove, "Internal");
        testDir.mkdirs();
        testDirMoveInternal.mkdirs();
        File newFile = new File(testDir, fileName);
        newFile.createNewFile();
        assertEquals(true, FileUtils.fileAtPathExists(newFile.getPath()));

        /* Testing write/read. */
        FileUtils.writeStringToFile(testString, newFile.getPath());
        assertEquals(testString, FileUtils.readFileToString(newFile.getPath()));

        /* Testing move/copy. */
        FileUtils.moveFile(newFile, testDirMove, newName);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirMove.getPath(), newName)));
        assertEquals(false, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDir.getPath(), fileName)));
        FileUtils.copyDirectoryContents(testDirMove, testDir);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirMove.getPath(), newName)));
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDir.getPath(), newName)));

        /* Testing delete. */
        FileUtils.deleteDirectoryAtPath(testDirMove.getPath());
        assertEquals(false, FileUtils.fileAtPathExists(testDirMove.getPath()));
    }

    @Test
    public void zipTest() throws Exception {
        String testString = "123";
        String zipFileName = "test.zip";
        String zipEntryFileName = "mytext.txt";
        File testDirZip = new File(Environment.getExternalStorageDirectory(), "/TestZip");
        testDirZip.mkdir();
        File testDirZipMove = new File(Environment.getExternalStorageDirectory(), "/TestZipMove");
        testDirZipMove.mkdir();
        File zip = new File(testDirZip, zipFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        ZipEntry e = new ZipEntry(zipEntryFileName);
        out.putNextEntry(e);
        byte[] data = testString.getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        FileUtils.unzipFile(zip, testDirZipMove);
        assertEquals(true, FileUtils.fileAtPathExists(FileUtils.appendPathComponent(testDirZipMove.getPath(), zipEntryFileName)));
    }

    @Test(expected = IOException.class)
    public void zipTestFail1() throws Exception {
        File file = Mockito.mock(File.class);
        File sourceFile = Mockito.mock(File.class);
        Mockito.doReturn(false).when(file).exists();
        Mockito.doReturn(false).when(file).mkdirs();
        Mockito.doReturn(file).when(sourceFile).getParentFile();
        FileUtils.unzipSingleFile(Mockito.mock(ZipEntry.class), sourceFile, new byte[10], Mockito.mock(ZipInputStream.class));
    }

    @Test(expected = IOException.class)
    public void zipTestFail() throws Exception {
        File mocked = mockTestDirMoveMkDir();
        ZipEntry entry = Mockito.mock(ZipEntry.class);
        Mockito.doReturn(true).when(entry).isDirectory();
        ZipInputStream zipInputStream = Mockito.mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        FileUtils.unzipSingleFile(entry, mocked, buffer, zipInputStream);
    }

    @Test(expected = IOException.class)
    public void zipTestFail2() throws Exception {
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test");
        testDir.mkdirs();
        File mocked = new File(testDir, fileName);
        mocked = Mockito.spy(mocked);
        Mockito.doReturn(false).when(mocked).setLastModified(Mockito.anyLong());
        ZipEntry entry = Mockito.mock(ZipEntry.class);
        Mockito.doReturn((long) 1).when(entry).getTime();
        ZipInputStream zipInputStream = Mockito.mock(ZipInputStream.class);
        byte[] buffer = new byte[1024];
        Mockito.doReturn(-1).when(zipInputStream).read(buffer);
        FileUtils.unzipSingleFile(entry, mocked, buffer, zipInputStream);
    }

    @Test(expected = IOException.class)
    public void fileUnzipDestinationMkDirFailTest() throws Exception {
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test1");
        testDir.mkdirs();
        File newFile = new File(testDir, fileName);
        newFile.createNewFile();
        File testDirMove = mockTestDirMoveMkDir();
        FileUtils.unzipFile(newFile, testDirMove);
    }

    @Test(expected = IOException.class)
    public void fileMoveDestinationMkDirFailTest() throws Exception {
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test1");
        testDir.mkdirs();
        File testDirMove = mockTestDirMoveMkDir();
        FileUtils.moveFile(testDir, testDirMove, "file1.txt");
    }

    private File mockTestDirMoveMkDir() {
        File testDirMove = new File(Environment.getExternalStorageDirectory(), "Test35941");
        if (testDirMove.exists()) {
            try {
                FileUtils.deleteDirectoryAtPath(testDirMove.getPath());
            } catch (IOException e) {
            }
        }
        testDirMove = Mockito.spy(testDirMove);
        Mockito.doReturn(false).when(testDirMove).mkdirs();
        return testDirMove;
    }

    @Test(expected = IOException.class)
    public void fileMoveRenameToFailTest() throws Exception {
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test1");
        File testDirMove = new File(Environment.getExternalStorageDirectory(), "Test35941");
        testDir.mkdirs();
        File newFile = new File(testDir, fileName);
        newFile.createNewFile();
        newFile = Mockito.spy(newFile);
        Mockito.doReturn(false).when(newFile).renameTo(Mockito.any(File.class));
        FileUtils.moveFile(newFile, testDirMove, "file1.txt");
    }

    @Test(expected = IOException.class)
    public void deleteDirectoryNull() throws Exception {
        FileUtils.deleteDirectoryAtPath(null);
    }

    @Test(expected = IOException.class)
    public void copyDestDirMkDirsFails() throws Exception {
        File destDir = mockTestDirMoveMkDir();
        FileUtils.copyDirectoryContents(new File("/"), destDir);
    }

    @Test(expected = IOException.class)
    public void copySourceDirListFilesFails() throws Exception {
        File sourceDir = Mockito.mock(File.class);
        Mockito.doReturn(null).when(sourceDir).listFiles();
        FileUtils.copyDirectoryContents(sourceDir, new File("/"));
    }
}
