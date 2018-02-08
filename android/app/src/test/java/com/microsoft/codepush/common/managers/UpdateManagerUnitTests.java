package com.microsoft.codepush.common.managers;

import android.os.Environment;

import com.microsoft.codepush.common.apirequests.DownloadPackageTask;
import com.microsoft.codepush.common.exceptions.CodePushDownloadPackageException;
import com.microsoft.codepush.common.testutils.CommonTestPlatformUtils;
import com.microsoft.codepush.common.utils.CodePushUpdateUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Test;

import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * This class contains {@link CodePushUpdateManager} tests, that for some reasons can't be executed in instrumental module.
 */
public class UpdateManagerUnitTests {

    /**
     * Download package should throw a {@link CodePushDownloadPackageException} if an {@link InterruptedException} is thrown during {@link DownloadPackageTask#get()}.
     * If executing an {@link android.os.AsyncTask} fails, downloading package should fail, too.
     */
    @Test(expected = CodePushDownloadPackageException.class)
    public void downloadFailsIfPackageDownloaderFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        CodePushUtils codePushUtils = CodePushUtils.getInstance(fileUtils);
        CodePushUpdateUtils codePushUpdateUtils = CodePushUpdateUtils.getInstance(fileUtils, codePushUtils);
        CodePushUpdateManager codePushUpdateManager = new CodePushUpdateManager(new File(Environment.getExternalStorageDirectory(), "/Test").getPath(),
                CommonTestPlatformUtils.getInstance(),
                fileUtils, codePushUtils, codePushUpdateUtils);
        codePushUpdateManager = spy(codePushUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test/HASH").getPath()).when(codePushUpdateManager).getPackageFolderPath(anyString());
        DownloadPackageTask downloadPackageJob = mock(DownloadPackageTask.class);
        when(downloadPackageJob.get()).thenThrow(new InterruptedException());
        codePushUpdateManager.downloadPackage("", downloadPackageJob);
    }
}
