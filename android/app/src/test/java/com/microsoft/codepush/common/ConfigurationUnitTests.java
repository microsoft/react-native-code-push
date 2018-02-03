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
public class ConfigurationUnitTests {

    private final static String CLIENT_UNIQUE_ID = "YHFv65";
    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String APP_VERSION = "2.2.1";
    private final static String PACKAGE_HASH = "HASH";
    private final static String SERVER_URL = "https";

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void correctConfigurationTest() {
        CodePushConfiguration correctConfig = new CodePushConfiguration();
        correctConfig.setAppVersion(APP_VERSION)
                .setClientUniqueId(CLIENT_UNIQUE_ID)
                .setDeploymentKey(DEPLOYMENT_KEY)
                .setPackageHash(PACKAGE_HASH)
                .setServerUrl(SERVER_URL);
        assertEquals(APP_VERSION, correctConfig.getAppVersion());
        assertEquals(CLIENT_UNIQUE_ID, correctConfig.getClientUniqueId());
        assertEquals(DEPLOYMENT_KEY, correctConfig.getDeploymentKey());
        assertEquals(PACKAGE_HASH, correctConfig.getPackageHash());
        assertEquals(SERVER_URL, correctConfig.getServerUrl());

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