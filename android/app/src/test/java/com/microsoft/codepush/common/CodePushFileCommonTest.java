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

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CodePushUtils.class)
public class CodePushFileCommonTest {
    @Before
    public void setUp() {
        mockStatic(CodePushUtils.class);
        BDDMockito.given(CodePushUtils.finalizeResources(anyList(), anyString())).willReturn(new IOException());
    }

    @Test(expected = CodePushFinalizeException.class)
    public void download_fails_ifFinalizeFails() throws Exception {
        String fileName = "file.txt";
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test");
        File copyFolder = new File(Environment.getExternalStorageDirectory(), "TestMove");
        testFolder.mkdirs();
        File newFile = new File(testFolder, fileName);
        newFile.createNewFile();
        FileUtils.copyDirectoryContents(testFolder, copyFolder);
    }

    @Test(expected = CodePushFinalizeException.class)
    public void read_fails_ifFinalizeFails() throws Exception {
        String fileContent = "123";
        String fileName = "file.txt";
        File testFolder = new File(Environment.getExternalStorageDirectory(), "Test");
        testFolder.mkdirs();
        File newFile = new File(testFolder, fileName);
        newFile.createNewFile();
        FileUtils.writeStringToFile(fileContent, newFile.getPath());
        FileUtils.readFileToString(newFile.getPath());
    }

    @Test(expected = CodePushFinalizeException.class)
    public void unzip_fails_ifFinalizeFails() throws Exception {
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