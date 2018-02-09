package com.microsoft.codepush.reactv2.utils;

import android.content.Context;

import com.microsoft.codepush.common.datacontracts.CodePushLocalPackage;
import com.microsoft.codepush.common.exceptions.CodePushGeneralException;
import com.microsoft.codepush.common.utils.PlatformUtils;

import java.io.File;
import java.io.IOException;

import static com.microsoft.codepush.common.CodePushConstants.CODE_PUSH_APK_BUILD_TIME_KEY;

public class ReactNativeUtils implements PlatformUtils {

    private Context mContext;

    public ReactNativeUtils(Context context) {
        mContext = context;
    }

    @Override
    public String getUpdateFolderPath(String hash) {
        //TODO implement it
        return null;
    }

    @Override
    public boolean isPackageLatest(CodePushLocalPackage packageMetadata, String currentAppVersion) throws CodePushGeneralException {
        try {
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = packageMetadata.getBinaryModifiedTime();
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }
            String packageAppVersion = packageMetadata.getAppVersion();
            long binaryResourcesModifiedTime = getBinaryResourcesModifiedTime();
            return binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    currentAppVersion.equals(packageAppVersion);
        } catch (NumberFormatException e) {
            throw new CodePushGeneralException("Error in reading binary modified date from package metadata", e);
        }
    }

    @Override
    public void clearDebugCache() throws IOException {
        // This needs to be kept in sync with https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/devsupport/DevSupportManager.java#L78
        File cachedDevBundle = new File(mContext.getFilesDir(), "ReactNativeDevBundle.js");
        if (cachedDevBundle.exists()) {
            if (!cachedDevBundle.delete()) {
                throw new IOException("Couldn't delete debug cache.");
            }
        }
    }

    private long getBinaryResourcesModifiedTime() throws NumberFormatException {
        String packageName = mContext.getPackageName();
        int codePushApkBuildTimeId = mContext.getResources().getIdentifier(CODE_PUSH_APK_BUILD_TIME_KEY, "string", packageName);

        /* Double quotes replacement is needed for correct restoration of long values from strings.xml.
         * See https://github.com/Microsoft/cordova-plugin-code-push/issues/264 */
        String codePushApkBuildTime = mContext.getResources().getString(codePushApkBuildTimeId).replaceAll("\"", "");
        return Long.parseLong(codePushApkBuildTime);
    }
}
