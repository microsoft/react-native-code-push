package com.microsoft.codepush.common.managers;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGetPackageException;
import com.microsoft.codepush.common.utils.CodePushUtils;

import org.json.JSONObject;

/**
 * Wrapper around {@link CodePushUpdateManager}, returning data mapped to real objects.
 */
public class CodePushUpdateManagerDeserializer {

    /**
     * Instance of {@link CodePushUpdateManager}.
     */
    private CodePushUpdateManager mUpdateManager;

    /**
     * Creates new instance of deserializer using {@link CodePushUpdateManager}.
     *
     * @param updateManager instance of {@link CodePushUpdateManager}.
     */
    public CodePushUpdateManagerDeserializer(CodePushUpdateManager updateManager) {
        mUpdateManager = updateManager;
    }

    /**
     * Gets current package contents.
     *
     * @return object with current package info.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public CodePushLocalPackage getCurrentPackage() throws CodePushGetPackageException {
        JSONObject currentPackage = mUpdateManager.getCurrentPackage();
        if (currentPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(currentPackage, CodePushLocalPackage.class);
        }
        return null;
    }

    /**
     * Gets package object using package identifier (hash).
     *
     * @param packageHash package identifier (hash).
     * @return object with current package info.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public CodePushLocalPackage getPackage(String packageHash) throws CodePushGetPackageException {
        JSONObject localPackage = mUpdateManager.getPackage(packageHash);
        if (localPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(localPackage, CodePushLocalPackage.class);
        }
        return null;
    }

    /**
     * Gets previous installed package object.
     *
     * @return previous installed package object.
     * @throws CodePushGetPackageException exception occurred when obtaining a package.
     */
    public CodePushLocalPackage getPreviousPackage() throws CodePushGetPackageException {
        JSONObject previousPackage = mUpdateManager.getPreviousPackage();
        if (previousPackage != null) {
            return CodePushUtils.convertJsonObjectToObject(previousPackage, CodePushLocalPackage.class);
        }
        return null;
    }
}
