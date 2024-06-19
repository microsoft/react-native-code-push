module.exports = {
    project:{
        ios:{
            automaticPodsInstallation:true
        }
    },
    dependency: {
        platforms: {
            android: {
                packageInstance:
                    "new CodePush(getResources().getString(R.string.CodePushDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)"
            }
        }
    }
};
