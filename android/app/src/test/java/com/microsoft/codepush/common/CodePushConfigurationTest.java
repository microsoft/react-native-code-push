package com.microsoft.codepush.common;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AppCenterLog.class})
public class CodePushConfigurationTest {
    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void correctConfigurationTest() {
        CodePushConfiguration correctConfig = new CodePushConfiguration();
        correctConfig.setAppVersion("1.0.0")
                .setClientUniqueId("clientUniqueId")
                .setDeploymentKey("deploymentKey")
                .setPackageHash("packageHash")
                .setServerUrl("serverUrl");
        assertEquals("1.0.0", correctConfig.getAppVersion());
        assertEquals("clientUniqueId", correctConfig.getClientUniqueId());
        assertEquals("deploymentKey", correctConfig.getDeploymentKey());
        assertEquals("packageHash", correctConfig.getPackageHash());
        assertEquals("serverUrl", correctConfig.getServerUrl());

        /* Package hash can be null. */
        correctConfig.setPackageHash(null);
        assertEquals(null, correctConfig.getPackageHash());
        verifyStatic(times(0));
        AppCenterLog.error(eq(CodePush.LOG_TAG), anyString());
    }

    @Test
    public void wrongConfigurationTest() {
        CodePushConfiguration wrongConfig = new CodePushConfiguration();
        wrongConfig.setAppVersion(null)
                .setClientUniqueId(null)
                .setDeploymentKey(null)
                .setServerUrl(null);
        assertEquals(null, wrongConfig.getAppVersion());
        assertEquals(null, wrongConfig.getClientUniqueId());
        assertEquals(null, wrongConfig.getDeploymentKey());
        assertEquals(null, wrongConfig.getServerUrl());

        verifyStatic(times(4));
        AppCenterLog.error(eq(CodePush.LOG_TAG), anyString());
    }
}
