package com.microsoft.codepush.common;

import com.microsoft.codepush.common.interfaces.CodePushRestartListener;
import com.microsoft.codepush.common.managers.CodePushRestartManager;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is for testing {@link com.microsoft.codepush.common.managers.CodePushRestartManager}.
 */
public class RestartManagerUnitTests {

    /**
     * Instance of {@link CodePushRestartManager}.
     */
    private CodePushRestartManager mRestartManager;

    /**
     * Mocked {@link CodePushRestartListener}.
     */
    private CodePushRestartListener mRestartListener;

    @Before
    public void setUp() throws Exception {
        mRestartListener = mock(CodePushRestartListener.class);
        when(mRestartListener.onRestartReady(anyBoolean())).thenReturn(true);
        mRestartManager = new CodePushRestartManager(mRestartListener);
    }

    /**
     * Tests the case disallow -> add pending restart -> allow -> perform restart.
     * {@link CodePushRestartListener#onRestartReady(boolean)} should be called.
     */
    @Test
    public void testDisallow() throws Exception {
        mRestartManager.disallowRestarts();
        mRestartManager.restartApp(false);
        mRestartManager.allowRestarts();
        verify(mRestartListener, timeout(0).times(1)).onRestartReady(anyBoolean());
    }

    /**
     * Tests the case disallow -> allow.
     * {@link CodePushRestartListener#onRestartReady(boolean)} should NOT be called.
     */
    @Test
    public void testDisallowEmptyQueue() throws Exception {
        mRestartManager.disallowRestarts();
        mRestartManager.allowRestarts();
        verify(mRestartListener, timeout(0).times(0)).onRestartReady(anyBoolean());
    }

    /**
     * Tests the case disallow -> add pending restart -> clear the queue -> allow.
     * {@link CodePushRestartListener#onRestartReady(boolean)} should NOT be called.
     */
    @Test
    public void testDisallowClearedQueue() throws Exception {
        mRestartManager.disallowRestarts();
        mRestartManager.restartApp(false);
        mRestartManager.clearPendingRestart();
        mRestartManager.allowRestarts();
        verify(mRestartListener, timeout(0).times(0)).onRestartReady(anyBoolean());
    }

    /**
     * Tests the case with two restarts.
     */
    @Test
    public void testMultipleRestarts() throws Exception {
        mRestartManager.restartApp(false);
        mRestartManager.restartApp(false);
    }

    /**
     * Tests the case with restart in queue.
     */
    @Test
    public void testMultipleRestartsInQueue() throws Exception {
        when(mRestartListener.onRestartReady(anyBoolean())).thenReturn(false);
        mRestartManager = new CodePushRestartManager(mRestartListener);
        mRestartManager.disallowRestarts();
        mRestartManager.restartApp(false);
        mRestartManager.restartApp(false);
        mRestartManager.allowRestarts();
        verify(mRestartListener, timeout(0).times(2)).onRestartReady(anyBoolean());
    }
}
