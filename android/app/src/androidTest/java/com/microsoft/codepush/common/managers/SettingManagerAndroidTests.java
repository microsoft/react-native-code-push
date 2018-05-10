package com.microsoft.codepush.common.managers;

import android.support.test.InstrumentationRegistry;

import com.google.gson.JsonSyntaxException;
import com.microsoft.codepush.common.CodePushStatusReportIdentifier;
import com.microsoft.codepush.common.datacontracts.CodePushDeploymentStatusReport;
import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPackage;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.enums.CodePushDeploymentStatus;
import com.microsoft.codepush.common.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.common.testutils.CommonSettingsCompatibilityUtils;
import com.microsoft.codepush.common.utils.CodePushUtils;
import com.microsoft.codepush.common.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SettingManagerAndroidTests {

    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String LABEL = "awesome package";
    private final static boolean FAILED_INSTALL = false;
    private final static String APP_VERSION = "2.2.1";
    private final static String DESCRIPTION = "short description";
    private final static boolean IS_MANDATORY = true;
    private final static String PACKAGE_HASH = "HASH";

    /**
     * Instance of {@link CodePushUtils} to work with.
     */
    private CodePushUtils mCodePushUtils;

    /**
     * Instance of {@link SettingsManager} to work with.
     */
    private SettingsManager mSettingsManager;

    @Before
    public void setUp() throws Exception {
        mCodePushUtils = CodePushUtils.getInstance(FileUtils.getInstance());
        mSettingsManager = new SettingsManager(InstrumentationRegistry.getContext(), mCodePushUtils);
    }

    /**
     * Checks that retrieving pending update saved using earlier version of the sdk returns valid data.
     */
    @Test
    public void pendingUpdateCompatibilityTest() throws Exception {
        CommonSettingsCompatibilityUtils.savePendingUpdate(PACKAGE_HASH, true, InstrumentationRegistry.getContext());
        CodePushPendingUpdate codePushPendingUpdate = mSettingsManager.getPendingUpdate();
        assertEquals(codePushPendingUpdate.getPendingUpdateHash(), PACKAGE_HASH);
        assertEquals(codePushPendingUpdate.isPendingUpdateLoading(), true);
        assertTrue(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
        assertFalse(mSettingsManager.isPendingUpdate(""));
        assertTrue(mSettingsManager.isPendingUpdate(null));
    }

    /**
     * Checks that saving -> retrieving pending update returns valid data.
     */
    @Test
    public void pendingUpdateTest() throws Exception {
        CodePushPendingUpdate codePushPendingUpdate = new CodePushPendingUpdate();
        codePushPendingUpdate.setPendingUpdateHash(PACKAGE_HASH);
        codePushPendingUpdate.setPendingUpdateIsLoading(false);
        mSettingsManager.savePendingUpdate(codePushPendingUpdate);
        codePushPendingUpdate = mSettingsManager.getPendingUpdate();
        assertEquals(codePushPendingUpdate.getPendingUpdateHash(), PACKAGE_HASH);
        assertEquals(codePushPendingUpdate.isPendingUpdateLoading(), false);
        assertFalse(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
    }

    /**
     * {@link SettingsManager#isPendingUpdate(String)} should return <code>false</code> if pending update is <code>null</code>.
     */
    @Test
    public void pendingUpdateNullTest() throws Exception {
        mSettingsManager.removePendingUpdate();
        assertFalse(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
        assertFalse(mSettingsManager.isPendingUpdate(null));
    }

    /**
     * {@link SettingsManager#getPendingUpdate()} should throw a {@link CodePushMalformedDataException}
     * if a {@link JsonSyntaxException} is thrown during parsing pending info.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void pendingUpdateParseError() throws Exception {
        CommonSettingsCompatibilityUtils.saveStringToPending("abc", InstrumentationRegistry.getContext());
        mSettingsManager.getPendingUpdate();
    }

    /**
     * {@link SettingsManager#getPendingUpdate()} should return <code>null</code> if there is no info about the pending update.
     */
    @Test
    public void pendingUpdateIsNull() throws Exception {
        mSettingsManager.removePendingUpdate();
        CodePushPendingUpdate codePushPendingUpdate = mSettingsManager.getPendingUpdate();
        assertNull(codePushPendingUpdate);
    }

    /**
     * Checks that retrieving failed updates saved using earlier version of the sdk returns valid data.
     */
    @Test
    public void failedUpdateCompatibilityTest() throws Exception {
        mSettingsManager.removeFailedUpdates();
        CodePushPackage codePushLocalPackage = createPackage(PACKAGE_HASH);
        CommonSettingsCompatibilityUtils.saveFailedUpdate(mCodePushUtils.convertObjectToJsonObject(codePushLocalPackage), InstrumentationRegistry.getContext());
        List<CodePushPackage> codePushLocalPackages = mSettingsManager.getFailedUpdates();
        codePushLocalPackage = codePushLocalPackages.get(0);
        assertEquals(codePushLocalPackage.getDeploymentKey(), DEPLOYMENT_KEY);
        codePushLocalPackage = createLocalPackage("123");
        CommonSettingsCompatibilityUtils.saveFailedUpdate(mCodePushUtils.convertObjectToJsonObject(codePushLocalPackage), InstrumentationRegistry.getContext());
        codePushLocalPackages = mSettingsManager.getFailedUpdates();
        codePushLocalPackage = codePushLocalPackages.get(1);
        assertEquals(codePushLocalPackage.getPackageHash(), "123");
    }

    /**
     * Creates instance of {@link CodePushLocalPackage} for testing.
     *
     * @param packageHash hash to create package with.
     * @return instance of {@link CodePushLocalPackage}.
     */
    private CodePushLocalPackage createLocalPackage(String packageHash) {
        CodePushPackage codePushPackage = createPackage(packageHash);
        return CodePushLocalPackage.createLocalPackage(false, false, false, false, "", codePushPackage);
    }

    private CodePushPackage createPackage(String packageHash) {
        CodePushPackage codePushPackage = new CodePushPackage();
        codePushPackage.setAppVersion(APP_VERSION);
        codePushPackage.setDeploymentKey(DEPLOYMENT_KEY);
        codePushPackage.setDescription(DESCRIPTION);
        codePushPackage.setFailedInstall(FAILED_INSTALL);
        codePushPackage.setLabel(LABEL);
        codePushPackage.setMandatory(IS_MANDATORY);
        codePushPackage.setPackageHash(packageHash);
        return codePushPackage;
    }

    /**
     * Checks that saving -> retrieving failed updates returns valid data.
     */
    @Test
    public void failedUpdateTest() throws Exception {
        mSettingsManager.removeFailedUpdates();
        CodePushPackage codePushPackage = createPackage("newHash");
        mSettingsManager.saveFailedUpdate(codePushPackage);
        codePushPackage = createLocalPackage(PACKAGE_HASH);
        mSettingsManager.saveFailedUpdate(codePushPackage);
        List<CodePushPackage> codePushPackages = mSettingsManager.getFailedUpdates();
        codePushPackage = codePushPackages.get(0);
        assertEquals(codePushPackage.getDeploymentKey(), DEPLOYMENT_KEY);
        assertTrue(mSettingsManager.existsFailedUpdate(PACKAGE_HASH));
        assertFalse(mSettingsManager.existsFailedUpdate(null));
    }

    /**
     * {@link SettingsManager#existsFailedUpdate(String)} should return <code>false</code> if failed update info is empty.
     */
    @Test
    public void failedUpdateNullTest() throws Exception {
        mSettingsManager.removeFailedUpdates();
        assertFalse(mSettingsManager.existsFailedUpdate(PACKAGE_HASH));
    }

    /**
     * {@link SettingsManager#getFailedUpdates()} should throw a {@link CodePushMalformedDataException}
     * if a {@link JsonSyntaxException} is thrown during parsing failed updates info.
     */
    @Test(expected = CodePushMalformedDataException.class)
    public void failedUpdateParseError() throws Exception {
        CommonSettingsCompatibilityUtils.saveStringToFailed("abc", InstrumentationRegistry.getContext());
        mSettingsManager.getFailedUpdates();
    }

    /**
     * {@link SettingsManager#getFailedUpdates()} should return empty array if there is no info about the failed updates.
     */
    @Test
    public void failedUpdateIsNull() throws Exception {
        mSettingsManager.removeFailedUpdates();
        List<CodePushPackage> codePushPackages = mSettingsManager.getFailedUpdates();
        assertEquals(0, codePushPackages.size());
    }

    /**
     * Tests workflow save identifier -> get identifier.
     */
    @Test
    public void identifierTest() throws Exception {
        mSettingsManager.saveIdentifierOfReportedStatus(new CodePushStatusReportIdentifier("123", "1.2"));
        CodePushStatusReportIdentifier codePushStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        assertEquals(codePushStatusReportIdentifier.getDeploymentKey(), "123");
    }

    /**
     * Tests workflow save status report -> get status report -> remove report -> get report and assert that it is <code>null</code>.
     */
    @Test
    public void statusReportTest() throws Exception {
        CodePushDeploymentStatusReport codePushDeploymentStatusReport = new CodePushDeploymentStatusReport();
        codePushDeploymentStatusReport.setAppVersion("1.2");
        codePushDeploymentStatusReport.setStatus(CodePushDeploymentStatus.SUCCEEDED);
        codePushDeploymentStatusReport.setPreviousDeploymentKey("123");
        codePushDeploymentStatusReport.setPreviousLabelOrAppVersion("1.2");
        codePushDeploymentStatusReport.setClientUniqueId("111");
        mSettingsManager.saveStatusReportForRetry(codePushDeploymentStatusReport);
        codePushDeploymentStatusReport = mSettingsManager.getStatusReportSavedForRetry();
        assertEquals(codePushDeploymentStatusReport.getAppVersion(), "1.2");
        mSettingsManager.removeStatusReportSavedForRetry();
        codePushDeploymentStatusReport = mSettingsManager.getStatusReportSavedForRetry();
        assertNull(codePushDeploymentStatusReport);
    }
}