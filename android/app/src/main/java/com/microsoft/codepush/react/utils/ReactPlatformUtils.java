package com.microsoft.codepush.react.utils;

import android.content.Context;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.interfaces.CodePushPlatformUtils;

import java.io.File;
import java.io.IOException;

import static com.microsoft.codepush.common.CodePushConstants.CODE_PUSH_APK_BUILD_TIME_KEY;

/**
 * React-specific instance of {@link CodePushPlatformUtils}.
 * Represents bridge between {@link com.microsoft.codepush.common} and {@link com.microsoft.codepush.react}.
 */
public class ReactPlatformUtils implements CodePushPlatformUtils {

    /**
     * Instance of {@link ReactPlatformUtils}. Singleton.
     */
    private static ReactPlatformUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private ReactPlatformUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static ReactPlatformUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ReactPlatformUtils();
        }
        return INSTANCE;
    }

    @Override
    public boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion, Context context) throws CodePushGeneralException {
        try {
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = packageMetadata.getBinaryModifiedTime();
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }
            String packageAppVersion = packageMetadata.getAppVersion();
            long binaryResourcesModifiedTime = getBinaryResourcesModifiedTime(context);
            return binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    currentAppVersion.equals(packageAppVersion);
        } catch (NumberFormatException e) {
            throw new CodePushGeneralException("Error in reading binary modified date from package metadata", e);
        }
    }

    @Override
    public long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException {
        String packageName = context.getPackageName();
        int codePushApkBuildTimeId = context.getResources().getIdentifier(CODE_PUSH_APK_BUILD_TIME_KEY, "string", packageName);

        /* Double quotes replacement is needed for correct restoration of long values from strings.xml.
         * See https://github.com/Microsoft/cordova-plugin-code-push/issues/264 */
        String codePushApkBuildTime = context.getResources().getString(codePushApkBuildTimeId).replaceAll("\"", "");
        return Long.parseLong(codePushApkBuildTime);
    }

    @Override 
    public void clearDebugCache(Context context) throws IOException {

        /* This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78 */
        File cachedDevBundle = new File(context.getFilesDir(), "ReactNativeDevBundle.js");
        if (cachedDevBundle.exists()) {
            if (!cachedDevBundle.delete()) {
                throw new IOException("Couldn't delete debug cache.");
            }
        }
    }
}
