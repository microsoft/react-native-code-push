var fs = require("fs");
var glob = require("glob");
var path = require("path");

module.exports = () => {

    console.log("Running android postunlink script");

    var ignoreFolders = { ignore: ["node_modules/**", "**/build/**"] };
    var manifestPath = glob.sync("**/AndroidManifest.xml", ignoreFolders)[0];

    function findMainApplication() {
        if (!manifestPath) {
            return null;
        }

        var manifest = fs.readFileSync(manifestPath, "utf8");

        // Android manifest must include single 'application' element
        var matchResult = manifest.match(/application\s+android:name\s*=\s*"(.*?)"/);
        if (matchResult) {
            var appName = matchResult[1];
        } else {
            return null;
        }

        var nameParts = appName.split('.');
        var searchPath = glob.sync("**/" + nameParts[nameParts.length - 1] + ".java", ignoreFolders)[0];
        return searchPath;
    }

    var mainApplicationPath = findMainApplication() || glob.sync("**/MainApplication.java", ignoreFolders)[0];

    // 1. Remove the getJSBundleFile override
    var getJSBundleFileOverride = "\n\n\t\t@Override\n\t\tprotected String getJSBundleFile()" +
    "{\n\t\t\treturn CodePush.getJSBundleFile();\n\t\t}\n";

    function isAlreadyRemoved(codeContents) {
        return !/@Override\s*\n\s*protected String getJSBundleFile\(\)\s*\{[\s\S]*?\}/.test(codeContents);
    }

    if (mainApplicationPath) {
        var mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
        if (isAlreadyRemoved(mainApplicationContents)) {
            console.log(`"getJSBundleFile" is already removed`);
        } else {
            mainApplicationContents = mainApplicationContents.replace(`${getJSBundleFileOverride}`, "");
            fs.writeFileSync(mainApplicationPath, mainApplicationContents);
        }
    } else {
        var mainActivityPath = glob.sync("**/MainActivity.java", ignoreFolders)[0];
        if (mainActivityPath) {
            var mainActivityContents = fs.readFileSync(mainActivityPath, "utf8");
            if (isAlreadyRemoved(mainActivityContents)) {
                console.log(`"getJSBundleFile" is already removed`);
            } else {
                mainActivityContents = mainActivityContents.replace(getJSBundleFileOverride, "");
                fs.writeFileSync(mainActivityPath, mainActivityContents);
            }
        } else {
            console.log(`Couldn't find Android application entry point. You might need to update it manually. \
    Please refer to plugin configuration section for Android at \
    https://github.com/microsoft/react-native-code-push#plugin-configuration-android for more details`);
        }
    }

    // 2. Remove the codepush.gradle build task definitions
    var buildGradlePath = path.join("android", "app", "build.gradle");

    if (!fs.existsSync(buildGradlePath)) {
        console.log(`Couldn't find build.gradle file. You might need to update it manually. \
    Please refer to plugin installation section for Android at \
    https://github.com/microsoft/react-native-code-push#plugin-installation-android---manual`);
    } else {
        var buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
        var codePushGradleLink = `\napply from: "../../node_modules/react-native-code-push/android/codepush.gradle"`;
        if (!~buildGradleContents.indexOf(codePushGradleLink)) {
            console.log(`"codepush.gradle" is already unlinked in the build definition`);
        } else {
            buildGradleContents = buildGradleContents.replace(`${codePushGradleLink}`,"");
            fs.writeFileSync(buildGradlePath, buildGradleContents);
        }
    }

    //3. Remove deployment key
    var stringsResourcesPath = glob.sync("**/strings.xml", ignoreFolders)[0];
    if (!stringsResourcesPath) {
        return Promise.reject(new Error("Couldn't find strings.xml. You might need to update it manually."));
    } else {
        var stringsResourcesContent = fs.readFileSync(stringsResourcesPath, "utf8");
        var deploymentKeyName = "reactNativeCodePush_androidDeploymentKey";
        if (!~stringsResourcesContent.indexOf(deploymentKeyName)) {
            console.log(`${deploymentKeyName} already removed from the strings.xml`);
        } else {
            var AndroidDeploymentKey = stringsResourcesContent.match(/(<string moduleConfig="true" name="reactNativeCodePush_androidDeploymentKey">.*<\/string>)/);
            if (AndroidDeploymentKey) {
                stringsResourcesContent = stringsResourcesContent.replace(`\n\t${AndroidDeploymentKey[0]}`,"");
                fs.writeFileSync(stringsResourcesPath, stringsResourcesContent);
            }
        };
    }
    return Promise.resolve();
}
