package com.microsoft.codepush.react;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

class DownloadProgress {
    private long mTotalBytes;
    private long mReceivedBytes;

    public DownloadProgress (long totalBytes, long receivedBytes){
        mTotalBytes = totalBytes;
        mReceivedBytes = receivedBytes;
    }

    public WritableMap createWritableMap() {
        WritableMap map = new WritableNativeMap();
        if (mTotalBytes < Integer.MAX_VALUE) {
            map.putInt("totalBytes", (int) mTotalBytes);
            map.putInt("receivedBytes", (int) mReceivedBytes);
        } else {
            map.putDouble("totalBytes", mTotalBytes);
            map.putDouble("receivedBytes", mReceivedBytes);
        }
        return map;
    }

    public boolean isCompleted() {
        return mTotalBytes == mReceivedBytes;
    }
}
