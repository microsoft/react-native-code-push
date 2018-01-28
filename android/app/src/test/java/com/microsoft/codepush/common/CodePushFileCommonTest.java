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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Tests all the data classes.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CodePushUtils.class)
public class CodePushFileCommonTest {
    @Before
    public void setUp() {
        mockStatic(CodePushUtils.class);
        BDDMockito.given(CodePushUtils.finalizeResources(anyList(), anyString())).willReturn(new IOException());
    }

    @Test(expected = CodePushFinalizeException.class)
    public void finalizeCopyTest() throws Exception {

         /* Creating files and directories. */
        String fileName = "file.txt";
        File testDir = new File(Environment.getExternalStorageDirectory(), "Test");
        File testDirMove = new File(Environment.getExternalStorageDirectory(), "TestMove");
        File testDirMoveInternal = new File(testDirMove, "Internal");
        testDir.mkdirs();
        testDirMoveInternal.mkdirs();
        File newFile = new File(testDir, fileName);
        newFile.createNewFile();
        FileUtils.copyDirectoryContents(testDir, testDirMove);
    }

    @Test(expected = CodePushFinalizeException.class)
    public void finalizeReadTest() throws Exception {

        /* Creating files and directories. */
        String testString = "123";
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
        FileUtils.readFileToString(newFile.getPath());
    }

    @Test(expected = CodePushFinalizeException.class)
    public void finalizeUnzip() throws Exception {
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
    }
}