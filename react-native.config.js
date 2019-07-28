module.exports = {
    project: {
        ios: {
            sharedLibraries: [
                "libz"
            ]
        },
        android: {
            packageInstance: "new CodePush(getResources().getString(R.string.reactNativeCodePush_androidDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)"
        }
    }
};