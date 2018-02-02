package com.microsoft.codepush.common;

import android.support.test.InstrumentationRegistry;

import com.google.gson.JsonSyntaxException;
import com.microsoft.codepush.common.datacontracts.CodePushPendingUpdate;
import com.microsoft.codepush.common.managers.SettingsManager;
import com.microsoft.codepush.common.utils.CompatibilityUtils;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SettingManagerTests {

    private String PACKAGE_HASH = "3242hjf";

    private SettingsManager mSettingsManager;

    @Before
    public void setUp() throws Exception {
        mSettingsManager = new SettingsManager(InstrumentationRegistry.getContext());
    }

    /**
     * Checks that retrieving pending update saved using earlier version of the sdk returns valid data.
     */
    @Test
    public void pendingUpdateCompatibilityTest() throws Exception {
        CompatibilityUtils.savePendingUpdate(PACKAGE_HASH, true, InstrumentationRegistry.getContext());
        CodePushPendingUpdate codePushPendingUpdate = mSettingsManager.getPendingUpdate();
        assertEquals(codePushPendingUpdate.getPendingUpdateHash(), PACKAGE_HASH);
        assertEquals(codePushPendingUpdate.isPendingUpdateLoading(), true);
        assertTrue(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
    }

    /**
     * {@link SettingsManager#getPendingUpdate()} should return <code>null</code>
     * if a {@link JsonSyntaxException} is thrown during parsing pending info.
     */
    @Test
    public void pendingUpdateParseError() throws Exception {
        CompatibilityUtils.saveStringToPending("abc", InstrumentationRegistry.getContext());
        CodePushPendingUpdate codePushPendingUpdate = mSettingsManager.getPendingUpdate();
        assertNull(codePushPendingUpdate);
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
}
