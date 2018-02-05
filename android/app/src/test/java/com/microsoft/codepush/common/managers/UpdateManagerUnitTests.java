package com.microsoft.codepush.common.managers;

import android.os.Environment;

import com.microsoft.codepush.common.connection.PackageDownloader;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.exceptions.CodePushSignatureVerificationException;
import com.microsoft.codepush.common.exceptions.CodePushUnzipException;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.testutils.CommonTestPlatformUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * This class is for testing those {@link CodePushUpdateManager} test cases that depend on {@link FileUtils} static methods failure.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, CodePushUpdateUtils.class})
public class UpdateManagerUnitTests {

    private final static String PACKAGE_HASH = "FHJDKF648723f";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Instance of testable {@link CodePushUpdateManager}.
     */
    private CodePushUpdateManager codePushUpdateManager;

    @Before
    public void setUp() {
        codePushUpdateManager = new CodePushUpdateManager(new File(Environment.getExternalStorageDirectory(), "/Test").getPath(), CommonTestPlatformUtils.getInstance());
    }

    /**
     * Download package should throw a {@link CodePushDownloadPackageException}
     * if an {@link IOException} is thrown during {@link FileUtils#deleteDirectoryAtPath(String)}.
     * If deleting file at path where a new update should be located fails, the whole method should fail.
     */
    @Test(expected = CodePushDownloadPackageException.class)
    public void downloadFailsIfDeleteNewUpdateFolderPathFails() throws Exception {
        mockStatic(FileUtils.class);
        doThrow(new IOException()).when(FileUtils.class, "deleteDirectoryAtPath", anyString());
        doReturn(true).when(FileUtils.class, "fileAtPathExists", anyString());
        codePushUpdateManager.downloadPackage("", mock(PackageDownloader.class));
    }

    /**
     * Unzip should throw a {@link CodePushUnzipException}
     * if an {@link IOException} is thrown during {@link FileUtils#unzipFile(File, File)}.
     */
    @Test(expected = CodePushUnzipException.class)
    public void unzipFailsIfUnzipFileFails() throws Exception {
        mockStatic(FileUtils.class);
        doThrow(new IOException()).when(FileUtils.class, "unzipFile", any(File.class), any(File.class));
        codePushUpdateManager = spy(codePushUpdateManager);
        when(codePushUpdateManager, "getUnzippedFolderPath").thenReturn("");
        codePushUpdateManager.unzipPackage(mock(File.class));
    }

    /**
     * Download package should throw a {@link CodePushDownloadPackageException} if an {@link InterruptedException} is thrown during {@link PackageDownloader#get()}.
     * If executing an {@link android.os.AsyncTask} fails, downloading package should fail, too.
     */
    @Test(expected = CodePushDownloadPackageException.class)
    public void downloadFailsIfPackageDownloaderFails() throws Exception {
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test/HASH").getPath()).when(codePushUpdateManager).getPackageFolderPath(anyString());
        PackageDownloader packageDownloader = mock(PackageDownloader.class);
        when(packageDownloader.get()).thenThrow(new InterruptedException());
        codePushUpdateManager.downloadPackage("", packageDownloader);
    }

    /**
     * Verifying signature should throw a {@link CodePushSignatureVerificationException}
     * if {@link CodePushUpdateUtils#verifyFolderHash(String, String)} throws an {@link IOException}.
     */
    @Test(expected = CodePushSignatureVerificationException.class)
    public void verifyFailsIfVerifyFolderHashFails() throws Exception {
        mockStatic(CodePushUpdateUtils.class);
        doThrow(new IOException()).when(CodePushUpdateUtils.class, "verifyFolderHash", anyString(), anyString());
        codePushUpdateManager.verifySignature(null, PACKAGE_HASH, true);
    }
}
