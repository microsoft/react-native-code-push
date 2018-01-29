package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.managers.CodePushUpdateManager;
import com.microsoft.codepush.common.managers.CodePushUpdateManagerDeserializer;
import com.microsoft.codepush.common.utils.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_PUBLIC_KEY;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.checkDoInBackgroundFails;
import static com.microsoft.codepush.common.AndroidTestUtils.createPackageDownloader;
import static com.microsoft.codepush.common.AndroidTestUtils.executeDownload;
import static com.microsoft.codepush.common.AndroidTestUtils.executeWorkflow;
import static com.microsoft.codepush.common.CodePushConstants.APP_ENTRY_POINT_PATH_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class CodePushUpdateManagerTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private CodePushUpdateManager codePushUpdateManager;
    private CodePushUpdateManagerDeserializer codePushUpdateManagerDeserializer;
    private JSONObject packageObject;

    @Before
    public void setUp() throws Exception {
        codePushUpdateManager = new CodePushUpdateManager(Environment.getExternalStorageDirectory().getPath());
        codePushUpdateManagerDeserializer = new CodePushUpdateManagerDeserializer(codePushUpdateManager);
        packageObject = new JSONObject();
        packageObject.put("failedInstall", false);
        packageObject.put("description", "description");
        packageObject.put("deploymentKey", "FDSFD");
        packageObject.put("label", "fdfds");
        packageObject.put("packageHash", FULL_PACKAGE_HASH);
        packageObject.put("downloadUrl", FULL_PACKAGE_URL);
        packageObject.put("appVersion", "1.2");
        packageObject.put("isMandatory", false);
        packageObject.put("packageSize", 1024);
        packageObject.put(APP_ENTRY_POINT_PATH_KEY, "/www/index.html");
        packageObject.put("updateAppVersion", false);
        File codePushFolder = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        codePushFolder.mkdirs();
    }

    @Test
    public void workflowTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        packageObject.put("packageHash", FULL_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, FULL_PACKAGE_URL);
        String appEntryPoint = codePushUpdateManager.mergeDiff(FULL_PACKAGE_HASH, null, "index.html");
        assertEquals("/www/index.html", appEntryPoint);

        codePushUpdateManager.installPackage(packageObject, false);
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, json.getString("currentPackage"));

        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(FULL_PACKAGE_HASH);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        CodePushUtils.writeJsonToFile(packageObject, newUpdateMetadataPath);

        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, DIFF_PACKAGE_URL);
        new File(codePushUpdateManager.getCurrentPackageFolderPath()).mkdirs();
        appEntryPoint = codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, null, "index.html");
        assertEquals("/www/index.html", appEntryPoint);

        codePushUpdateManager.installPackage(packageObject, false);
        json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(DIFF_PACKAGE_HASH, json.getString("currentPackage"));

        newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        CodePushUtils.writeJsonToFile(packageObject, newUpdateMetadataPath);

        CodePushLocalPackage codePushPrevPackage = codePushUpdateManagerDeserializer.getPreviousPackage();
        CodePushLocalPackage codePushCurrentPackage = codePushUpdateManagerDeserializer.getCurrentPackage();
        CodePushLocalPackage codePushPackage = codePushUpdateManagerDeserializer.getPackage(DIFF_PACKAGE_HASH);
        assertEquals(FULL_PACKAGE_HASH, codePushPrevPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushCurrentPackage.getPackageHash());

        assertTrue(FileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));

        packageObject.put(APP_ENTRY_POINT_PATH_KEY, null);
        CodePushUtils.writeJsonToFile(packageObject, newUpdateMetadataPath);
        assertFalse(FileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));

        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getCurrentPackage();
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath("index.html"));
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackageFailsIfGetJsonFails() throws Exception {
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        new File(newUpdateMetadataPath).delete();
        codePushUpdateManager.getPackage(DIFF_PACKAGE_HASH);
    }

    @Test
    public void verifyTest() throws Exception {
        packageObject.put("packageHash", SIGNED_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_PUBLIC_KEY, "index.html");
        executeWorkflow(codePushUpdateManager, packageObject, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, null, "index.html");
    }

    @Test
    public void updateManagerClearTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath(""));
        assertFalse(FileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath()));
        CodePushLocalPackage codePushLocalPackage = codePushUpdateManagerDeserializer.getCurrentPackage();
        assertNull(codePushLocalPackage);
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath(""));
    }

    @Test
    public void installTestTestConfig() throws Exception {
        CodePushUpdateManager.setUsingTestConfiguration(true);
        File one = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(one, "TestPackages").mkdirs();
        codePushUpdateManager.installPackage(packageObject, true);
        codePushUpdateManager.installPackage(packageObject, false);
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, json.getString("currentPackage"));
        CodePushUpdateManager.setUsingTestConfiguration(false);
    }

    @Test
    public void installTestRollback() throws Exception {
        codePushUpdateManager.installPackage(packageObject, false);
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        codePushUpdateManager.installPackage(packageObject, false);
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertNotSame(FULL_PACKAGE_HASH, json.getString("currentPackage"));
        assertEquals(FULL_PACKAGE_HASH, codePushUpdateManager.getPreviousPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, json.getString("currentPackage"));
        codePushUpdateManager.rollbackPackage();
        json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, json.getString("currentPackage"));
        packageObject.put("packageHash", "fff");
        new File(codePushUpdateManager.getPackageFolderPath("fff")).mkdirs();
        codePushUpdateManager.installPackage(packageObject, true);
        packageObject.put("packageHash", "etee");
        codePushUpdateManager.installPackage(packageObject, false);
        packageObject.put("packageHash", "eee");
        codePushUpdateManager.installPackage(packageObject, false);
    }

    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfWrongAppEntryPoint() throws Exception {
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, DIFF_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, null, "indexw.html");
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackageFailsIfGetPreviousPackageHashFails() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doThrow(new IOException()).when(codePushUpdateManager).getPreviousPackageHash();
        codePushUpdateManager.getPreviousPackage();
    }

    @Test
    public void getPreviousPackageNullIfGetPreviousPackageHashNull() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getPreviousPackageHash();
        assertNull(codePushUpdateManager.getPreviousPackage());
    }

    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfNoSignatureWhereShouldBe() throws Exception {
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, DIFF_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, "", "index.html");
    }

    @Test(expected = CodePushInstallException.class)
    public void installTestFail() throws Exception {
        FileUtils.deleteDirectoryAtPath(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath());
        codePushUpdateManager.installPackage(packageObject, false);
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackageEntryPathFailsIfGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.getCurrentPackageEntryPath("");
    }

    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.mergeDiff(FULL_PACKAGE_HASH, null, "");
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackageFailsIfGetPackageHashFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageHash();
        spiedUpdateManager.getCurrentPackage();
    }

    @Test
    public void getCurrentPackageEntryPathReturnNullWhenPackageIsNull() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        assertNull(spiedUpdateManager.getCurrentPackageEntryPath(""));
    }

    @Test(expected = CodePushRollbackException.class)
    public void rollbackFailsIfGetCurrentPackageFails() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        spiedUpdateManager.rollbackPackage();
    }

    @Test
    public void downloadPackageFailsIfPackageDownloaderFails() throws Exception {
        CodePushDownloadPackageResult codePushDownloadPackageResult = executeDownload(codePushUpdateManager, packageObject, false, "/");
        assertNotNull(codePushDownloadPackageResult.getCodePushDownloadPackageException());
    }

    @Test
    public void noPackageInfoTest() throws Exception {
        File codePush = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).delete();
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertTrue(json.isNull("currentPackage"));
    }

    @Test(expected = CodePushMalformedDataException.class)
    public void invalidPackageTest() throws Exception {
        File codePush = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).delete();
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).createNewFile();
        codePushUpdateManager.getCurrentPackageInfo();
    }

    @Test
    public void downloadFailsIfBytesMismatch() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader();
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        doReturn(100).when(connectionMock).getContentLength();
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    @Test
    public void downloadFailsIfCloseFails() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader();
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doThrow(new IOException()).when(bufferedInputStream).close();
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    @Test
    public void downloadFailsIfReadFails() throws Exception {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        PackageDownloader packageDownloader = createPackageDownloader(downloadFolder);
        checkDoInBackgroundFails(packageDownloader);
    }
}
