module.exports = {
    dependency: {
        platforms: {
            android: {
                packageInstance:
                    "new CodePush(getResources().getString(R.string.reactNativeCodePush_androidDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)"
            }
        }
    }
};
