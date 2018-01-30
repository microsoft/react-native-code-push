package com.microsoft.codepush.common;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
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
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.DIFF_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.FULL_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_HASH;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_PUBLIC_KEY;
import static com.microsoft.codepush.common.AndroidTestUtils.SIGNED_PACKAGE_URL;
import static com.microsoft.codepush.common.AndroidTestUtils.checkDoInBackgroundFails;
import static com.microsoft.codepush.common.AndroidTestUtils.checkDoInBackgroundNotFails;
import static com.microsoft.codepush.common.AndroidTestUtils.createPackageDownloader;
import static com.microsoft.codepush.common.AndroidTestUtils.executeDownload;
import static com.microsoft.codepush.common.AndroidTestUtils.executeFullWorkflow;
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

/**
 * This class tests all the {@link CodePushUpdateManager} and {@link CodePushUpdateManagerDeserializer} scenarios.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CodePushUpdateManagerTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Instance of update manager.
     */
    private CodePushUpdateManager codePushUpdateManager;

    /**
     * Instance of update manager deserializer.
     */
    private CodePushUpdateManagerDeserializer codePushUpdateManagerDeserializer;

    /**
     * Instance of package json object.
     */
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

    /**
     * This tests a full update workflow. Download -> unzip -> merge install several packages.
     */
    @Test
    public void fullWorkflowTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        executeFullWorkflow(packageObject, codePushUpdateManager, FULL_PACKAGE_HASH, FULL_PACKAGE_URL);
        executeFullWorkflow(packageObject, codePushUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        CodePushLocalPackage codePushPreviousPackage = codePushUpdateManagerDeserializer.getPreviousPackage();
        CodePushLocalPackage codePushCurrentPackage = codePushUpdateManagerDeserializer.getCurrentPackage();
        CodePushLocalPackage codePushPackage = codePushUpdateManagerDeserializer.getPackage(DIFF_PACKAGE_HASH);
        assertEquals(FULL_PACKAGE_HASH, codePushPreviousPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushCurrentPackage.getPackageHash());
        assertTrue(FileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));
    }

    /**
     * This tests the case when relative entry path is <code>null</code>.
     */
    @Test
    public void relativeEntryPathNullTest() throws Exception {
        packageObject.put(APP_ENTRY_POINT_PATH_KEY, null);
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(packageObject).when(codePushUpdateManager).getCurrentPackage();
        doReturn("").when(codePushUpdateManager).getCurrentPackageFolderPath();
        assertFalse(FileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));
    }

    /**
     * {@link CodePushUpdateManager#getCurrentPackageEntryPath(String)} should return <code>null</code>
     * if {@link CodePushUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test
    public void entryPathIsNullWhenPackageIsNull() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getCurrentPackage();
        doReturn("").when(codePushUpdateManager).getCurrentPackageFolderPath();
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath("index.html"));
    }

    /**
     * This tests {@link CodePushUpdateManager#verifySignature(String, String, boolean)} method.
     * It downloads signed package and tests case when it is verified and when no public key passed to signed package.
     */
    @Test
    public void verifyTest() throws Exception {
        packageObject.put("packageHash", SIGNED_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_PUBLIC_KEY, "index.html");
        executeWorkflow(codePushUpdateManager, packageObject, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, null, "index.html");
    }

    /**
     * This tests that clearing updates works properly.
     */
    @Test
    public void updateManagerClearTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath(""));
        assertFalse(FileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath()));
        CodePushLocalPackage codePushLocalPackage = codePushUpdateManagerDeserializer.getCurrentPackage();
        assertNull(codePushLocalPackage);
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath(""));
    }

    /**
     * This tests installation with the test configuration set.
     */
    @Test
    public void installTestTestConfig() throws Exception {
        CodePushUpdateManager.setUsingTestConfiguration(true);
        File one = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(one, "TestPackages").mkdirs();
        codePushUpdateManager.installPackage(packageObject, true);
        CodePushUpdateManager.setUsingTestConfiguration(false);
    }

    /**
     * Tests installing the package with the same hash.
     */
    @Test
    public void installTheSamePackage() throws Exception {
        /* Install the same package. */
        packageObject.put("packageHash", "dfd");
        codePushUpdateManager.installPackage(packageObject, true);
        codePushUpdateManager.installPackage(packageObject, true);

        /* Install some new package. */
        packageObject.put("packageHash", "ffffff");
        codePushUpdateManager.installPackage(packageObject, false);

        /* Install the same as previous. */
        packageObject.put("packageHash", "dfd");
        codePushUpdateManager.installPackage(packageObject, false);
        codePushUpdateManager = spy(codePushUpdateManager);

        /* Both current and passed package hashes are null and therefore equal. */
        doReturn(null).when(codePushUpdateManager).getCurrentPackageHash();
        packageObject.put("packageHash", null);
        codePushUpdateManager.installPackage(packageObject, true);
    }

    /**
     * {@link CodePushUpdateManagerDeserializer#getPackage(String)} should return <code>null</code>
     * if {@link CodePushUpdateManager#getPackage(String)} returns <code>null</code>.
     */
    @Test
    public void getPackageNullForDeserializer() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getPackage(anyString());
        CodePushUpdateManagerDeserializer codePushUpdateManagerDeserializer = new CodePushUpdateManagerDeserializer(codePushUpdateManager);
        assertNull(codePushUpdateManagerDeserializer.getPackage(""));
    }

    /**
     * {@link CodePushUpdateManagerDeserializer#getPreviousPackage()} should return <code>null</code>
     * if {@link CodePushUpdateManager#getPreviousPackage()} returns <code>null</code>.
     */
    @Test
    public void getPreviousPackageNullForDeserializer() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getPreviousPackage();
        CodePushUpdateManagerDeserializer codePushUpdateManagerDeserializer = new CodePushUpdateManagerDeserializer(codePushUpdateManager);
        assertNull(codePushUpdateManagerDeserializer.getPreviousPackage());
    }

    /**
     * Tests download package with null callback.
     */
    @Test
    public void nullDownloadProgressCallBack() throws Exception {
        PackageDownloader packageDownloader = new PackageDownloader();
        packageObject.put("downloadUrl", FULL_PACKAGE_URL);
        codePushUpdateManager.downloadPackage(packageObject, null, packageDownloader);
    }

    /**
     * Tests rollback workflow. Install -> install -> rollback.
     */
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
    }

    /**
     * Current package folder path should be deleted before installation.
     */
    @Test
    public void packageFolderIsDeleted() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()).when(codePushUpdateManager).getCurrentPackageFolderPath();
        codePushUpdateManager.installPackage(packageObject, true);
        assertFalse(FileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()));
    }

    /**
     * Previous package folder path should be deleted before installation.
     */
    @Test
    public void previousPackageFolderIsDeleted() throws Exception {
        packageObject.put("packageHash", "ddd");
        codePushUpdateManager.installPackage(packageObject, false);
        packageObject.put("packageHash", "fdsf");
        codePushUpdateManager.installPackage(packageObject, false);
        packageObject.put("packageHash", "fds");
        codePushUpdateManager.installPackage(packageObject, false);
    }

    /**
     * {@link CodePushUpdateManager#getPreviousPackage()} should throw a {@link CodePushGetPackageException}
     * if {@link CodePushUtils#getJsonObjectFromFile(String)} throws a {@link CodePushMalformedDataException}.
     */
    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackageFailsIfGetJsonFails() throws Exception {
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = FileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        new File(newUpdateMetadataPath).delete();
        codePushUpdateManager.getPackage(DIFF_PACKAGE_HASH);
    }

    /**
     * Merge should throw a {@link CodePushMergeException} if wrong app entry point passed.
     */
    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfWrongAppEntryPoint() throws Exception {
        codePushUpdateManager.clearUpdates();
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, DIFF_PACKAGE_URL);
        codePushUpdateManager.installPackage(packageObject, false);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, null, "indexw.html");
    }

    /**
     * {@link CodePushUpdateManager#getPreviousPackage()} should throw a {@link CodePushGetPackageException}
     * if {@link CodePushUpdateManager#getPreviousPackageHash()} throws an {@link IOException}.
     */
    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackageFailsIfGetPreviousPackageHashFails() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doThrow(new IOException()).when(codePushUpdateManager).getPreviousPackageHash();
        codePushUpdateManager.getPreviousPackage();
    }

    /**
     * {@link CodePushUpdateManager#getPreviousPackage()} should return <code>null</code>
     * if {@link CodePushUpdateManager#getPreviousPackageHash()} returns <code>null</code>.
     */
    @Test
    public void getPreviousPackageNullIfGetPreviousPackageHashNull() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(null).when(codePushUpdateManager).getPreviousPackageHash();
        assertNull(codePushUpdateManager.getPreviousPackage());
    }

    /**
     * Merge should throw a {@link CodePushMergeException}
     * if a public key passed but the package contains no signature.
     */
    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfNoSignatureWhereShouldBe() throws Exception {
        codePushUpdateManager.clearUpdates();
        packageObject.put("packageHash", DIFF_PACKAGE_HASH);
        executeWorkflow(codePushUpdateManager, packageObject, DIFF_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, "", "index.html");
    }

    /**
     * Installing a package should throw a {@link CodePushInstallException}
     * if {@link CodePushUpdateManager#updateCurrentPackageInfo(JSONObject)} throws an {@link IOException} due to {@link java.io.FileNotFoundException}.
     */
    @Test(expected = CodePushInstallException.class)
    public void installTestFail() throws Exception {
        FileUtils.deleteDirectoryAtPath(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath());
        codePushUpdateManager.installPackage(packageObject, false);
    }

    /**
     * {@link CodePushUpdateManager#getCurrentPackageEntryPath(String)} should throw a {@link CodePushGetPackageException}
     * if {@link CodePushUpdateManager#getCurrentPackageFolderPath()} throws a {@link CodePushMalformedDataException}.
     */
    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackageEntryPathFailsIfGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.getCurrentPackageEntryPath("");
    }

    /**
     * Merge should throw a {@link CodePushMergeException}
     * if {@link CodePushUpdateManager#getCurrentPackageFolderPath()} throws a {@link CodePushMalformedDataException}.
     */
    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfGetFolderPathFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.mergeDiff(FULL_PACKAGE_HASH, null, "");
    }

    /**
     * Get current package should throw a {@link CodePushGetPackageException}
     * if {@link CodePushUpdateManager#getCurrentPackageHash()} throws a {@link CodePushMalformedDataException}.
     */
    @Test(expected = CodePushGetPackageException.class)
    public void getCurrentPackageFailsIfGetPackageHashFails() throws Exception {
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doThrow(mock(CodePushMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageHash();
        spiedUpdateManager.getCurrentPackage();
    }

    /**
     * {@link CodePushUpdateManager#getCurrentPackageEntryPath(String)} should return <code>null</code>
     * if {@link CodePushUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test
    public void returnNullOnGetCurrentPackageEntryPathWhenPackageIsNull() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        assertNull(spiedUpdateManager.getCurrentPackageEntryPath(""));
    }

    /**
     * Rollback should throw a {@link CodePushRollbackException}
     * if {@link CodePushUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test(expected = CodePushRollbackException.class)
    public void rollbackFailsIfGetCurrentPackageFails() throws Exception {
        codePushUpdateManager.clearUpdates();
        CodePushUpdateManager spiedUpdateManager = Mockito.spy(codePushUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        spiedUpdateManager.rollbackPackage();
    }

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if a {@link java.net.MalformedURLException} is thrown when attempting to download.
     */
    @Test
    public void downloadPackageFailsIfPackageDownloaderFails() throws Exception {
        CodePushDownloadPackageResult codePushDownloadPackageResult = executeDownload(codePushUpdateManager, packageObject, false, "/");
        assertNotNull(codePushDownloadPackageResult.getCodePushDownloadPackageException());
    }

    /**
     * If status file does not exist, package info should be empty.
     */
    @Test
    public void noPackageInfoTest() throws Exception {
        File codePush = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).delete();
        JSONObject json = codePushUpdateManager.getCurrentPackageInfo();
        assertTrue(json.isNull("currentPackage"));
    }

    /**
     * Getting current package info should throw a {@link CodePushMalformedDataException}
     * if the status file where the info is located is corrupt or contains wrong data.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void invalidPackageTest() throws Exception {
        File codePush = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).delete();
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).createNewFile();
        codePushUpdateManager.getCurrentPackageInfo();
    }

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if the amount of <code>receivedBytes</code> does not match the amount of <code>totalBytes</code>.
     * The amount of <code>totalBytes</code> is more than <code>receivedBytes</code>.
     */
    @Test
    public void downloadFailsIfBytesMismatchMore() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader();
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        doReturn(100).when(connectionMock).getContentLength();
        BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
        doReturn(-1).when(bufferedInputStream).read(any(byte[].class), anyInt(), anyInt());
        doReturn(bufferedInputStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundFails(packageDownloader);
    }

    /**
     * Downloading files should not return a {@link CodePushDownloadPackageException}
     * if the amount of <code>receivedBytes</code> does not match the amount of <code>totalBytes</code>, but <code>totalBytes</code> is less that zero.
     */
    @Test
    public void downloadNotFailsIfBytesMismatchLess() throws Exception {
        PackageDownloader packageDownloader = createPackageDownloader();
        HttpURLConnection realConnection = (HttpURLConnection) (new URL(FULL_PACKAGE_URL)).openConnection();
        BufferedInputStream realStream = new BufferedInputStream(realConnection.getInputStream());
        HttpURLConnection connectionMock = Mockito.mock(HttpURLConnection.class);
        doReturn(-1).when(connectionMock).getContentLength();
        doReturn(realStream).when(connectionMock).getInputStream();
        doReturn(connectionMock).when(packageDownloader).createConnection(anyString());
        checkDoInBackgroundNotFails(packageDownloader);
    }

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if a {@link java.io.InputStream#read()} throws an {@link IOException} when closing.
     */
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

    /**
     * Downloading files should return a {@link CodePushDownloadPackageException}
     * if a {@link java.io.InputStream#read()} throws an {@link IOException}.
     */
    @Test
    public void downloadFailsIfReadFails() throws Exception {
        File codePushPath = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        File downloadFolder = new File(codePushPath.getPath());
        downloadFolder.mkdirs();
        PackageDownloader packageDownloader = createPackageDownloader(downloadFolder);
        checkDoInBackgroundFails(packageDownloader);
    }
}
