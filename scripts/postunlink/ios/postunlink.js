var fs = require("fs");
var glob = require("glob");
var path = require("path");
var plist = require("plist");
var xcode = require("xcode");
var semver = require('semver');

var packageFile = require('../../../../../package.json');

module.exports = () => {

    console.log("Running ios postunlink script");

    var ignoreNodeModules = { ignore: "node_modules/**" };
    var ignoreNodeModulesAndPods = { ignore: ["node_modules/**", "ios/Pods/**"] };
    var appDelegatePaths = glob.sync("**/AppDelegate.+(mm|m)", ignoreNodeModules);

    // Fix for https://github.com/Microsoft/react-native-code-push/issues/477
    // Typical location of AppDelegate.m for newer RN versions: $PROJECT_ROOT/ios/<project_name>/AppDelegate.m
    // Let's try to find that path by filtering the whole array for any path containing <project_name>
    // If we can't find it there, play dumb and pray it is the first path we find.
    var appDelegatePath = findFileByAppName(appDelegatePaths, packageFile ? packageFile.name : null) || appDelegatePaths[0];

    if (!appDelegatePath) {
        console.log(`Couldn't find AppDelegate. You might need to update it manually \
    Please refer to plugin configuration section for iOS at \
    https://github.com/microsoft/react-native-code-push#plugin-configuration-ios`);
    } else {
        var appDelegateContents = fs.readFileSync(appDelegatePath, "utf8");

        // 1. Remove the header import statement
        var codePushHeaderImportStatement = `#import <CodePush/CodePush.h>`;
        if (!~appDelegateContents.indexOf(codePushHeaderImportStatement)) {
            console.log(`"CodePush.h" header already removed.`);
        } else {
            appDelegateContents = appDelegateContents.replace(`\n${codePushHeaderImportStatement}`, "");
        }

        // 2. Modify jsCodeLocation value assignment
        var codePushBundleUrl = "[CodePush bundleURL]";
        if (!~appDelegateContents.indexOf(codePushBundleUrl)) {
            console.log(`"jsCodeLocation" already not pointing to "[CodePush bundleURL]".`);
        } else {
            var reactNativeVersion = packageFile && packageFile.dependencies && packageFile.dependencies["react-native"];
            if (!reactNativeVersion) {
                console.log(`Can't take react-native version from package.json`);
            } else if (semver.gte(semver.coerce(reactNativeVersion), "0.59.0")) {
                var oldBundleUrl = "[[NSBundle mainBundle] URLForResource:@\"main\" withExtension:@\"jsbundle\"]";
                appDelegateContents = appDelegateContents.replace(codePushBundleUrl, oldBundleUrl);
                fs.writeFileSync(appDelegatePath, appDelegateContents);
            } else {
                var linkedJsCodeLocationAssignmentStatement = "jsCodeLocation = [CodePush bundleURL];";
                var jsCodeLocations = appDelegateContents.match(/(jsCodeLocation = .*)/g);
                if (!jsCodeLocations || jsCodeLocations.length !== 2 || !~appDelegateContents.indexOf(linkedJsCodeLocationAssignmentStatement)) {
                    console.log(`AppDelegate isn't compatible for unlinking`);
                } else {
                    if (semver.eq(semver.coerce(reactNativeVersion), "0.57.8")) {
                        // If version of react-native application is 0.57.8 then on default there are two different
                        // jsCodeLocation for debug and release and we should replace only release
                        var unlinkedJsCodeLocations = `jsCodeLocation = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];`;
                        appDelegateContents = appDelegateContents.replace(linkedJsCodeLocationAssignmentStatement,
                            unlinkedJsCodeLocations);
                    } else {
                        // If version of react-native application is not 0.57.8 and lower than 0.59.0 then on default there are only one
                        // jsCodeLocation and we should stay on only it
                        var defaultJsCodeLocationAssignmentStatement = jsCodeLocations[0];
                        var linkedCodeLocationPatch = `\n#ifdef DEBUG\n\t${defaultJsCodeLocationAssignmentStatement}\n#else\n\t${linkedJsCodeLocationAssignmentStatement}\n#endif`;
                        appDelegateContents = appDelegateContents.replace(linkedCodeLocationPatch,
                            defaultJsCodeLocationAssignmentStatement);
                    }
                    fs.writeFileSync(appDelegatePath, appDelegateContents);
                }
            }
        }
    }

    var plistPath = getPlistPath();

    if (!plistPath) {
        return Promise.reject(`Couldn't find .plist file. You might need to update it manually \
    Please refer to plugin configuration section for iOS at \
    https://github.com/microsoft/react-native-code-push#plugin-configuration-ios`);
    }

    var plistContents = fs.readFileSync(plistPath, "utf8");

    // 3. Remove CodePushDeploymentKey from plist file
    var parsedInfoPlist = plist.parse(plistContents);
    if (!parsedInfoPlist.CodePushDeploymentKey) {
        console.log(`"CodePushDeploymentKey" already removed from the plist file.`);
    } else {
        delete parsedInfoPlist.CodePushDeploymentKey;
        plistContents = plist.build(parsedInfoPlist);
        fs.writeFileSync(plistPath, plistContents);
    }

    return Promise.resolve();

    // Helper that filters an array with AppDelegate.m paths for a path with the app name inside it
    // Should cover nearly all cases
    function findFileByAppName(array, appName) {
        if (array.length === 0 || !appName) return null;

        for (var i = 0; i < array.length; i++) {
            var path = array[i];
            if (path && path.indexOf(appName) !== -1) {
                return path;
            }
        }

        return null;
    }

    function getDefaultPlistPath() {
        //this is old logic in case we are unable to find PLIST from xcode/pbxproj - at least we can fallback to default solution
        return glob.sync(`**/${packageFile.name}/*Info.plist`, ignoreNodeModules)[0];
    }

    // This is enhanced version of standard implementation of xcode 'getBuildProperty' function
    // but allows us to narrow results by PRODUCT_NAME property also.
    // So we suppose that proj name should be the same as package name, otherwise fallback to default plist path searching logic
    function getBuildSettingsPropertyMatchingTargetProductName(parsedXCodeProj, prop, targetProductName, build){
        var target;
        var COMMENT_KEY = /_comment$/;
        var PRODUCT_NAME_PROJECT_KEY = 'PRODUCT_NAME';
        var TV_OS_DEPLOYMENT_TARGET_PROPERTY_NAME = 'TVOS_DEPLOYMENT_TARGET';
        var TEST_HOST_PROPERTY_NAME = 'TEST_HOST';

        var configs = parsedXCodeProj.pbxXCBuildConfigurationSection();
        for (var configName in configs) {
            if (!COMMENT_KEY.test(configName)) {
                var config = configs[configName];
                if ( (build && config.name === build) || (build === undefined) ) {
                    if (targetProductName) {
                        if (config.buildSettings[prop] !== undefined && config.buildSettings[PRODUCT_NAME_PROJECT_KEY] == targetProductName) {
                            target = config.buildSettings[prop];
                        }
                    } else {
                        if (config.buildSettings[prop] !== undefined  &&
                        //exclude tvOS projects
                        config.buildSettings[TV_OS_DEPLOYMENT_TARGET_PROPERTY_NAME] == undefined &&
                        //exclude test app
                        config.buildSettings[TEST_HOST_PROPERTY_NAME] == undefined) {
                            target = config.buildSettings[prop];
                        }
                    }
                }
            }
        }
        return target;
    }

    function getPlistPath(){
        var xcodeProjectPaths = glob.sync(`**/*.xcodeproj/project.pbxproj`, ignoreNodeModulesAndPods);
        if (!xcodeProjectPaths){
            return getDefaultPlistPath();
        }

        if (xcodeProjectPaths.length !== 1) {
            console.log('Could not determine correct xcode proj path to retrieve related plist file, there are multiple xcodeproj under the solution.');
            return getDefaultPlistPath();
        }

        var xcodeProjectPath = xcodeProjectPaths[0];
        var parsedXCodeProj;

        try {
            var proj = xcode.project(xcodeProjectPath);
            //use sync version because there are some problems with async version of xcode lib as of current version
            parsedXCodeProj = proj.parseSync();
        }
        catch(e) {
            console.log('Couldn\'t read info.plist path from xcode project - error: ' + e.message);
            return getDefaultPlistPath();
        }

        var INFO_PLIST_PROJECT_KEY = 'INFOPLIST_FILE';
        var RELEASE_BUILD_PROPERTY_NAME = "Release";
        var targetProductName = packageFile ? packageFile.name : null;

        //Try to get 'Release' build of ProductName matching the package name first and if it doesn't exist then try to get any other if existing
        var plistPathValue = getBuildSettingsPropertyMatchingTargetProductName(parsedXCodeProj, INFO_PLIST_PROJECT_KEY, targetProductName, RELEASE_BUILD_PROPERTY_NAME) ||
            getBuildSettingsPropertyMatchingTargetProductName(parsedXCodeProj, INFO_PLIST_PROJECT_KEY, targetProductName) ||
            getBuildSettingsPropertyMatchingTargetProductName(parsedXCodeProj, INFO_PLIST_PROJECT_KEY, null, RELEASE_BUILD_PROPERTY_NAME) ||
            getBuildSettingsPropertyMatchingTargetProductName(parsedXCodeProj, INFO_PLIST_PROJECT_KEY) ||
            parsedXCodeProj.getBuildProperty(INFO_PLIST_PROJECT_KEY, RELEASE_BUILD_PROPERTY_NAME) ||
            parsedXCodeProj.getBuildProperty(INFO_PLIST_PROJECT_KEY);

        if (!plistPathValue){
            return getDefaultPlistPath();
        }

        //also remove surrounding quotes from plistPathValue to get correct path resolved
        //(see https://github.com/Microsoft/react-native-code-push/issues/534#issuecomment-302069326 for details)
        return path.resolve(path.dirname(xcodeProjectPath), '..', plistPathValue.replace(/^"(.*)"$/, '$1'));
    }
}
