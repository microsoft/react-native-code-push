var linkTools = require('../../tools/linkToolsAndroid');
var fs = require("fs");
var inquirer = require('inquirer');

module.exports = () => {

    console.log("Running android postlink script");

    var buildGradlePath = linkTools.getBuildGradlePath();
    var mainApplicationPath = linkTools.getMainApplicationLocation();

    // 1. Add the getJSBundleFile override
    var getJSBundleFileOverride = linkTools.getJSBundleFileOverride;

    if (mainApplicationPath) {
        var mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
        if (linkTools.isJsBundleOverridden(mainApplicationContents)) {
            console.log(`"getJSBundleFile" is already overridden`);
        } else {
            var reactNativeHostInstantiation = linkTools.reactNativeHostInstantiation;
            mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiation,
                `${reactNativeHostInstantiation}${getJSBundleFileOverride}`);
            fs.writeFileSync(mainApplicationPath, mainApplicationContents);
        }
    } else {
        var mainActivityPath = linkTools.getMainActivityPath();
        if (mainActivityPath) {
            var mainActivityContents = fs.readFileSync(mainActivityPath, "utf8");
            if (linkTools.isJsBundleOverridden(mainActivityContents)) {
                console.log(`"getJSBundleFile" is already overridden`);
            } else {
                var mainActivityClassDeclaration = linkTools.mainActivityClassDeclaration;
                mainActivityContents = mainActivityContents.replace(mainActivityClassDeclaration,
                    `${mainActivityClassDeclaration}${getJSBundleFileOverride}`);
                fs.writeFileSync(mainActivityPath, mainActivityContents);
            }
        } else {
            return Promise.reject(`Couldn't find Android application entry point. You might need to update it manually. \
    Please refer to plugin configuration section for Android at \
    https://github.com/microsoft/react-native-code-push/blob/master/docs/setup-android.md#plugin-configuration-for-react-native-lower-than-060-android for more details`);
        }
    }

    if (!fs.existsSync(buildGradlePath)) {
        return Promise.reject(`Couldn't find build.gradle file. You might need to update it manually. \
    Please refer to plugin installation section for Android at \
    https://github.com/microsoft/react-native-code-push/blob/master/docs/setup-android.md#plugin-installation-android---manual`);
    }

    // 2. Add the codepush.gradle build task definitions
    var buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
    var reactGradleLink = buildGradleContents.match(/\napply from: ["'].*?react\.gradle["']/)[0];
    var codePushGradleLink = linkTools.codePushGradleLink;
    if (~buildGradleContents.indexOf(codePushGradleLink)) {
        console.log(`"codepush.gradle" is already linked in the build definition`);
    } else {
        buildGradleContents = buildGradleContents.replace(reactGradleLink,
            `${reactGradleLink}${codePushGradleLink}`);
        fs.writeFileSync(buildGradlePath, buildGradleContents);
    }

    //3. Add deployment key
    var stringsResourcesPath = linkTools.getStringsResourcesPath();
    if (!stringsResourcesPath) {
        return Promise.reject(new Error(`Couldn't find strings.xml. You might need to update it manually.`));
    } else {
        var stringsResourcesContent = fs.readFileSync(stringsResourcesPath, "utf8");
        var deploymentKeyName = linkTools.deploymentKeyName;
        if (~stringsResourcesContent.indexOf(deploymentKeyName)) {
            console.log(`${deploymentKeyName} already specified in the strings.xml`);
        } else {
            return inquirer.prompt({
                "type": "input",
                "name": "androidDeploymentKey",
                "message": "What is your CodePush deployment key for Android (hit <ENTER> to ignore)"
            }).then(function(answer) {
                var insertAfterString = "<resources>";
                var deploymentKeyString = `\t<string moduleConfig="true" name="${deploymentKeyName}">${answer.androidDeploymentKey || "deployment-key-here"}</string>`;
                stringsResourcesContent = stringsResourcesContent.replace(insertAfterString,`${insertAfterString}\n${deploymentKeyString}`);
                fs.writeFileSync(stringsResourcesPath, stringsResourcesContent);
                return Promise.resolve();
            });
        }
    }

    return Promise.resolve();
}
