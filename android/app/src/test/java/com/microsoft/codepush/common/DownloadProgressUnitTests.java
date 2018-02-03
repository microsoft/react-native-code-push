package com.microsoft.codepush.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class DownloadProgressUnitTests {

    private DownloadProgress downloadProgress;

    @Before
    public void setUp() {
        downloadProgress = new DownloadProgress(1024, 512);
    }

    @Test
    public void testGetTotalBytes() {
        assertEquals(1024, downloadProgress.getTotalBytes());
    }

    @Test
    public void testGetReceivedBytes() {
        assertEquals(512, downloadProgress.getReceivedBytes());
    }

    @Test
    public void testIsCompleted() {
        assertFalse(downloadProgress.isCompleted());
        downloadProgress = new DownloadProgress(1024, 1024);
        assertTrue(downloadProgress.isCompleted());
    }
}
