package com.microsoft.codepush.common;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.codepush.common.exceptions.CodePushIllegalArgumentException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
    public void correctConfigurationTest() throws Exception {
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
    }

    @Test(expected = CodePushIllegalArgumentException.class)
    public void wrongConfigurationAppVersionNull() throws Exception {
        CodePushConfiguration wrongConfig = new CodePushConfiguration();
        wrongConfig.setAppVersion(null);
    }

    @Test(expected = CodePushIllegalArgumentException.class)
    public void wrongConfigurationClientIdNull() throws Exception {
        CodePushConfiguration wrongConfig = new CodePushConfiguration();
        wrongConfig.setClientUniqueId(null);
    }

    @Test(expected = CodePushIllegalArgumentException.class)
    public void wrongConfigurationDeploymentKeyNull() throws Exception {
        CodePushConfiguration wrongConfig = new CodePushConfiguration();
        wrongConfig.setDeploymentKey(null);
    }

    @Test(expected = CodePushIllegalArgumentException.class)
    public void wrongConfigurationServerUrlNull() throws Exception {
        CodePushConfiguration wrongConfig = new CodePushConfiguration();
        wrongConfig.setServerUrl(null);
    }
}
