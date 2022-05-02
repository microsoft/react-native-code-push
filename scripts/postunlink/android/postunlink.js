var linkTools = require('../../tools/linkToolsAndroid');
var fs = require("fs");

module.exports = () => {

    console.log("Running android postunlink script");

    var mainApplicationPath = linkTools.getMainApplicationLocation();

    // 1. Remove the getJSBundleFile override
    var getJSBundleFileOverride = linkTools.getJSBundleFileOverride;

    if (mainApplicationPath) {
        var mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
        if (!linkTools.isJsBundleOverridden(mainApplicationContents)) {
            console.log(`"getJSBundleFile" is already removed`);
        } else {
            mainApplicationContents = mainApplicationContents.replace(`${getJSBundleFileOverride}`, "");
            fs.writeFileSync(mainApplicationPath, mainApplicationContents);
        }
    } else {
        var mainActivityPath = linkTools.getMainActivityPath();
        if (mainActivityPath) {
            var mainActivityContents = fs.readFileSync(mainActivityPath, "utf8");
            if (!linkTools.isJsBundleOverridden(mainActivityContents)) {
                console.log(`"getJSBundleFile" is already removed`);
            } else {
                mainActivityContents = mainActivityContents.replace(getJSBundleFileOverride, "");
                fs.writeFileSync(mainActivityPath, mainActivityContents);
            }
        } else {
            console.log(`Couldn't find Android application entry point. You might need to update it manually. \
    Please refer to plugin configuration section for Android at \
    https://github.com/microsoft/react-native-code-push/blob/master/docs/setup-android.md#plugin-configuration-for-react-native-lower-than-060-android for more details`);
        }
    }

    // 2. Remove the codepush.gradle build task definitions
    var buildGradlePath = linkTools.getBuildGradlePath();

    if (!fs.existsSync(buildGradlePath)) {
        console.log(`Couldn't find build.gradle file. You might need to update it manually. \
    Please refer to plugin installation section for Android at \
    https://github.com/microsoft/react-native-code-push/blob/master/docs/setup-android.md#plugin-installation-android---manual`);
    } else {
        var buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
        var codePushGradleLink = linkTools.codePushGradleLink;
        if (!~buildGradleContents.indexOf(codePushGradleLink)) {
            console.log(`"codepush.gradle" is already unlinked in the build definition`);
        } else {
            buildGradleContents = buildGradleContents.replace(`${codePushGradleLink}`,"");
            fs.writeFileSync(buildGradlePath, buildGradleContents);
        }
    }

    // 3. Remove deployment key
    var stringsResourcesPath = linkTools.getStringsResourcesPath();
    if (!stringsResourcesPath) {
        return Promise.reject(new Error("Couldn't find strings.xml. You might need to update it manually."));
    } else {
        var stringsResourcesContent = fs.readFileSync(stringsResourcesPath, "utf8");
        var deploymentKeyName = linkTools.deploymentKeyName;
        if (!~stringsResourcesContent.indexOf(deploymentKeyName)) {
            console.log(`${deploymentKeyName} already removed from the strings.xml`);
        } else {
            var AndroidDeploymentKey = stringsResourcesContent.match(/(<string moduleConfig="true" name="CodePushDeploymentKey">.*<\/string>)/);
            if (AndroidDeploymentKey) {
                stringsResourcesContent = stringsResourcesContent.replace(`\n\t${AndroidDeploymentKey[0]}`,"");
                fs.writeFileSync(stringsResourcesPath, stringsResourcesContent);
            }
        }
    }
    return Promise.resolve();
}
