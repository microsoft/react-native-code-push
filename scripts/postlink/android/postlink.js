var fs = require("fs");
var glob = require("glob");
var path = require("path");

var ignoreNodeModules = { ignore: "node_modules/**" };
var mainApplicationPath = glob.sync("**/MainApplication.java", ignoreNodeModules)[0];
var mainActivityPath = glob.sync("**/MainActivity.java", ignoreNodeModules)[0];
var buildGradlePath = path.join("android", "app", "build.gradle");

// 1. Add the getJSBundleFile override
var getJSBundleFileOverride = `
    @Override
    protected String getJSBundleFile() {
      return CodePush.getJSBundleFile();
    }
`;

function isAlreadyOverridden(codeContents) {
    return /@Override\s*\n\s*protected String getJSBundleFile\(\)\s*\{[\s\S]*?\}/.test(codeContents);
}

if (mainApplicationPath) {
    var mainApplicationContents = fs.readFileSync(mainApplicationPath, "utf8");
    if (isAlreadyOverridden(mainApplicationContents)) {
        console.log(`"getJSBundleFile" is already overridden`);
    } else {
        var reactNativeHostInstantiation = "new ReactNativeHost(this) {";
        mainApplicationContents = mainApplicationContents.replace(reactNativeHostInstantiation,
            `${reactNativeHostInstantiation}\n${getJSBundleFileOverride}`);
        fs.writeFileSync(mainApplicationPath, mainApplicationContents);
    }
} else {
    var mainActivityContents = fs.readFileSync(mainActivityPath, "utf8");
    if (isAlreadyOverridden(mainActivityContents)) {
        console.log(`"getJSBundleFile" is already overridden`);
    } else {
        var mainActivityClassDeclaration = "public class MainActivity extends ReactActivity {";
        mainActivityContents = mainActivityContents.replace(mainActivityClassDeclaration,
            `${mainActivityClassDeclaration}\n${getJSBundleFileOverride}`);
        fs.writeFileSync(mainActivityPath, mainActivityContents);
    }
}

// 2. Add the codepush.gradle build task definitions
var buildGradleContents = fs.readFileSync(buildGradlePath, "utf8");
var reactGradleLink = buildGradleContents.match(/\napply from: ".*?react\.gradle"/)[0];
var codePushGradleLink = `apply from: "../../node_modules/react-native-code-push/android/codepush.gradle"`;
if (~buildGradleContents.indexOf(codePushGradleLink)) {
    console.log(`"codepush.gradle" is already linked in the build definition`);
} else {
    buildGradleContents = buildGradleContents.replace(reactGradleLink,
        `${reactGradleLink}\n${codePushGradleLink}`);
    fs.writeFileSync(buildGradlePath, buildGradleContents);
}