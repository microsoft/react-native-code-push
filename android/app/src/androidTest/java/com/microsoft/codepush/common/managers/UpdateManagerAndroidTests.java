package com.microsoft.codepush.common.managers;

import android.os.Environment;

import com.microsoft.codepush.common.CodePushConstants;
import com.microsoft.codepush.common.connection.DownloadPackageJob;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackageInfo;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.exceptions.CodePushInstallException;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.exceptions.CodePushMergeException;
import com.microsoft.codepush.common.exceptions.CodePushRollbackException;
import com.microsoft.codepush.common.testutils.CommonTestPlatformUtils;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;
import com.microsoft.codepush.common.utils.PlatformUtils;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.microsoft.codepush.common.CodePushConstants.CODE_PUSH_FOLDER_PREFIX;
import static com.microsoft.codepush.common.testutils.UpdateManagerAndroidTestUtils.executeDownload;
import static com.microsoft.codepush.common.testutils.UpdateManagerAndroidTestUtils.executeFullWorkflow;
import static com.microsoft.codepush.common.testutils.UpdateManagerAndroidTestUtils.executeWorkflow;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * This class tests all the {@link CodePushUpdateManager} scenarios.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UpdateManagerAndroidTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Url to download a package that represents a full update.
     */
    private final static String FULL_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/6CjTRZUgaYrHlhH3mKy2JsQVIJtsa0021bd2-9be1-4904-b4c6-16ce9c797779";

    /**
     * Url to download a package that represents a diff update.
     */
    private final static String DIFF_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/8wuI2wwTlf4RioIb1cLRtyQyzRW80840428d-683e-4d30-a120-c592a355a594";

    /**
     * Url to download a package that represents a signed package.
     */
    private final static String SIGNED_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/OWIRaqwJQUbNeiX60nDnijj9HxMza0021bd2-9be1-4904-b4c6-16ce9c797779";

    /**
     * Hash of the package that represents a full update.
     */
    private final static String FULL_PACKAGE_HASH = "a1d28a073a1fa45745a8b1952ccc5c2bd4753e533e7b9e48459a6c186ecd32af";

    /**
     * Hash of the package that represents a diff update.
     */
    private final static String DIFF_PACKAGE_HASH = "ff46674f196ae852ccb67e49346a11cb9d8c0243ba24003e11b83dd7469b5dd4";

    /**
     * Hash of the signed package.
     */
    private final static String SIGNED_PACKAGE_HASH = "ce9148e0d0422dc7ffefba3a82f527a0e75f51c449f34a5f7dabab6f36251aaf";

    /**
     * Public key used to sign a test package.
     */
    private final static String SIGNED_PACKAGE_PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM4bfGAHAEx+IVl5/qaRHisPvpGfCY47O7EkW8XhZVer+bo1k6VT3s8hPBMQfcFw/ZQotWwLkvStelvrQptJFiUCAwEAAQ";

    /**
     * Instance of {@link CodePushUpdateManager} to work with.
     */
    private CodePushUpdateManager codePushUpdateManager;

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Instance of package json object.
     */
    private CodePushLocalPackage packageObject;

    @Before
    public void setUp() throws Exception {
        PlatformUtils platformUtils = CommonTestPlatformUtils.getInstance();
        mFileUtils = FileUtils.getInstance();
        mCodePushUtils = CodePushUtils.getInstance(mFileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(mFileUtils, mCodePushUtils);
        codePushUpdateManager = new CodePushUpdateManager(Environment.getExternalStorageDirectory().getPath(), platformUtils, mFileUtils, mCodePushUtils, codePushUpdateUtils);
        CodePushPackage codePushPackage = new CodePushPackage();
        codePushPackage.setAppVersion("1.2");
        codePushPackage.setPackageHash(FULL_PACKAGE_HASH);
        codePushPackage.setLabel("fdfds");
        codePushPackage.setDeploymentKey("FDSFD");
        codePushPackage.setDescription("description");
        codePushPackage.setFailedInstall(false);
        codePushPackage.setMandatory(false);
        packageObject = CodePushLocalPackage.createLocalPackage(false, false, false, false, "/www/index.html", codePushPackage);
        File codePushFolder = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        codePushFolder.mkdirs();
    }

    /**
     * This tests a full update workflow. Download -> unzip -> merge install several packages.
     */
    @Test
    public void fullWorkflowTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        executeFullWorkflow(codePushUpdateManager, FULL_PACKAGE_HASH, FULL_PACKAGE_URL);
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(FULL_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        packageObject.setPackageHash(FULL_PACKAGE_HASH);
        mCodePushUtils.writeObjectToJsonFile(packageObject, newUpdateMetadataPath);
        executeFullWorkflow(codePushUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        packageObject.setPackageHash(DIFF_PACKAGE_HASH);
        mCodePushUtils.writeObjectToJsonFile(packageObject, newUpdateMetadataPath);
        CodePushLocalPackage codePushPreviousPackage = codePushUpdateManager.getPreviousPackage();
        CodePushLocalPackage codePushCurrentPackage = codePushUpdateManager.getCurrentPackage();
        CodePushLocalPackage codePushPackage = codePushUpdateManager.getPackage(DIFF_PACKAGE_HASH);
        assertEquals(FULL_PACKAGE_HASH, codePushPreviousPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushCurrentPackage.getPackageHash());
        assertTrue(mFileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));
    }

    /**
     * This tests the case when relative entry path is <code>null</code>.
     */
    @Test
    public void relativeEntryPathNullTest() throws Exception {
        packageObject.setAppEntryPoint(null);
        packageObject.setPackageHash(DIFF_PACKAGE_HASH);
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(packageObject).when(codePushUpdateManager).getCurrentPackage();
        doReturn("").when(codePushUpdateManager).getCurrentPackageFolderPath();
        assertFalse(mFileUtils.fileAtPathExists(codePushUpdateManager.getCurrentPackageEntryPath("index.html")));
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
        executeWorkflow(codePushUpdateManager, SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_PUBLIC_KEY, "index.html");
        executeWorkflow(codePushUpdateManager, SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(SIGNED_PACKAGE_HASH, null, "index.html");
    }

    /**
     * This tests that clearing updates works properly.
     */
    @Test
    public void updateManagerClearTest() throws Exception {
        codePushUpdateManager.clearUpdates();
        assertNull(codePushUpdateManager.getCurrentPackageEntryPath(""));
        assertFalse(mFileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath()));
        CodePushLocalPackage codePushLocalPackage = codePushUpdateManager.getCurrentPackage();
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
        codePushUpdateManager.installPackage(packageObject.getPackageHash(), true);
        CodePushUpdateManager.setUsingTestConfiguration(false);
    }

    /**
     * Tests installing the package with the same hash.
     */
    @Test
    public void installTheSamePackage() throws Exception {
        /* Install the same package. */
        codePushUpdateManager.installPackage("dfd", true);
        codePushUpdateManager.installPackage("dfd", true);

        /* Install some new package. */
        codePushUpdateManager.installPackage("ffffff", false);

        /* Install the same as previous. */
        codePushUpdateManager.installPackage("dfd", false);
        codePushUpdateManager = spy(codePushUpdateManager);

        /* Both current and passed package hashes are null and therefore equal. */
        doReturn(null).when(codePushUpdateManager).getCurrentPackageHash();
        codePushUpdateManager.installPackage(null, true);
    }

    /**
     * Tests download package with null callback.
     */
    @Test
    public void nullDownloadProgressCallBack() throws Exception {
        DownloadPackageJob downloadPackageJob = new DownloadPackageJob(mFileUtils);
        File downloadFolder = new File(Environment.getExternalStorageDirectory(), CODE_PUSH_FOLDER_PREFIX);
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, CodePushConstants.DOWNLOAD_FILE_NAME);
        downloadPackageJob.setParameters(FULL_PACKAGE_URL, downloadFilePath, null);
        codePushUpdateManager.downloadPackage(FULL_PACKAGE_HASH, downloadPackageJob);
    }

    /**
     * Tests rollback workflow. Install -> install -> rollback.
     */
    @Test
    public void installTestRollback() throws Exception {
        codePushUpdateManager.installPackage(packageObject.getPackageHash(), false);
        codePushUpdateManager.installPackage(DIFF_PACKAGE_HASH, false);
        CodePushPackageInfo codePushPackageInfo = codePushUpdateManager.getCurrentPackageInfo();
        assertNotSame(FULL_PACKAGE_HASH, codePushPackageInfo.getCurrentPackage());
        assertEquals(FULL_PACKAGE_HASH, codePushUpdateManager.getPreviousPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, codePushPackageInfo.getCurrentPackage());
        codePushUpdateManager.rollbackPackage();
        codePushPackageInfo = codePushUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, codePushPackageInfo.getCurrentPackage());
    }

    /**
     * Current package folder path should be deleted before installation.
     */
    @Test
    public void packageFolderIsDeleted() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()).when(codePushUpdateManager).getCurrentPackageFolderPath();
        codePushUpdateManager.installPackage(packageObject.getPackageHash(), true);
        assertFalse(mFileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()));
    }

    /**
     * Previous package folder path should be deleted before installation.
     */
    @Test
    public void previousPackageFolderIsDeleted() throws Exception {
        codePushUpdateManager.installPackage("ddd", false);
        codePushUpdateManager.installPackage("fdsf", false);
        codePushUpdateManager.installPackage("fds", false);
    }

    /**
     * {@link CodePushUpdateManager#getPreviousPackage()} should throw a {@link CodePushGetPackageException}
     * if {@link CodePushUtils#getJsonObjectFromFile(String)} throws a {@link CodePushMalformedDataException}.
     */
    @Test(expected = CodePushGetPackageException.class)
    public void getPreviousPackageFailsIfGetJsonFails() throws Exception {
        String newUpdateFolderPath = codePushUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, CodePushConstants.PACKAGE_FILE_NAME);
        new File(newUpdateMetadataPath).delete();
        codePushUpdateManager.getPackage(DIFF_PACKAGE_HASH);
    }

    /**
     * Merge should throw a {@link CodePushMergeException} if wrong app entry point passed.
     */
    @Test(expected = CodePushMergeException.class)
    public void mergeFailsIfWrongAppEntryPoint() throws Exception {
        codePushUpdateManager.clearUpdates();
        executeWorkflow(codePushUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        codePushUpdateManager.installPackage(DIFF_PACKAGE_HASH, false);
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
        executeWorkflow(codePushUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        codePushUpdateManager.mergeDiff(DIFF_PACKAGE_HASH, "", "index.html");
    }

    /**
     * Installing a package should throw a {@link CodePushInstallException}
     * if {@link CodePushUpdateManager#updateCurrentPackageInfo(CodePushPackageInfo)} )} throws an {@link IOException} due to {@link java.io.FileNotFoundException}.
     */
    @Test(expected = CodePushInstallException.class)
    public void installTestFail() throws Exception {
        mFileUtils.deleteDirectoryAtPath(new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX).getPath());
        codePushUpdateManager.installPackage(packageObject.getPackageHash(), false);
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
     * Downloading files should throw a {@link CodePushDownloadPackageException}
     * if a {@link java.net.MalformedURLException} is thrown when attempting to download.
     */
    @Test(expected = CodePushDownloadPackageException.class)
    public void downloadPackageFailsIfPackageDownloaderFails() throws Exception {
        executeDownload(codePushUpdateManager, "fff", false, "/");
    }

    /**
     * If status file does not exist, package info should be empty.
     */
    @Test
    public void noPackageInfoTest() throws Exception {
        File codePush = new File(Environment.getExternalStorageDirectory(), CodePushConstants.CODE_PUSH_FOLDER_PREFIX);
        new File(codePush, CodePushConstants.STATUS_FILE_NAME).delete();
        CodePushPackageInfo codePushPackageInfo = codePushUpdateManager.getCurrentPackageInfo();
        assertNull(codePushPackageInfo.getCurrentPackage());
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
}
