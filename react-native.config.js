module.exports = {
    dependency: {
        platforms: {
            android: {
                packageInstance:
                    "CodePush.getInstance(getResources().getString(R.string.CodePushDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)"
            }
        }
    }
};
