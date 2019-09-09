module.exports = {
  dependency: {
    platforms: {
      android: {
        packageInstance:
          "new CodePush(getResources().getString(R.string.reactNativeCodePush_androidDeploymentKey), getApplicationContext(), BuildConfig.DEBUG)"
      },
      ios: {
        sharedLibraries: ["libz"]
      }
    },
    hooks: {
      postlink: "node node_modules/react-native-code-push/scripts/postlink/run",
      postunlink: "node node_modules/react-native-code-push/scripts/postunlink/run"
    }
  }
};
