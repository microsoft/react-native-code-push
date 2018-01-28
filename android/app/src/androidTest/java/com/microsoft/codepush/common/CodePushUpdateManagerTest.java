package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.interfaces.DownloadProgressCallback;
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

import java.io.File;
import java.io.IOException;

import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_URL;
import static com.microsoft.codepush.common.CodePushConstants.APP_ENTRY_POINT_PATH_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;

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
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackageEntryPath_fails_ifGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.getCurrentPackageEntryPath("");
    }

    @Test(expected = CodePushMergeException.class)
    public void merge_fails_ifGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.mergeDiff(FULL_PACKAGE_HASH, null, "");
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackage_fails_ifGetPackageHashFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageHash();
        spiedUpdateManager.getCurrentPackage();
    }

    @Test
    public void getCurrentPackageEntryPath_returnNull_whenPackageIsNull() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        assertNull(spiedUpdateManager.getCurrentPackageEntryPath(""));
    }

    @Test(expected = CodePushRollbackException.class)
    public void rollback_fails_ifGetCurrentPackageFails() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        spiedUpdateManager.rollbackPackage();
    }

    @Test
    public void downloadPackage_fails_ifPackageDownloader_Fails() throws Exception {
        CodePushDownloadPackageResult codePushDownloadPackageResult = executeDownload(false, "/");
        assertNotNull(codePushDownloadPackageResult.getCodePushDownloadPackageException());
    }

    @Test
    public void workflowTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(true, FULL_PACKAGE_URL);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
        String appEntryPoint = codePushUpdateManager.mergeDiff(FULL_PACKAGE_HASH, null, "index.html");
        assertEquals("/www/index.html", appEntryPoint);

        packageObject.put("packageHash", FULL_PACKAGE_HASH);
        codePushUpdateManager.installPackage(packageObject, false);
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, json.getString("currentPackage"));

        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(FULL_PACKAGE_HASH);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        CodePushUtils.writeJsonToFile(packageObject, newUpdateMetadataPath);

        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        downloadPackageResult = executeDownload(true, DIFF_PACKAGE_URL);
        downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
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
    public void getPreviousPackage_fails_ifGetJsonFails() throws Exception {
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        new File(newUpdateMetadataPath).delete();
        codePushUpdateManager.getPackage(DIFF_PACKAGE_HASH);
    }

    @Test(expected = CodePushMergeException.class)
    public void merge_fails_ifWrongAppEntryPoint() throws Exception {
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(true, DIFF_PACKAGE_URL);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, null, "indexw.html");
    }

    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackage_fails_ifGetPreviousPackageHashFails() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doThrow(new IOException()).when(codePushUpdateManager).getPreviousPackageHash();
        codePushUpdateManager.getPreviousPackage();
    }

    /*@Test
    public void verifyTest() throws Exception {
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(true, SIGNED_PACKAGE_URL);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_PUBLIC_KEY, "index.html");
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, null, "index.html");
    }*/

    @Test
    public void getPreviousPackage_null_ifGetPreviousPackageHashNull() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getPreviousPackageHash();
        assertNull(codePushUpdateManager.getPreviousPackage());
    }

    @Test(expected = CodePushMergeException.class)
    public void merge_fails_ifNoSignatureWhereShouldBe() throws Exception {
        CodePushDownloadPackageResult downloadPackageResult = executeDownload(true, DIFF_PACKAGE_URL);
        File downloadFile = downloadPackageResult.getDownloadFile();
        codePushUpdateManager.unzipPackage(downloadFile);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, "", "index.html");
    }

    private CodePushDownloadPackageResult executeDownload(boolean verify, String url) throws Exception {
        PackageDownloader packageDownloader = new PackageDownloader();
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        packageObject.put("downloadUrl", url);
        CodePushDownloadPackageResult codePushDownloadPackageResult = codePushUpdateManager.downloadPackage(packageObject, downloadProgressCallback, packageDownloader);
        if (verify) {
            Mockito.verify(downloadProgressCallback, timeout(5000).atLeast(1)).call(any(DownloadProgress.class));
        }
        return codePushDownloadPackageResult;
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

    @Test(expected = CodePushInstallException.class)
    public void installTestFail() throws Exception {
        FileUtils.deleteDirectoryAtPath(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath());
        codePushUpdateManager.installPackage(packageObject, false);
    }

    @Test
    public void installTestTestConfig() throws Exception {
        codePushUpdateManager.setUsingTestConfiguration(true);
        File one = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        one.mkdirs();
        new File(one, "TestPackages").mkdirs();
        codePushUpdateManager.installPackage(packageObject, true);
        codePushUpdateManager.installPackage(packageObject, false);
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, json.getString("currentPackage"));
        codePushUpdateManager.setUsingTestConfiguration(false);
    }

    @Test
    public void installTestRollback() throws Exception {
        File g = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        g.mkdirs();
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


/*

    @Test(expected = CodePushInvalidUpdateException.class)
    public void publicKeyFailTest() throws Exception {
        CodePushDownloadPackageResult codePushDownloadPackageResult = executeDownload(packageHash);
        codePushUpdateManager.installPackage(packageObject, false);
        CodePushDownloadPackageResult codePushDownloadPackageResult1 = Mockito.spy(codePushDownloadPackageResult);
        codePushUpdateManager.unzipPackage(packageObject, codePushDownloadPackageResult1, "index.html", "fff");
    }

    @Test
    public void updateManagerTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        assertNull(codePushUpdateManager.getPreviousPackage());
        assertNull(codePushUpdateManagerDeserializer.getPreviousPackage());
        CodePushDownloadPackageResult codePushDownloadPackageResult = executeDownload(packageHash);
        codePushUpdateManager.installPackage(packageObject, false);
        CodePushDownloadPackageResult codePushDownloadPackageResult1 = Mockito.spy(codePushDownloadPackageResult);
        codePushUpdateManager.unzipPackage(packageObject, codePushDownloadPackageResult1, "index.html", null);
        CodePushLocalPackage codePushLocalPackage = codePushUpdateManagerDeserializer.getCurrentPackage();
        assertEquals(packageHash, codePushLocalPackage.getPackageHash());
        FileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html"));
        String newHash = "wgggwwftds";
        packageObject.put("packageHash", newHash);
        codePushDownloadPackageResult = executeDownload(newHash);
        codePushUpdateManager.installPackage(packageObject, false);
        codePushDownloadPackageResult1 = Mockito.spy(codePushDownloadPackageResult);

         Current package folder path not null
        packageObject.put("packageHash", packageHash);
        codePushUpdateManager.unzipPackage(packageObject, codePushDownloadPackageResult1, "index.html", null);

        CodePushLocalPackage codePushPrevPackage = codePushUpdateManagerDeserializer.getPreviousPackage();
        CodePushLocalPackage codePushPackage = codePushUpdateManagerDeserializer.getPackage(packageHash);
        assertEquals(packageHash, codePushPrevPackage.getPackageHash());
        assertEquals(packageHash, codePushPackage.getPackageHash());
        CodePushLocalPackage codePushPackageNull = codePushUpdateManagerDeserializer.getPackage("notExist");
        assertNull(codePushPackageNull);
    }*/

}
