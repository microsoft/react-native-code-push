package com.microsoft.codepush.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.codepush.common.CodePushConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utils to test settings manager compatibility with the old version of the sdk.
 * Contains simplified version of the old storing methods.
 */
public class CompatibilityUtils {

    /**
     * Key for getting/storing pending CodePush failed update.
     */
    private static final String FAILED_UPDATES_KEY = "CODE_PUSH_FAILED_UPDATES";

    /**
     * Package hash key for pending CodePush update.
     */
    private static final String PENDING_UPDATE_HASH_KEY = "hash";

    /**
     * Key for getting/storing pending CodePush update that is loading.
     */
    private static final String PENDING_UPDATE_IS_LOADING_KEY = "isLoading";

    /**
     * Key for getting/storing pending CodePush update.
     */
    private static final String PENDING_UPDATE_KEY = "CODE_PUSH_PENDING_UPDATE";

    public static void saveFailedUpdate(JSONObject failedPackage, Context context) throws JSONException {
        SharedPreferences mSettings = context.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
        String failedUpdatesString = mSettings.getString(FAILED_UPDATES_KEY, null);
        JSONArray failedUpdates;
        if (failedUpdatesString == null) {
            failedUpdates = new JSONArray();
        } else {
            failedUpdates = new JSONArray(failedUpdatesString);
        }
        failedUpdates.put(failedPackage);
        mSettings.edit().putString(FAILED_UPDATES_KEY, failedUpdates.toString()).commit();
    }

    /**
     * Saves a pending update using old version of the method.
     *
     * @param packageHash hash of the pending update.
     * @param isLoading   whether this update is loading.
     * @param context     application context.
     */
    public static void savePendingUpdate(String packageHash, boolean isLoading, Context context) throws JSONException {
        SharedPreferences mSettings = context.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
        JSONObject pendingUpdate = new JSONObject();
        pendingUpdate.put(PENDING_UPDATE_HASH_KEY, packageHash);
        pendingUpdate.put(PENDING_UPDATE_IS_LOADING_KEY, isLoading);
        mSettings.edit().putString(PENDING_UPDATE_KEY, pendingUpdate.toString()).commit();
    }

    /**
     * Saves any string under the <code>PENDING_UPDATE_KEY</code>.
     *
     * @param fakeString string to be saved.
     * @param context    application context.
     */
    public static void saveStringToPending(String fakeString, Context context) {
        SharedPreferences mSettings = context.getSharedPreferences(CodePushConstants.CODE_PUSH_PREFERENCES, 0);
        mSettings.edit().putString(PENDING_UPDATE_KEY, fakeString).commit();
    }
}
