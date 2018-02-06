package com.microsoft.codepush.common.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests all the enum classes.
 */
public class EnumUnitTests {

    @Test
    public void enumsTest() throws Exception {
        CodePushCheckFrequency codePushCheckFrequency = CodePushCheckFrequency.MANUAL;
        int checkFrequencyValue = codePushCheckFrequency.getValue();
        assertEquals(2, checkFrequencyValue);
        CodePushDeploymentStatus codePushDeploymentStatus = CodePushDeploymentStatus.SUCCEEDED;
        String deploymentStatusValue = codePushDeploymentStatus.getValue();
        assertEquals("DeploymentSucceeded", deploymentStatusValue);
        CodePushInstallMode codePushInstallMode = CodePushInstallMode.IMMEDIATE;
        int installModeValue = codePushInstallMode.getValue();
        assertEquals(0, installModeValue);
        CodePushSyncStatus codePushSyncStatus = CodePushSyncStatus.AWAITING_USER_ACTION;
        int syncStatusValue = codePushSyncStatus.getValue();
        assertEquals(6, syncStatusValue);
        CodePushUpdateState codePushUpdateState = CodePushUpdateState.LATEST;
        int updateStateValue = codePushUpdateState.getValue();
        assertEquals(2, updateStateValue);

        /* Test <code>valueOf()</code> and <code>values()</code>. */
        assertEquals(3, CodePushCheckFrequency.values().length);
        assertEquals(2, CodePushDeploymentStatus.values().length);
        assertEquals(4, CodePushInstallMode.values().length);
        assertEquals(9, CodePushSyncStatus.values().length);
        assertEquals(3, CodePushUpdateState.values().length);
        assertEquals(CodePushUpdateState.RUNNING, CodePushUpdateState.valueOf("RUNNING"));
        assertEquals(CodePushDeploymentStatus.FAILED, CodePushDeploymentStatus.valueOf("FAILED"));
        assertEquals(CodePushInstallMode.IMMEDIATE, CodePushInstallMode.valueOf("IMMEDIATE"));
        assertEquals(CodePushSyncStatus.AWAITING_USER_ACTION, CodePushSyncStatus.valueOf("AWAITING_USER_ACTION"));
        assertEquals(CodePushCheckFrequency.MANUAL, CodePushCheckFrequency.valueOf("MANUAL"));
    }
}
