package com.microsoft.codepush.react.managers;

import com.microsoft.codepush.react.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.react.managers.CodePushUpdateManager;
import com.microsoft.codepush.react.utils.CodePushUtils;

import org.json.JSONObject;

public class CodePushUpdateManagerDeserializer {
    private CodePushUpdateManager mUpdateManager;

    public CodePushUpdateManagerDeserializer(CodePushUpdateManager updateManager) {
        mUpdateManager = updateManager;
    }

    public CodePushLocalPackage getCurrentPackage() {
        JSONObject currentPackage = mUpdateManager.getCurrentPackage();
        if (currentPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(currentPackage, CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPackage(String packageHash) {
        JSONObject localPackage = mUpdateManager.getPackage(packageHash);
        if (localPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(localPackage, CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPreviousPackage() {
        JSONObject previousPackage = mUpdateManager.getPreviousPackage();
        if (previousPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(previousPackage, CodePushLocalPackage.class);
        }
        return null;
    }
}
