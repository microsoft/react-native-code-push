package com.microsoft.codepush.common.managers;

import android.os.Environment;

import com.microsoft.codepush.common.apirequests.ApiHttpRequest;
import com.microsoft.codepush.common.apirequests.DownloadPackageTask;
import com.microsoft.codepush.common.datacontracts.CodePushDownloadPackageResult;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushSignatureVerificationException;
import com.microsoft.codepush.common.exceptions.CodePushUnzipException;
import com.microsoft.codepush.common.testutils.CommonTestPlatformUtils;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * This class is for testing those {@link CodePushUpdateManager} test cases that depend on {@link FileUtils} methods failure.
 */
public class UpdateManagerAndroidFileTests {

    /**
     * Test package hash.
     */
    private final static String PACKAGE_HASH = "FHJDKF648723f";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Instance of {@link CommonTestPlatformUtils} to work with.
     */
    private CommonTestPlatformUtils mPlatformUtils;

    /**
     * Instance of testable {@link CodePushUpdateManager}.
     */
    private CodePushUpdateManager codePushUpdateManager;

    @Before
    public void setUp() {
        mPlatformUtils = CommonTestPlatformUtils.getInstance();
        FileUtils fileUtils = FileUtils.getInstance();
        CodePushUtils codePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, codePushUtils);
        recreateUpdateManager(fileUtils, codePushUtils, codePushUpdateUtils);
    }

    /**
     * Recreates {@link CodePushUpdateManager} with the new mocks of utils.
     *
     * @param fileUtils           mocked instance of {@link FileUtils}.
     * @param codePushUtils       mocked instance of {@link CodePushUtils}.
     * @param codePushUpdateUtils mocked instance of {@link CodePushUpdateUtils}.
     */
    private void recreateUpdateManager(FileUtils fileUtils, CodePushUtils codePushUtils, CodePushUpdateUtils codePushUpdateUtils) {
        codePushUpdateManager = new CodePushUpdateManager(new File(Environment.getExternalStorageDirectory(), "/Test").getPath(), mPlatformUtils, fileUtils, codePushUtils, codePushUpdateUtils);
    }

    /**
     * Download package should throw a {@link CodePushDownloadPackageException}
     * if an {@link IOException} is thrown during {@link FileUtils#deleteDirectoryAtPath(String)}.
     * If deleting file at path where a new update should be located fails, the whole method should fail.
     */
    @Test(expected = CodePushDownloadPackageException.class)
    public void downloadFailsIfDeleteNewUpdateFolderPathFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        fileUtils = spy(fileUtils);
        doThrow(new IOException()).when(fileUtils).deleteDirectoryAtPath(anyString());
        doReturn(true).when(fileUtils).fileAtPathExists(anyString());
        CodePushUtils codePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, codePushUtils);
        recreateUpdateManager(fileUtils, codePushUtils, codePushUpdateUtils);
        codePushUpdateManager.downloadPackage("", mock(ApiHttpRequest.class));
    }

    /**
     * Unzip should throw a {@link CodePushUnzipException}
     * if an {@link IOException} is thrown during {@link FileUtils#unzipFile(File, File)}.
     */
    @Test(expected = CodePushUnzipException.class)
    public void unzipFailsIfUnzipFileFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        fileUtils = spy(fileUtils);
        doThrow(new IOException()).when(fileUtils).unzipFile(any(File.class), any(File.class));
        CodePushUtils codePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, codePushUtils);
        recreateUpdateManager(fileUtils, codePushUtils, codePushUpdateUtils);
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn("").when(codePushUpdateManager).getUnzippedFolderPath();
        codePushUpdateManager.unzipPackage(mock(File.class));
    }

    /**
     * Verifying signature should throw a {@link CodePushSignatureVerificationException}
     * if {@link CodePushUpdateUtils#verifyFolderHash(String, String)} throws an {@link IOException}.
     */
    @Test(expected = CodePushSignatureVerificationException.class)
    public void verifyFailsIfVerifyFolderHashFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        CodePushUtils codePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, codePushUtils);
        codePushUpdateUtils = spy(codePushUpdateUtils);
        doThrow(new IOException()).when(codePushUpdateUtils).verifyFolderHash(anyString(), anyString());
        recreateUpdateManager(fileUtils, codePushUtils, codePushUpdateUtils);
        codePushUpdateManager.verifySignature(null, PACKAGE_HASH, true);
    }
}
